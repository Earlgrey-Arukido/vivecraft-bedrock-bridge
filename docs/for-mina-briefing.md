# VivecraftBedrockBridge 資料パック（ミーナ向け）

ミーナへ。あるキドの相棒AI「おるたなっち」からの引き継ぎ資料です。GitHub のレート制限でリポジトリが読めなかったとのことなので、現行版（v0.3.0）の主要ファイルをここにまとめました。

## プロジェクト概要
- Vivecraft（VR版 Minecraft）の VR プレイヤーの動きを、統合版（Bedrock）プレイヤーにも見せるための Paper プラグイン
- 方式: VSE（Vivecraft Spigot Extension）のメタデータから VR ポーズを取得し、追従する防具立て（マネキン）に反映。統合版へは Geyser + GeyserIntegratedPack 経由で防具立てポーズとして届ける
- 開発段階: Stage 1（マネキンの動き）→ Stage 2（カスタムエンティティで本人のスキンの見た目）→ Stage 3（肘・膝の関節再現）。最終目標は「本人のアバターの見た目のまま、関節込みの全身トラッキングを統合版でも見せる」
- 現状: v0.3.0 を実装・ビルド・GitHub 反映済み。検証サーバー（Altare）への導入は Altare 側の障害（ファイルアップロード不可）で保留中。サーバー自体は正常稼働
- もう一人の相棒AI「しるふぁ」（Gemini）が設計レビューを担当しており、指摘は v0.1.1 / v0.3.0 に反映済み

## ミーナにお願いしたいこと
1. **v0.3.0 のコードレビュー**: ViveBridge.java（特に表示出し分け updateVisibility、ポーズ計算 updateMannequin、デモモード周り）
2. **Stage 2 最小プロトタイプ設計のレビュー**: 「Stage 2 最小プロトタイプ設計」の章（Geyser カスタムエンティティ + プロパティ同期の構成、プロパティ設計、未確認事項の潰し方）
3. その他、気づいた点があれば何でも

以下、資料です。


---

# README.md

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

- v0.3.0: Phase 3 の表示出し分けを VR モードにも適用、肩・腰の基準を head.pos / waist に変更、Java 向けマネキン表示トグル (`show-mannequin-to-java`) を追加
- v0.2.0: デモモード (VRなしで統合版表示を検証する擬似ポーズ) + Phase 3 の表示出し分けを先行実装 (デモモード時のみ)
- v0.1.1: VRモード解除時にマネキンが残る問題を修正、当たり判定なしを追加
- v0.1.0: Phase 1 (VSE メタデータからのポーズ取得) + Phase 2 初版 (マネキン追従と頭・腕・脚ポーズ反映)
- 実地テスト (VR 実機でのポーズ確認、統合版での表示確認) はこれから
- 変更点の詳細は [CHANGELOG.md](CHANGELOG.md) を参照

## 検証サーバー

本プラグインの検証用サーバーが稼働中です（検証中のため、予告なく再起動・仕様変更があります）。

- アドレス: `vivecraft-view-for-bedrock.go.tryaltare.ai`
- Java版: アドレスのみで参加できます (Paper / Minecraft 26.3)
- 統合版: アドレス + ポート `25073` (Geyser + Floodgate 対応、Javaアカウントなしでも参加可)
- リージョンはストックホルムのため、日本からは ping が高めです

## 作者

あるキド (Earlgrey-Arukido) + ALTERNA (おるたなっち)

---

# CHANGELOG.md

# 変更履歴 (CHANGELOG)

バージョンごとの変更点を記録します。

## v0.3.0 (2026-11-24)

