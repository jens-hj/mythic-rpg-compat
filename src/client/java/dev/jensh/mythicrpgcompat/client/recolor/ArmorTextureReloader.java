package dev.jensh.mythicrpgcompat.client.recolor;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jensh.mythicrpgcompat.MythicRpgCompat;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Builds the recoloured worn-armor textures that upgraded stacks point armor_model_api at
 * ({@code mythic_rpg_compat:textures/armor/<variant>/<mod>/<set>.png}).
 */
public final class ArmorTextureReloader extends SimpleReloadListener<Map<Identifier, NativeImage>> {
	public static final Identifier ID = MythicRpgCompat.id("armor_textures");
	private static final Identifier MANIFEST = MythicRpgCompat.id("recolor/armor.json");

	private record Manifest(List<RecolorVariant> variants, List<RecolorEntry> textures) {
		static final Codec<Manifest> CODEC = RecordCodecBuilder.create(i -> i.group(
				RecolorVariant.CODEC.listOf().fieldOf("variants").forGetter(Manifest::variants),
				RecolorEntry.CODEC.listOf().fieldOf("textures").forGetter(Manifest::textures)
		).apply(i, Manifest::new));
	}

	private final List<Identifier> registered = new ArrayList<>();

	@Override
	protected Map<Identifier, NativeImage> prepare(PreparableReloadListener.SharedState state) {
		ResourceManager manager = state.resourceManager();
		Map<Identifier, NativeImage> images = new HashMap<>();
		Optional<Resource> manifestResource = manager.getResource(MANIFEST);
		if (manifestResource.isEmpty()) {
			return images;
		}
		try (Reader reader = manifestResource.get().openAsReader()) {
			Manifest manifest = Manifest.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
			Map<RecolorVariant, int[]> palettes = new HashMap<>();
			for (RecolorVariant variant : manifest.variants()) {
				int[] palette = variant.loadPalette(manager);
				if (palette != null) {
					palettes.put(variant, palette);
				}
			}
			for (RecolorEntry entry : manifest.textures()) {
				try (NativeImage source = GemRecolor.read(manager, RecolorEntry.file(entry.texture()));
					 NativeImage twin = entry.twin().isPresent() ? GemRecolor.read(manager, RecolorEntry.file(entry.twin().get())) : null) {
					if (source == null) {
						continue;
					}
					for (var variant : palettes.entrySet()) {
						if (entry.accepts(variant.getKey())) {
							images.put(RecolorEntry.file(entry.recolored(variant.getKey().key())),
									GemRecolor.recolor(source, twin, variant.getValue(), entry.diffMode()));
						}
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			MythicRpgCompat.LOGGER.error("Could not build infused armor textures", e);
		}
		return images;
	}

	@Override
	protected void apply(Map<Identifier, NativeImage> images, PreparableReloadListener.SharedState state) {
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		registered.forEach(textures::release);
		registered.clear();
		images.forEach((id, image) -> {
			textures.register(id, new DynamicTexture(id::toString, image));
			registered.add(id);
		});
	}
}
