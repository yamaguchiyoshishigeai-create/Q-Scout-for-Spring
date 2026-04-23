# 公開サンプル個別検証: spring-boot-realworld-example-app

## 1. 文書目的

本書は、公開サンプル評価運用において保留候補とした `spring-boot-realworld-example-app` について、保留解除のための個別検証論点を整理するためのタスク文書である。

現時点では、中核候補 / 補助候補 / 重量級候補 / 保留候補の正式分類は `docs/00_プロジェクト管理/05_横断運用規程/公開サンプル評価運用方針.md` に従う。

## 2. 現状整理

`samples/sample-comparison-summary.md` の結果では、`spring-boot-realworld-example-app` は以下の状態である。

- Clone: OK
- Compile: FAIL
- Q-Scout: FAIL
- source structure: `pom.xml=No`, `main=Yes`, `test=Yes`
- 現時点の位置づけ: 保留候補

`samples/CodexExec.result` では、主な失敗要因として以下が確認されている。

- Gradle 7.4 実行時に `Unsupported class file major version 65`
- `run-cli.bat` 実行時に `pom.xml was not found under ... spring-boot-realworld-example-app`

## 3. 保留理由

本サンプルは実務寄り API 構成として有望である一方、現時点では以下の理由により常用採用を見送る。

1. build が成立していない
2. Q-Scout 実行入口が `pom.xml` 前提に合致していない
3. 失敗要因が sample 自体の価値不足ではなく、build 系前提差異と入力前提差異にまたがっている
4. したがって、単純な「不採用」ではなく、個別検証後に再判定すべき対象である

## 4. 個別検証タスク

### 4.1 build 前提差異の切り分け

確認対象:
- Gradle wrapper / Gradle version
- Java version との互換性
- Groovy / plugin 側の class file major version 65 対応状況

確認目的:
- 現行 ARM64 / JDK 21 環境で build 成立可能か
- 追加の JDK 切替または Gradle 側調整が必要か

### 4.2 Q-Scout 入力前提差異の切り分け

確認対象:
- `run-cli.bat` または本体 CLI が `pom.xml` を前提としているか
- Gradle プロジェクトをそのまま入力対象にできるか
- 代替としてサブディレクトリ指定、build file 判定追加、Gradle 対応強化が必要か

確認目的:
- 本サンプル固有の問題か
- Q-Scout 側の汎用入力前提の改善対象か

### 4.3 採用再判定条件

次のいずれかを満たした場合、保留解除を再検討する。

- Compile が成立し、Q-Scout も成立する
- Compile は未成立でも、Q-Scout が安定して成立し、実務寄り比較対象として十分価値がある
- build / Q-Scout の両方が未成立でも、その原因が Q-Scout 側の改善タスクとして明確に吸収可能である

## 5. 暫定優先度

- 状態: 保留
- 優先度: 中
- 実施主体: Codex

理由:
- 中核候補群と補助候補群の初期整備は完了しており、直ちに最優先で解く必須性はない
- ただし、実務寄り API 構成の比較対象として価値が高く、将来的な保留解除候補として追跡価値がある

## 6. 今後の扱い

- 当面の正式分類は「保留候補」のままとする
- 中核 / 補助 / 重量級の初期運用を回した後、必要なら個別検証を再開する
- 本文書を起点として、必要時に台帳反映または実装タスクへ分解する
