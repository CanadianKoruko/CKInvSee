package net.ckinvsee.forge;

import dev.architectury.platform.forge.EventBuses;
import net.ckinvsee.CKInvSee;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CKInvSee.MOD_ID)
public class CKInvSeeModForge {
    public CKInvSeeModForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(CKInvSee.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        CKInvSee.init();
    }
}
