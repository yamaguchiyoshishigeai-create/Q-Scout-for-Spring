# Codexプロンプト作成ルール: Git操作スクリプト昇格ゲート方式

## 1. 本ルールの目的

本ルールは、ChatGPTがCodexへ投入する指示プロンプトを作成する際に、Codexが`.git`への書き込みを伴うGit操作を通常実行経路で試行して失敗し、その後に権限付き再実行へ分岐する無駄な処理経路を抑止するための規約である。

ChatGPTは、Codex向けプロンプトを作成する際、本ルールを厳守し、Git書き込み・push・PR作成を通常のCodex実行経路ではなく、明示的に生成した`ps1`または`sh`スクリプト経由の権限付き実行に限定する。

---

## 2. 基本方針

### 2.1 採用方式

採用方式は、**Git操作スクリプト昇格ゲート方式**とする。

Codex本体には、原則として以下を担当させる。

- ファイル編集
- ファイル作成
- テスト実行
- lint実行
- build実行
- 差分確認
- 変更要約
- 検証結果報告

一方で、以下のような`.git`への書き込み、外部送信、GitHub上の副作用を伴う操作は、Codexが通常コマンドとして直接実行してはならない。

- branch作成
- commit
- tag作成
- reset / rebase / merge
- push
- PR作成

これらは、Codexが生成する専用スクリプトに閉じ込めたうえで、そのスクリプトのみを権限付き実行対象とする。

---

## 3. 実行者区分

ChatGPTがCodex向けプロンプトを作成する際は、作業を以下の4区分に整理する。

| 区分 | 担当内容 | 判断基準 |
|---|---|---|
| ユーザー(人) | 最終判断、動作確認、mainへのmerge、外部的・主観的な確認 | AIが代替できない判断または最終承認 |
| ChatGPT(会話内) | Codexへの指示作成、手順整理、レビュー、方針設計 | 会話内で完結する整理・設計作業 |
| ChatGPT(リポジトリ編集) | 小〜中規模ファイルの局所編集、README更新、軽微な設定変更 | 対象ファイルを完全取得でき、差分が局所的な場合 |
| Codex | 実装、複数ファイル修正、テスト実行、build確認、反復修正、PR準備 | リポジトリ上での実作業・検証が必要な場合 |

本ルールの対象は、主にChatGPT(会話内)がCodex向け指示を作成する場面である。

---

## 4. Codexプロンプト作成時の必須制約

ChatGPTは、Codex向けプロンプトの冒頭または実行ポリシー欄に、以下の制約を必ず含める。

```text
【実行ポリシー】
このタスクでは、Git操作をCodexの通常コマンド経路で直接実行しないでください。
.git への書き込みを伴う操作は、必ず専用スクリプトに切り出してから実行してください。

【通常実行で許可する作業】
- ファイル編集
- ファイル作成
- テスト
- lint
- build
- git status
- git diff
- git diff --stat
- git log --oneline -n 5 などの読み取り専用Git操作

【通常実行で禁止する作業】
以下を直接実行してはいけません。
- git branch
- git checkout -b
- git switch -c
- git add
- git commit
- git tag
- git reset
- git rebase
- git merge
- git push
- gh pr create
- .git 配下への書き込みを伴う全操作
- 上記の失敗後に同一コマンドを権限付きで再実行すること

【権限付き実行の許可経路】
Git書き込みまたはPR作成が必要な場合、直接コマンドを試行せず、次のいずれかのスクリプトを生成してください。

Windows:
- codex_start_branch.ps1
- codex_finish_pr.ps1

Linux/macOS/WSL:
- codex_start_branch.sh
- codex_finish_pr.sh

その後、実行するコマンドは次の形式に限定してください。

Windows:
powershell -ExecutionPolicy Bypass -File .\codex_start_branch.ps1
powershell -ExecutionPolicy Bypass -File .\codex_finish_pr.ps1

Linux/macOS/WSL:
chmod +x ./codex_start_branch.sh && ./codex_start_branch.sh
chmod +x ./codex_finish_pr.sh && ./codex_finish_pr.sh

【重要】
Git書き込みが必要だと判断した時点で、通常コマンドとして git branch / git commit / git push を先に試してはいけません。
必ずスクリプト生成を先に行い、そのスクリプト実行のみを権限付き実行リクエストの対象にしてください。
```

---

## 5. ブランチ作成スクリプトの必須要件

ChatGPTがCodex向けプロンプトにブランチ作成処理を含める場合、Codexに次の要件を満たすスクリプトを生成させる。

### 5.1 対象スクリプト

