package com.mtg.neoforge;

import com.mtg.MtgConstants;
import com.mtg.api.MtgLibrary;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(MtgConstants.MOD_ID)
public class NeoForgeMtgMod {
    public NeoForgeMtgMod() {
        MtgLibrary.initialize(FMLPaths.CONFIGDIR.get());
    }
}
