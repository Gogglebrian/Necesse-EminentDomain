package eminentdomain;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.maps.levelData.OneWorldNPCVillageData;

@ModEntry
public class EminentDomainMod {
    public boolean debugTestVillagerRespawns = false;

    public void init() {
        ObjectRegistry.registerObject("eminentdomainflag", new EminentDomainFlagObject(), 500F, true);

        if (debugTestVillagerRespawns) {
            OneWorldNPCVillageData.MAX_VILLAGER_RESPAWN_MINUTES = 1;
            OneWorldNPCVillageData.MIN_VILLAGER_RESPAWN_MINUTES = 0;
            OneWorldNPCVillageData.MIN_PLAYER_PREVENTED_DISTANCE = 10;
        }
    }

    public void postInit() {
        Recipes.registerModRecipe(new Recipe(
                "eminentdomainflag",
                1,
                RecipeTechRegistry.DEMONIC_WORKSTATION,
                new Ingredient[] {
                        new Ingredient("demonicbar",10),
                        new Ingredient("coin",5000),
                }).showAfter("settlementflag"));
    }
}
