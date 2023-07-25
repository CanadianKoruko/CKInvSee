package net.ckinvsee.fabric;

import net.ckinvsee.CKInvSeeExpectPlatform;
import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;

@SuppressWarnings("unused")
public class CKInvSeeExpectPlatformImpl {
    /**
     * This is our actual method to {@link CKInvSeeExpectPlatform#getConfigDirectory()}.
     */
    @SuppressWarnings("unused")
    public static Path getConfigDirectory() {
        return QuiltLoader.getConfigDir();
    }
}