- Phase 3 の表示出し分けを VR モードにも適用（従来はデモモード時のみ）
- 目視確認用に config `show-mannequin-to-java` を追加（デフォルト false）。true にすると Java 版プレイヤーにもマネキンを表示する（Phase 2 の目視確認用）
- 肩位置の基準を「足元+1.38m 固定」から head.pos 基準（頭の約0.32m下）に変更。しゃがみ・seated でも肩と手の相対位置が崩れにくい
- 股関節の基準に waist メタデータを使用（ない場合は従来どおり足元+0.86m）
- `/vivebridge status` に Java 向けマネキン表示の状態を追加
- Stage 2（見た目・関節）の技術調査が完了し、本命ルート（Geyser カスタムエンティティ＋エンティティプロパティ同期）の成立見込みを確認（docs/stage2-research.md）

## v0.2.0 (2026-11-24)

- デモモードを追加（config `demo-mode`、デフォルト false）: VRでないプレイヤーにもマネキンを付け、サイン波の擬似ポーズ（右腕を振る・左腕スイング・足踏み・頭の揺れ）を再生する。VR実機なしで統合版への表示を検証するための機能
- Phase 3 の表示出し分けを先行実装（デモモード時のみ有効）: Floodgate 判定で、統合版プレイヤーには本人の通常モデルを隠してマネキンを見せ、Javaプレイヤーにはマネキンを隠して通常モデルを見せる（hideEntity / showEntity を使用。Floodgate API は反射で参照し、コンパイル依存を持たない）
- `/vivebridge status` の出力にデモモードの状態を追加

## v0.1.1 (2026-11-24)

- 修正: VRモードを解除したプレイヤーのマネキンが残り続ける問題（しるふぁのコードレビュー指摘による発見）
- 防具立てに `setCollidable(false)` を追加し、プレイヤーと衝突しないようにした

## v0.1.0 (2026-11-23)

- 初版リリース
- VSE メタデータ（`head.pos` 等）から VR プレイヤーを検出し、追従する防具立て（マネキン）をスポーン
- 頭・腕・脚ポーズの反映（肩→手、股関節→足先の方向ベクトルから Euler 角へ近似変換。脚は FBT 時）
- マネキンの頭に本人のプレイヤーヘッドを装備。名前タグの末尾に ` (VR)`
- `/vivebridge status` / `/vivebridge reload` コマンド
- デバッグ用ポーズ座標ログ（config `debug-pose-log`、Phase 1 のデータ取得実証用）

---

