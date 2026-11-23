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
 * VivecraftBedrockBridge (v0.3.1)
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
    private final Map<UUID, Float> bodyYawLag = new HashMap<>();
    private org.bukkit.scheduler.BukkitTask tickTask = null;
    private int tickCounter = 0;

    // Floodgate API への反射アクセス (コンパイル依存を持たないための疎結合)
    private java.lang.reflect.Method floodgateIsPlayerMethod = null;
    private Object floodgateApiHolder = null;
    private long lastFloodgateAttemptTick = -1000L;

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
        scheduleTickTask();
        getLogger().info("VivecraftBedrockBridge v" + getDescription().getVersion() + " が有効になりました。");
    }

    /** メインループのスケジュールを config の間隔で (再) 登録する。reload 時にも呼ばれる。 */
    private void scheduleTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        int interval = Math.max(1, getConfig().getInt("update-interval-ticks", 2));
        tickTask = Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, interval);
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
        bodyYawLag.remove(event.getPlayer().getUniqueId());
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
        // tickCounter は「実行回数」でなく「経過 tick 数」で数える (ミーナのレビュー指摘, v0.3.1)
        tickCounter += Math.max(1, getConfig().getInt("update-interval-ticks", 2));
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
            bodyYawLag.remove(entry.getKey());
            ArmorStand stand = entry.getValue();
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
            getLogger().info(owner.getName() + " のマネキンを削除しました (対象外になったため)。");
            return true;
        });
        boolean spawnedAny = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean vr = isVrPlayer(player);
            if (!vr && !demoMode) {
                continue;
            }
            ArmorStand stand = mannequins.get(player.getUniqueId());
            if (stand == null || stand.isDead()) {
                stand = spawnMannequin(player);
                mannequins.put(player.getUniqueId(), stand);
                spawnedAny = true;
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
        // 生成直後は即座に出し分けを適用し、新規マネキンや本人モデルが見える時間をなくす
        if (spawnedAny || (tickCounter % 20 == 0 && !mannequins.isEmpty())) {
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
        // デフォルトでは全員から隠し、updateVisibility で見せる相手だけ showEntity する
        // (生成直後に全員へ見えてしまう穴と、本人に自分のマネキンが見える問題の対策。ミーナのレビュー指摘, v0.3.1)
        stand.setVisibleByDefault(false);
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

        // 体の向き: 腰 (FBT時) -> 頭 の優先順で決める。
        // 腰がない場合は頭に即時追従させず、遅れて追従させる (首振りが全身回転になるのを防ぐ。ミーナのレビュー指摘, v0.3.1)
        Location waistLoc = bodyPartPos(player, "waist");
        float bodyYaw;
        if (waistLoc != null) {
            bodyYaw = waistLoc.getYaw();
            bodyYawLag.put(player.getUniqueId(), bodyYaw);
        } else {
            float target = headLoc.getYaw();
            Float prev = bodyYawLag.get(player.getUniqueId());
            if (prev == null) {
                bodyYaw = target;
            } else {
                float delta = normalizeAngle(target - prev);
                float maxStep = 6.0f; // 1回の更新で最大6度まで (間隔2tickなら約60度/秒)
                float step = Math.max(-maxStep, Math.min(maxStep, delta));
                bodyYaw = prev + step;
            }
            bodyYawLag.put(player.getUniqueId(), bodyYaw);
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

        // tickCounter は経過 tick 数。係数は従来 (実行回数ベース) と同じ波の速さになるよう調整
        double t = tickCounter * 0.125;
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
        // 解決できていない間は 100 tick に1回だけ再試行する
        // (getInstance() が null を返す時期に失敗を固定しないため。ミーナのレビュー指摘, v0.3.1)
        if (floodgateIsPlayerMethod == null && tickCounter - lastFloodgateAttemptTick >= 100) {
            lastFloodgateAttemptTick = tickCounter;
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

    /**
     * 関節 (肩/腰) から末端 (手/足) へのワールド方向を、防具立ての部位ポーズ
     * (EulerAngle, ラジアン) に変換する近似。
     * 部位のデフォルト方向は真下 (0,-1,0)。X 回転 = 前後の振り、Z 回転 = 左右の開き。
     * rx の分母は localY 単体でなく hypot(localX, localY) を使う: 腕が水平付近
     * (localY≈0) でも前後角が ±90 度に張り付かず、斜め方向を正しく分解できる
     * (ミーナのレビュー指摘, v0.3.1)。符号の最終確認はデモ/実機で行う。
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
        double rx = Math.atan2(-localZ, Math.hypot(localX, localY));
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
                scheduleTickTask();
                sender.sendMessage("[ViveBridge] config を再読み込みし、更新間隔も再適用しました。");
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
