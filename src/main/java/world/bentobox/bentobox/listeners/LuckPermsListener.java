package world.bentobox.bentobox.listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.node.NodeAddEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

import java.util.Objects;
import java.util.UUID;

public class LuckPermsListener implements Listener {
	private BentoBox plugin;
	private LuckPerms luckPerms;

	public LuckPermsListener(@NonNull BentoBox plugin) {
		this.plugin = plugin;
		luckPerms = Bukkit.getServicesManager().getRegistration(LuckPerms.class).getProvider();
		luckPerms.getEventBus().subscribe(this.plugin, NodeAddEvent.class, this::onNodeAdd);
	}

	public void onNodeAdd(NodeAddEvent e) {
		if (!e.isUser()) return;
		var luckpermsUser = (net.luckperms.api.model.user.User) e.getTarget();
		Player p = plugin.getServer().getPlayer(luckpermsUser.getUniqueId());

		User u = User.getInstance(p);

		// Update the island range of the islands the player owns
		updateIslandRange(u);

		// Set island max members and homes based on permissions if this player is the owner of an island
		plugin.getIWM().getOverWorlds().stream()
				.map(w -> plugin.getIslands().getIsland(w, luckpermsUser.getUniqueId()))
				.filter(Objects::nonNull)
				.filter(i -> luckpermsUser.getUniqueId().equals(i.getOwner()))
				.forEach(i -> {
					plugin.getIslands().getMaxMembers(i, RanksManager.MEMBER_RANK);
					plugin.getIslands().getMaxMembers(i, RanksManager.COOP_RANK);
					plugin.getIslands().getMaxMembers(i, RanksManager.TRUSTED_RANK);
					plugin.getIslands().getMaxHomes(i);
				});
	}

	private void updateIslandRange(User user) {
		plugin.getIWM().getOverWorlds().stream()
				.filter(world -> plugin.getIslands().isOwner(world, user.getUniqueId()))
				.forEach(world -> {
					Island island = plugin.getIslands().getIsland(world, user);
					if (island != null) {
						// Check if new owner has a different range permission than the island size
						int range = user.getPermissionValue(plugin.getIWM().getAddon(island.getWorld()).map(GameModeAddon::getPermissionPrefix).orElse("") + "island.range", island.getProtectionRange());
						// Range cannot be greater than the island distance * 2
						range = Math.min(range, 2 * plugin.getIWM().getIslandDistance(island.getWorld()));
						// Range can go up or down
						if (range != island.getProtectionRange()) {
							user.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER, String.valueOf(range));
							plugin.log("Island protection range changed from " + island.getProtectionRange() + " to "
									+ range + " for " + user.getName() + " due to permission.");

							// Get old range for event
							int oldRange = island.getProtectionRange();

							island.setProtectionRange(range);

							// Call Protection Range Change event. Does not support canceling.
							IslandEvent.builder()
									.island(island)
									.location(island.getProtectionCenter())
									.reason(IslandEvent.Reason.RANGE_CHANGE)
									.involvedPlayer(user.getUniqueId())
									.admin(true)
									.protectionRange(range, oldRange)
									.build(true);
						}
					}
				});
	}
}
