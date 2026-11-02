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
		return recolor(source, twin, palette, false);
	}

	/**
	 * @param diffOnly mask every pixel that differs from the twin, without the netherite-grey test
	 *                 (for Ruby/Aeternium/Crystal textures, whose twin is the netherite version)
	 */
	public static NativeImage recolor(NativeImage source, @Nullable NativeImage twin, int[] palette, boolean diffOnly) {
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
				boolean differs = useTwin && (distance(c, twin.getPixel(x, y)) > TWIN_THRESHOLD || ARGB.alpha(twin.getPixel(x, y)) == 0);
				boolean netherite = diffOnly ? differs : looksNetherite(c) && (!useTwin || differs);
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

	/** Size of a Mythic palette strip; texture-derived palettes are resampled to the same length. */
	private static final int PALETTE_SIZE = 10;

	/**
	 * Builds a dark-to-light palette from a material texture (e.g. an ingot): its distinct opaque
	 * colours sorted by luminance, resampled to {@link #PALETTE_SIZE} entries.
	 */
	public static int[] paletteFromTexture(ResourceManager manager, Identifier file) throws IOException {
		try (NativeImage image = read(manager, file)) {
			if (image == null) {
				throw new IOException("Missing palette texture " + file);
			}
			TreeMap<Double, Integer> byLuminance = new TreeMap<>();
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int c = image.getPixel(x, y);
					if (ARGB.alpha(c) == 255) {
						byLuminance.putIfAbsent(luminance(c), c);
					}
				}
			}
			Integer[] colors = byLuminance.values().toArray(Integer[]::new);
			if (colors.length == 0) {
				throw new IOException("Empty palette texture " + file);
			}
			int[] palette = new int[PALETTE_SIZE];
			for (int i = 0; i < PALETTE_SIZE; i++) {
				palette[i] = colors[(int) Math.round((double) i * (colors.length - 1) / (PALETTE_SIZE - 1))];
			}
			return palette;
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