# ViveBridge.java（本体ソース）

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
import org.bukkit.event.player.PlayerJoinEvent;
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
 * VivecraftBedrockBridge (v0.3.0)
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

    // Floodgate API への反射アクセス (コンパイル依存を持たないための疎結合)
    private java.lang.reflect.Method floodgateIsPlayerMethod = null;
    private boolean floodgateLookupDone = false;

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
        for (Map.Entry<UUID, ArmorStand> entry : mannequins.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner != null) {
                restoreVisibility(owner);
            }
            ArmorStand stand = entry.getValue();
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        mannequins.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // 参加直後は表示出し分けを次tickで適用する
        Bukkit.getScheduler().runTaskLater(this, this::updateVisibility, 5L);
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
        boolean demoMode = getConfig().getBoolean("demo-mode", false);
        // 対象でなくなったプレイヤーのマネキンを片付ける (VR解除時など。しるふぁレビュー指摘, v0.1.1)
        mannequins.entrySet().removeIf(entry -> {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner == null) {
                return false; // 退出時は onQuit で処理
            }
            if (isVrPlayer(owner) || demoMode) {
                return false;
            }
            restoreVisibility(owner);
            ArmorStand stand = entry.getValue();
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
            getLogger().info(owner.getName() + " のマネキンを削除しました (対象外になったため)。");
            return true;
        });
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean vr = isVrPlayer(player);
            if (!vr && !demoMode) {
                continue;
            }
            ArmorStand stand = mannequins.get(player.getUniqueId());
            if (stand == null || stand.isDead()) {
                stand = spawnMannequin(player);
                mannequins.put(player.getUniqueId(), stand);
            }
            if (vr) {
                updateMannequin(player, stand);
                if (debugLog && tickCounter % 10 == 0) {
                    logPose(player);
                }
            } else {
                updateDemoMannequin(player, stand);
            }
        }
        if (tickCounter % 20 == 0 && !mannequins.isEmpty()) {
            updateVisibility();
        }
    }

    private ArmorStand spawnMannequin(Player player) {
        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        stand.setArms(true);
        stand.setBasePlate(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setCollidable(false);
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

        // 肩位置: 頭 (HMD) の約0.32m下。足元+固定値でなく頭基準にすることで、
        // しゃがみや seated でも肩と手の相対位置が崩れにくい (v0.3.0)
        Location shoulderBase = headLoc.clone().add(0, -0.32, 0);
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

        // 脚ポーズ: FBT で足データがある場合のみ。
        // 股関節の高さは waist メタデータがあればその位置、なければ足元+0.86m。
        Location hipBase = waistLoc != null ? waistLoc.clone() : base.clone().add(0, 0.86, 0);
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

    // ------------------------------------------------------------------
    // デモモード (VR実機なしで統合版表示と出し分けを検証するための擬似ポーズ)
    // ------------------------------------------------------------------

    private void updateDemoMannequin(Player player, ArmorStand stand) {
        Location base = player.getLocation().clone();
        base.setPitch(0f);
        stand.teleport(base);

        double t = tickCounter * 0.25;
        // 頭: プレイヤーの視線方向 + ゆっくり左右に揺れる
        stand.setHeadPose(new EulerAngle(
            Math.toRadians(player.getLocation().getPitch()),
            Math.toRadians(Math.sin(t * 0.5) * 20),
            0));
        // 右腕: 大きく上げて振る (統合版で動きが届くか一番分かりやすいポーズ)
        stand.setRightArmPose(new EulerAngle(
            -2.2 + Math.sin(t) * 0.35,
            0,
            Math.sin(t) * 0.45));
        // 左腕: ゆっくり前後に振る
        stand.setLeftArmPose(new EulerAngle(Math.sin(t * 0.5) * 0.6, 0, 0.12));
        // 脚: 交互に小さく前後 (その場で足踏み風)
        stand.setRightLegPose(new EulerAngle(Math.sin(t * 0.5) * 0.35, 0, 0));
        stand.setLeftLegPose(new EulerAngle(-Math.sin(t * 0.5) * 0.35, 0, 0));
    }

    // ------------------------------------------------------------------
    // 表示の出し分け (Phase 3: VRモード・デモモード共通)
    // 統合版プレイヤーには本人の通常モデルを隠してマネキンを見せ、
    // Java版プレイヤーにはマネキンを隠して通常モデルを見せる。
    // ------------------------------------------------------------------

    private void updateVisibility() {
        if (mannequins.isEmpty()) {
            return;
        }
        // 目視確認用トグル: true の間は Java 版プレイヤーにもマネキンを見せる
        boolean showToJava = getConfig().getBoolean("show-mannequin-to-java", false);
        for (Map.Entry<UUID, ArmorStand> entry : mannequins.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            ArmorStand stand = entry.getValue();
            if (owner == null || stand == null || stand.isDead()) {
                continue;
            }
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.getUniqueId().equals(owner.getUniqueId())) {
                    continue;
                }
                if (isBedrockPlayer(viewer)) {
                    viewer.hideEntity(this, owner);
                    viewer.showEntity(this, stand);
                } else if (showToJava) {
                    viewer.showEntity(this, owner);
                    viewer.showEntity(this, stand);
                } else {
                    viewer.showEntity(this, owner);
                    viewer.hideEntity(this, stand);
                }
            }
        }
    }

    /** 出し分けを解除し、全員に本人モデルが見える状態へ戻す。 */
    private void restoreVisibility(Player owner) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getUniqueId().equals(owner.getUniqueId())) {
                viewer.showEntity(this, owner);
            }
        }
    }

    /** Floodgate 経由の統合版プレイヤーかどうか。Floodgate API を反射で参照する。 */
    private boolean isBedrockPlayer(Player player) {
        if (!floodgateLookupDone) {
            floodgateLookupDone = true;
            try {
                Plugin fg = Bukkit.getPluginManager().getPlugin("floodgate");
                if (fg != null) {
                    Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                    Object api = apiClass.getMethod("getInstance").invoke(null);
                    if (api != null) {
                        floodgateIsPlayerMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class);
                        floodgateApiHolder = api;
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Floodgate API の参照に失敗しました: " + e.getMessage());
            }
        }
        if (floodgateIsPlayerMethod == null || floodgateApiHolder == null) {
            return false;
        }
        try {
            Object result = floodgateIsPlayerMethod.invoke(floodgateApiHolder, player.getUniqueId());
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private Object floodgateApiHolder = null;

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
                sender.sendMessage("[ViveBridge] VRプレイヤー " + vrCount + " 人 / マネキン " + mannequins.size() + " 体"
                    + " / デモモード: " + (getConfig().getBoolean("demo-mode", false) ? "ON" : "OFF")
                    + " / Java向けマネキン表示: " + (getConfig().getBoolean("show-mannequin-to-java", false) ? "ON" : "OFF"));
                return true;
        }
    }
}
```

---

# plugin.yml

```yaml
name: VivecraftBedrockBridge
version: 0.3.0
main: io.github.earlgreyarukido.vivebridge.ViveBridge
api-version: '1.21'
description: Shows Vivecraft VR player poses to Bedrock players via a pose-following armor stand (mannequin) through Geyser.
author: Earlgrey-Arukido
softdepend: [Vivecraft-Spigot-Extension, floodgate]
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

