package com.amadeu.landprotection;

import com.amadeu.landprotection.command.ClaimCommands;
import com.amadeu.landprotection.dragon.DragonEggSystem;
import com.amadeu.landprotection.event.BlockBreakHandler;
import com.amadeu.landprotection.event.BlockPlaceHandler;
import com.amadeu.landprotection.event.FluidProtectionHandler;
import com.amadeu.landprotection.event.UseBlockHandler;
import com.amadeu.landprotection.event.VillagerProtectionHandler;
import com.amadeu.landprotection.storage.ClaimStorage;
import com.amadeu.landprotection.visual.ClaimVisualizationManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import com.amadeu.landprotection.event.EntityProtectionHandler;
import com.amadeu.landprotection.event.PvpProtectionHandler;
import com.amadeu.landprotection.event.ColizeuKeepInventoryHandler;

public class LandProtectionMod implements ModInitializer {

    public static final String MOD_ID = "landprotection";

    @Override
    public void onInitialize() {
        BlockPlaceHandler.register();
        BlockBreakHandler.register();
        UseBlockHandler.register();
        FluidProtectionHandler.register();
        VillagerProtectionHandler.register();
        EntityProtectionHandler.register();
        PvpProtectionHandler.register();
        ColizeuKeepInventoryHandler.register();
        DragonEggSystem.register();

        ClaimCommands.register();

        ClaimVisualizationManager.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ClaimStorage.loadAll(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ClaimStorage.saveAll(server);
        });
    }
}
