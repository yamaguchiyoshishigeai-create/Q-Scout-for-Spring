提案名：RENDER_CLOUDFLARE_CF_CONNECTING_IP_HARDENING_V1 実装指示書 v1.0

目的:
Render 本番環境で観測した結果、`request.getRemoteAddr()` は固定少数 IP ではなく Cloudflare 系の複数 IP に変動した。
そのため、固定 IP を `qscout.client-ip.trusted-proxies` に列挙して `X-Forwarded-For` を trusted proxy 限定で採用する現行方式は、
Render / Cloudflare 前提では安定しない。
この問題を解消するため、Render / Cloudflare 前提の恒久対応として、
Cloudflare 経由通信に限って `CF-Connecting-IP` を安全に採用する実装へ改修する。

背景:
- Render 本番観測では `remoteAddr` が `172.68.x.x`、`172.64.x.x`、`172.70.x.x` など複数値へ変動した
- そのため、固定 IP ベタ書き allowlist 方式は不安定
- Render は Cloudflare の DDoS 保護基盤を利用しており、Cloudflare は origin に `CF-Connecting-IP` を付与する
- よって Render / Cloudflare 前提では、Cloudflare から来た通信に限って `CF-Connecting-IP` を採用する方式が妥当

今回の方針:
- `X-Forwarded-For` 主体ではなく `CF-Connecting-IP` 主体へ切り替える
- ただし無条件採用は禁止
- `remoteAddr` が Cloudflare 信頼レンジに属するときだけ `CF-Connecting-IP` を採用する
- それ以外は従来どおり `remoteAddr` を使う
- 観測ログは通常無効のまま残してよい
- Render 本番向けに最小差分で成立させる

対象ファイル:
- src/main/java/com/qscout/spring/web/service/ClientIpResolver.java
- src/main/java/com/qscout/spring/web/service/TrustedProxyPolicy.java
- src/main/resources/application.properties
- src/test/java/com/qscout/spring/web/service/ClientIpResolverTest.java
- 必要最小限なら README.md
- CodexExec.result

実装要件:
1. `ClientIpResolver` に `CF-Connecting-IP` の解決ロジックを追加する
   - ヘッダ名: `CF-Connecting-IP`
   - `remoteAddr` が Cloudflare trusted proxy と判定された場合のみ、この値を採用候補にする
   - `CF-Connecting-IP` が空または不正な IP の場合は fallback する
   - 最終 fallback は `remoteAddr`

2. `TrustedProxyPolicy` を Cloudflare 信頼レンジに対応させる
   - 既存の完全一致のみの `trusted-proxies` 方式ではなく、CIDR レンジ判定を追加する
   - 最小案としては新規設定を追加してもよい
   - 例:
     - `qscout.client-ip.trusted-proxy-cidrs`
   - 既存の完全一致設定を残してもよいが、Render 本番では CIDR 方式を優先できるようにする

3. Cloudflare 信頼レンジは設定値で持つ
   - ハードコード最小化
   - `application.properties` には安全側デフォルトを置く
   - 本番用の実値投入を見越した構造にする
   - 既存の `qscout.client-ip.trust-forwarded-headers` / `trusted-proxies` をどう扱うかは整理してよいが、
     Render / Cloudflare 前提の恒久対応として意味が通ることを優先する

4. `X-Forwarded-For` は Render 本番恒久方式の主軸から外す
   - 必要なら後方互換のため残してよい
   - ただし Render / Cloudflare モードでは `CF-Connecting-IP` を優先すること

5. テストを追加・更新する
   - Cloudflare trusted range に含まれる `remoteAddr` + 正常な `CF-Connecting-IP` → `CF-Connecting-IP` 採用
   - Cloudflare trusted range に含まれない `remoteAddr` + `CF-Connecting-IP` あり → `remoteAddr` を維持
   - `CF-Connecting-IP` が malformed → fallback
   - 既存 fallback 挙動維持
   - 可能なら CIDR 判定単体も確認

6. README 更新は最小限
   - 必要なら Render 本番では Cloudflare 経由前提で client IP を解決する旨を短く追記
   - 長文化は不要

設計上の優先順位:
1. 安全性
2. Render 本番での安定性
3. 最小差分
4. 後方互換

禁止事項:
- Cloudflare / Render 前提なのに `CF-Connecting-IP` を無条件で信頼すること
- 固定少数 IP を再び `trusted-proxies` にベタ書きして解決しようとすること
- `remoteAddr` fallback を失うこと
- 観測ログを本番デフォルトで有効化すること
- アプリ本体と無関係な大規模リファクタ
- Render 設定変更まで勝手に広げること

確認項目:
1. `mvnw.cmd test` が通る
2. Cloudflare trusted range 内外で解決挙動が変わること
3. malformed header で安全側 fallback すること
4. 既存のレート制限キーが client IP 解決結果をそのまま利用できること
5. README を更新した場合、説明が過剰でないこと

CodexExec.result 記録方針:
- TASK_ID は `RENDER_CLOUDFLARE_CF_CONNECTING_IP_HARDENING_V1`
- 実行結果、変更ファイル、テスト結果、PR URL を記録する
- PR 優先確認運用に従うこと

コミットコメント:
security: Render Cloudflare 向け client IP 解決を恒久化

完了条件:
- Render / Cloudflare 前提で fixed IP allowlist 依存を脱却できている
- Cloudflare trusted range に限って `CF-Connecting-IP` を採用できる
- fallback が維持されている
- テストが通っている
- CodexExec.result 記録と PR 作成が完了している
