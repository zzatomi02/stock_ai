# Git / GitHub — 이 저장소만 zzatomi02

저장소: https://github.com/zzatomi02/stock_ai

## 계정 분리

| 범위 | GitHub 계정 |
|------|-------------|
| **`stock` 폴더 (이 repo)** | `zzatomi02` |
| 다른 프로젝트 | `Noono0` (기존 전역 설정 유지) |

이 폴더에는 **로컬 Git 설정**만 적용되어 있습니다 (`git config --local`).

## 원격 (HTTPS 권장)

```text
origin  https://github.com/zzatomi02/stock_ai.git
```

push / pull:

```powershell
cd "C:\dev\2026 new prjt\stock"
git pull
git push
```

브라우저 또는 Git Credential Manager로 `zzatomi02` 로그인.

## SSH (선택)

SSH 키·`~/.ssh/config` 를 설정한 뒤에만:

```text
git@github-zzatomi02:zzatomi02/stock_ai.git
```

키 파일이 없으면 SSH remote 로 push 하지 마세요. HTTPS가 이미 동작합니다.

## 커밋 시 주의

- `.next/`, `node_modules/`, `.pnpm-store/`, `target/`, `*.egg-info/` → **커밋 금지** (루트 `.gitignore` 참고)
- `.env`, API 키 파일 → **커밋 금지**

## 문서 위치

사용 설명서는 **백엔드가 아니라** `stock/docs/` 에 있습니다.
