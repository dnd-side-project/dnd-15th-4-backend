# Git

## 브랜치

- GitHub Flow를 쓴다. 주 브랜치는 `develop` 하나다
- 작업은 `feature/<기능요약>` 브랜치에서 하고, PR로만 `develop`에 머지한다.
  `develop`에 직접 커밋하지 않는다
- 기능요약은 snake_case로 쓴다. 예: `feature/login`, `feature/sign_in`

## 커밋

- 메시지는 `<타입>: <한국어 요약>`으로 쓴다. 예: `feat: 헬스체크 API 추가`
- 타입은 `feat` `fix` `refactor` `docs` `test` `chore` 중에서 고른다
- 커밋 전에 `./gradlew spotlessApply`를 직접 실행한다 (CLAUDE.md '커밋 전')

## PR

- 베이스 브랜치는 `develop`이다

## pre-commit hook

- `./gradlew installGitHooks`를 1회 실행하면 커밋할 때 `spotlessCheck`가 돈다
- 검사만 하고 자동 수정은 하지 않는다. 실패하면 `./gradlew spotlessApply` 후 다시 커밋한다