- Windows: `codex_start_branch.ps1`
- Linux/macOS/WSL: `codex_start_branch.sh`

### 5.2 必須処理

ブランチ作成スクリプトには、最低限以下を含める。

1. Gitリポジトリ内であることを確認する。
2. 現在のbranch名を確認する。
3. 作業ツリーがdirtyでないことを確認する。
4. dirtyの場合は処理を中止し、`git status --short`を表示する。
5. `origin`からfetchする。
6. `main`または指定されたbase branchへ切り替える。
7. `git pull --ff-only`で最新化する。
8. 作業用branchを作成する。
9. 作成後のbranch名を表示する。
10. 成功時に`[OK]`で始まる完了メッセージを出す。
11. 失敗時に`[FAIL]`で始まる失敗メッセージを出す。

### 5.3 禁止事項

ブランチ作成スクリプトでは以下を禁止する。

- `git reset --hard`
- `git clean -fd`
- `git push --force`
- `git rebase`
- `git merge`
- `.git`ディレクトリの直接操作
- 認証情報、トークン、秘密情報の表示

---

## 6. 終了・PR作成スクリプトの必須要件

ChatGPTがCodex向けプロンプトに作業終了処理を含める場合、Codexに次の要件を満たすスクリプトを生成させる。

### 6.1 対象スクリプト

- Windows: `codex_finish_pr.ps1`
- Linux/macOS/WSL: `codex_finish_pr.sh`

### 6.2 必須処理

終了・PR作成スクリプトには、最低限以下を含める。

1. 現在のbranch名を確認する。
2. 現在のbranchが`main`または`master`の場合は処理を中止する。
3. `git status --short`を表示する。
4. 変更が存在しない場合は処理を中止する。
5. `git add -A`を実行する。
6. 指定されたcommit messageで`git commit`を実行する。
7. `git push -u origin <branch>`を実行する。
8. GitHub CLI `gh` が利用可能な場合、`gh pr create`でPRを作成する。
9. `gh`が利用できない場合は、手動PR作成に必要な情報を表示する。
10. 成功時にPR URLまたは作成先情報を表示する。
11. 成功時に`[OK]`で始まる完了メッセージを出す。
12. 失敗時に`[FAIL]`で始まる失敗メッセージを出す。

### 6.3 禁止事項

終了・PR作成スクリプトでは以下を禁止する。

- `git push --force`
- `git push --mirror`
- `git reset --hard`
- `git clean -fd`
- `git rebase`
- `git filter-branch`
- `git update-ref`
- `rm -rf .git`
- `.git`ディレクトリの直接操作
- 認証情報、トークン、秘密情報の表示
- `.env`、secret、token、key類の内容表示

---

## 7. OS判定とスクリプト選択ルール

Codex向けプロンプトでは、OS分岐に関する無駄な試行を避けるため、以下を明示する。

```text
【OS判定ルール】
- WSLまたはLinux/macOS環境で実行中なら `.sh` を使用する。
- Windows native PowerShell環境で実行中なら `.ps1` を使用する。
- 必要に応じて `.sh` と `.ps1` の両方を生成してよい。
- ただし、実行するのは現在環境に対応する片方のみとする。
- どちらで実行するか不明な場合は、`uname`、`$PSVersionTable`、`Get-Host`等の読み取り専用確認だけを行う。
- `.ps1`実行に失敗した後で`.sh`を試す、またはその逆を行う場合は、Git書き込み処理を重複実行しないようにする。
```

---

## 8. Codexへの直接実行禁止文

ChatGPTは、CodexにGit書き込みを伴う作業を依頼する場合、以下の禁止文を必ず含める。

```text
Git書き込み操作を通常コマンドで直接試行しないでください。
特に、git switch -c、git checkout -b、git add、git commit、git push、gh pr create は、通常実行では禁止です。
これらが必要な場合は、必ず codex_start_branch または codex_finish_pr スクリプトに切り出し、そのスクリプトのみを権限付き実行リクエストの対象にしてください。
通常実行で失敗してから同じGit操作を権限付きで再実行する経路は禁止します。
```

---

## 9. Codexの完了報告要件

ChatGPTは、Codex向けプロンプトの完了条件に、最低限以下を含める。

```text
【完了報告】
最後に以下を報告してください。
1. 作成したbranch名
2. 変更ファイル一覧
3. 変更内容の要約
4. 実行した確認コマンドと結果
5. commit hash
6. push先branch
7. PR URL、またはPR未作成の場合の理由
8. ユーザーが動作確認・検証すべき観点
9. mainへのmerge前に確認すべき注意点
```

