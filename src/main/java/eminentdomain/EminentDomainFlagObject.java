package eminentdomain;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.networkField.BooleanNetworkField;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SettlementFlagObjectEntity;
import necesse.gfx.GameColor;
import necesse.gfx.GameResources;
import necesse.gfx.HumanLook;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.objectItem.SettlementFlagObjectItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectHoverHitbox;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.notifications.SettlementNotificationManager;
import necesse.level.maps.levelData.settlementData.notifications.SettlementNotificationSeverity;
import necesse.level.maps.light.GameLight;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class EminentDomainFlagObject extends GameObject {
    public GameTexture texture;

    public EminentDomainFlagObject() {
        super(new Rectangle(6, 6, 20, 20));
        this.mapColor = new Color(171, 174, 190);
        this.toolType = ToolType.ALL;
        this.isLightTransparent = true;
        this.setItemCategory(new String[]{"objects", "misc"});
        this.setCraftingCategory(new String[]{"objects", "misc"});
    }

    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        LootTable lootTable = new LootTable();

        return lootTable;
    }

    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/eminentdomainflag");
    }

    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, Level level, int tileX, int tileY, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        GameLight light = level.getLightLevel(tileX, tileY);
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        final TextureDrawOptions options = this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight()).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - this.texture.getHeight() + 32);

        list.add(new LevelSortedDrawable(this, tileX, tileY) {
            public int getSortY() {
                return 16;
            }

            public void draw(TickManager tickManager) {
                options.draw();
            }
        });
    }

    public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY);
        this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32);
    }

    public List<ObjectHoverHitbox> getHoverHitboxes(Level level, int layerID, int tileX, int tileY) {
        List<ObjectHoverHitbox> list = super.getHoverHitboxes(level, layerID, tileX, tileY);
        list.add(new ObjectHoverHitbox(layerID, tileX, tileY, 4, -38, 24, 38));
        return list;
    }

    public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
        String error = super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
        if (error != null) {
            return error;
        } else {
            return !level.getIdentifier().equals(LevelIdentifier.SURFACE_IDENTIFIER) ? "notsurface" : null;
        }
    }

    public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
        ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
        tooltips.add(Localization.translate("itemtooltip", "eminentdomainflagtip1"));
        tooltips.add(Localization.translate("itemtooltip", "eminentdomainflagtip2"));
        tooltips.add(Localization.translate("itemtooltip", "eminentdomainflagtip3"));
        tooltips.add(Localization.translate("itemtooltip", "eminentdomainflagtip4"));
        return tooltips;
    }

    public Item generateNewObjectItem() {
        return new EminentDomainFlagObjectItem(this);
    }

    public void onMouseHover(Level level, int x, int y, GameCamera camera, PlayerMob perspective, boolean debug) {
        super.onMouseHover(level, x, y, camera, perspective, debug);
        if (perspective != null) {
            int regionX = level.regionManager.getRegionCoordByTile(x);
            int regionY = level.regionManager.getRegionCoordByTile(y);
            String regionString = Localization.translate("misc","eminentdomainregion") + " " + regionX + "," + regionY;

            StringTooltips tooltips = new StringTooltips();
            tooltips.add(regionString);
            tooltips.add(Localization.translate("misc","eminentdomainflaghovertooltip"));
            GameTooltipManager.addTooltip(tooltips, TooltipLocation.INTERACT_FOCUS);
        }

    }
}
