# Spring開発基盤テンプレート抽出設計書 v0.1

> 本書は、Q-Scout-for-Spring の現行 Web MVP / CLI / 診断ルールに対する直接要件ではなく、
> 今後の Spring Framework / Spring Boot ベースの他ツール開発へ、Q-Scout 開発過程で整備された
> docs 構成・改善タスク管理・AI作業証跡管理・Codex連携運用を横展開するための将来構想文書である。
>
> 本書の内容は、現行 Q-Scout の実装仕様やリリース必須条件ではなく、
> 将来のテンプレートリポジトリ作成に向けた抽出設計として扱う。

## 1. 文書概要

### 1.1 文書目的

本書は、現在開発中の `Q-Scout-for-Spring` リポジトリで整備された開発運用基盤をもとに、今後の Spring Framework / Spring Boot ベースの新規ツール開発へ再利用可能なテンプレートリポジトリを設計するための抽出方針を定義する。

対象とするテンプレートは、特定プロダクト固有の診断ロジックや UI を含むものではなく、Spring 系ツール開発に共通して必要となる以下の基盤を提供することを目的とする。

- docs 階層構成
- 企画、要件定義、基本設計、詳細設計の文書体系
- 改善タスク管理機構
- AIリポジトリ作業証跡管理
- Codex / ChatGPT 作業分担ルール
- PR 中心の確認・証跡運用
- 新規チャット開始時プロンプト方式
- Public Free / Private Pro 分離運用のひな形
- CLI / Web 両対応ツールを想定した基本設計骨子

本書は、テンプレートリポジトリ実体を作成する前段階の設計書であり、テンプレート化対象、除外対象、置換方針、作成手順、今後の展開方針を明確化する。

---

## 2. 背景

### 2.1 Q-Scout-for-Spring の位置づけ

`Spring-Frameworkベースツール案.txt` では、Q-Scout for Spring は初期10案のうち「⑥ Springコード品質スコアリングAI」として位置づけられている。

同資料では、Q-Scout for Spring は MVP成立が早く、汎用性が高く、将来の他ツールへの入口プロダクトになり得るため、最優先で着手すべき案として整理されている。

また、Q-Scout for Spring は単体ツールではなく、将来の「Spring開発支援プラットフォーム」の中核として位置づけられている。

### 2.2 テンプレート化の必要性

今後、以下のような Spring 系ツールを順次開発する場合、各リポジトリで毎回ゼロから docs 構成、改善タスク管理、AI作業ルール、PR証跡管理、開始時プロンプトを整備するのは非効率である。

- Spring API自動設計コンパイラ
- Springマイクロサービス依存可視化ツール
- Springセキュリティポリシーテスター
- Spring設定ミス検出CLI
- Springテスト自動生成エンジン
- Springレガシーコード自動リファクタリングツール

一方、Q-Scout-for-Spring では、開発過程で以下がすでに実運用レベルまで整備されている。

- docs 階層構造
- 改善タスク台帳
- 個別 TSK 管理
- 解決済み化フロー
- Codex連携運用ルール
- AIリポジトリ作業証跡管理
- ChatGPT / Codex / ユーザーの作業主体分類
- 開始時プロンプト
- README / docs / 実装 / PR を照合する作業準備方式

このため、Q-Scout-for-Spring の開発基盤を抽象化し、他の Spring 系ツールへ流用できるテンプレートとして整備する価値が高い。

---

## 3. 基本方針

### 提案名
Q-Scout由来 Spring開発基盤テンプレート化方式

### 優先度
高

### 方針概要

Q-Scout-for-Spring のうち、プロダクト固有ではなく、Spring 系ツール開発に横断的に有用な構成を抽出し、テンプレートリポジトリとして再利用可能な形に整える。

テンプレート化にあたっては、次の方針を採用する。