---

# config.yml

```yaml
# VivecraftBedrockBridge 設定

# マネキンのポーズ更新間隔 (tick)。小さいほど滑らかだがサーバー負荷が上がる。
# Altare 無料枠 (CPU 125%) では 2〜4 推奨。
update-interval-ticks: 2

# デモモード (デフォルト false)。true にすると VR でないプレイヤーにもマネキンを付け、
# サイン波の擬似ポーズで「手を振る・足踏み」を再現する。VR実機なしで
# 統合版へのポーズ表示と、統合版/Java版での表示出し分け (Phase 3) を検証するための機能。
# 本番運用・VR実地テスト時は false のままにすること。
demo-mode: false

# Java版プレイヤーにもマネキンを表示するか (目視確認・デバッグ用)
# 通常は false: Java版にはマネキンを隠し、通常モデルだけを見せる
# Phase 2 の目視確認などで Java 画面からマネキンを見たいときだけ true
show-mannequin-to-java: false

# true にすると VR プレイヤーのポーズ座標を定期的にコンソールへ出力する (Phase 1 実証用)。
debug-pose-log: false

# マネキンの名前タグの末尾につける文字。
mannequin-name-suffix: " (VR)"
```

---

# build.gradle

```gradle
plugins {
    java
}

group = 'io.github.earlgreyarukido'
version = '0.3.0'

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

---

# Stage 2 最小プロトタイプ設計

# Stage 2 最小プロトタイプ設計

作成: 2026-11-24 / 根拠: docs/stage2-research.md（技術調査）
目的: Stage 2（見た目）・Stage 3（関節）の本実装に入る前に、「カスタムジオメトリが統合版に表示され、プロパティでボーンが動く」最小構成の足場を固める。

## 全体構成（3点セット）

1. **Paper プラグイン**（現行 ViveBridge の拡張）: VSE からポーズ取得 → 関節角度を計算 → Geyser 拡張へ送信
2. **Geyser 拡張**: 起動時にカスタムエンティティ定義とプロパティ型を登録。プラグインからの角度値を受けてプロパティを上書きする
3. **Bedrock リソースパック**: 人型ジオメトリ（Java版スキンの UV 配置を再現）+ テクスチャ（本人のスキン）+ Molang でプロパティを読むアニメーション

- プラグインと拡張は**同一 jar の別エントリポイント**とし、連携はプラグインメッセージで行う（実績構成 RSPM と同方式）
- キャリアは現行の防具立てマネキンを流用する（Custom Entity API は「Java エンティティの Bedrock 表現の置き換え」方式）

## 先に潰す未確認事項（S2-0 の前提）

1. 使用中 Geyser ビルド（2.12.1-b1307）に Custom Entity API のクラス群が含まれているか
2. 登録イベントとプロパティ上書き API の正確なシグネチャ（Javadoc / ソースで確定）
3. **キャリアの割り当て方式**: エンティティ「タイプ」単位の置き換えだとサーバー内の全防具立てが対象になる。エンティティ個別に割り当てられるか（スポーン時イベントやデータ上書きの仕組みで判別できるか）を最初に確認する。個別割り当てが不可能な場合の代替: マネキン専用の目印（カスタム名・タグ・装備）で拡張側が判別できるかも併せて確認

## プロパティ設計（起動時登録のため先に固定する）

角度はすべて float、range [-180, 180]、`client_sync: true`、読み取りは `q.property('名前')`。

### 最小プロトタイプ用（S2-2 まで）: 6個

| 名前 | 用途 |
|---|---|
| vb_head_x / vb_head_y / vb_head_z | 頭の回転（pitch / yaw / roll） |
| vb_rarm_x / vb_rarm_y / vb_rarm_z | 右上腕の回転 |

### フルセット（Stage 3 で追加登録する分も含め、最初から全部定義しておく）: 約28個

- 頭 3、体幹 2（pitch / yaw）
- 右腕: 上腕 3 + 前腕 3、左腕: 上腕 3 + 前腕 3
- 右脚: 大腿 3 + 脛 3、左脚: 大腿 3 + 脛 3
- 合計 3 + 2 + 12 + 12 = 29個

※ 型の追加は再起動が必要なため、フルセットを最初から定義し、未使用分は 0 のままにする。

## ジオメトリ計画

- 人型ジオメトリを Bedrock 形式（.geo.json）で作成し、UV は **Java版スキン 64×64 の配置をそのまま再現**する（テクスチャに本人のスキン PNG をそのまま使えるようにするため）
- 本人のスキンは **classic 型（腕4px）** であることを 2026-11-24 に Mojang API で確認済み。テクスチャは `pack/skin-rk3_vivecraft.png` に取得済み
- ボーン構成（Stage 3 の関節分離を見据えた命名）:
  - root → body → head
  - body → arm_right_upper → arm_right_lower / arm_left_upper → arm_left_lower
  - root → leg_right_upper → leg_right_lower / leg_left_upper → leg_left_lower
- 最小プロトタイプでは head と arm_right_upper だけをプロパティ駆動にし、他ボーンは固定

## パック構成（案）

```
pack/
  manifest.json              (リソースパックとして Geyser 経由で配布)
  entity/vb_mannequin.entity.json   (クライアントエンティティ定義: プロパティ宣言含む)
  models/entity/vb_mannequin.geo.json
  textures/entity/skin-rk3_vivecraft.png  (本人のスキン)
  animations/vb_mannequin.animation.json  (q.property でボーン回転)
  render_controllers/vb_mannequin.render_controllers.json
