package net.ckinvsee.fabric;

import net.ckinvsee.fabriclike.ExampleModFabricLike;
import net.fabricmc.api.ModInitializer;

public class CKInvSeeModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleModFabricLike.init();
    }
}