1. Q-Scout 固有の診断ルール、スコアリング仕様、UI文言はテンプレート本体から除外する。
2. docs 構成、改善タスク管理、AI作業証跡管理、Codex連携運用は汎用骨子として残す。
3. 固有名詞は `{{PROJECT_NAME}}`、`{{TOOL_NAME}}`、`{{PRODUCT_NAME}}` などの変数へ置換する。
4. テンプレートは、CLI専用、Web専用、CLI+Web両対応のいずれにも拡張できる構成とする。
5. 最初のテンプレートは過度に自動化せず、人間とAIが理解しやすい文書基盤を優先する。
6. 将来的に複数ツール開発で再利用された後、生成スクリプトやセットアップ自動化を追加する。

---

## 4. テンプレート化対象

### 4.1 docs 全体構成

以下の docs 階層は、Q-Scout 固有性が低く、他の Spring 系ツールにも適用可能であるため、テンプレート化対象とする。

    docs/
      README.md

      00_プロジェクト管理/
        01_プロジェクト計画/
        02_改善タスク管理/
        05_横断運用規程/

      10_企画/
        15_将来構想/

      20_要件定義/

      30_基本設計/

      40_詳細設計/
        41_詳細設計正本/
        42_UI差分設計/
        43_UI運用・Figma/
        44_実装指示書/
        45_整理方針・補助文書/

      50_実装/

      60_単体テスト/

      70_結合テスト/

      90_アーカイブ/

### 4.2 docs/README.md の構成思想

docs 全体の入口文書として、以下の内容をテンプレート化する。

- docs 直下の基本方針
- 大分類フォルダの位置づけ
- 現行正本と補助仕様の区別
- 履歴資料の扱い
- プレースホルダ文書の扱い
- 管理系文書を `00_プロジェクト管理/` に集約する方針
- 要件、基本設計、詳細設計の境界

### 4.3 プロジェクト管理文書

以下はテンプレート化対象とする。

    docs/00_プロジェクト管理/01_プロジェクト計画/
      プロジェクト計画.md
      Public-Free版・Private-Pro版分離運用方針_テンプレート.md
      Public-Free版完成基準_テンプレート.md

    docs/00_プロジェクト管理/02_改善タスク管理/
      改善タスク管理ルール.md
      改善タスク課題一覧.md
      TSK-001.md
      解決済み/
      アーカイブ/

    docs/00_プロジェクト管理/05_横断運用規程/
      ChatGPTリポジトリ編集運用ルール.md
      Codex連携運用ルール.md
      AIリポジトリ作業証跡管理ルール.md
      実装系文書配置規程.md

### 4.4 改善タスク管理機構

Q-Scout-for-Spring で実運用されている改善タスク管理方式は、テンプレートの中核機能として抽出する。

テンプレート化する要素は以下とする。

- `TSK-***` 採番方式
- 状態分類
  - 未解決
  - 解決中
  - 確認待ち
  - 保留
  - 解決済み
- 個別タスクファイル方式
- 解決済みタスクの移動方式
- 課題一覧の集計欄
- 課題一覧から個別ファイルへのリンク
- 解決済み化前の確認ゲート
- 台帳と個別ファイルの整合確認ルール

初期テンプレートでは、以下のサンプルタスクを含める。

    TSK-001.md

内容は、プロダクト固有課題ではなく、以下のような汎用初期タスクとする。

    件名: 初期READMEとdocs入口文書の整合を確認する
    状態: 未解決
    担当候補: ChatGPT(会話内) / ChatGPT(リポジトリ編集)
    目的: 新規プロジェクト開始時に README、docs/README.md、開始時プロンプトの整合を確認する

### 4.5 AIリポジトリ作業証跡管理

以下の考え方をテンプレート化する。

- AIによるリポジトリ変更では証跡を残す
- PRあり作業では PR本文または PRコメントに記録する
- PRなし作業ではコミットメッセージ本文に短縮記録を残す
- 長大ログは git 管理対象にしない
- ローカル実行結果ファイルは正式履歴ではなく補助扱いとする
- merge前確認は原則 PR ブランチで行う
- main 上の確認は merge 後の最終確認とする

