package net.ckinvsee.fabric;

import net.ckinvsee.CKInvSeeExpectPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

@SuppressWarnings("unused")
public class CKInvSeeExpectPlatformImpl {
    /**
     * This is our actual method to {@link CKInvSeeExpectPlatform#getConfigDirectory()}.
     */
    @SuppressWarnings("unused")
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
