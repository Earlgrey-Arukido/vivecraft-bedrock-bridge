# VivecraftBedrockBridge プロジェクト説明（最初から全部）

しるふぁへ。GitHubの中身が読めなかったとのことなので、プロジェクトの経緯・構成・実装・進捗を全部まとめて送ります。レビューと次のステップの提案をお願いします。

## 0. まず全体像

- **プロジェクト名**: VivecraftBedrockBridge
- **やろうとしていること**: Java版のVRゲーム「Vivecraft」で遊ぶプレイヤーの頭・腕・脚の動きを、統合版（Bedrock）から参加しているプレイヤーにも見えるようにする、Paperサーバー用プラグインの開発
- **なぜ難しいか**: 通常、VivecraftのVRポーズはVivecraftクライアント同士でしか表示されず、Geyser経由の統合版プレイヤーには通常のプレイヤーモデルしか見えない。公開事例を日英で検索したが、同じ試みを公開している人は見つからなかった（世界初挑戦クラスと見ている）
- **体制**: あるキド（企画・意思決定・実地テスト担当）と ALTERNA（Muse、調査・設計・実装・サーバー構築担当）の2人プロジェクト。しるふぁの提案はALTERNAが照合済みで、方針がほぼ一致した
- **開始日**: 2026-11-23

## 1. 最終目標と進め方（決定済み）

最終目標は「本人のアバター（スキン）の見た目のまま、関節（肘・膝）まで含む全身フルトラッキングの動きを、統合版でも見せられるようにする」こと。
ただし一気には無理なので、3要素に割って本人が決めた順序で攻略する。

1. **Stage 1（動き）**: 防具立てマネキン方式で全身の動きを統合版に見せる ← 現在地
2. **Stage 2（見た目）**: 統合版で見える姿を本人のスキンの見た目に寄せる（カスタムエンティティ＋スキンテクスチャ移植が本命）
3. **Stage 3（関節）**: 肘・膝を曲げられるようにする（カスタムエンティティに上腕/前腕・太もも/すねのボーンを持たせて角度を流す）

## 2. サーバー環境（検証サーバー、稼働中）

- ホスティング: Altare 無料枠（メモリ4 GiB / CPU 125% / ディスク40 GiB）、リージョンはストックホルム（東京が満員だったため。日本からはping高め）
- サーバーソフト: Paper 26.3-259（Minecraft 26.3）
- 導入済みプラグイン: Vivecraft Spigot Extension (VSE) 1.3.17-0 / Geyser 2.12.1-b1307（ベータビルド）/ Floodgate 2.2.5-SNAPSHOT / GeyserUpdater（GeyserとFloodgateを12時間ごとに自動更新）
- 接続先: vivecraft-view-for-bedrock.go.tryaltare.ai（Java版はアドレスのみ。統合版は同じアドレス＋ポート25073、Javaアカウントなしで参加可）
- 本プラグインのjarはビルド済みだが、サーバーへの導入はこれから（後述の通り実地テストが新PC待ちのため）

## 3. リポジトリ構成

```
vivecraft-bedrock-bridge/
├─ README.md            … 日本語README（全文は次節）
├─ README.en.md         … 英語README
├─ docs/PROJECT.md      … 計画書（全文は次々節）
├─ build.gradle         … Paper API 1.21.8 を compileOnly、Java 21
├─ settings.gradle
├─ .gitignore
└─ src/main/
   ├─ java/io/github/earlgreyarukido/vivebridge/ViveBridge.java  … 本体（単一ファイル、307行）
   └─ resources/
      ├─ plugin.yml
      └─ config.yml
```

## 4. README.md 全文
```markdown
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
```

