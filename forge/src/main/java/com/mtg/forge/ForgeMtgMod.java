package com.mtg.forge;

import com.mtg.MtgConstants;
import com.mtg.api.MtgLibrary;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(MtgConstants.MOD_ID)
public class ForgeMtgMod {
    public ForgeMtgMod() {
        MtgLibrary.initialize(FMLPaths.CONFIGDIR.get());
    }
}