テンプレートには、以下のブロック形式を含める。

    ===== AI_REPO_RESULT_BEGIN =====
    ACTOR: ChatGPT(リポジトリ編集) / Codex
    TASK_ID: <作業ID>
    TITLE: <表題>
    STATUS: [OK] / [FAIL]
    changed files:
    - ...

    summary:
    - ...

    verification:
    - ...

    unverified:
    - ...

    commit:
    - <commit sha / none>
    message:
    - <commit message>
    push:
    - success / failed / none
    pr:
    - <PR number / none>
    pr url:
    - <URL / none>
    ===== AI_REPO_RESULT_END =====

### 4.6 Codex連携運用

Codex連携運用は、以下をテンプレート化する。

- Codex に渡すべき作業の判断基準
- ChatGPT(会話内) で先行すべき作業
- ChatGPT(リポジトリ編集) で先行すべき作業
- Codex節約型運用
- Codex向けプロンプトの安全チェック
- Git書き込み操作を通常コマンドで直接実行させない方針
- PR / commit / push などの証跡確認方式
- CodexExec.md / CodexExec.result の扱い

### 4.7 開始時プロンプト方式

各プロダクトごとに、以下のような開始時プロンプトを持てるようにする。

    {{PROJECT_NAME}}_開始時プロンプト_完全版.md

テンプレート化する内容は以下とする。

- 対象リポジトリ
- 対象ブランチ
- 最初に読むファイル一覧
- docs の読み順
- 要件定義・基本設計・詳細設計の参照順
- 実装確認用ファイル
- 現行フェーズ認識
- 未解決課題確認
- PR / commit / CodexExec.result の扱い
- 文書と実装が矛盾する場合の優先順位

変数例は以下とする。

    {{PROJECT_NAME}}
    {{PRODUCT_NAME}}
    {{REPOSITORY_FULL_NAME}}
    {{DEFAULT_BRANCH}}
    {{MAIN_APPLICATION_CLASS}}
    {{PRIMARY_RUNTIME}}
    {{TARGET_FRAMEWORK}}
    {{PRIMARY_DELIVERABLES}}

### 4.8 README ひな形

テンプレート README には以下を含める。

- プロダクト概要
- 解決する課題
- このツールでできること
- 主な対象ユーザー
- 技術的な見どころ
- 差別化ポイント
- Public Free版としての位置づけ
- ビルド手順
- Web起動手順
- CLI起動手順
- 補助スクリプト
- 出力ファイル
- 基本的な利用フロー

ただし、各項目は Q-Scout 固有文言ではなく、変数と記入ガイドを含むテンプレート文にする。

---

## 5. テンプレート化から除外する対象

### 5.1 Q-Scout 固有の診断仕様

以下はテンプレート本体から除外する。

- R001 ControllerToRepositoryDirectAccessRule
- R002 FieldInjectionRule
- R003 TransactionalMisuseRule
- R004 ExceptionSwallowingRule
- R005 MissingTestRule
- R006 PackageDependencyViolationRule
- Q-Scout 固有のルール重大度判定
- Q-Scout 固有の改善ヒント定義
- Q-Scout 固有のルール詳細解説ページ

ただし、診断系ツールの参考例として、別途 `examples/qscout-derived/` に抜粋を置くことは検討可能とする。

### 5.2 Q-Scout 固有の成果物仕様

以下はテンプレート本体から除外する。

- `qscout-report.md`
- `qscout-ai-input.md`
- Q-Scout 用 Markdown レポート構成
- Q-Scout 用 AI投入 Markdown 構成
- スコアリング結果表示固有の UI

テンプレートでは、成果物名を以下のように一般化する。

    {{HUMAN_REPORT_FILE}}
    {{AI_INPUT_FILE}}
    {{MACHINE_READABLE_OUTPUT_FILE}}
    {{EXPORT_ARTIFACT_FILE}}

### 5.3 Q-Scout 固有の Web UI

以下は除外する。

- Q-Scout トップページ固有レイアウト
- Q-Scout 成果物サンプル
- Q-Scout スコア表示カード
- Q-Scout ルールヘルプ導線
- Q-Scout 固有 CSS / JS

ただし、Web UI を持つプロダクト向けの汎用 UI 設計フォルダとして、`40_詳細設計/42_UI差分設計/` は残す。

