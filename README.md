# VivecraftBedrockBridge

日本語 | [English](README.en.md)

**Vivecraft（Java版VR）のプレイヤーの動きを、統合版（Bedrock）のプレイヤーにも見せる**ための Paper プラグインです。

通常、Vivecraft の頭・腕・脚の動きは Vivecraft 同士でしか見えません。Geyser 経由の統合版プレイヤーには通常のプレイヤーモデルしか表示されません。
本プラグインは、Vivecraft Spigot Extension (VSE) の開発者向けメタデータから VR プレイヤーのポーズを読み取り、追従する防具立て（マネキン）に同じポーズを取らせることで、Geyser 公式パック (GeyserIntegratedPack) の「防具立てカスタムポーズ」対応を通じて統合版へ動きを届けることを目指します。

プロジェクトの背景・調査結果・フェーズ計画は [docs/PROJECT.md](docs/PROJECT.md) を参照してください。

## 動作環境

- Paper サーバー (Minecraft 1.21 以降想定。開発サーバーは 26.3)
- [Vivecraft Spigot Extension](https://modrinth.com/project/o8TGTrsR) (VSE)
- 統合版対応には Geyser + Floodgate + GeyserIntegratedPack

## 仕組み

1. VSE が各 VR プレイヤーの `Player` に付けているメタデータ (`head.pos`, `righthand.pos`, `lefthand.pos`, FBT 時は `waist` / `*foot` など) を読み取る
2. VR プレイヤーの位置に防具立て（マネキン）を追従させる
3. 頭の向き・肩→手の方向・腰→足の方向から各部位のポーズ角度を計算してマネキンに反映する（腕・脚は簡易 IK 近似）
4. 統合版クライアントは GeyserIntegratedPack 経由で防具立てのポーズを受け取り、マネキンが VR プレイヤーと同じように動いて見える

ポーズ精度の注意: 統合版へ送られる角度は Geyser 側で 1 度単位に丸められます（手先換算で約 1cm 刻み）。

## コマンド

- `/vivebridge status` — VR プレイヤーとマネキンの状態表示
- `/vivebridge reload` — config 再読み込み (権限: `vivebridge.admin`、デフォルト OP)

## 設定 (config.yml)

- `update-interval-ticks` — ポーズ更新間隔 (デフォルト 2)
- `debug-pose-log` — ポーズ座標の定期ログ出力 (デフォルト false)
- `mannequin-name-suffix` — マネキンの名前タグ末尾 (デフォルト ` (VR)`)

## ビルド

```
./gradlew jar
```

出力: `build/libs/VivecraftBedrockBridge.jar` をサーバーの `plugins/` に入れてください。

## 開発状況

- v0.1.0: Phase 1 (VSE メタデータからのポーズ取得) + Phase 2 初版 (マネキン追従と頭・腕・脚ポーズ反映)
- 実地テスト (VR 実機でのポーズ確認、統合版での表示確認) はこれから

## 検証サーバー

本プラグインの検証用サーバーが稼働中です（検証中のため、予告なく再起動・仕様変更があります）。

- アドレス: `vivecraft-view-for-bedrock.go.tryaltare.ai`
- Java版: アドレスのみで参加できます (Paper / Minecraft 26.3)
- 統合版: アドレス + ポート `25073` (Geyser + Floodgate 対応、Javaアカウントなしでも参加可)
- リージョンはストックホルムのため、日本からは ping が高めです

## 作者

あるキド (Earlgrey-Arukido) + ALTERNA (おるたなっち)
