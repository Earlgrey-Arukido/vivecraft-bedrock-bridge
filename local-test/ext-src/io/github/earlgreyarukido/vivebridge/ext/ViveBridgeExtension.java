package io.github.earlgreyarukido.vivebridge.ext;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * S2-0 skeleton: registers the custom entity definition and the full
 * Stage 2/3 float property set, and swaps the definition for armor
 * stands whose UUID is listed in dataFolder/mannequins.txt
 * (one UUID per line; stands in for the plugin-side registry until
 * the plugin&lt;-&gt;extension bridge is decided).
 */
public class ViveBridgeExtension implements Extension {

    static final Identifier ENTITY_ID = Identifier.of("vivecraftbedrockbridge", "vr_avatar");

    static final String[] PROPERTY_NAMES = {
            "vb_head_x", "vb_head_y", "vb_head_z",
            "vb_torso_x", "vb_torso_y",
            "vb_rarm_x", "vb_rarm_y", "vb_rarm_z",
            "vb_rfore_x", "vb_rfore_y", "vb_rfore_z",
            "vb_larm_x", "vb_larm_y", "vb_larm_z",
            "vb_lfore_x", "vb_lfore_y", "vb_lfore_z",
            "vb_rleg_x", "vb_rleg_y", "vb_rleg_z",
            "vb_rshin_x", "vb_rshin_y", "vb_rshin_z",
            "vb_lleg_x", "vb_lleg_y", "vb_lleg_z",
            "vb_lshin_x", "vb_lshin_y", "vb_lshin_z"
    };

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        logger().info("[ViveBridge-S2-0] extension initialized");
    }

    @Subscribe
    public void onDefineEntities(GeyserDefineEntitiesEvent event) {
        event.register(CustomEntityDefinition.of(ENTITY_ID));
        logger().info("[ViveBridge-S2-0] registered custom entity definition " + ENTITY_ID);
    }

    @Subscribe
    public void onDefineEntityProperties(GeyserDefineEntityPropertiesEvent event) {
        for (String name : PROPERTY_NAMES) {
            event.registerFloatProperty(ENTITY_ID,
                    Identifier.of("vivecraftbedrockbridge", name),
                    -360.0f, 360.0f, 0.0f);
        }
        logger().info("[ViveBridge-S2-0] registered " + PROPERTY_NAMES.length + " float properties");
    }

    @Subscribe
    public void onSpawn(ServerSpawnEntityEvent event) {
        boolean match = loadMannequinUuids().contains(event.uuid());
        logger().info("[ViveBridge-S2-0] spawn uuid=" + event.uuid()
                + " entityId=" + event.entityId()
                + " type=" + event.entityType()
                + " match=" + match);
        if (match) {
            event.definition(CustomEntityDefinition.of(ENTITY_ID));
            logger().info("[ViveBridge-S2-0] definition swapped for uuid=" + event.uuid());
        }
    }

    private Set<UUID> loadMannequinUuids() {
        Set<UUID> uuids = new HashSet<>();
        Path file = dataFolder().resolve("mannequins.txt");
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file)) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        uuids.add(UUID.fromString(line));
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            logger().error("[ViveBridge-S2-0] failed to read mannequins.txt: " + e.getMessage());
        }
        return uuids;
    }
}
