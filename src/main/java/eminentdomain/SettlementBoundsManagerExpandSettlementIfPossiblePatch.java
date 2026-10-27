package eminentdomain;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementDependantContainer;
import necesse.level.maps.levelData.settlementData.SettlementBoundsManager;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(target = SettlementBoundsManager.class, name = "expandSettlementIfPossible", arguments = {ServerClient.class, SettlementDependantContainer.class})
public class SettlementBoundsManagerExpandSettlementIfPossiblePatch {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onEnter(@Advice.Argument(0) ServerClient client,
                                  @Advice.Argument(1) SettlementDependantContainer container) {
        NPCVillageCanPlaceSettlementFlagAtPatch.latestRequester = client;

        // Don't skip the vanilla method
        return false;
    }

    @Advice.OnMethodExit()
    public static void onExit() {
        NPCVillageCanPlaceSettlementFlagAtPatch.sendNPCsMessageToRequesterIfAny();
    }
}
