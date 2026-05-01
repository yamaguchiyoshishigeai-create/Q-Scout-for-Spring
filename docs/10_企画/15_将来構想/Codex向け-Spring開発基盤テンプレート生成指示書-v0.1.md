# TPL-003 Codex向け Spring開発基盤テンプレート生成指示書 v0.1

> 本書は、Q-Scout-for-Spring の現行 Web MVP / CLI / 診断ルールに対する直接要件ではなく、
> TPL-001 / TPL-002 を受けて、将来作成する `Spring-Tool-Development-Template`
> リポジトリの初期ファイル群を Codex に生成させるための将来構想段階の実行指示書である。
>
> 本書の内容は、現行 Q-Scout のリリース必須条件ではなく、
> 別リポジトリである `Spring-Tool-Development-Template` 作成時に使用する予定の
> Codex向け指示書として扱う。

## 1. 文書概要

### 1.1 文書目的

本書は、`Spring-Tool-Development-Template` リポジトリを新規作成した後、Codex に対して初期ファイル群を生成させるための実行指示書である。

本書は、以下の先行文書を前提とする。

- TPL-001 Spring開発基盤テンプレート抽出設計書 v0.1
- TPL-002 Spring開発基盤テンプレートリポジトリ作成方針書 v0.1

TPL-003 の目的は、Q-Scout-for-Spring の開発過程で整備された docs 構成、改善タスク管理、AIリポジトリ作業証跡管理、Codex連携運用、開始時プロンプト方式を、Q-Scout 固有要素を除外したうえで、今後の Spring 系ツール開発に再利用可能なテンプレートリポジトリとして初期生成することである。

本書は Codex にそのまま渡すことを想定した実行指示書である。

---

## 2. 作業前提

### 2.1 対象リポジトリ

リポジトリ名：

    Spring-Tool-Development-Template

公開範囲：

    Private

既定ブランチ：

    main

作業ブランチ：

    feature/initial-template-structure

### 2.2 テンプレートの位置づけ

本テンプレートは、Spring Boot アプリケーションの実装コード雛形ではない。

本テンプレートの主目的は、Spring Framework / Spring Boot ベースの新規ツール開発に共通して利用できる **開発運用基盤** を提供することである。

対象とする主な基盤は以下である。

- docs 階層
- 企画書テンプレート
- 要件定義テンプレート
- 基本設計テンプレート
- 詳細設計入口
- 改善タスク管理
- AIリポジトリ作業証跡管理
- Codex連携ルール
- PR運用ルール
- 開始時プロンプト
- GitHub Actions による初期チェック基盤

---

## 3. 今回の確定条件

### 3.1 リポジトリ名

    Spring-Tool-Development-Template

### 3.2 公開範囲

    Private

理由：

- 今後、非公開プロンプト、商用開発ノウハウ、Private Pro 版テンプレート、顧客導入前提の運用文書を追加する可能性があるため
- 初期段階では外部公開よりも、テンプレート品質の確立と内部運用の安定化を優先するため

### 3.3 初期構成

    フルセット

理由：

- Q-Scout-for-Spring で整備された docs / 改善タスク / AI証跡管理 / 詳細設計 / テスト工程配置を広く再利用するため
- 初期利用時は最小セットのみ使えるように README で案内しつつ、将来の拡張先を最初から確保するため

### 3.4 初回 scripts

    含める

対象候補：

- `check_codex_prompt_git_safety.py`
- `test_codex_prompt_git_safety.py`
- `check_ai_repo_result.py`
- `check_tsk_index_consistency.py`
- `scripts/README.md`

### 3.5 GitHub Actions

    最初から含める

対象ワークフロー候補：

- docs / TSK 整合チェック
- Codex プロンプト安全チェック
- AI_REPO_RESULT 形式チェック

ただし、Java / Maven / Docker / Render など、実装方式に依存する CI は初回対象外とする。

### 3.6 実装コード雛形

    初回は含めない

理由：

- Spring API生成、Config Doctor、セキュリティ診断、テスト生成など、プロダクト種別によってコード構成が大きく異なるため
- 初期テンプレートでは開発運用基盤の再利用性を優先するため

### 3.7 初回適用候補

    未定

理由：

- テンプレート本体の完成後、どの Spring 系ツールへ初回適用するかは別途判断するため
- Config Doctor を固定せず、テンプレート完成度と事業優先順位を見て選定するため

---

## 4. Codexへの作業依頼概要

### 4.1 作業主体

    Codex

### 4.2 Codexを選ぶ理由

本作業は、多数ファイルの新規作成、ディレクトリ構成の生成、テンプレート文書の配置、GitHub Actions の初期設定、scripts の配置を伴う。

