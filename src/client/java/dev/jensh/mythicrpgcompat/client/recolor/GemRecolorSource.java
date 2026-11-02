package dev.jensh.mythicrpgcompat.client.recolor;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jensh.mythicrpgcompat.MythicRpgCompat;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

/**
 * Atlas sprite source {@code mythic_rpg_compat:gem_recolor}: adds a gem-recoloured copy of each
 * listed texture for each gem, read from whatever mods are installed.
 */
public record GemRecolorSource(List<String> gems, List<RecolorEntry> textures) implements SpriteSource {
	public static final Identifier TYPE = MythicRpgCompat.id("gem_recolor");
	public static final MapCodec<GemRecolorSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("gems").forGetter(GemRecolorSource::gems),
			RecolorEntry.CODEC.listOf().fieldOf("textures").forGetter(GemRecolorSource::textures)
	).apply(i, GemRecolorSource::new));

	@Override
	public void run(ResourceManager manager, Output output) {
		Map<String, int[]> palettes = new HashMap<>();
		for (String gem : gems) {
			int[] palette = GemRecolor.gemPaletteOrNull(manager, gem);
			if (palette != null) {
				palettes.put(gem, palette); // missing when Mythic Upgrades isn't installed
			}
		}
		if (palettes.isEmpty()) {
			return;
		}

		for (RecolorEntry entry : textures) {
			try (NativeImage source = GemRecolor.read(manager, RecolorEntry.file(entry.texture()));
				 NativeImage twin = entry.twin().isPresent() ? GemRecolor.read(manager, RecolorEntry.file(entry.twin().get())) : null) {
				if (source == null) {
					continue; // mod not installed
				}
				for (String gem : palettes.keySet()) {
					Identifier id = entry.recolored(gem);
					NativeImage image = GemRecolor.recolor(source, twin, palettes.get(gem));
					output.add(id, new Loader(id, image));
				}
			} catch (IOException e) {
				MythicRpgCompat.LOGGER.warn("Could not recolour {}", entry.texture(), e);
			}
		}
	}

	@Override
	public MapCodec<GemRecolorSource> codec() {
		return MAP_CODEC;
	}

	private record Loader(Identifier id, NativeImage image) implements DiscardableLoader {
		@Override
		public @Nullable SpriteContents get(SpriteResourceLoader loader) {
			return new SpriteContents(id, new FrameSize(image.getWidth(), image.getHeight()), image);
		}

		@Override
		public void discard() {
			image.close();
		}
	}
}
