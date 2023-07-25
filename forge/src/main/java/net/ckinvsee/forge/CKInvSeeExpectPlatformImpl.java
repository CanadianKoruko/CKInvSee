package net.ckinvsee.forge;

import net.ckinvsee.CKInvSeeExpectPlatform;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@SuppressWarnings("unused")
public class CKInvSeeExpectPlatformImpl {
    /**
     * This is our actual method to {@link CKInvSeeExpectPlatform#getConfigDirectory()}.
     */
    @SuppressWarnings("unused")
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
