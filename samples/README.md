# samples ディレクトリ運用

`samples/` は、公開 Spring サンプルをローカルに取得し、Q-Scout for Spring の比較評価に利用するための作業ディレクトリです。

## 原則

- 公開サンプル本体のディレクトリは、ローカル取得物として扱い、原則として git 管理対象にしません。
- `samples/` 直下の取得済みサンプル、評価出力、比較サマリ、実行ログは、通常のPRに含めません。
- 管理対象にするのは、この README のような運用説明や、将来必要になったメタ情報に限定します。

## サンプル取得・評価

公開サンプルの取得と評価は、リポジトリルートから以下のスクリプトを実行して行います。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-sample-evaluation-under-samples.ps1
```

## git 管理対象外の例

以下は原則として commit しません。

- `samples/bookstore/`
- `samples/spring-petclinic/`
- `samples/spring-petclinic-microservices/`
- `samples/spring-boot-monolith/`
- `samples/sample-output/`
- `samples/CodexExec.result`
- `samples/sample-comparison-summary.md`

## 注意

公開サンプル実体をレビュー対象に含める必要がある場合は、通常の評価運用とは分離し、目的・範囲・取得元・ライセンス確認方法をPR本文に明記してください。
