# VivecraftBedrockBridge

[日本語](README.md) | English

**A Paper plugin that lets Bedrock Edition players see the movements of Vivecraft (Java VR) players.**

Normally, a Vivecraft player's head, arm and leg movements are only visible to other Vivecraft players. Bedrock players joining through Geyser just see a regular player model.
This plugin reads VR player poses from the developer metadata provided by the Vivecraft Spigot Extension (VSE), applies them to a following armor stand (a "mannequin"), and uses the Geyser official pack (GeyserIntegratedPack) and its custom armor stand pose support to deliver those movements to Bedrock players.

See [docs/PROJECT.md](docs/PROJECT.md) for the background, research notes and development phases (currently in Japanese only).

## Requirements

- Paper server (Minecraft 1.21 or later; the development server runs 26.3)
- [Vivecraft Spigot Extension](https://modrinth.com/project/o8TGTrsR) (VSE)
- For Bedrock support: Geyser + Floodgate + GeyserIntegratedPack

## How it works

1. Read the metadata VSE attaches to each VR player (`head.pos`, `righthand.pos`, `lefthand.pos`, and with full body tracking also `waist`, `*foot`, etc.)
2. Spawn an armor stand (mannequin) that follows the VR player
3. Compute pose angles for each body part from the head orientation and the joint-to-hand/foot directions (a simple IK approximation for arms and legs) and apply them to the mannequin
4. Bedrock clients receive the armor stand poses through GeyserIntegratedPack, so the mannequin moves just like the VR player

Note on precision: angles sent to Bedrock are rounded to whole degrees by Geyser (roughly 1 cm steps at the hand).

## Commands

- `/vivebridge status` — show VR players and mannequins
- `/vivebridge reload` — reload the config (permission: `vivebridge.admin`, OP by default)

## Configuration (config.yml)

- `update-interval-ticks` — pose update interval (default: 2)
- `debug-pose-log` — periodically log pose coordinates (default: false)
- `mannequin-name-suffix` — suffix for the mannequin's name tag (default: ` (VR)`)

## Building

```
./gradlew jar
```

Put the output `build/libs/VivecraftBedrockBridge.jar` into your server's `plugins/` folder.

## Status

- v0.2.0: Demo mode (synthetic poses to verify Bedrock rendering without VR) + early Phase 3 per-viewer visibility (demo mode only)
- v0.1.1: Fixed mannequins remaining after a player leaves VR mode; mannequins are now non-collidable
- v0.1.0: Phase 1 (reading poses from VSE metadata) + first version of Phase 2 (mannequin following with head/arm/leg poses)
- Live testing (checking poses in VR and on Bedrock) is still to come
- See [CHANGELOG.md](CHANGELOG.md) for details

## Test Server

A public test server for this plugin is running (expect restarts and changes without notice).

- Address: `vivecraft-view-for-bedrock.go.tryaltare.ai`
- Java Edition: join with the address only (Paper / Minecraft 26.3)
- Bedrock Edition: address + port `25073` (via Geyser + Floodgate, no Java account needed)
- The server is located in Stockholm, so ping from Japan is on the higher side

## Authors

Arukido (Earlgrey-Arukido) + ALTERNA
