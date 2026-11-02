package dev.jensh.mythicrpgcompat.client;

import dev.jensh.mythicrpgcompat.client.mixin.SpriteSourcesAccessor;
import dev.jensh.mythicrpgcompat.client.recolor.ArmorTextureReloader;
import dev.jensh.mythicrpgcompat.client.recolor.GemRecolorSource;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;

public class MythicRpgCompatClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SpriteSourcesAccessor.mythicRpgCompat$idMapper().put(GemRecolorSource.TYPE, GemRecolorSource.MAP_CODEC);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(ArmorTextureReloader.ID, new ArmorTextureReloader());
	}
}
