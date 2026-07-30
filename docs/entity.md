# 엔티티 규약

필드 구성의 기준은 ERD다. ERD가 작업 컨텍스트에 없으면 필드를 추측해서 만들지 말고,
멈춰서 사용자에게 요청한다.
다만 이 문서에 적힌 ERD 결론(FK 구성, BaseTimeEntity를 상속하는 3개 테이블)은
ERD 없이 그대로 따른다.

## 클래스 선언

| 반드시 붙인다 | 붙이지 않는다 |
|---|---|
| `@Entity` | **클래스 레벨 `@Builder`** |
| `@Getter` | `@Setter` `@Data` `@ToString` `@EqualsAndHashCode` `@NonNull` |
| `@NoArgsConstructor(access = AccessLevel.PROTECTED)` | |
| `@Table(name = "<테이블명>")` — snake_case 복수 | |

오른쪽 열의 lombok 애노테이션은 `lombok.config`가 컴파일 에러로 막는다.

클래스 레벨 `@Builder`는 막혀 있지 않지만 쓰지 않는다.

**왜**

`id`까지 빌더에 노출되어 `save()`가 insert 대신 merge로 동작한다.
그리고 all-args 생성자가 생겨서 생성자에서 정한 기본값이 전부 무시된다.

## 필드

### 기본값이 틀려서 반드시 명시해야 하는 것

| 대상 | 반드시 쓴다 | JPA 기본값 | 빼먹으면 |
|---|---|---|---|
| `@ManyToOne` 연관 | `fetch = FetchType.LAZY` | `EAGER` | 조회할 때마다 연관 엔티티까지 끌고 온다 |
| enum 컬럼 | `@Enumerated(EnumType.STRING)` | `ORDINAL` | enum이 숫자로 저장된다. 상수 순서를 바꾸면 기존 데이터의 의미가 바뀐다 |

**둘 다 컴파일되고 테스트도 통과한다. DB에만 틀린 값이 쌓인다.**

### 나머지 필드 규칙

- PK는 `Long id`, `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- NOT NULL 컬럼에는 `@Column(nullable = false)`

## 연관관계

- **1:N 관계는 N쪽에만 `@ManyToOne`을 둔다.** 1쪽에는 N을 가리키는 필드를 만들지 않는다
- 따라서 **`@OneToMany`를 쓰지 않는다.** 양방향 연관도 열지 않는다
- 연관 필드에는 `@JoinColumn(name = "<참조테이블>_id")`. 필수 연관이면 `optional = false`도 함께 쓴다
- `fetch = FetchType.LAZY`를 반드시 쓴다 (위 기본값 표)
- 1쪽에서 N을 찾아야 하면 리포지토리 메서드로 한다. `findByPuzzlePageId` 같은 이름을 쓴다

**단방향만 쓰는 이유**

ERD상 모든 자식 테이블이 이미 FK를 갖고 있어서 양방향은 추가 작업일 뿐이다.
그리고 양방향을 열면 N+1이 발생해도 쿼리 로그를 보기 전까지 드러나지 않는다.

## 조립과 상태 변경

- **필수값은 생성자 파라미터로 받는다.**
  하나 빠지면 컴파일 에러가 나는 것이 필수값을 강제하는 유일한 수단이다
- **생성자에 `@Builder`를 붙이지 않는다.**
  붙이는 순간 그 필수 파라미터가 옵셔널한 빌더 세터로 바뀌어서, 값을 안 넣어도 `.build()`가
  그냥 컴파일된다. 유일한 예외는 필수 파라미터가 8개를 넘는 경우다
- 파생 기본값(생성 시점의 초기 상태 등)은 **생성자 안에서** 정한다. 호출자가 넘기게 하지 않는다
- 상태 변경은 setter가 아니라 **의도가 드러나는 메서드**로 한다.
  `markArrived()`, `updateEta(...)`, `revealPiece()` 같은 이름을 쓴다.
  메서드 하나가 함께 바뀌어야 하는 필드를 전부 책임진다.
  예를 들어 소요시간을 갱신하면 계산 시각도 같은 메서드에서 갱신한다

## BaseTimeEntity

- `@MappedSuperclass` + `@EntityListeners(AuditingEntityListener.class)`를 붙인
  공통 상위 클래스로 만든다
- `@EnableJpaAuditing`은 `global/config/JpaAuditingConfig` **한 곳에만** 둔다
- **첫 엔티티를 만드는 PR에서 함께 만든다**
- 상속 대상은 ERD상 `created_at`과 `updated_at`을 **둘 다** 가진
  `users` / `meeting` / `meeting_member` **3개뿐**이다.
  `created_at`만 있는 `member_image` / `puzzle_page`는 상속하지 않는다

**왜 첫 PR에서 함께 만드나**

나중에 넣으면 그 전에 저장된 행의 `created_at`이 이미 비어 있고, 마이그레이션으로 복구할 수 없다.
