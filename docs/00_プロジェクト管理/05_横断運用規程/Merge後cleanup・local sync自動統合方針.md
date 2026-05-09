# Merge後cleanup・local sync自動統合方針

## 1. 目的

本書は、Q-Scout for SpringでPRをmainへmergeした後に必要となるremote branch cleanup、local prune、local main fast-forward syncを、不要な確認待ちや複数回のユーザー操作に分断しないための方針を定める。

Q-Scoutでは、docs更新、診断・改善ロジック修正、Codex投入、ChatExec2実行を伴うPRが継続的に発生する。merge後の後処理を残すと、GUI上の枝残り、ローカルmain遅れ、次タスク開始時の状態不整合が発生しやすい。

## 2. 基本方針

1. PRをsquash mergeした後、作業branchは原則削除対象とする。
2. GitHub connectorでmergeした場合、remote branch削除が自動実行されないことがあるため、merge後に残存確認する。
3. remote branchが残っている場合は、cleanup対象として扱う。
4. cleanupが必須工程である場合、ユーザーへ再確認して停止せず、cleanup用スクリプト作成まで自動で進める。
5. remote branch cleanup、`git fetch origin --prune`、local main fast-forward syncは、原則として1本のChatExec2スクリプトへ統合する。
6. 成功時のユーザー操作は、1回のダブルクリックまたは単一コマンド実行を目標とする。

## 3. 統合対象工程

原則として、以下を1本のChatExec2スクリプトに含める。

1. GitHub CLIまたは利用可能な手段の確認。
2. 対象リポジトリの固定パス確認。
3. 対象remote branchの存在確認。
4. 対象remote branchの削除。
5. remote branch不存在確認。
6. `git fetch origin --prune`。
7. local current branch確認。
8. local worktree clean確認。
9. 必要に応じた `git checkout main`。
10. `git pull --ff-only origin main`。
11. remote tracking branch不存在確認。
12. 最終 `git status --short --branch`。
13. 必要に応じたPRコメントまたは結果ファイル出力。

## 4. 分割してよい例外

以下の場合は、cleanupとlocal syncを分割してよい。

| 例外 | 理由 |
|---|---|
| local worktreeがdirty | 自動pullによりローカル変更を壊すリスクがあるため |
| current branchがmainではなく、安全にmainへ切り替えられない | 意図しないbranch切替やpullを避けるため |
| remote branch削除権限がない | 権限不足時は削除未実施として明示する必要があるため |
| GitHub CLI認証がない | local syncだけ実行できる場合があるため |
| repository pathが不明 | 固定パス確認またはユーザー指定が必要なため |
| ユーザーが明示的に分割を希望 | 操作意図を優先するため |

例外に該当する場合でも、どの工程が完了し、どの工程が未実施かを明示する。

## 5. 失敗時の報告方針

スクリプトが途中で失敗した場合、以下を結果に含める。

1. JOB_ID。
2. 対象repository。
3. 対象PR番号。
4. 対象remote branch。
5. 完了済み工程。
6. 未実施工程。
7. 失敗工程。
8. FailureReason。
9. 次に必要な対応。

失敗しているにもかかわらず、`SUCCESS`、`DONE`、`完了` と報告してはならない。

## 6. Windows実行UX

Windows向けChatExec2スクリプトでは、以下を満たす。

1. ダブルクリック実行を想定する。
2. 成功・失敗にかかわらず、結果末尾を表示する。
3. `[OK] ..._DONE` または `[FAIL] ..._FAILED` を表示する。
4. 結果表示後にウィンドウを保持する。
5. `.cmd` / `.bat` が環境上不安定な場合は、CP932 / CRLFの最小 `.bat` へ切り替える。
6. それでも不安定な場合は、常駐ターミナルでの単一コマンド方式へ切り替える。

## 7. 次タスク開始条件

次の正規TSKへ進む前に、直前PRについて以下を確認する。

1. PRがmainへmerge済みである。
2. remote作業branchが削除済みである。
3. `git fetch origin --prune` が完了している。
4. local mainがorigin/mainに追従している。
5. worktreeがcleanである。
6. 必要な課題管理整理が完了している。

これらが未完了の場合は、次タスクのbranch作成、実装、PR作成へ進まない。

## 8. 完了条件

merge後cleanupとlocal syncを完了扱いにするには、以下を満たす。

1. remote作業branchが削除済み、または既に存在しないことを確認済みである。
2. local remote tracking branchが消えている。
3. local mainがorigin/mainに追従している。
4. worktreeがcleanである。
5. 結果がChatExec2 result、PRコメント、またはチャット上の実行結果として確認可能である。