---

## 10. 権限付き実行前の安全確認

ChatGPTは、Codex向けプロンプトに次の確認ルールを含める。

```text
【権限付き実行前の確認】
権限付き実行リクエストを出す前に、生成したスクリプトの内容を要約してください。
特に、以下を明示してください。
- 実行対象スクリプト名
- 実行されるGit操作
- push先
- PR作成先base branch
- 禁止操作が含まれていないこと
- 認証情報や秘密情報を表示しないこと

ただし、作業全体を不必要に停止させる質問は避け、承認対象を明確化するための説明として提示してください。
```

---

## 11. 標準スクリプト雛形

ChatGPTがCodex向けプロンプトを作成する際、必要に応じて以下の雛形を使用する。

### 11.1 `codex_start_branch.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

BASE_BRANCH="${BASE_BRANCH:-main}"
WORK_BRANCH="${WORK_BRANCH:-codex/work-$(date +%Y%m%d-%H%M%S)}"

echo "[INFO] base branch: ${BASE_BRANCH}"
echo "[INFO] work branch: ${WORK_BRANCH}"

git rev-parse --is-inside-work-tree >/dev/null

if [ -n "$(git status --porcelain)" ]; then
  echo "[FAIL] working tree is not clean"
  git status --short
  exit 1
fi

git fetch origin
git switch "${BASE_BRANCH}"
git pull --ff-only origin "${BASE_BRANCH}"
git switch -c "${WORK_BRANCH}"

echo "[OK] branch created: ${WORK_BRANCH}"
```

### 11.2 `codex_finish_pr.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

PR_TITLE="${PR_TITLE:-Codex work}"
PR_BODY="${PR_BODY:-Automated changes prepared by Codex.}"
BASE_BRANCH="${BASE_BRANCH:-main}"
CURRENT_BRANCH="$(git branch --show-current)"

if [ -z "${CURRENT_BRANCH}" ]; then
  echo "[FAIL] cannot determine current branch"
  exit 1
fi

if [ "${CURRENT_BRANCH}" = "main" ] || [ "${CURRENT_BRANCH}" = "master" ]; then
  echo "[FAIL] refusing to commit directly on ${CURRENT_BRANCH}"
  exit 1
fi

git status --short

if [ -z "$(git status --porcelain)" ]; then
  echo "[FAIL] no changes to commit"
  exit 1
fi

git add -A
git commit -m "${PR_TITLE}"
git push -u origin "${CURRENT_BRANCH}"

if command -v gh >/dev/null 2>&1; then
  gh pr create --title "${PR_TITLE}" --body "${PR_BODY}" --base "${BASE_BRANCH}" --head "${CURRENT_BRANCH}"
else
  echo "[WARN] gh command not found. Create PR manually from pushed branch: ${CURRENT_BRANCH}"
fi

echo "[OK] finish_pr completed on ${CURRENT_BRANCH}"
```

### 11.3 `codex_start_branch.ps1`

```powershell
$ErrorActionPreference = "Stop"

$BaseBranch = if ($env:BASE_BRANCH) { $env:BASE_BRANCH } else { "main" }
$WorkBranch = if ($env:WORK_BRANCH) { $env:WORK_BRANCH } else { "codex/work-$(Get-Date -Format 'yyyyMMdd-HHmmss')" }

Write-Host "[INFO] base branch: $BaseBranch"
Write-Host "[INFO] work branch: $WorkBranch"

git rev-parse --is-inside-work-tree | Out-Null

$status = git status --porcelain
if ($status) {
    Write-Host "[FAIL] working tree is not clean"
    git status --short
    exit 1
}

git fetch origin
git switch $BaseBranch
git pull --ff-only origin $BaseBranch
git switch -c $WorkBranch

Write-Host "[OK] branch created: $WorkBranch"
```

### 11.4 `codex_finish_pr.ps1`

```powershell
$ErrorActionPreference = "Stop"

$PrTitle = if ($env:PR_TITLE) { $env:PR_TITLE } else { "Codex work" }
$PrBody = if ($env:PR_BODY) { $env:PR_BODY } else { "Automated changes prepared by Codex." }
$BaseBranch = if ($env:BASE_BRANCH) { $env:BASE_BRANCH } else { "main" }

$CurrentBranch = git branch --show-current

if (-not $CurrentBranch) {
    Write-Host "[FAIL] cannot determine current branch"
    exit 1
}

if ($CurrentBranch -eq "main" -or $CurrentBranch -eq "master") {
    Write-Host "[FAIL] refusing to commit directly on $CurrentBranch"
    exit 1
}