## 5. docs/PROJECT.md（計画書）全文
```markdown
# 大型プロジェクト: Vivecraftの動きを統合版にも見せる（2026-11-23開始）

## 目標
Java版Vivecraft（VR）で遊ぶ あるキド の頭・腕の動きを、統合版（Bedrock）から参加しているプレイヤーにも見えるようにする。
現行の標準構成（Paper＋Vivecraft Spigot Extension＋Geyser/Floodgate）では、VivecraftのポーズデータはVivecraftクライアント同士でしか表示できない。本プロジェクトはその壁を自作プラグインで突破する挑戦。

## 最終目標（2026-11-23 本人定義）
**本人のアバター（スキン）の見た目のまま、関節（肘・膝）まで含む全身フルトラッキングの動きを、統合版でも見せられるようにする。**
- 現行のマネキン方式（防具立て）は中間到達点。見た目が人形になる・肘膝が曲がらない、の2点が最終目標との差分
- 最終目標にはカスタムエンティティ＋独自リソースパック、場合によってはGeyser側の仕組みの調査・拡張が要る見込み（Phase 5以降の研究フェーズ）。現時点の部品だけでは確約できない

## 調査で分かったこと（2026-11-23）
1. **データは取れる**: Vivecraft Spigot Extension（VSE）には開発者向けAPIがあり、他のプラグインからVRプレイヤーの全部位のポーズ履歴データにアクセスできる（公式説明）。VSEはGitHubでオープンソース（Vivecraft/Vivecraft-Spigot-Extension）。
2. **統合版への表示経路がある**: Geyser公式の制限事項ページで「Custom armor stand poses（防具立てのカスタムポーズ）」は同梱の **GeyserIntegratedPack** で対応済みと明記されている。同パックの開発者ドキュメントには、防具立ての部位の回転値をBedrock側へエンコードして送る仕組み（回転を整数化＋符号フラグで圧縮）が実装されていることが書かれている。
3. **プレイヤー本人を統合版でだけ隠せる見込み**: Geyser-Spigot経由の統合版プレイヤーはサーバー上では通常のPlayerとして扱われるため、プラグインの表示制御（hidePlayer等）で「統合版からだけ本人の通常モデルを隠し、マネキンを代わりに見せる」構成が理論上可能。

## 基本方針（マネキン方式）
- 自作プラグインがVSE APIから あるキド のポーズ（頭・両手・体の向き）を毎tick読む
- 彼の位置に追従する防具立て（マネキン）にポーズを反映する。頭＝頭部ポーズ、両手の位置→腕ポーズは簡易IK（肩→手の方向と肘の曲がり）で近似する
- 統合版プレイヤーには本人の通常モデルを隠し、マネキンを見せる。Java版（Vivecraft使用者）にはマネキンを隠す（本物が見えているため）
- 見た目の近似: 頭は彼のプレイヤーヘッドを装備、体は色付き防具などで寄せる。スキンそのものの再現は不可

## フェーズ
- **Phase 0 調査**: VSE APIの正確な仕様（GitHub/wiki）とGeyserIntegratedPackのポーズ仕様の読み込み ← 開始済み
- **Phase 1 データ取得実験**: 小さなプラグインでVSE APIから彼のポーズ（頭・両手の座標/回転）を取得し、コンソールに出力して実証する
- **Phase 2 マネキン表示**: 防具立てを彼の位置に追従させ、頭・腕ポーズを反映。Java版フラット画面で見た目を確認
- **Phase 3 統合版検証**: 統合版クライアントからマネキンのポーズが見えるか検証。本人の通常モデルを統合版からだけ隠す処理を入れる
- **Phase 4 仕上げ**: IKの自然さ調整、更新頻度とサーバー負荷（AltareはCPU 125%制限）のバランス、名前表示、配信向け調整

## 前提と制約（正直メモ）
- 実地テスト（Phase 1以降のポーズデータ）は、彼が実際にVRでサーバーに入る必要がある → **新PC完成後**。コードと下調べは先行できる
- マネキン方式のため完全な再現ではない（腕のIKは近似、脚は簡易追従、スキンは近似）
- 統合版側でポーズが見えるのはGeyserIntegratedPackが効いていることが前提（Geyserの標準配布パック）
- Altare無料枠のCPU 125%が律速になる可能性。更新は毎tickではなく間引き前提で設計する
- 先行事例: 2026-11-23に英語・日本語でWeb検索して確認。Vivecraft＋Geyserを併用しているサーバーは複数あるが、VRポーズを統合版に見せる公開事例・プラグインは見つからなかった（見つけた範囲では世界初挑戦クラス）。失敗してもPhase 1の「ポーズデータ取得」自体は他のプラグイン開発に転用できる

## 分担
- 私（ALTERNA）: 調査、プラグインの設計・実装・ビルド、サーバーへの導入
- あるキド: VRでの実地テスト、新PC完成後の検証プレイ、見た目の判断

## 進め方の優先順位（2026-11-23 本人決定: ①→③→②）
最終目標を3要素に割り、以下の順で攻略する。
1. **Stage 1（要素① 動き）**: マネキン方式で全身の動きを統合版に見せる ← 現在地（Phase 0〜4）
2. **Stage 2（要素③ 見た目）**: 統合版で見える姿を本人のアバター（スキン）の見た目に寄せる。カスタムエンティティ＋スキンテクスチャ移植が本命ルート
3. **Stage 3（要素② 関節）**: 肘・膝の関節を曲げられるようにする。Stage 2のカスタムエンティティに上腕/前腕・太もも/すねのボーンを持たせ、角度データを流し込む

## 外部レビュー: しるふぁ（Gemini）提案との照合（2026-11-24）
- しるふぁも独立に同じ2ルート（王道=カスタムジオメトリ+Molang+エンティティプロパティ／妥協案=防具立て+プレイヤーヘッド+本体非表示）に到達し、プロトタイプ先行の順序も一致。当方の設計方針が裏付けられた
- 差分: しるふぁ案のDisplay Entity（ItemDisplay）はGeyserが統合版へ変換しないため届かない。防具立て一択が正解
- Floodgate API（isFloodgatePlayer）での統合版プレイヤー判定＋hidePlayerはPhase 3の想定どおり
```
## 6. 実装の詳細（v0.1.0）