### 5.4 Q-Scout 固有のサンプル評価

以下はテンプレート本体から除外する。

- spring-petclinic 等の Q-Scout 評価用サンプル分類
- Q-Scout 用 sample-output 構成
- Q-Scout 用公開サンプル評価スクリプト

ただし、テンプレートには以下の抽象文書を含める。

    公開サンプル評価運用方針_テンプレート.md

内容は、各プロダクトが評価用サンプルを選定する際の基準を定義するものとする。

### 5.5 Q-Scout 固有の事業表現

以下はテンプレート本体では汎用化する。

- Q-Scout for Spring
- Springコード品質スコアリングAI
- 品質診断ツール
- 6ルール
- スコアリング
- qscout で始まる識別子

置換例は以下とする。

    Q-Scout for Spring
    → {{PRODUCT_NAME}}

    Springコード品質スコアリングAI
    → {{PRODUCT_CATEGORY}}

    品質診断ツール
    → {{TOOL_DESCRIPTION}}

    qscout-report.md
    → {{HUMAN_REPORT_FILE}}

---

## 6. 変数化方針

テンプレート内では、固有値を以下の変数に置換する。

| 変数 | 説明 | 例 |
|---|---|---|
| `{{PROJECT_NAME}}` | プロジェクト名 | Spring Config Doctor |
| `{{PRODUCT_NAME}}` | プロダクト名 | Config Doctor for Spring |
| `{{REPOSITORY_FULL_NAME}}` | GitHubリポジトリ名 | owner/config-doctor-for-spring |
| `{{DEFAULT_BRANCH}}` | 既定ブランチ | main |
| `{{PRODUCT_CATEGORY}}` | 製品カテゴリ | Spring設定ミス検出CLI |
| `{{TARGET_FRAMEWORK}}` | 対象技術 | Spring Boot / Spring Framework |
| `{{PRIMARY_RUNTIME}}` | 主実行環境 | Java 17 / Spring Boot |
| `{{MAIN_APPLICATION_CLASS}}` | Web起動クラス | com.example.App |
| `{{CLI_MAIN_CLASS}}` | CLI起動クラス | com.example.cli.Main |
| `{{HUMAN_REPORT_FILE}}` | 人間向け成果物 | report.md |
| `{{AI_INPUT_FILE}}` | AI向け成果物 | ai-input.md |
| `{{PUBLIC_SCOPE}}` | Public Free版の範囲 | Maven単一モジュール対応 |
| `{{PRIVATE_SCOPE}}` | Private Pro版の範囲 | 大規模・商用向け機能 |

---

## 7. テンプレートリポジトリ構成案

テンプレートリポジトリの初期構成は以下とする。

    Spring-Tool-Development-Template/
      README.md
      PROJECT_START_PROMPT_TEMPLATE.md

      docs/
        README.md

        00_プロジェクト管理/
          01_プロジェクト計画/
            プロジェクト計画.md
            Public-Free版・Private-Pro版分離運用方針_テンプレート.md
            Public-Free版完成基準_テンプレート.md

          02_改善タスク管理/
            改善タスク管理ルール.md
            改善タスク課題一覧.md
            TSK-001.md
            解決済み/
            アーカイブ/

          05_横断運用規程/
            ChatGPTリポジトリ編集運用ルール.md
            Codex連携運用ルール.md
            AIリポジトリ作業証跡管理ルール.md
            実装系文書配置規程.md

        10_企画/
          プロジェクト企画書_テンプレート.md
          15_将来構想/
            AI利用戦略_テンプレート.md

        20_要件定義/
          20_要件定義ガイド.md
          MVP最小要件定義書_テンプレート.md
          全体設計方針書_テンプレート.md
          実行制約・運用前提_テンプレート.md
          多言語化対応仕様書_テンプレート.md

        30_基本設計/
          基本設計.md

        40_詳細設計/
          詳細設計.md
          41_詳細設計正本/
            詳細設計正本.md
          42_UI差分設計/
          43_UI運用・Figma/
          44_実装指示書/
          45_整理方針・補助文書/

        50_実装/
          実装.txt

        60_単体テスト/
          単体テスト.txt

        70_結合テスト/
          結合テスト.txt

        90_アーカイブ/

      scripts/
        check_codex_prompt_git_safety.py
        test_codex_prompt_git_safety.py
        check_ai_repo_result.py
        check_tsk_index_consistency.py

      .github/
        PULL_REQUEST_TEMPLATE.md

