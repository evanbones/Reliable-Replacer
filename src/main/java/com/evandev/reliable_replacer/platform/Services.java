package com.evandev.reliable_replacer.platform;

//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
//?}
//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
*///?}

import java.nio.file.Path;

public class Services {
    public static final Services PLATFORM = new Services();

    public String getPlatformName() {
        //? if fabric {
        return "Fabric";
        //?}
        //? if neoforge {
        /*return "NeoForge";
        *///?}
    }

    public boolean isModLoaded(String modId) {
        //? if fabric {
        return FabricLoader.getInstance().isModLoaded(modId);
        //?}
        //? if neoforge {
        /*return ModList.get().isLoaded(modId);
        *///?}
    }

    public boolean isDevelopmentEnvironment() {
        //? if fabric {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
        //?}
        //? if neoforge && <1.21.2 {
        /*return !FMLLoader.isProduction();
        *///?} else if neoforge {
        /*return !FMLLoader.getCurrent().isProduction();
        *///?}
    }

    public String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    public Path getConfigDirectory() {
        //? if fabric {
        return FabricLoader.getInstance().getConfigDir();
        //?}
        //? if neoforge {
        /*return FMLPaths.CONFIGDIR.get();
        *///?}
    }

    public boolean isPhysicalClient() {
        //? if fabric {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
        //?}
        //? if neoforge && <1.21.2 {
        /*return FMLLoader.getDist() == Dist.CLIENT;
        *///?} else if neoforge {
        /*return FMLLoader.getCurrent().getDist() == Dist.CLIENT;
        *///?}
    }
}
