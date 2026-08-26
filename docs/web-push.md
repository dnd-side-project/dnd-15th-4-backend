# WebPush 연동·운영 계약

이 문서는 백엔드 구현과 프런트엔드 Service Worker, 운영 환경 사이의 WebPush 계약을 정리한다.
이번 범위의 알림은 `FRIEND_ARRIVAL`, `QUICK_MESSAGE`, `DEPARTURE_REMINDER` 세 종류다.
약속 시작 알림은 자정 상태 전이와 실제 약속 시각이 일치하지 않아 포함하지 않는다.

## 전달 보장

- 도착·퀵메시지 트랜잭션이 커밋된 뒤 `notificationExecutor`에서 비동기로 발송한다.
- 출발 준비 알림은 DB 조건부 갱신에 성공한 실행자만 한 번 발송을 시도한다.
- 발송, 비동기 queue 포화, 프로세스 종료 후 재시도하지 않는 best-effort 방식이다.
- 한 구독의 발송 실패는 다른 구독의 발송을 중단시키지 않는다.
- Push Service가 `404` 또는 `410`을 반환한 구독은 발송 당시 키가 그대로일 때만 삭제한다.
- `FRIEND_ARRIVAL`은 참여방의 `friendArrival`, `QUICK_MESSAGE`는 `chatBubble` 설정으로
  선별한다. `locationPermission`은 WebPush 카테고리 토글이 아니며 출발 준비 알림에는 별도 토글이 없다.

## 인증 API

세 API 모두 Bearer access token이 필요하다.

| Method | Path | 요청·응답 |
|---|---|---|
| `GET` | `/api/v1/notifications/vapid-public-key` | `{ "data": { "publicKey": "..." } }` |
| `POST` | `/api/v1/notifications/push-subscriptions` | `{ "endpoint": "...", "keys": { "p256dh": "...", "auth": "..." } }` |
| `DELETE` | `/api/v1/notifications/push-subscriptions` | `{ "endpoint": "..." }` |

`POST`는 신규 등록과 키 갱신을 모두 `200`으로 처리한다. 같은 endpoint를 다른 계정에서 등록하면
현재 인증 사용자에게 소유권이 이전된다. `DELETE`는 사용자와 endpoint 범위에서 멱등이며 존재 여부를
응답으로 노출하지 않는다.

서버는 HTTPS, 기본 443 port, userInfo·fragment 없음과 키 형식을 검증한다. endpoint host는 다음
Push Service와 그 하위 도메인만 허용한다.

- `fcm.googleapis.com`
- `push.services.mozilla.com`
- `push.apple.com`
- `notify.windows.com`

## 푸시 payload

```json
{
  "type": "FRIEND_ARRIVAL",
  "title": "PuzzleMeet",
  "body": "친구가 약속 장소에 도착했어요.",
  "meetingId": 1
}
```

payload에는 FE 경로, 퀵메시지 원문, 사용자·참여자 엔티티 정보를 넣지 않는다. 알림 클릭 후 필요한
상세 정보는 인증된 약속 조회 API에서 가져온다.

## 프런트엔드 동기화

로그인된 foreground에서 다음 순서로 동기화한다.

1. Service Worker 준비를 기다린다.
2. 인증된 VAPID 공개키 API를 호출한다.
3. `pushManager.getSubscription()`으로 기존 구독을 조회한다.
4. 기존 구독의 `applicationServerKey`와 현재 공개키를 byte 단위로 비교한다.
5. 키가 다르면 기존 구독을 해지하고 현재 키로 다시 구독한다.
6. 현재 구독의 endpoint와 keys를 서버 `POST`로 upsert한다.

`pushsubscriptionchange`에서는 access token을 가정해 인증 API를 직접 호출하지 않는다. 변경된 구독은
다음 로그인 foreground에서 다시 등록한다. 로그아웃·계정 전환·명시적인 “이 기기 알림 끄기”에서만
서버 `DELETE`와 브라우저 `unsubscribe()`를 함께 수행한다. 위치·친구 도착·말풍선 개별 설정 변경은
브라우저 구독을 해지하지 않는다.

Service Worker는 `push` 이벤트 안에서 `event.waitUntil()`로
`registration.showNotification()` 완료를 기다려야 한다. macOS Safari와 iOS/iPadOS 16.4 이상을
지원하려면 manifest, `display: standalone`, 홈 화면 추가 안내, 사용자 제스처 안에서의 권한 요청이
필요하다. [Apple Web Push 문서](https://developer.apple.com/documentation/usernotifications/sending-web-push-notifications-in-web-apps-and-browsers)

## 운영 환경과 키 회전

EC2의 `puzzlemeet.env`에 다음 값을 먼저 추가한 뒤 앱을 배포한다. private key는 FE 번들, Git, 로그에
노출하지 않는다.

```text
WEBPUSH_VAPID_PUBLIC_KEY=<base64url P-256 public key>
WEBPUSH_VAPID_PRIVATE_KEY=<base64url P-256 private key>
WEBPUSH_VAPID_SUBJECT=mailto:<운영 연락처>
```

VAPID 키는 평상시 고정한다. 유출 시 다음 순서로 회전한다.

1. `push_subscriptions`의 기존 구독을 모두 삭제한다.
2. EC2 `puzzlemeet.env`의 public/private key를 한 쌍으로 교체한다.
3. `docker compose up -d --force-recreate app`으로 앱 컨테이너를 다시 만든다.
4. health 확인 후 로그인 foreground에서 새 키로 구독을 다시 등록한다.

## 배포 후 수동 확인

자동 테스트는 실제 Push Service를 호출하지 않는다. HTTPS 배포 환경에서 아래 조합을 각각 확인한다.

- Chrome, Edge, Firefox
- macOS Safari
- iOS/iPadOS 16.4 이상 홈 화면 PWA

각 브라우저에서 권한 허용, foreground 구독 등록, background 알림 표시, 로그아웃 시 기기 구독 해지,
VAPID 공개키 변경 뒤 foreground 재구독을 확인한다.
