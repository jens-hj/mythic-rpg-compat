package dev.jensh.mythicrpgcompat;

import java.util.ArrayList;
import java.util.List;
import net.jewelry.gems.GemComponents;
import net.jewelry.gems.SocketsComponent;
import net.minecraft.world.item.ItemStack;

/** Jewelry mod integration. Only loaded when Jewelry is installed. */
final class JewelryCompat {
	private JewelryCompat() {
	}

	/** Removes the last socket if it is empty and untyped (the one 0.2.0 added). */
	static void removeTrailingEmptySocket(ItemStack stack) {
		SocketsComponent current = stack.get(GemComponents.SOCKETS);
		if (current == null || current.sockets().isEmpty()) {
			return;
		}
		List<SocketsComponent.Socket> sockets = new ArrayList<>(current.sockets());
		SocketsComponent.Socket last = sockets.getLast();
		if (last.isEmpty() && last.type().isEmpty()) {
			sockets.removeLast();
			stack.set(GemComponents.SOCKETS, new SocketsComponent(sockets));
		}
	}
}