そのため、ChatGPT(会話内)よりも Codex によるリポジトリ操作が適している。

### 4.3 Codexに実施させること

Codex は、`Spring-Tool-Development-Template` リポジトリ内で以下を実施する。

1. 作業ブランチ `feature/initial-template-structure` を作成する
2. フルセットのディレクトリ構成を作成する
3. ルート README を作成する
4. `PROJECT_START_PROMPT_TEMPLATE.md` を作成する
5. docs 配下のテンプレート文書を作成する
6. 改善タスク管理の初期ファイルを作成する
7. AIリポジトリ作業証跡管理ルールを作成する
8. Codex連携運用ルールを作成する
9. scripts を作成または移植する
10. GitHub Actions ワークフローを作成する
11. PRテンプレートを作成する
12. 生成後に構成確認を行う
13. PR を作成する

---

## 5. Codexで実施しないこと

以下は今回の対象外とする。

1. Java / Spring Boot 実装コードの作成
2. Maven / Gradle プロジェクトの作成
3. Dockerfile の作成
4. Render / Fly.io / Railway 等のデプロイ設定
5. Web UI テンプレートの作成
6. Thymeleaf / React / Vue 等の画面実装
7. Q-Scout の診断ルール移植
8. Q-Scout のスコアリング仕様移植
9. Q-Scout のレポート生成仕様移植
10. Q-Scout のサンプル評価スクリプト移植
11. Config Doctor 等、特定プロダクト向け仕様の作成
12. 外部公開用 README の作成
13. 商用 Pro 版固有文書の作成

---

## 6. 初期ディレクトリ構成

Codex は、以下の構成を生成すること。

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
              README.md
            アーカイブ/
              README.md

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
            README.md
          43_UI運用・Figma/
            README.md
          44_実装指示書/
            README.md
          45_整理方針・補助文書/
            README.md

        50_実装/
          実装.txt

        60_単体テスト/
          単体テスト.txt

        70_結合テスト/
          結合テスト.txt

        90_アーカイブ/
          README.md

      scripts/
        README.md
        check_codex_prompt_git_safety.py
        test_codex_prompt_git_safety.py
        check_ai_repo_result.py
        check_tsk_index_consistency.py

      .github/
        PULL_REQUEST_TEMPLATE.md
        workflows/
          template-checks.yml

---

## 7. ルートREADME作成方針

`README.md` は、テンプレート利用者向けの入口文書として作成する。

含める内容は以下とする。

1. このテンプレートの目的
2. このテンプレートが含むもの
3. このテンプレートが含まないもの
4. Private リポジトリとして扱う理由
5. 最小セットの使い方
6. フルセットの使い方
7. 新規プロダクトへ適用する手順
8. 変数置換一覧
9. ChatGPT / Codex / ユーザー の作業分担
10. 初回セットアップ手順
11. GitHub Actions の内容
12. 今後の拡張予定

README では、以下を明記する。

- 本テンプレートは Spring Boot アプリの実装コード雛形ではない
- 本テンプレートは開発運用基盤テンプレートである
- 実装コード雛形は後続フェーズで追加する
- 初期適用候補は未定である

---

## 8. PROJECT_START_PROMPT_TEMPLATE.md 作成方針

`PROJECT_START_PROMPT_TEMPLATE.md` は、新規チャット開始時に AI がプロジェクト状況を同期するためのプロンプトひな形とする。

含める内容は以下とする。

1. Memory を読むこと
2. 対象リポジトリを読むこと
3. README を読むこと
4. docs/README.md を読むこと
5. 要件定義ガイドを読むこと
6. 基本設計を読むこと
7. 詳細設計入口を読むこと
8. 改善タスク一覧を読むこと
9. 未解決 / 解決中 / 確認待ちタスクを確認すること
10. 直近PRを確認すること
11. 文書と実装が矛盾する場合の優先順位
12. 作業主体分類
13. 次作業準備完了時の報告形式

変数は以下を利用する。

    {{PROJECT_NAME}}
    {{PRODUCT_NAME}}
    {{REPOSITORY_FULL_NAME}}
    {{DEFAULT_BRANCH}}
    {{PRIMARY_RUNTIME}}
    {{BUILD_TOOL}}
    {{MAIN_APPLICATION_CLASS}}
    {{CLI_MAIN_CLASS}}

---

## 9. docs/README.md 作成方針

`docs/README.md` は docs 全体の入口文書として作成する。

含める内容は以下とする。

- docs 直下の基本方針
- 大分類フォルダの位置づけ
- 現行正本の考え方
- 現行補助仕様の考え方
- 将来構想の扱い
- 履歴資料の扱い
- 未作成プレースホルダ文書の扱い
- フォルダ維持方針

Q-Scout 固有文言は使わず、テンプレート用に一般化すること。

