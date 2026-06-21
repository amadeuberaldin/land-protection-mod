package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ColizeuKeepInventoryHandler {

    private static final Map<UUID, SavedInventory> SAVED_INVENTORIES = new HashMap<>();

    private ColizeuKeepInventoryHandler() {
    }

    public static void captureInventory(ServerPlayer player) {

        String dimension = player.level().dimension().toString();

        Claim colizeu = ClaimManager.getServerClaimAt(
                player.blockPosition(),
                dimension);

        if (colizeu == null) {
            return;
        }

        SAVED_INVENTORIES.put(
                player.getUUID(),
                SavedInventory.from(player));
    }

    public static boolean hasSavedInventory(ServerPlayer player) {
        return SAVED_INVENTORIES.containsKey(player.getUUID());
    }

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {

            SavedInventory savedInventory = SAVED_INVENTORIES.remove(oldPlayer.getUUID());

            if (savedInventory == null) {
                return;
            }

            savedInventory.restoreTo(newPlayer);
        });
    }

    private static class SavedInventory {

        private final ItemStack[] slots;
        private final int experienceLevel;
        private final int totalExperience;
        private final float experienceProgress;

        private SavedInventory(
                ItemStack[] slots,
                int experienceLevel,
                int totalExperience,
                float experienceProgress) {

            this.slots = slots;
            this.experienceLevel = experienceLevel;
            this.totalExperience = totalExperience;
            this.experienceProgress = experienceProgress;
        }

        private static SavedInventory from(ServerPlayer player) {

            Inventory inventory = player.getInventory();

            ItemStack[] slots = new ItemStack[inventory.getContainerSize()];

            for (int i = 0; i < slots.length; i++) {
                slots[i] = inventory.getItem(i).copy();
            }

            return new SavedInventory(
                    slots,
                    player.experienceLevel,
                    player.totalExperience,
                    player.experienceProgress);
        }

        private void restoreTo(ServerPlayer player) {

            Inventory inventory = player.getInventory();

            for (int i = 0; i < slots.length; i++) {
                inventory.setItem(i, slots[i].copy());
            }

            player.experienceLevel = experienceLevel;
            player.totalExperience = totalExperience;
            player.experienceProgress = experienceProgress;

            inventory.setChanged();
        }
    }
}