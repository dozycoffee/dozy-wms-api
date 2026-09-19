# Git / PR 워크플로우 규칙

이 문서는 사람 기여자를 위한 안내가 아니라, **이 레포에서 커밋을 만들거나 브랜치를 생성하거나 PR을
작성하는 AI(Claude Code 등)가 해당 작업을 수행하기 전에 반드시 따라야 할 규칙**이다. 전역
`~/.claude/CLAUDE.md`에도 유사한 커밋 규칙이 있지만, 이 문서는 이 프로젝트에서만 적용되는 규칙(브랜치
네이밍 등)을 독립적으로 관리하기 위해 별도로 둔다 — 전역 규칙과 어긋나면 이 문서가 이 레포에서는
우선한다.

## Commit Format

```text
<type>(<scope>): <description>
```

### Commit Types

| Type | 용도 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `test` | 테스트 추가 또는 수정 |
| `docs` | 문서 변경 |
| `chore` | 빌드, 설정 등 기타 변경 |
| `perf` | 성능 개선 |

### Commit Rules

- 하나의 커밋에는 하나의 논리적 변경만 포함한다.
- 서로 관련 없는 변경사항을 하나의 커밋으로 묶지 않는다.
- 커밋 메시지만 보고 변경 목적을 명확히 알 수 있도록 작성한다.
- 커밋 제목은 명사형으로 작성한다.
  - 좋은 예: `사용자 인증 기능 추가`
  - 나쁜 예: `사용자 인증 기능을 추가한다`
- 커밋 제목 끝에는 마침표나 기타 구두점을 사용하지 않는다.
- 커밋 본문이 필요한 경우 각 변경사항을 `-` 단위로 작성한다.
- 테스트가 실패한 상태에서는 커밋하지 않는다.
- 커밋, 이슈, PR 등 어디에도 `Claude-Session` 트레일러 또는 링크를 포함하지 않는다.

## Branch Strategy

- `main` → production; 모든 feature/fix 브랜치는 여기로 직접 PR
- `feat/[domain]-[feature]` → feature branches (e.g., `feat/inventory-fefo-allocation-logic`)
- `fix/[description]` → bug fix branches
- `chore/[description]` → 인프라, 빌드, 설정 변경

## PR 작성 시 확인

- PR 본문에도 `Claude-Session` 트레일러/링크를 포함하지 않는다.
- PR을 열기 전 관련 커밋들이 위 Commit Rules를 따르는지 확인한다.
- PR을 열기 전, 연결된 이슈의 "작업 상세 내용" 체크박스 중 이번 PR에서 완료한 항목을
  `gh issue edit`로 `- [x]`로 체크해둔다. 이슈를 닫는다고 체크박스가 자동으로 갱신되지 않으므로
  직접 반영해야 한다.
