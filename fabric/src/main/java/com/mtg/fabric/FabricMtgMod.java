package com.mtg.fabric;

import com.mtg.api.MtgLibrary;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class FabricMtgMod implements ModInitializer {
    @Override
    public void onInitialize() {
        MtgLibrary.initialize(FabricLoader.getInstance().getConfigDir());
    }
}
