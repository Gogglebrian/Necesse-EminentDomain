package eminentdomain;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.objectItem.SettlementFlagObjectItem;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import net.bytebuddy.asm.Advice;

import java.awt.geom.Line2D;

@ModMethodPatch(target = SettlementFlagObjectItem.class, name = "canPlace", arguments = {Level.class, ObjectPlaceOption.class, PlayerMob.class, Line2D.class, InventoryItem.class, GNDItemMap.class})
public class SettlementFlagItemCanPlacePatch {

        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean onEnter(@Advice.Argument(0) Level level,
                                      @Advice.Argument(1) ObjectPlaceOption po,
                                      @Advice.Argument(2) PlayerMob player,
                                      @Advice.Argument(3) Line2D playerPositionLine,
                                      @Advice.Argument(4) InventoryItem item,
                                      @Advice.Argument(5) GNDItemMap mapContent) {
            if (level.isServer() && player != null) {
                NPCVillageCanPlaceSettlementFlagAtPatch.latestRequester = player.getServerClient();
            }

            // Don't skip the vanilla method
            return false;
        }

        @Advice.OnMethodExit()
        public static void onExit() {
            NPCVillageCanPlaceSettlementFlagAtPatch.sendNPCsMessageToRequesterIfAny();
        }
}