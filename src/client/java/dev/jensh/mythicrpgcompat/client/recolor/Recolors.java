package dev.jensh.mythicrpgcompat.client.recolor;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

/**
 * The palettes of every installed variant, and the recoloured copies they make of a texture: one
 * per gem, one per forge tier and, for class armor, one per gem + forge pair (two-tone, keyed
 * {@code <gem>_<forge variant>}, e.g. {@code ruby_forged_aeternium}).
 */
public final class Recolors {
	private final Map<RecolorVariant, int[]> palettes = new LinkedHashMap<>();

	public Recolors(List<RecolorVariant> variants, ResourceManager manager) {
		for (RecolorVariant variant : variants) {
			int[] palette = variant.loadPalette(manager);
			if (palette != null) {
				palettes.put(variant, palette); // missing when the variant's mod isn't installed
			}
		}
	}

	public boolean isEmpty() {
		return palettes.isEmpty();
	}

	public void forEach(RecolorEntry entry, NativeImage source, @Nullable NativeImage twin, BiConsumer<String, NativeImage> output) {
		for (var variant : palettes.entrySet()) {
			RecolorVariant v = variant.getKey();
			if (!entry.accepts(v)) {
				continue;
			}
			output.accept(v.key(), GemRecolor.recolor(source, twin, variant.getValue(), entry.diffMode()));
			if (v.forge()) {
				continue;
			}
			for (var forge : palettes.entrySet()) {
				if (forge.getKey().forge() && entry.accepts(forge.getKey())) {
					output.accept(v.key() + "_" + forge.getKey().key(),
							GemRecolor.recolor(source, twin, variant.getValue(), forge.getValue(), entry.diffMode()));
				}
			}
		}
	}
}
