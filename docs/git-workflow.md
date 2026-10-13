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

## 이슈 작성

PR을 올리기 전에 대응하는 이슈를 먼저 만든다. 이슈 없이 올리는 PR은 아래 예외뿐이다.

- 문서만 바꾸는 PR(ADR, `docs/`, `PROGRESS.md` 등): 이슈 템플릿에 문서용이 없으므로 이슈 없이 올리고 "연관된 이슈"는 `없음`으로 쓴다.
- 이미 올라간 PR: 소급해서 이슈를 만들지 않는다.

### 이슈 형식

- `.github/ISSUE_TEMPLATE`의 템플릿(Feature, Bug, Refactor) 중 변경 성격에 맞는 것을 쓰고, 템플릿의 헤딩을 그대로 유지한다.
- 제목은 `[FEATURE] `, `[BUG] `, `[REFACTOR] ` 접두사 뒤에 명사형으로 쓴다.
- Assignee는 `jinwoojwa`로 지정하고, 라벨은 PR과 같은 것을 붙인다(feature → `🛠️ feature`, bug → `🐞 bug`, refactor → `🌱 refactor`).
- "작업 상세 내용"의 체크박스는 PR에 포함된 항목만 `- [x]`로 체크한다. 배포 등록처럼 코드 밖에서 해야 하는 후속 작업은 체크하지 않고 남긴다.

```bash
gh issue create --assignee jinwoojwa --label "🛠️ feature" \
  --title "[FEATURE] 제목" --body-file 이슈본문.md
```

### PR과 연결

- PR 본문의 "연관된 이슈"에 `- Closes #이슈번호`를 쓴다. 머지되면 이슈가 닫힌다.
- PR의 Assignee도 `jinwoojwa`로 지정하고 이슈와 같은 라벨을 붙인다(`gh pr create --assignee jinwoojwa --label "🛠️ feature"`).

## PR 작성 시 확인

- PR 본문에도 `Claude-Session` 트레일러/링크를 포함하지 않는다.
- PR을 열기 전 관련 커밋들이 위 Commit Rules를 따르는지 확인한다.
- PR을 열기 전, 연결된 이슈의 "작업 상세 내용" 체크박스 중 이번 PR에서 완료한 항목을
  `gh issue edit`로 `- [x]`로 체크해둔다. 이슈를 닫는다고 체크박스가 자동으로 갱신되지 않으므로
  직접 반영해야 한다.
