package net.ckinvsee.quilt;

import net.ckinvsee.fabriclike.ExampleModFabricLike;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

@SuppressWarnings("unused")
public class CKInvSeeModQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        ExampleModFabricLike.init();
    }
}
