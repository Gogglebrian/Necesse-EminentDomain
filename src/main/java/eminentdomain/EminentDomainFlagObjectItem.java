package eminentdomain;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.engine.util.Zoning;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.levelData.SingleRegionBasedLevelData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.Settler;

import java.awt.*;
import java.awt.geom.Line2D;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.stream.Collectors;

public class EminentDomainFlagObjectItem extends ObjectItem {
    public EminentDomainFlagObjectItem(EminentDomainFlagObject object) {
        super(object);
        this.itemCooldownTime.setBaseValue(2000);
        this.rarity = Rarity.QUEST;
    }

    public String canPlace(Level level, ObjectPlaceOption po, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
        if (level.isServer() && player != null) {
            Server server = level.getServer();
            SettlementsWorldData settlementsData = SettlementsWorldData.getSettlementsData(server);
            ServerClient client = player.getServerClient();

            try {
                // allow access to NPCVillageRegionData.villageTiles
                Field villageTilesField = OneWorldNPCVillageData.NPCVillageRegionData.class.getDeclaredField("villageTiles");
                villageTilesField.setAccessible(true);

                // allow access to NPCVillageRegionData.villageTiles
                Field villagersField = OneWorldNPCVillageData.NPCVillageRegionData.class.getDeclaredField("villagers");
                villagersField.setAccessible(true);

                // allow access to SingleRegionBasedLevelData.getDataInRegion()
                Class[] parameterTypes = {int.class, int.class};
                Method getDataInRegionMethod = SingleRegionBasedLevelData.class.getDeclaredMethod("getDataInRegion", parameterTypes);
                getDataInRegionMethod.setAccessible(true);

                int regionX = level.regionManager.getRegionCoordByTile(po.tileX);
                int regionY = level.regionManager.getRegionCoordByTile(po.tileY);

                ServerSettlementData serverData = settlementsData.getServerDataAtTile(level.getIdentifier(), po.tileX, po.tileY);
                if (serverData != null) {
                    return "alreadyplayersettlement";
                }

                OneWorldNPCVillageData villageData = OneWorldNPCVillageData.getVillageData(level, false);
                if (villageData == null) {
                    return "errornovillagedata";
                }

                OneWorldNPCVillageData.NPCVillageRegionData regionData = (OneWorldNPCVillageData.NPCVillageRegionData) getDataInRegionMethod.invoke(villageData, regionX, regionY);
                if (regionData == null) {
                    return "alreadyfree";
                }

                PointHashSet villageTiles = (PointHashSet) villageTilesField.get(regionData);
                if (villageTiles == null || villageTiles.isEmpty()) {
                    return "alreadyfree";
                }

                // Now check nearby villagers and catch any who live in the region
                java.util.LinkedList<HumanMob> npcsLivingInRegion = new java.util.LinkedList<HumanMob>();
                java.util.List<Mob> mobs = villageData.getLevel().entityManager.mobs.stream().collect(Collectors.toList());
                for (Mob mob : mobs) {
                    if (mob.isHuman && !mob.isPlayer) {
                        HumanMob human = (HumanMob) mob;
                        if (human.villagerData != null) {
                            if (villageTiles.contains(human.villagerData.tileX, human.villagerData.tileY)
                                    || (level.regionManager.getRegionCoordByTile(human.villagerData.tileX) == regionX
                                    && level.regionManager.getRegionCoordByTile(human.villagerData.tileY) == regionY)) {
                                npcsLivingInRegion.add(human);
                            }
                        }
                    }
                }

                if (!npcsLivingInRegion.isEmpty()) {
                    StringBuilder evictNPCsStringBuilder = new StringBuilder("evict:");
                    int left = npcsLivingInRegion.size();
                    for (HumanMob human : npcsLivingInRegion) {
                        Settler settler = human.getSettler();
                        if (settler != null) {
                            evictNPCsStringBuilder.append(settler.getGenericMobName()).append(' ');
                        }
                        evictNPCsStringBuilder.append(human.getSettlerName());
                        left -= 1;
                        if (left > 0) {
                            evictNPCsStringBuilder.append(", ");
                        }
                    }

                    return evictNPCsStringBuilder.toString();
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "errorexception";
            }
        }

        return super.canPlace(level, po, player, playerPositionLine, item, mapContent);
    }

    public InventoryItem onAttemptPlace(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent, String error) {
        boolean sendUpdate = false;

        if (error.contains("evict:")) {
            player.getServerClient().sendChatMessage(new LocalMessage("misc", "eminentdomainevictlist"));
            String evictListMessage = error.replace("evict:", "");
            player.getServerClient().sendChatMessage(evictListMessage);
            sendUpdate = true;
        }
        else {
            switch (error) {
                case "alreadyplayersettlement":
                    player.getServerClient().sendChatMessage(new LocalMessage("misc", "eminentdomainalreadysettlement"));
                    sendUpdate = true;
                    break;
                case "errornovillagedata":
                    player.getServerClient().sendChatMessage(new LocalMessage("misc", "eminentdomainerrornovillagedata"));
                    sendUpdate = true;
                    break;
                case "alreadyfree":
                    player.getServerClient().sendChatMessage(new LocalMessage("misc", "eminentdomainalreadyfree"));
                    sendUpdate = true;
                    break;
                case "errorexception":
                    player.getServerClient().sendChatMessage(new LocalMessage("misc", "eminentdomainerrorgeneral"));
                    sendUpdate = true;
            }
        }

        if (sendUpdate) {
            ObjectPlaceOption po = this.getPlaceOptionFromMap(mapContent);
            if (po != null) {
                player.getServerClient().sendPacket(new PacketChangeObject(level, 0, po.tileX, po.tileY));
            }

            player.endAttackHandler(false);
            return item;
        } else {
            return super.onAttemptPlace(level, x, y, player, item, mapContent, error);
        }
    }

    public boolean onPlaceObject(GameObject object, Level level, int layerID, int tileX, int tileY, int rotation, ServerClient client, InventoryItem item) {
        boolean success = super.onPlaceObject(object, level, layerID, tileX, tileY, rotation, client, item);

        if (success && level.isServer() && client != null) {
            try {
                // allow access to NPCVillageRegionData.villageTiles
                Field villageTilesField = OneWorldNPCVillageData.NPCVillageRegionData.class.getDeclaredField("villageTiles");
                villageTilesField.setAccessible(true);

                // allow access to NPCVillageRegionData.villageTiles
                Field villagersField = OneWorldNPCVillageData.NPCVillageRegionData.class.getDeclaredField("villagers");
                villagersField.setAccessible(true);

                // allow access to SingleRegionBasedLevelData.getDataInRegion() and setDataInRegion()
                Method getDataInRegionMethod = SingleRegionBasedLevelData.class.getDeclaredMethod("getDataInRegion", int.class, int.class);
                getDataInRegionMethod.setAccessible(true);
                Method setDataInRegionMethod = SingleRegionBasedLevelData.class.getDeclaredMethod("setDataInRegion", int.class, int.class, SingleRegionBasedLevelData.RegionData.class);
                setDataInRegionMethod.setAccessible(true);

                OneWorldNPCVillageData villageData = OneWorldNPCVillageData.getVillageData(level, false);
                if (villageData != null) {
                    int regionX = level.regionManager.getRegionCoordByTile(tileX);
                    int regionY = level.regionManager.getRegionCoordByTile(tileY);
                    OneWorldNPCVillageData.NPCVillageRegionData regionData = (OneWorldNPCVillageData.NPCVillageRegionData) getDataInRegionMethod.invoke(villageData, regionX, regionY);
                    if (regionData != null) {
                        villagersField.set(regionData, new PointHashMap<OneWorldNPCVillageData.NPCVillagerData>()); // wipe region villagers respawn data
                        villageTilesField.set(regionData, new PointHashSet()); // wipe region village tiles
                        setDataInRegionMethod.invoke(villageData, regionX, regionY, regionData);
                    }

                    client.sendChatMessage(new LocalMessage("misc", "eminentdomainsuccess"));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return success;
    }

    public void drawPlacePreview(Level level, int x, int y, GameCamera camera, PlayerMob player, InventoryItem item, PlayerInventorySlot slot) {
        super.drawPlacePreview(level, x, y, camera, player, item, slot);
        String canPlace = this.canPlace(level, x, y, player, (Line2D)null, item, (GNDItemMap)null);
        if (canPlace == null) {
            ObjectPlaceOption po = this.getBestPlaceOption(level, x, y, item, player, (Line2D)null, false);
            if (po != null) {
                int regionX = level.regionManager.getRegionCoordByTile(po.tileX);
                int regionY = level.regionManager.getRegionCoordByTile(po.tileY);
                int regionTileX = level.regionManager.getTileCoordByRegion(regionX);
                int regionTileY = level.regionManager.getTileCoordByRegion(regionY);
                Rectangle levelRectangle = new Rectangle(GameMath.getLevelCoordinate(regionTileX), GameMath.getLevelCoordinate(regionTileY), GameMath.getLevelCoordinate(16), GameMath.getLevelCoordinate(16));
                SharedTextureDrawOptions options = Zoning.getRectangleDrawOptions(levelRectangle, new Color(242, 128, 13), new Color(1.0F, 1.0F, 1.0F, 0.0F), 32, camera);
                options.draw();

                // Add region tooltip
                String regionString = Localization.translate("misc","eminentdomainregion") + ' ' + regionX + ',' + regionY;
                StringTooltips tooltips = new StringTooltips();
                tooltips.add(regionString);
                GameTooltipManager.addTooltip(tooltips, TooltipLocation.INTERACT_FOCUS);
            }
        }
    }
}
