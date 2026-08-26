# 약속 상세 조회 API

`GET /api/v1/meetings/{meetingId}`

전체 약속 목록에서 id로 걸러서 상세 화면에 넘기던 것을 대체한다. 인증 토큰과 `meetingId`만으로 조회하고, 해당 약속 참여자(방장 포함)만 호출할 수 있다.

## 요청

경로 변수 `meetingId` 외에 파라미터 없음. `Authorization: Bearer {accessToken}` 헤더 필요.

## 응답

```json
{
  "code": "API_SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "meetingId": 10,
    "title": "한강 피크닉",
    "dateTime": "2026-08-10T14:00:00",
    "place": "서울 여의도 한강공원",
    "latitude": 37.5283,
    "longitude": 126.9320,
    "memo": "돗자리 챙기기",
    "status": "WAITING",
    "inviteCode": "ABCD1234",
    "participants": [
      {
        "id": 1,
        "name": "효창",
        "profileImageUrl": "https://.../profiles/1.png",
        "puzzleImageUrl": "https://.../puzzles/1.png",
        "defaultNicknameUsed": false,
        "defaultImageUsed": false
      }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `inviteCode` | string | 이 약속의 초대 코드. 참여자 전체에게 내려간다 (방장만 조회 가능한 `/invite-code`와 다름) |
| `participants` | array | 참여자 목록(본인 포함). 요청한 사용자 본인 항목도 이 배열 안에서만 내려가고, FE는 본인 `id`로 찾아서 쓴다 |
| `participants[].puzzleImageUrl` | string | 해당 참여자가 등록한 퍼즐 이미지 URL |
| `participants[].defaultNicknameUsed` | boolean | 해당 참여자가 서비스 기본 닉네임을 쓰는지 여부. 참여 시 닉네임을 직접 설정했으면 `false` |
| `participants[].defaultImageUsed` | boolean | 해당 참여자가 서비스 기본 이미지로 참여했는지 여부. 직접 이미지를 설정했으면 `false` |

## 에러

| HTTP 상태 | code | message |
|---|---|---|
| 401 | `AUTH_TOKEN_INVALID` | 유효하지 않은 인증 토큰입니다. |
| 404 | `MEETING_NOT_FOUND` | 존재하지 않는 약속입니다. |
| 403 | `AUTH_FORBIDDEN` | 접근 권한이 없습니다. (해당 약속 참여자가 아닌 경우) |
