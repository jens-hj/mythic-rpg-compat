package dev.jensh.mythicrpgcompat.client.recolor;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/**
 * Recolours the netherite parts of a texture with a Mythic gem palette.
 *
 * <p>Netherite parts are dark, low-saturation greys. A pixel is recoloured when it looks like
 * that and, if a pre-netherite "twin" texture exists (e.g. {@code ranger_armor} for
 * {@code netherite_ranger_armor}), also differs from the twin, so shared outlines and greys stay.
 * Masked colours are ranked by luminance and mapped onto the gem palette's dark-to-light ramp.
 */
public final class GemRecolor {
	/** Range of the 10-entry Mythic palette used; the extremes are near-black and near-white. */
	private static final int PALETTE_LO = 1;
	private static final int PALETTE_HI = 7;
	private static final int TWIN_THRESHOLD = 12;
	private static final double MAX_SATURATION = 0.32;
	private static final int MIN_VALUE = 30;
	private static final int MAX_VALUE = 170;

	private GemRecolor() {
	}

	public static NativeImage recolor(NativeImage source, @Nullable NativeImage twin, int[] palette) {
		int w = source.getWidth();
		int h = source.getHeight();
		boolean useTwin = twin != null && twin.getWidth() == w && twin.getHeight() == h;
		boolean[] mask = new boolean[w * h];
		TreeMap<Double, Integer> byLuminance = new TreeMap<>();

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int c = source.getPixel(x, y);
				if (ARGB.alpha(c) == 0) {
					continue;
				}
				boolean netherite = looksNetherite(c)
						&& (!useTwin || distance(c, twin.getPixel(x, y)) > TWIN_THRESHOLD || ARGB.alpha(twin.getPixel(x, y)) == 0);
				if (netherite) {
					mask[y * w + x] = true;
					byLuminance.putIfAbsent(luminance(c), c & 0xFFFFFF);
				}
			}
		}

		// Rank distinct colours by luminance -> palette index.
		Integer[] ranked = byLuminance.values().toArray(Integer[]::new);
		int n = ranked.length;
		java.util.HashMap<Integer, Integer> mapping = new java.util.HashMap<>();
		for (int i = 0; i < n; i++) {
			double t = n == 1 ? 0.5 : (double) i / (n - 1);
			int idx = (int) Math.round(PALETTE_LO + t * (Math.min(PALETTE_HI, palette.length - 1) - PALETTE_LO));
			mapping.put(ranked[i], palette[Math.max(0, Math.min(palette.length - 1, idx))] & 0xFFFFFF);
		}

		NativeImage out = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int c = source.getPixel(x, y);
				if (mask[y * w + x]) {
					Integer rgb = mapping.get(c & 0xFFFFFF);
					if (rgb != null) {
						c = ARGB.color(ARGB.alpha(c), rgb);
					}
				}
				out.setPixel(x, y, c);
			}
		}
		return out;
	}

	/** Reads a horizontal palette strip into ARGB colours, left to right. */
	public static int[] readPalette(ResourceManager manager, Identifier file) throws IOException {
		try (NativeImage image = read(manager, file)) {
			if (image == null) {
				throw new IOException("Missing palette " + file);
			}
			int[] colors = new int[image.getWidth()];
			for (int x = 0; x < colors.length; x++) {
				colors[x] = image.getPixel(x, 0);
			}
			return colors;
		}
	}

	public static int[] gemPalette(ResourceManager manager, String gem) throws IOException {
		return readPalette(manager, Identifier.fromNamespaceAndPath("mythicupgrades", "textures/" + gem + "_palette.png"));
	}

	/** The gem's palette, or null when Mythic Upgrades (and so its palettes) isn't installed. */
	public static int @Nullable [] gemPaletteOrNull(ResourceManager manager, String gem) {
		try {
			return gemPalette(manager, gem);
		} catch (IOException e) {
			return null;
		}
	}

	public static @Nullable NativeImage read(ResourceManager manager, Identifier file) throws IOException {
		Optional<Resource> resource = manager.getResource(file);
		if (resource.isEmpty()) {
			return null;
		}
		try (InputStream in = resource.get().open()) {
			return NativeImage.read(in);
		}
	}

	private static double luminance(int c) {
		return 0.2126 * ARGB.red(c) + 0.7152 * ARGB.green(c) + 0.0722 * ARGB.blue(c);
	}

	private static int distance(int a, int b) {
		return Math.abs(ARGB.red(a) - ARGB.red(b)) + Math.abs(ARGB.green(a) - ARGB.green(b)) + Math.abs(ARGB.blue(a) - ARGB.blue(b));
	}

	private static boolean looksNetherite(int c) {
		int max = Math.max(ARGB.red(c), Math.max(ARGB.green(c), ARGB.blue(c)));
		int min = Math.min(ARGB.red(c), Math.min(ARGB.green(c), ARGB.blue(c)));
		double saturation = max == 0 ? 0 : (double) (max - min) / max;
		return saturation < MAX_SATURATION && max >= MIN_VALUE && max <= MAX_VALUE;
	}
}
