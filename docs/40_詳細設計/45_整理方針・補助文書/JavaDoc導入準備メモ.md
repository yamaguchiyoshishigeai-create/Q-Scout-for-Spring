# JavaDoc導入準備メモ

## 1. 目的

本メモは、`docs/40_詳細設計/45_整理方針・補助文書/JavaDoc導入方針書.md` を前提として、
今回の導入準備フェーズで先に着手する対象、`package-info.java` の追加対象、および JavaDoc 記述粒度を固定するための補助文書である。

今回は全面導入ではなく、最小成功パターンの確立を優先する。

---

## 2. 試行導入対象クラス

今回の試行導入対象クラスは、以下の 5 クラスとする。

### 2.1 最優先

- `SharedAnalysisService`
- `WebAnalysisService`
- `CliApplication`

### 2.2 次点

- `WebPageController`
- `WebPreviewController`

現行確認時点では、上記 5 クラスに責務境界を明示するクラス JavaDoc および主要 public メソッド JavaDoc は未整備である。

---

## 3. package-info.java 導入対象

今回、`package-info.java` を追加する対象 package は、以下の 3 package とする。

- `com.qscout.spring.web.controller`
- `com.qscout.spring.web.service`
- `com.qscout.spring.application`

いずれも `docs/30_基本設計/基本設計.md` における責務分割と対応づけやすく、JavaDoc 導入の起点として妥当である。

---

## 4. 今回の記述粒度

今回の導入では、クラス JavaDoc と主要 public メソッド JavaDoc の粒度を以下で統一する。

### 4.1 クラス JavaDoc

クラス JavaDoc には、少なくとも以下を含める。

- そのクラスの責務
- 主な利用場面
- 他層との境界
- 短く書ける場合は、担わない責務

テンプレートは、概ね以下に合わせる。

```java
/**
 * （このクラスの責務を一文で示す）
 *
 * <p>（主な利用場面、委譲関係、境界を短く補足する）</p>
 *
 * <p>（必要なら、担わない責務や注意点を短く補足する）</p>
 */
```

### 4.2 public メソッド JavaDoc

主要 public メソッドには、必要な範囲で以下を含める。

- `@param`
- `@return`
- 必要に応じて `@throws`

テンプレートは、概ね以下に合わせる。

```java
/**
 * （そのメソッドが何をするかを簡潔に示す）
 *
 * @param xxx （引数の意味）
 * @return （戻り値の意味）
 * @throws XxxException （必要な場合のみ）
 */
```

ただし、全 public メソッドへ機械的に長文を付与するのではなく、文脈なしでは意味を取りづらい引数・戻り値・例外を優先して説明する。

---

## 5. 今回見送る対象

今回、全面導入を見送る対象は、以下のとおりとする。

- `RequestAccessTokenService`
- `UploadValidationService`
- `DownloadArtifactService`
- `MarkdownPreviewRenderer`
- 主要 DTO 群
- 他の Controller 群
- 代表的な infrastructure サービス群

これらは次フェーズの候補として保持し、今回の記述粒度と文面方針を踏まえて段階展開する。