---

## 10. 改善タスク管理ファイル作成方針

### 10.1 改善タスク管理ルール.md

以下を定義する。

- TSK 採番方式
- 状態分類
- 優先度
- 担当主体
- 確認待ち
- 解決済み移動
- 再オープン
- 課題一覧との整合
- AI作業時の証跡

状態分類は以下とする。

    未解決
    解決中
    確認待ち
    保留
    解決済み

### 10.2 改善タスク課題一覧.md

初期状態は以下とする。

    最大TSK番号: TSK-001
    保留: 0
    未解決: 1
    確認待ち: 0
    解決中: 0
    解決済み: 0

初期タスク一覧に `TSK-001` を掲載する。

### 10.3 TSK-001.md

内容は以下とする。

    管理ID: TSK-001
    件名: 初期READMEとdocs入口文書の整合を確認する
    状態: 未解決
    主担当候補: ChatGPT(会話内) / ChatGPT(リポジトリ編集)
    目的: 新規プロジェクト開始時に README、docs/README.md、PROJECT_START_PROMPT_TEMPLATE.md の整合を確認する

---

## 11. AIリポジトリ作業証跡管理ルール

`AIリポジトリ作業証跡管理ルール.md` には、以下を定義する。

- AIによるリポジトリ変更では証跡を残す
- PRあり作業では PR本文またはPRコメントに記録する
- PRなし作業ではコミットメッセージ本文に短縮記録を残す
- 長大ログは git 管理対象にしない
- ローカル実行結果ファイルは正式履歴ではなく補助扱いとする
- merge前確認は原則 PR ブランチで行う
- main 上の確認は merge 後の最終確認とする

AI_REPO_RESULT ブロック形式を含めること。

インデント形式で以下を掲載する。

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

---

## 12. Codex連携運用ルール

`Codex連携運用ルール.md` には、以下を定義する。

- Codex に渡すべき作業
- ChatGPT(会話内) で先行すべき作業
- ChatGPT(リポジトリ編集) で先行すべき作業
- Codex節約型運用
- Codex向けプロンプトの安全チェック
- Git書き込み操作を通常コマンドで直接実行させない方針
- PR / commit / push の証跡確認方式
- CodexExec.md / CodexExec.result の扱い

---

## 13. scripts 作成方針

### 13.1 scripts/README.md

scripts の目的、使い方、初期スクリプト一覧を記載する。

### 13.2 check_codex_prompt_git_safety.py

Codex向け指示書内に危険な Git 操作がないかを検査するスクリプトとする。

検査対象例：

- `git push --force`
- `git reset --hard`
- `git clean -fdx`
- `git rebase`
- `git checkout main`
- `git commit --amend`
- `rm -rf .git`

検査対象文書は引数で受け取れるようにする。

### 13.3 test_codex_prompt_git_safety.py

`check_codex_prompt_git_safety.py` の単体テストを作成する。

### 13.4 check_ai_repo_result.py

PR本文やテキストファイル内に `AI_REPO_RESULT` ブロックが存在し、必須項目が含まれるかを検査する。

### 13.5 check_tsk_index_consistency.py

改善タスク課題一覧と個別 `TSK-*.md` の整合を簡易確認する。

初回は過度に複雑にせず、以下を確認する程度でよい。

- `TSK-001.md` が存在する
- `改善タスク課題一覧.md` に `TSK-001` が含まれる
- 状態分類の表記が想定値内である

---

## 14. GitHub Actions 作成方針

`.github/workflows/template-checks.yml` を作成する。

### 14.1 実行タイミング

    pull_request
    push

### 14.2 実行内容

以下を実行する。

    python scripts/test_codex_prompt_git_safety.py
    python scripts/check_tsk_index_consistency.py
    python scripts/check_ai_repo_result.py .github/PULL_REQUEST_TEMPLATE.md

### 14.3 注意事項

- Java / Maven / Gradle / Docker は使わない
- Spring Boot アプリのビルドは行わない
- テンプレート文書と運用ルールの初期整合確認に限定する
- 依存ライブラリは標準ライブラリのみを前提とする
- OS は `ubuntu-latest` でよい

---

## 15. PRテンプレート作成方針

`.github/PULL_REQUEST_TEMPLATE.md` を作成する。

含める内容は以下とする。

- 概要
- 変更内容
- 変更対象外
- 確認内容
- 未確認事項
- AI_REPO_RESULT ブロック

PRテンプレート内の AI_REPO_RESULT は、`check_ai_repo_result.py` の初期検査に通る形式にする。

---

## 16. Q-Scout固有語の扱い

テンプレート本体には、以下の Q-Scout 固有語を原則として残さない。