git status --short

$status = git status --porcelain
if (-not $status) {
    Write-Host "[FAIL] no changes to commit"
    exit 1
}

git add -A
git commit -m $PrTitle
git push -u origin $CurrentBranch

if (Get-Command gh -ErrorAction SilentlyContinue) {
    gh pr create --title $PrTitle --body $PrBody --base $BaseBranch --head $CurrentBranch
} else {
    Write-Host "[WARN] gh command not found. Create PR manually from pushed branch: $CurrentBranch"
}

Write-Host "[OK] finish_pr completed on $CurrentBranch"
```

---

## 12. ChatGPTがCodexプロンプトを作成する際の標準構成

ChatGPTは、Codex向けプロンプトを以下の構成で作成する。

```text
# Codex実行指示

## 1. 目的
- 今回の作業目的を明記する。

## 2. 実行者区分
- Codexが担当する作業を明記する。
- ユーザーが最終確認・main mergeを担当することを明記する。

## 3. Git操作ポリシー
- Git操作スクリプト昇格ゲート方式を明記する。
- 通常実行で許可するGit読み取り操作を明記する。
- 通常実行で禁止するGit書き込み操作を明記する。

## 4. ブランチ作成
- codex_start_branch.ps1 / codex_start_branch.sh を生成させる。
- 現在環境に応じた片方のみを実行させる。

## 5. 作業内容
- 対象ファイル、修正内容、確認観点を明記する。

## 6. 検証
- テスト、lint、build、差分確認を実行させる。

## 7. 終了処理
- codex_finish_pr.ps1 / codex_finish_pr.sh を生成させる。
- commit / push / PR作成はこのスクリプト経由に限定する。

## 8. 完了報告
- branch名、変更ファイル、確認結果、commit hash、PR URL、ユーザー検証観点を報告させる。
```

---

## 13. 例外運用

### 13.1 Git操作を行わない作業

ドキュメント確認、設計レビュー、差分案作成など、Git書き込みを必要としない作業では、本ルールのスクリプト生成は不要である。

ただし、CodexがGit書き込みを試行しないよう、以下の簡略文を含める。

```text
このタスクではGit書き込み操作を行わないでください。
git status / git diff などの読み取り専用操作のみ許可します。
commit、push、branch作成、PR作成は不要です。
```

### 13.2 既存branch上で作業する場合

ユーザーが既に作業branchを作成済みの場合、`codex_start_branch`は不要である。

ただし、終了処理でcommit / push / PR作成を行う場合は、`codex_finish_pr`スクリプト経由に限定する。

### 13.3 PR作成を行わない場合

PR作成を行わない場合でも、commit / pushをCodexに実行させるなら、`codex_finish_pr`または名称を調整した終了スクリプト経由に限定する。

---

## 14. 最終チェックリスト

ChatGPTは、Codex向けプロンプトを作成する際、以下を最終チェックする。

- [ ] Git書き込み操作を通常コマンドで直接実行させていない。
- [ ] `.git`書き込みを伴う処理をスクリプトへ切り出している。
- [ ] branch作成は`codex_start_branch`経由になっている。
- [ ] commit / push / PR作成は`codex_finish_pr`経由になっている。
- [ ] 失敗後に同じGit操作を権限付き再実行する経路を禁止している。
- [ ] 実行対象OSに応じて`.ps1`または`.sh`の片方のみを実行するよう指示している。
- [ ] 危険なGit操作を禁止している。
- [ ] secret / token / credentialを表示しないよう指示している。
- [ ] ユーザーがmainへmergeする前提を明記している。
- [ ] Codexの完了報告にbranch名、commit hash、PR URL、検証観点を含めている。

---

## 15. 運用上の結論

ChatGPTは、Codexに対してリポジトリ作業を依頼するプロンプトを作成する際、原則として本ルールを適用する。

特に、ユーザーが以下の流れを希望している場合は、本ルールを必須適用する。

```text
Codexへプロンプト投入
→ プロンプト内から新規branch作成用ps1/shを生成
→ 権限付与して実行
→ 作成された作業用branch上で作業
→ 作業終了後にcommit / push / PR作成用ps1/shを生成
→ 権限付与して実行
→ ユーザーがbranchを動作確認・検証
→ ユーザーがmainへmerge
```

この運用により、Codexが`.git`書き込みを通常経路で試行して失敗し、その後に権限付き再実行へ分岐する処理を抑制し、承認対象をレビュー可能なスクリプトに集約する。
