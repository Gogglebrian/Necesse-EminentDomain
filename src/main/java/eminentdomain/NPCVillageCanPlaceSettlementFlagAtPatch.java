package eminentdomain;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.level.maps.levelData.LevelData;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.levelData.SingleRegionBasedLevelData;
import necesse.level.maps.levelData.settlementData.SettlementBoundsManager;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.stream.Collectors;

import necesse.level.maps.levelData.settlementData.settler.Settler;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(target = OneWorldNPCVillageData.class, name = "canPlaceSettlementFlagAt", arguments = {int.class, int.class, int.class})
public class NPCVillageCanPlaceSettlementFlagAtPatch {

    // Kinda gross ways of interfacing with the other patched methods allowing their patches to remain minimally invasive
    public static ServerClient latestRequester; /// player (ServerClient) requesting settlement placement or expansion
    public static String latestRegionsListMessage;

    /// Sends the most recent NPCs list to the latest requester.
    /// The point of this is to let us build the result message during the check, but SEND the message
    /// AFTER the calling method has sent its own message (and without having to deeply patch those methods).
    public static void sendNPCsMessageToRequesterIfAny() {
        if (latestRegionsListMessage != null && latestRequester != null) {
            latestRequester.sendChatMessage(new LocalMessage("misc","eminentdomainregionsclaimed"));
            latestRequester.sendChatMessage(new StaticMessage(latestRegionsListMessage));
        }

        latestRequester = null;
        latestRegionsListMessage = null;
    }

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onEnter() {

        // Skip the original method body
        return true;
    }

    @Advice.OnMethodExit
    static void onExit(@Advice.This OneWorldNPCVillageData villageData,
                       @Advice.Argument(0) int tileX,
                       @Advice.Argument(1) int tileY,
                       @Advice.Argument(2) int flagTier,
                       @Advice.Return(readOnly = false) boolean returned) {
        try {
            // allow access to NPCVillageRegionData.villageTiles
            Field villageTilesField = OneWorldNPCVillageData.NPCVillageRegionData.class.getDeclaredField("villageTiles");
            villageTilesField.setAccessible(true);

            // allow access to SingleRegionBasedLevelData.getDataInRegion()
            Class[] parameterTypes = {int.class, int.class};
            Method getDataInRegionMethod = SingleRegionBasedLevelData.class.getDeclaredMethod("getDataInRegion", parameterTypes);
            getDataInRegionMethod.setAccessible(true);

            PointHashSet regionsWithVillageTiles = new PointHashSet();

            // Loop through regions in the proposed settlement bounds
            Rectangle regionRectangle = SettlementBoundsManager.getRegionRectangleFromTier(tileX, tileY, flagTier);
            for(int regionX = regionRectangle.x; regionX < regionRectangle.x + regionRectangle.width; ++regionX) {
                for(int regionY = regionRectangle.y; regionY < regionRectangle.y + regionRectangle.height; ++regionY) {
                    villageData.getLevel().regionManager.ensureRegionIsLoaded(regionX, regionY);

                    OneWorldNPCVillageData.NPCVillageRegionData regionData = (OneWorldNPCVillageData.NPCVillageRegionData) getDataInRegionMethod.invoke(villageData, regionX, regionY);
                    if (regionData != null) { // For each non-null region:

                        PointHashSet villageTiles = (PointHashSet) villageTilesField.get(regionData);
                        if (villageTiles != null && !villageTiles.isEmpty()) {
                            regionsWithVillageTiles.add(regionX, regionY);
                        }
                    }
                }
            }

            // No regions with village tiles: done and clear for expansion/placement
            if (regionsWithVillageTiles.isEmpty()) {
                returned = true;
                latestRequester = null;
                latestRegionsListMessage = null;
                return;
            }
            // If we don't know which ServerClient requested this check, no need for a string; we're done
            else if (latestRequester == null) {
                latestRegionsListMessage = null;
                returned = false;
                return;
            }
            else {
                latestRegionsListMessage = createRegionsListMessage(regionsWithVillageTiles);
                returned = false;
            }

        } catch (Exception e) {
            returned = false;
            latestRequester = null;
            latestRegionsListMessage = null;
            e.printStackTrace();
        }
    }

    public static String createRegionsListMessage(PointHashSet regions) {
        StringBuilder builder = new StringBuilder();
        long left = regions.stream().count();
        for (Point region : regions) {
            builder.append('(').append(region.x).append(',').append(region.y).append(')');
            left -= 1;
            if (left > 0) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }
}