- Q-Scout
- qscout
- Springコード品質スコアリングAI
- 6ルール
- qscout-report.md
- qscout-ai-input.md
- ControllerToRepositoryDirectAccessRule
- FieldInjectionRule
- TransactionalMisuseRule
- ExceptionSwallowingRule
- MissingTestRule
- PackageDependencyViolationRule

ただし、由来説明として必要な場合のみ、以下のように明記する。

    本テンプレートは、別リポジトリである Q-Scout-for-Spring の開発運用基盤を参考に汎用化したものである。

この場合も、Q-Scout 固有の診断仕様や実装内容は記載しない。

---

## 17. 変数化方針

テンプレート文書では、プロダクト固有値を以下の変数で表現する。

    {{PROJECT_NAME}}
    {{PRODUCT_NAME}}
    {{REPOSITORY_FULL_NAME}}
    {{DEFAULT_BRANCH}}
    {{PRODUCT_CATEGORY}}
    {{TOOL_DESCRIPTION}}
    {{TARGET_FRAMEWORK}}
    {{PRIMARY_RUNTIME}}
    {{BUILD_TOOL}}
    {{MAIN_APPLICATION_CLASS}}
    {{CLI_MAIN_CLASS}}
    {{HUMAN_REPORT_FILE}}
    {{AI_INPUT_FILE}}
    {{PUBLIC_SCOPE}}
    {{PRIVATE_SCOPE}}

各テンプレート文書には、必要に応じて「この文書を新規プロダクトへ適用する場合は上記変数を置換する」と明記する。

---

## 18. 検証手順

Codex はファイル生成後、以下を確認する。

### 18.1 ファイル構成確認

以下を確認する。

- README.md が存在する
- PROJECT_START_PROMPT_TEMPLATE.md が存在する
- docs/README.md が存在する
- 改善タスク管理ファイルが存在する
- scripts が存在する
- .github/PULL_REQUEST_TEMPLATE.md が存在する
- .github/workflows/template-checks.yml が存在する

### 18.2 Q-Scout固有語確認

以下の文字列が残っていないか確認する。

    qscout
    qscout-report
    qscout-ai-input
    ControllerToRepositoryDirectAccessRule
    FieldInjectionRule
    TransactionalMisuseRule
    ExceptionSwallowingRule
    MissingTestRule
    PackageDependencyViolationRule

`Q-Scout-for-Spring` は由来説明として1回程度残してよいが、診断仕様として残してはならない。

### 18.3 スクリプト確認

以下を実行する。

    python scripts/test_codex_prompt_git_safety.py
    python scripts/check_tsk_index_consistency.py
    python scripts/check_ai_repo_result.py .github/PULL_REQUEST_TEMPLATE.md

### 18.4 GitHub Actions確認

ローカルで YAML の厳密検証までは不要。

ただし、以下を目視確認する。

- `on: [push, pull_request]` または同等の設定がある
- Python セットアップがある
- scripts の3系統チェックが実行される
- Java / Maven / Docker が含まれていない

---

## 19. PR作成方針

Codex は作業完了後、PRを作成する。

PRタイトル：

    docs: initialize Spring tool development template

PR本文には以下を含める。

- 概要
- 変更内容
- 変更対象外
- 確認内容
- 未確認事項
- AI_REPO_RESULT ブロック

AI_REPO_RESULT の TASK_ID は以下とする。

    TPL-003

---

## 20. Codex実行後の報告形式

Codex は、実行完了後に以下の形式で報告する。

    ===== AI_REPO_RESULT_BEGIN =====
    ACTOR: Codex
    TASK_ID: TPL-003
    TITLE: Spring開発基盤テンプレートリポジトリ初期生成
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

---

## 21. 成功条件

本作業の成功条件は以下とする。

1. `Spring-Tool-Development-Template` にフルセット構成が生成されている
2. 実装コード雛形が含まれていない
3. Private リポジトリ前提の説明になっている
4. GitHub Actions が最初から含まれている
5. 初回適用候補が未定として扱われている
6. Q-Scout固有の診断仕様が残っていない
7. scripts が初期配置されている
8. PRテンプレートに AI_REPO_RESULT が含まれている
9. テンプレート利用者が README から使い方を理解できる
10. Codex実行結果が PR 本文またはコメントに証跡として残っている

---

## 22. 最終指示

Codex は、本書に従って `Spring-Tool-Development-Template` リポジトリの初期テンプレート構成を生成すること。

今回の主目的は、Spring 系ツール開発のための **開発運用基盤テンプレート** を作ることである。

実装コード、プロダクト固有仕様、Q-Scout固有診断ロジック、Spring Bootアプリ雛形は作成しない。

以上を、TPL-003 Codex向け Spring開発基盤テンプレート生成指示書 v0.1 とする。