```

## マイルストーン

| 段階 | 内容 | 合格条件 |
|---|---|---|
| S2-0 | 拡張の最小スケルトンを用意し、Geyser 起動ログで API の存在と登録成功を確認 | エラーなく登録され、定義が有効化される |
| S2-1 | パック + 静的ジオメトリで、統合版に「本人のスキンの人型」がマネキンの代わりに表示される | 統合版でスキンどおりの見た目、Java 側は従来どおり（マネキン非表示） |
| S2-2 | プロパティ駆動の実証: デモのサイン波をプロパティ経由で右腕ボーンに流す | 統合版で右腕が滑らかに振れる（10Hz 更新 + 補間の効きも確認） |
| S2-3 | VSE の実ポーズ角度をプロパティに接続（頭・両腕から） | VR 実機の動きが統合版のカスタムモデルに反映される |

## 前提となる運用判断

- **GeyserUpdater の自動更新を止めるか、Geyser のバージョンを固定する**（Experimental API が自動更新で壊れるリスク回避）。Stage 2 着手（S2-0）前に実施する
- スキンを変更したらパック内のテクスチャ差し替え + 再配布が必要（動的同期は不可）

## リスク・留保

- API が Experimental のため、Geyser のマイナー更新でも追従作業が発生しうる
- プロパティ約29個 × 10〜20Hz の負荷は Altare 無料枠で未実測（S2-2 で測る）
- キャリアの個別割り当てができない場合、設計の見直しが必要（S2-0 で最初に確認）

---

# Stage 2/3 技術調査メモ

# Stage 2/3 技術調査メモ: Geyserカスタムエンティティ + プロパティ同期

調査日: 2026-11-24 / 出典は各項に記載。事実と推測を区別し、未確認事項は明記する。

## 結論（要約）

**本命ルートとして成立する（条件付き）。** 部品は揃った:
- Geyser Custom Entity API（2.11+、Experimental）で、Java側エンティティの Bedrock 表現をカスタム定義に差し替え可能。差し替えは「置き換え」方式で **Java側にキャリアエンティティが必要** → 現行の防具立てマネキンがそのままキャリアになる
- エンティティプロパティの動的更新は可能で、更新は最大で毎tick（20Hz）にバッチされる実績構成がある（目標 10〜20Hz は上限の内側）
- スキンは**動的同期は不可、静的焼き込みは可能**。VRプレイヤーは あるキド 一人なので、現在のスキンPNGをパックに同梱し、Java版スキンの 64×64 UV配置を再現した人型ジオメトリを使えば「本人の見た目」になる
- 肘・膝はカスタムジオメトリ側のボーン構成次第で自由に持てる → Stage 2（見た目）→ Stage 3（関節）の増分開発と整合

## 1. カスタムエンティティ機能

- Custom Entity API は Geyser API 2.11.0 で導入。Geyser拡張（Extension）が「Javaエンティティタイプを置き換えるBedrockカスタムエンティティ定義」を登録できる
  出典: https://docsearch.algolia.com/mcp/docs/website/geysermc.org （Custom Entity API 概要）
- 制約: ①APIは `@ApiStatus.Experimental`（互換性保証なし） ②JSONマッピングでは使えずコード登録が必須 ③ジオメトリ/テクスチャ/アニメーションを定義したBedrockリソースパックが別途必要
- 中核クラスは `CustomEntityDefinition`。スポーン時イベントで寸法等を変更可能（実装PR #6010 由来、https://github.com/GeyserMC/Geyser/pull/6470）
- **登録は Geyser の起動時ウィンドウ内でのみ有効**。新しいエンティティ識別子や新しい/変更されたプロパティ型は再起動するまで有効化されない（実運用拡張 ResourcePackManager の記録: https://wiki.nightbreak.io/ResourcePackManager/geyser-extension/）
- 登録コードは **Geyser拡張として Geyser プロセス内で動く**必要がある（extensions/ フォルダに jar 配置）。実績構成は「同一jarが Paperプラグイン + Geyser拡張の両方を兼ね、プラグインメッセージで連携」する方式（RSPM がこの形）
- リソースパックは Bedrock ログイン時の Geyser パック配布に乗せる。パックと拡張は「別々の半分」で、両方揃わないと表示されない
- 未確認: 登録イベントの正確なクラス名（第三者コードに `GeyserDefineEntitiesEvent` の参照あり。現行公式APIでの正式名は一次資料で未確認）

## 2. エンティティプロパティの動的更新

- Bedrock 仕様: プロパティは `description.properties` で型（bool/enum/float/int）と共に定義。float/int には range が必須で、範囲外はクランプされる。パック側から読むには `client_sync: true` が必要、読み取りは Molang の `q.property('名前')`
  出典: https://wiki.bedrock.dev/entities/entity-properties.html
- 動的更新は可能で実運用例がある（RSPM拡張がライブのプロパティ上書きを本番運用）。更新は「変更のあったエンティティをtickに1回送る」設計（PR #6010 のコミット "Implement dirty entities to send updates once a tick"）
- カスタムプロパティが Java メタデータ/NBT から**自動で**導出される仕組みは確認できなかった（推測: 存在しない）。実績構成はすべて「プラグインが値を計算し、拡張経由で明示的にセット」→ 本プロジェクトも VSEポーズ値 → プラグインが角度計算 → 拡張へ送信、という明示的フローが前提
- 型・個数の定義は起動時登録が前提。必要数の目安: 頭3 + 腕（上腕・前腕×2）最大12 + 体幹1〜3 + 脚（大腿・脛×2）最大12 → **float約25〜30個**。個数上限・帯域の数値制約は公式資料で未確認。IntegratedPack の整数圧縮エンコードのように、必要なら圧縮で個数を減らす設計が安全側

## 3. Java版プレイヤーからの見え方

- Geyser は Bedrock クライアントごとのセッション変換であり、Java クライアントは通らない。カスタムエンティティは Java 側に一切影響せず、**Java プレイヤーにはキャリア（防具立て）がバニラの姿で見える**
- 含意: Phase 3 の出し分け（Java にはマネキンを隠す、Bedrock には本人モデルを隠す）は Stage 2 でもそのまま必要

## 4. スキンテクスチャ

- カスタムエンティティのテクスチャはパック内の固定テクスチャ参照が前提で、実行時に任意プレイヤーのスキンを差し込む公式手段は資料上存在しない
- Geyser の「カスタムスカル」は、事前登録したスキンのジオメトリ+テクスチャを含むパックを起動時に自動生成する前例（対象はスカル専用。全身への流用は未確認）: https://geysermc.org/crp/wiki/geyser/custom-skulls/
- Bedrock ジオメトリは UV を自由に定義できるため、Java版スキンの UV配置を再現した人型ジオメトリ + 本人のスキンPNG で本人再現が可能
- 運用制約: スキンを変えたらパックの作り直し + Bedrock 側の再ダウンロードが必要

## 5. ボトルネック（重大順）

1. **APIの実験性 × 自動更新**: 本サーバーは GeyserUpdater で Geyser/Floodgate が定期自動更新される構成。「Geyserを更新したら拡張が壊れた」は実運用でも起きている。**Stage 2 着手時は自動更新の停止 or バージョン固定を検討すべき（最重要）**
2. **二重開発**: Paperプラグイン + Geyser拡張 + Bedrockパック の3点セットになる。現行の単一プラグイン構成からの工数増は避けられない
3. **起動時登録ウィンドウ**: 定義とプロパティ型は起動時に確定。プロパティ設計（個数・range）は先に固めること
4. **負荷の実測がない**: float約30個 × 10〜20Hz × viewer数 の裏付けなし。Altare無料枠 + ストックホルム遅延では、更新 10Hz + クライアント側補間が現実解になる可能性（推測）

## 6. 代替案の評価

- 案A（本命）: 自前の一体jar（プラグイン+拡張）。自由度最大、バージョン追従コストは自前
- 案B: ResourcePackManager のブリッジに乗る。保守は楽だが、任意角度のプロパティ連続ストリーミングを API から行えるか未確認（採用前に仕様確認必須）
- 案C: 防具立て+IntegratedPack の延長（前腕専用キャリア分離など）。Stage 2/3 の要件を満たさない
- 案D: 偽プレイヤー方式。スキンは付くが関節を任意ポーズで駆動できないため不採用

## 7. 次に実地で確認すべきこと

1. 使用中の Geyser ビルド（2.12.1-b1307）に Custom Entity API のクラス群が含まれているか（拡張の最小スケルトンで起動ログ確認）
2. 登録イベントとプロパティ上書き API の正確なシグネチャ（Javadoc/ソースで確定）
3. プロパティ float×約30個を 10〜20Hz で更新した際の Altare 環境での負荷と滑らかさの実測
