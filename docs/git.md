# Git

## 브랜치

- GitHub Flow를 쓴다. 주 브랜치는 `develop` 하나다
- 작업은 `<타입>/<기능요약>` 브랜치에서 하고, PR로만 `develop`에 머지한다.
  `develop`에 직접 커밋하지 않는다
- 타입은 커밋 타입과 같은 목록에서 고르고, 기능요약은 kebab-case로 쓴다.
  예: `feat/health-check-api`, `docs/ai-convention`, `ci/github-actions`

## 커밋

- 메시지는 `<타입>: <한국어 요약>`으로 쓴다. 예: `feat: 헬스체크 API 추가`
- 타입은 `feat` `fix` `refactor` `docs` `test` `chore` `ci` 중에서 고른다
- 커밋 전에 `./gradlew spotlessApply`를 직접 실행한다 (CLAUDE.md '커밋 전')

## PR

- 베이스 브랜치는 `develop`이다
- 머지 권한은 @Sojeong0430, @Hyochang098 두 명에게 있다.
  나머지는 리뷰까지만 하고 머지 버튼을 누르지 않는다
- **`develop`에 직접 푸시하거나 force-push하지 않는다.** 사람이 승인해도 하지 않는다
- `git push`·`gh pr create`·`gh pr ready`·`gh pr merge`는 실행 전에 매번 사람에게 확인받는다.
  한 번 확인받은 것이 다음 푸시까지 이어지지 않는다 (CLAUDE.md 4번 규칙)

## pre-commit hook

- `./gradlew installGitHooks`를 1회 실행하면 커밋할 때 `spotlessCheck`가 돈다
- 검사만 하고 자동 수정은 하지 않는다. 실패하면 `./gradlew spotlessApply` 후 다시 커밋한다