---

## 8. 作業主体別の実施方針

### 8.1 ChatGPT(会話内)

担当する作業は以下とする。

- テンプレート抽出方針の整理
- 汎用化対象と除外対象の分類
- 置換変数の定義
- 各テンプレート文書のドラフト作成
- Codex向け実装指示書の作成
- 生成後レビュー観点の整理

理由：
文書設計、比較検討、抽象化、プロンプト作成が中心であり、会話内で完結しやすいため。

### 8.2 ChatGPT(リポジトリ編集)

担当候補は以下とする。

- Q-Scout-for-Spring 内への抽出設計書追加
- 小規模な docs 追記
- 改善タスク登録
- README への軽微な導線追記
- テンプレート化方針書の PR 作成

理由：
小〜中規模の文書追加であれば、完全取得したうえで局所編集できるため。

### 8.3 Codex

担当候補は以下とする。

- テンプレートリポジトリのファイル群生成
- Q-Scout 固有語の一括置換
- 多数ファイルの配置
- スクリプト類の移植
- チェッカーの動作確認
- docsリンク整合確認
- build / test / lint 実行

理由：
多数ファイル生成、一括置換、ファイル移動、検証が発生するため、Codex の方が適している。

### 8.4 ユーザー(人)

担当する作業は以下とする。

- テンプレートリポジトリを実際に作成するかの最終判断
- リポジトリ名の決定
- Public / Private の扱い判断
- GitHub上での最終 merge
- 次に適用する Spring 系ツールの選定

理由：
事業判断、公開範囲、命名、優先順位は人間の判断が必要なため。

---

## 9. 初期テンプレート適用候補

Q-Scout-for-Spring の次にテンプレート適用しやすい候補は以下とする。

### 第1候補
Spring設定ミス検出CLI（Config Doctor）

理由：
- CLI中心で開始できる
- application.yml / properties を対象にしやすい
- Q-Scout の CLI / レポート / Web化構成を転用しやすい
- MVPが比較的小さく成立しやすい

### 第2候補
Springセキュリティポリシーテスター

理由：
- ルール診断型で Q-Scout と構造が近い
- 改善ヒント、レポート、チェック項目管理と相性が良い
- Public Free / Private Pro の分離もしやすい

### 第3候補
Springテスト自動生成エンジン

理由：
- 設計・実装・テスト方針の文書管理が重要
- AI連携との親和性が高い
- Codex / ChatGPT 連携基盤の価値が出やすい

### 第4候補
Springレガシーコード自動リファクタリングツール

理由：
- ビジネス価値は高い
- ただし実装難易度が高く、テンプレート成熟後に適用する方が望ましい

---

## 10. 実施ステップ案

### Phase 1: 設計書確定

作業主体：
ChatGPT(会話内)

実施内容：
- 本設計書 v0.1 をレビュー
- テンプレート化対象と除外対象を確定
- 変数化方針を確定
- 初期テンプレート構成を確定

成果物：
- Spring開発基盤テンプレート抽出設計書 v0.1

### Phase 2: Q-Scoutリポジトリ内への方針書登録

作業主体：
ChatGPT(リポジトリ編集)

実施内容：
- 本設計書を Q-Scout-for-Spring の docs 配下へ追加
- 改善タスクとしてテンプレート化検討タスクを登録
- 必要に応じて docs/README.md に導線を追加

配置候補：
    docs/00_プロジェクト管理/01_プロジェクト計画/Spring開発基盤テンプレート抽出設計書-v0.1.md

または：
    docs/10_企画/15_将来構想/Spring開発基盤テンプレート抽出設計書-v0.1.md

推奨配置：
    docs/10_企画/15_将来構想/Spring開発基盤テンプレート抽出設計書-v0.1.md