### データ取得の方式
- VSEへの依存は「メタデータ経由の疎結合」。VSEのソース（org.vivecraft.util.MetadataHelper）を直接読んで仕様を確認した
- VSEはVRプレイヤーの Player エンティティに、プラグイン名 "Vivecraft-Spigot-Extension" の所有でメタデータを付けている
  - 部位キー: head / righthand / lefthand / waist / rightfoot / leftfoot / rightknee / leftknee / rightelbow / leftelbow（腰・脚・肘・膝はFBTモード時のみ有効）
  - 各部位: `<部位>.pos` → Bukkit Location（向き付き）、`<部位>.dir` → Vector、`<部位>.rot` → float[]{w,x,y,z} クォータニオン
  - その他: seated（座り）/ height（身長スケール）/ lefthanded / activehand
  - 注意: ソース上、`.rot` の一部が部位によらずHEAD固定に見える箇所があったため、現行実装は `.pos` 中心で組み立てている
- VRプレイヤーの判定は「head.pos メタデータが存在するか」で行う

### マネキンへの反映（2tick間隔）
- VRプレイヤーを検出したら防具立てをスポーン（腕あり・土台なし・無重力・無敵・非永続、頭に本人のプレイヤーヘッド、名前は「名前 (VR)」）
- 体の向き: waist（FBT時）→ head の優先順で yaw を決定し、防具立てをプレイヤー位置へテレポート
- 頭: 体の向きとの相対 yaw と pitch を頭ポーズに設定
- 腕: 肩位置（体の中心から左右±0.24m、高さ1.38m）→ 手位置の方向ベクトルを、体の前方/右方向への射影から Euler 角（X=前後の振り、Z=左右の開き）に近似変換
- 脚: FBTで足データがある場合のみ、股関節（±0.11m、高さ0.86m）→ 足先方向を同じ式で変換
- プレイヤー退出時とプラグイン無効化時にマネキンを除去
- コマンド: /vivebridge status（VRプレイヤーとマネキンの状態）/ reload（config再読込、権限 vivebridge.admin、デフォルトOP）
- デバッグ用に、config の debug-pose-log を true にすると全部位の座標を定期ログ出力（Phase 1のデータ取得実証用）

### ソース全文（ViveBridge.java）

