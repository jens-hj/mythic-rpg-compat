package dev.jensh.mythicrpgcompat.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/** Mod Menu entrypoint: opens the config screen when Cloth Config is installed. */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return FabricLoader.getInstance().isModLoaded("cloth-config") ? ConfigScreen::create : parent -> null;
	}
}