理由：
テンプレート化は現行 Q-Scout の直接機能ではなく、将来の Spring 開発支援プラットフォーム構想に近いため。

### Phase 3: テンプレートリポジトリ雛形作成

作業主体：
Codex

実施内容：
- テンプレートリポジトリ用ファイル群の生成
- Q-Scout 固有語の変数化
- 初期 README 作成
- docs 初期構成作成
- 改善タスク初期ファイル作成
- AI運用ルール文書作成
- PRテンプレート作成

成果物：
- Spring-Tool-Development-Template リポジトリ雛形

### Phase 4: テンプレートの試験適用

作業主体：
ChatGPT(会話内) / Codex / ユーザー(人)

実施内容：
- Config Doctor など小規模候補にテンプレートを適用
- README / docs / 開始時プロンプトが自然に機能するか確認
- 不足文書や過剰文書を洗い出す

成果物：
- テンプレート改善タスク
- v0.2 改訂方針

---

## 11. 初期リスクと対策

### 11.1 Q-Scout 固有性が残りすぎるリスク

リスク：
テンプレート内に Q-Scout 固有のスコアリング、診断ルール、成果物名が残ると、他プロダクト適用時に誤解を生む。

対策：
- 固有語置換リストを作成する
- `Q-Scout`、`qscout`、`score`、`rule` などの残存 grep を行う
- 残す場合は「例」と明記する

### 11.2 テンプレートが重すぎるリスク

リスク：
初期MVP向けの小規模ツールに対して、docs や運用規程が重すぎる可能性がある。

対策：
- 最小セットと拡張セットを分ける
- `minimum/` と `full/` の二段構成を検討する
- 初期適用時は 00 / 10 / 20 / 30 / TSK のみ必須とする

### 11.3 実装テンプレートと文書テンプレートが混同されるリスク

リスク：
Spring Boot のコード雛形と、開発運用ドキュメント雛形が混同される。

対策：
- 本テンプレートの主目的は「開発運用基盤」と明記する
- 実装コード雛形は別フェーズで扱う
- 必要なら後から `app-template/` を追加する

### 11.4 複数プロダクト間で運用差分が出るリスク

リスク：
各プロダクトでテンプレート改変が進み、共通ルールが分岐する。

対策：
- テンプレート本体を独立リポジトリで管理する
- 各プロダクトには適用時点のコピーを置く
- 共通ルール更新時は、各リポジトリへ反映するか個別判断する

---

## 12. 成功条件

本テンプレート抽出の初期成功条件は以下とする。

1. Q-Scout 固有要素と汎用要素が明確に分離されている
2. Spring 系新規ツールのリポジトリ立ち上げ時に docs 構成を再利用できる
3. 改善タスク管理を初日から開始できる
4. ChatGPT / Codex / ユーザー の作業主体分類を再利用できる
5. AIリポジトリ作業証跡管理を再利用できる
6. 新規チャット開始時プロンプトを各プロダクト向けに生成できる
7. Config Doctor 等の次期候補へ試験適用できる
8. Q-Scout-for-Spring 側の現行開発を阻害しない

---

## 13. 最終結論

Q-Scout-for-Spring の開発過程で整備された docs 構成、改善タスク管理、AI作業証跡管理、Codex連携運用、開始時プロンプト方式は、Q-Scout 固有の成果ではなく、今後の Spring 系ツール開発全般に再利用可能な開発基盤である。

したがって、これらを抽出し、Q-Scout 固有の診断仕様や UI 文言を除外したうえで、`Spring-Tool-Development-Template` としてテンプレート化する方針は妥当である。

初期段階では、実装コード雛形よりも、文書体系・運用規程・改善タスク管理・AI協働開発ルールを中心とした「開発運用基盤テンプレート」として整備する。

その後、Config Doctor 等の比較的小規模な Spring 系ツールに試験適用し、テンプレートの過不足を確認したうえで、v0.2 以降で実装コード雛形や自動生成スクリプトを追加する。

以上を、Spring開発基盤テンプレート抽出設計 v0.1 とする。
