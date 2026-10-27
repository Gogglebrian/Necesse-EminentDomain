package eminentdomain;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.objectItem.SettlementFlagObjectItem;
import necesse.level.maps.Level;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(target = SettlementFlagObjectItem.class, name = "onAttemptPlace", arguments = {Level.class, int.class, int.class, PlayerMob.class, InventoryItem.class, GNDItemMap.class, String.class})
public class SettlementFlagItemOnAttemptPlacePatch {
    @Advice.OnMethodExit()
    public static void onExit(@Advice.Argument(0) Level level,
                              @Advice.Argument(1) int x,
                              @Advice.Argument(2) int y,
                              @Advice.Argument(3) PlayerMob player,
                              @Advice.Argument(4) InventoryItem item,
                              @Advice.Argument(5) GNDItemMap mapContent,
                              @Advice.Argument(6) String error) {
        if (error.equals("closevillage")) {
            NPCVillageCanPlaceSettlementFlagAtPatch.sendNPCsMessageToRequesterIfAny();
        }
    }
}
