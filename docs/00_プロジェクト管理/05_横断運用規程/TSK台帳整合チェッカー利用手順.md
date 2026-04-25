# TSK台帳整合チェッカー利用手順

## 1. 目的

本書は、`scripts/check_tsk_registry.py` と `scripts/generate_tsk_registry.py` の利用手順を定義する。

`check_tsk_registry.py` は、`docs/00_プロジェクト管理/02_改善タスク管理/改善タスク課題一覧.md` と個別 `TSK-***.md` / `解決済み/TSK-***.md` の整合を静的検査する。

`generate_tsk_registry.py` は、個別 `TSK-***.md` / `解決済み/TSK-***.md` から `改善タスク課題一覧.md` を生成・比較する。TSK-043時点では移行期間中の互換運用として、既存登録済みTSKの一覧行を保持しつつ、個別TSK側の存在、状態、配置、リンク整合を確認する。

## 2. 対象ファイル

- `docs/00_プロジェクト管理/02_改善タスク管理/改善タスク課題一覧.md`
- `docs/00_プロジェクト管理/02_改善タスク管理/TSK-*.md`
- `docs/00_プロジェクト管理/02_改善タスク管理/解決済み/TSK-*.md`
- `scripts/check_tsk_registry.py`
- `scripts/generate_tsk_registry.py`
- `scripts/test_tsk_registry.py`
- `scripts/test_generate_tsk_registry.py`

## 3. 基本コマンド

### 3.1 台帳整合チェック

リポジトリルートで以下を実行する。

    python scripts/check_tsk_registry.py

正常な場合は以下のように出力される。

    [INFO] registry rows: 35
    [INFO] max TSK number: TSK-035
    [PASS] TSK registry consistency check passed.

不整合がある場合は、以下を含む `FAIL` が出力される。

- ルールID
- 対象TSK
- 理由
- 推奨修正内容

### 3.2 台帳生成チェック

個別TSKから生成した一覧と、コミット済み `改善タスク課題一覧.md` の一致を確認する場合は、以下を実行する。

    python scripts/generate_tsk_registry.py --mode check

正常な場合は以下のように出力される。

    [PASS] generated TSK registry matches committed registry.

件数、最大TSK番号、状態別件数のみ確認する場合は以下を実行する。

    python scripts/generate_tsk_registry.py --mode summary

生成結果を別ファイルへ出力して差分確認する場合は以下を実行する。

    python scripts/generate_tsk_registry.py --mode generate --output tmp/generated_tsk_registry.md

生成結果で `改善タスク課題一覧.md` を上書きする場合は以下を実行する。ただし、上書きは差分確認後に限定する。

    python scripts/generate_tsk_registry.py --mode generate --write

## 4. 検査対象項目

### 4.1 `check_tsk_registry.py`

主な検査対象は以下である。

- `改善タスク課題一覧.md` に記載されたTSK行の抽出
- 一覧上のTSK番号重複
- 一覧リンク先の存在
- `解決済み` 状態のタスクが `解決済み/TSK-***.md` を参照していること
- `未解決`、`解決中`、`確認待ち`、`保留` 状態のタスクが通常配置側 `TSK-***.md` を参照していること
- 個別ファイル内の `- 状態: ...` と一覧上の状態が一致していること
- 一覧に載っていない通常配置側 `TSK-***.md` がないこと
- 一覧に載っていない `解決済み/TSK-***.md` がないこと
- 最大TSK番号の表示

### 4.2 `generate_tsk_registry.py`

主な検査対象は以下である。

- 通常配置側 `TSK-*.md` と `解決済み/TSK-*.md` の走査
- 個別TSKタイトル行からの管理ID抽出
- 個別TSK先頭メタデータからの状態抽出
- 個別TSKの状態と配置の一致確認
- 一覧側リンク先と個別TSK配置の一致確認
- 個別TSKから生成した一覧とコミット済み一覧の一致確認
- `--mode check` 失敗時のTSK単位・列単位の差分診断

## 5. 回帰テスト

チェッカー本体を変更した場合、または検査ルールを追加・修正した場合は、以下を実行する。

    python scripts/test_tsk_registry.py

期待結果は以下である。

    [PASS] all TSK registry regression cases passed: 9/9

生成スクリプト本体を変更した場合、または生成差分診断を変更した場合は、以下を実行する。

    python scripts/test_generate_tsk_registry.py

期待結果は以下である。

    [PASS] all TSK registry generator regression cases passed: 4/4

## 6. 運用タイミング

以下のタイミングで `check_tsk_registry.py` を実行する。

- 新規TSKを追加した後
- TSKを解決済み化した後
- `改善タスク課題一覧.md` を更新した後
- 個別 `TSK-***.md` の状態を変更した後
- `解決済み/` 配下へファイルを移動した後
- `scripts/check_tsk_registry.py` を変更した後

以下のタイミングで `generate_tsk_registry.py --mode check` を実行する。

- 新規TSKを追加した後
- TSK状態を変更した後
- `改善タスク課題一覧.md` を更新した後
- 個別 `TSK-***.md` の先頭メタデータを変更した後
- `generate_tsk_registry.py` を変更した後
- `test_generate_tsk_registry.py` を変更した後
- 台帳の正本・生成物分離に関係するPRを作成する前

## 7. PR作成時の推奨確認セット

改善タスク台帳、個別TSK、または台帳生成スクリプトに関係するPRでは、原則として以下を確認する。

    python scripts/generate_tsk_registry.py --mode summary
    python scripts/generate_tsk_registry.py --mode check
    python scripts/check_tsk_registry.py

`generate_tsk_registry.py` または `check_tsk_registry.py` 本体を変更した場合は、以下も実行する。

    python scripts/test_generate_tsk_registry.py
    python scripts/test_tsk_registry.py

## 8. 注意事項

これらのチェッカーは静的検査であり、タスク粒度や状態判断の妥当性までは判定しない。

以下は人間またはAIレビューを残す。

- タスクの状態変更が妥当か
- 確認待ちから解決済みへ移動してよいか
- 保留タスクの扱いが妥当か
- 新規タスク番号の内容的な妥当性
- 登録PRと実作業PRの分離が必要か
- 生成結果をそのまま `--write` してよいか
- 生成物化または索引化へ移行してよいか

## 9. 関連文書

- `docs/00_プロジェクト管理/02_改善タスク管理/改善タスク課題一覧.md`
- `docs/00_プロジェクト管理/02_改善タスク管理/TSK-030.md`
- `docs/00_プロジェクト管理/02_改善タスク管理/TSK-043.md`
- `docs/00_プロジェクト管理/05_横断運用規程/AI運用作業スクリプト化候補棚卸し.md`
- `docs/00_プロジェクト管理/05_横断運用規程/ChatGPTリポジトリ編集運用ルール.md`
