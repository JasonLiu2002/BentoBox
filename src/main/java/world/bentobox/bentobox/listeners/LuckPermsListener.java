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
		User u = (User) e.getTarget();
		Player p = plugin.getServer().getPlayer(u.getUniqueId());

		// Update the island range of the islands the player owns
		Util.updateIslandRange(u);

		// Set island max members and homes based on permissions if this player is the owner of an island
		plugin.getIWM().getOverWorlds().stream()
				.map(w -> plugin.getIslands().getIsland(w, u.getUniqueId()))
				.filter(Objects::nonNull)
				.filter(i -> u.getUniqueId().equals(i.getOwner()))
				.forEach(i -> {
					plugin.getIslands().getMaxMembers(i, RanksManager.MEMBER_RANK);
					plugin.getIslands().getMaxMembers(i, RanksManager.COOP_RANK);
					plugin.getIslands().getMaxMembers(i, RanksManager.TRUSTED_RANK);
					plugin.getIslands().getMaxHomes(i);
				});
	}

}