```java
package io.github.earlgreyarukido.vivebridge;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * VivecraftBedrockBridge (v0.1.0)
 *
 * Vivecraft (Java VR) プレイヤーのポーズを Vivecraft Spigot Extension (VSE) の
 * メタデータ経由で読み取り、追従する防具立て（マネキン）に反映するプラグイン。
 *
 * 目的: Geyser + GeyserIntegratedPack の「防具立てカスタムポーズ」対応を使い、
 * 統合版 (Bedrock) プレイヤーにも VR の動きが見えるようにすること。
 * 詳細は docs/PROJECT.md を参照。
 *
 * Phase 1: VSE メタデータからポーズが取得できることの実証 (debug ログ)
 * Phase 2: マネキンの追従と頭・腕・脚ポーズの反映 (本バージョンで初版実装)
 *
 * VSE メタデータ仕様 (VSE ソース org.vivecraft.util.MetadataHelper より):
 *   部位キー: head / righthand / lefthand / waist / rightfoot / leftfoot /
 *             rightknee / leftknee / rightelbow / leftelbow
 *             (手足・腰などは Vivecraft の FBT モードに応じて有効になる)
 *   各部位: "<key>.pos" -> Location (向き付き), "<key>.dir" -> Vector,
 *           "<key>.rot" -> float[]{w, x, y, z} (クォータニオン)
 *   その他: "seated" -> boolean, "height" -> float, "lefthanded" -> boolean
 */
public class ViveBridge extends JavaPlugin implements Listener {

    private static final String VSE_PLUGIN_NAME = "Vivecraft-Spigot-Extension";

    private final Map<UUID, ArmorStand> mannequins = new HashMap<>();
    private int tickCounter = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Plugin vse = Bukkit.getPluginManager().getPlugin(VSE_PLUGIN_NAME);
        if (vse == null) {
            getLogger().warning(VSE_PLUGIN_NAME + " が見つかりません。VRポーズは取得できませんが、プラグインは待機状態で起動します。");
        } else {
            getLogger().info(VSE_PLUGIN_NAME + " " + vse.getDescription().getVersion() + " を検出しました。");
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        int interval = Math.max(1, getConfig().getInt("update-interval-ticks", 2));
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, interval);
        getLogger().info("VivecraftBedrockBridge v" + getDescription().getVersion() + " が有効になりました。");
    }

    @Override
    public void onDisable() {
        for (ArmorStand stand : mannequins.values()) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        mannequins.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ArmorStand stand = mannequins.remove(event.getPlayer().getUniqueId());
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    // ------------------------------------------------------------------
    // VSE メタデータ読み取り
    // ------------------------------------------------------------------

    /** VSE が所有するメタデータ値を返す。なければ null。 */
    private Object vseMeta(Player player, String key) {
        if (!player.hasMetadata(key)) {
            return null;
        }
        MetadataValue fallback = null;
        for (MetadataValue value : player.getMetadata(key)) {
            Plugin owner = value.getOwningPlugin();
            if (owner != null && owner.getName().equals(VSE_PLUGIN_NAME)) {
                try {
                    return value.value();
                } catch (Exception e) {
                    return null;
                }
            }
            if (fallback == null) {
                fallback = value;
            }
        }
        try {
            return fallback != null ? fallback.value() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Location bodyPartPos(Player player, String part) {
        Object v = vseMeta(player, part + ".pos");
        return v instanceof Location ? (Location) v : null;
    }

    private boolean isVrPlayer(Player player) {
        return bodyPartPos(player, "head") != null;
    }

    // ------------------------------------------------------------------
    // メインループ
    // ------------------------------------------------------------------

    private void tick() {
        tickCounter++;
        boolean debugLog = getConfig().getBoolean("debug-pose-log", false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isVrPlayer(player)) {
                continue;
            }
            ArmorStand stand = mannequins.get(player.getUniqueId());
            if (stand == null || stand.isDead()) {
                stand = spawnMannequin(player);
                mannequins.put(player.getUniqueId(), stand);
            }
            updateMannequin(player, stand);
            if (debugLog && tickCounter % 10 == 0) {
                logPose(player);
            }
        }
    }

    private ArmorStand spawnMannequin(Player player) {
        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        stand.setArms(true);
        stand.setBasePlate(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setPersistent(false);
        stand.setCanPickupItems(false);
        stand.setSilent(true);
        stand.setCustomName(player.getName() + getConfig().getString("mannequin-name-suffix", " (VR)"));
        stand.setCustomNameVisible(true);
        // 頭にプレイヤーヘッドを被せて「その人らしさ」を近似する
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta) {
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        stand.getEquipment().setHelmet(head);
        getLogger().info(player.getName() + " のマネキンを生成しました。");
        return stand;
    }

    private void updateMannequin(Player player, ArmorStand stand) {
        Location headLoc = bodyPartPos(player, "head");
        if (headLoc == null) {
            return;
        }

        // 体の向き: 腰 (FBT時) -> 頭 -> プレイヤー本体 の優先順で決める
        Location waistLoc = bodyPartPos(player, "waist");
        float bodyYaw;
        if (waistLoc != null) {
            bodyYaw = waistLoc.getYaw();
        } else {
            bodyYaw = headLoc.getYaw();
        }

        Location base = player.getLocation().clone();
        base.setYaw(bodyYaw);
        base.setPitch(0f);
        stand.teleport(base);

        // 頭ポーズ: 体の向きとの相対角度にする
        float relYaw = normalizeAngle(headLoc.getYaw() - bodyYaw);
        stand.setHeadPose(new EulerAngle(Math.toRadians(headLoc.getPitch()), Math.toRadians(relYaw), 0));

        // 腕ポーズ: 肩位置から手位置への方向を角度に変換 (簡易IK近似)
        // Bukkit の yaw 方向ベクトル (前方) は (-sin yaw, 0, cos yaw)。
        // 右方向は前方を水平に -90 度回した (-cos yaw, 0, -sin yaw)。
        Vector forward = yawToVector(bodyYaw);
        Vector right = new Vector(-Math.cos(Math.toRadians(bodyYaw)), 0, -Math.sin(Math.toRadians(bodyYaw)));

        Location shoulderBase = base.clone().add(0, 1.38, 0);
        Location rightHand = bodyPartPos(player, "righthand");
        Location leftHand = bodyPartPos(player, "lefthand");
        if (rightHand != null) {
            Location shoulder = shoulderBase.clone().add(right.clone().multiply(0.24));
            stand.setRightArmPose(directionToArmPose(shoulder, rightHand, forward, right));
        }
        if (leftHand != null) {
            Location shoulder = shoulderBase.clone().add(right.clone().multiply(-0.24));
            stand.setLeftArmPose(directionToArmPose(shoulder, leftHand, forward, right));
        }

        // 脚ポーズ: FBT で足データがある場合のみ (腰下の付け根から足先への方向)
        Location hipBase = base.clone().add(0, 0.86, 0);
        Location rightFoot = bodyPartPos(player, "rightfoot");
        Location leftFoot = bodyPartPos(player, "leftfoot");
        if (rightFoot != null) {
            Location hip = hipBase.clone().add(right.clone().multiply(0.11));
            stand.setRightLegPose(directionToArmPose(hip, rightFoot, forward, right));
        }
        if (leftFoot != null) {
            Location hip = hipBase.clone().add(right.clone().multiply(-0.11));
            stand.setLeftLegPose(directionToArmPose(hip, leftFoot, forward, right));
        }
    }

    /**
     * 関節 (肩/腰) から末端 (手/足) へのワールド方向を、防具立ての部位ポーズ
     * (EulerAngle, ラジアン) に変換する近似。
     * 部位のデフォルト方向は真下 (0,-1,0)。X 回転 = 前後の振り、Z 回転 = 左右の開き。
     */
    private EulerAngle directionToArmPose(Location joint, Location target, Vector forward, Vector right) {
        Vector d = target.toVector().subtract(joint.toVector());
        if (d.lengthSquared() < 1.0e-6) {
            return EulerAngle.ZERO;
        }
        d.normalize();
        double localX = d.dot(right);       // 右が +
        double localY = d.getY();          // 上が +
        double localZ = d.dot(forward);    // 前が +
        double rx = Math.atan2(-localZ, -localY);
        double rz = Math.atan2(localX, -localY);
        return new EulerAngle(rx, 0, rz);
    }

    private Vector yawToVector(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vector(-Math.sin(rad), 0, Math.cos(rad));
    }

    private float normalizeAngle(float angle) {
        while (angle > 180f) {
            angle -= 360f;
        }
        while (angle < -180f) {
            angle += 360f;
        }
        return angle;
    }

    // ------------------------------------------------------------------
    // Phase 1 実証用ログ
    // ------------------------------------------------------------------

    private void logPose(Player player) {
        StringBuilder sb = new StringBuilder("[pose] ").append(player.getName());
        String[] parts = {"head", "righthand", "lefthand", "waist", "rightfoot", "leftfoot", "rightelbow", "leftelbow"};
        for (String part : parts) {
            Location loc = bodyPartPos(player, part);
            if (loc != null) {
                sb.append(String.format(" %s=(%.2f,%.2f,%.2f)", part, loc.getX(), loc.getY(), loc.getZ()));
            }
        }
        getLogger().info(sb.toString());
    }

    // ------------------------------------------------------------------
    // コマンド
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("vivebridge")) {
            return false;
        }
        String sub = args.length > 0 ? args[0].toLowerCase() : "status";
        switch (sub) {
            case "reload":
                reloadConfig();
                sender.sendMessage("[ViveBridge] config を再読み込みしました。");
                return true;
            case "status":
            default:
                int vrCount = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isVrPlayer(p)) {
                        vrCount++;
                        sender.sendMessage("[ViveBridge] VR: " + p.getName()
                            + " / マネキン: " + (mannequins.containsKey(p.getUniqueId()) ? "あり" : "なし"));
                    }
                }
                sender.sendMessage("[ViveBridge] VRプレイヤー " + vrCount + " 人 / マネキン " + mannequins.size() + " 体");
                return true;
        }
    }
}
```

