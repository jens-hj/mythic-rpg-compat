package dev.jensh.mythicrpgcompat;

import java.util.ArrayList;
import java.util.List;
import net.jewelry.gems.GemComponents;
import net.jewelry.gems.GemSockets;
import net.jewelry.gems.SocketsComponent;
import net.minecraft.world.item.ItemStack;

/** Jewelry mod integration. Only loaded when Jewelry is installed. */
final class JewelryCompat {
	private JewelryCompat() {
	}

	/** Adds one empty, untyped gem socket, keeping existing sockets and the gems in them. */
	static void addSocket(ItemStack stack) {
		if (!GemSockets.enabled()) {
			return;
		}
		SocketsComponent current = stack.get(GemComponents.SOCKETS);
		List<SocketsComponent.Socket> sockets = new ArrayList<>(current == null ? List.of() : current.sockets());
		sockets.add(SocketsComponent.Socket.EMPTY);
		stack.set(GemComponents.SOCKETS, new SocketsComponent(sockets));
	}
}
