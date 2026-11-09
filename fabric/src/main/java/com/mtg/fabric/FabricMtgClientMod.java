package com.mtg.fabric;

import com.mtg.MtgConstants;
import net.fabricmc.api.ClientModInitializer;

public class FabricMtgClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Strict separation: Loaded ONLY on physical clients
        System.out.println("[" + MtgConstants.MOD_NAME + "] Initializing Fabric Client HUD/UI...");
    }
}