## 7. 設定ファイル類
### plugin.yml
```yaml
name: VivecraftBedrockBridge
version: 0.1.0
main: io.github.earlgreyarukido.vivebridge.ViveBridge
api-version: '1.21'
description: Shows Vivecraft VR player poses to Bedrock players via a pose-following armor stand (mannequin) through Geyser.
author: Earlgrey-Arukido
softdepend: [Vivecraft-Spigot-Extension]
commands:
  vivebridge:
    description: VivecraftBedrockBridge の状態確認と設定リロード
    usage: /vivebridge [status|reload]
    permission: vivebridge.admin
permissions:
  vivebridge.admin:
    description: VivecraftBedrockBridge の管理コマンドを使える
    default: op
```
### config.yml
```yaml
# VivecraftBedrockBridge 設定

# マネキンのポーズ更新間隔 (tick)。小さいほど滑らかだがサーバー負荷が上がる。
# Altare 無料枠 (CPU 125%) では 2〜4 推奨。
update-interval-ticks: 2

# true にすると VR プレイヤーのポーズ座標を定期的にコンソールへ出力する (Phase 1 実証用)。
debug-pose-log: false

# マネキンの名前タグの末尾につける文字。
mannequin-name-suffix: " (VR)"
```
### build.gradle
```groovy
plugins {
    java
}

group = 'io.github.earlgreyarukido'
version = '0.1.0'

repositories {
    mavenCentral()
    maven {
        name = 'papermc'
        url = 'https://repo.papermc.io/repository/maven-public/'
    }
}

dependencies {
    // Paper API (コンパイルのみ)。Vivecraft Spigot Extension への依存は
    // メタデータ経由の疎結合なので、コンパイル時依存は不要。
    compileOnly 'io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    archiveFileName = 'VivecraftBedrockBridge.jar'
}
```

