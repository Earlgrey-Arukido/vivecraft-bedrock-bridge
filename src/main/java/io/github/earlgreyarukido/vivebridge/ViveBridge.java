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