## 8. 現在の進捗と未検証ポイント（正直な現状）

実装済み:
- v0.1.0 のコード実装と jar ビルド（ビルド成功・中身の検証済み）
- 検証サーバーの構築、VSE/Geyser/Floodgate の導入、ドメイン取得、GitHubでの公開

未検証・未実装（ハマりポイント候補）:
1. **実地テストがまだ一切できていない**。ポーズデータの実証には「本人がVRでサーバーに入る」必要があり、それは新PC完成後になる（現在PCを組み立て中）。そのため jar はサーバー未導入
2. **角度変換の近似が正しいか未検証**。directionToArmPose の Euler 角の符号・軸の対応は机上の計算で、実機で腕が正しい方向を向くかは未確認（Phase 4で調整予定）
3. **統合版で実際にマネキンのポーズが見えるか未検証**（Phase 3）。前提は GeyserIntegratedPack の防具立てポーズ対応で、Geyser公式の制限事項ページに「対応済み」と明記されていることと、同パックの開発者ドキュメントのエンコード仕様（回転を整数度に丸め＋符号フラグで圧縮。精度は手先換算で約1cm刻み）を確認済み、という段階
4. **Phase 3の「統合版からだけ本人モデルを隠す」は未実装**。現行版はマネキンを追加するだけで、全員にマネキンが見える状態。Floodgate API（isFloodgatePlayer）で統合版プレイヤーを判定し、hidePlayer で出し分ける想定
5. **Display Entity は不採用**。Geyserが統合版へ変換しないため届かない、という理由で防具立てに決めた
6. Altare無料枠の CPU 125% が律速になる可能性があり、更新間隔（現行2tick）とのバランスは実測が必要

## 9. レビューしてほしい点

1. 腕・脚の方向→Euler角変換（directionToArmPose）の近似の妥当性。防具立ての部位ポーズの回転順序・符号規約と照らして、明らかな誤りがないか
2. Stage 2（見た目）に向けて: Geyserのカスタムエンティティ＋エンティティプロパティで「角度データを毎tick流し込む」ことは現行Geyserの仕様で現実的か。プロパティの個数・更新頻度・帯域の制約について知見があれば
3. スキンの見た目をカスタムエンティティへ移植するルート（スキン画像の取得、ジオメトリのUV整合）の実現性
4. Phase 3の出し分けは hidePlayer ベースで十分か、パケット制御（PacketEvents等）まで必要になる場面があるか
5. その他、設計全体への指摘・代替案

以上です。よろしくお願いします。
