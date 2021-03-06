package de.alexanderritter.varo.commands;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import de.alexanderritter.varo.api.UUIDs;
import de.alexanderritter.varo.api.VaroMessages;
import de.alexanderritter.varo.ingame.PlayerManager;
import de.alexanderritter.varo.ingame.VaroPlayer;
import de.alexanderritter.varo.main.Varo;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;

public class CMDrevive implements CommandExecutor {
	
	Varo plugin;
	
	public CMDrevive(Varo plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!command.getName().equalsIgnoreCase("varo.revive")) return false;
		if(args.length != 1) return false;
		
		YamlConfiguration players = plugin.getPlayerConfig();
		String playerName = args[0];
				
		try {
			if(UUIDs.getUUID(playerName) != null) {
				String uuid = UUIDs.getUUID(playerName).toString();
				if(plugin.getRegistration().getAllUUIDs().contains(UUID.fromString(uuid))) {
									
					Boolean dead = players.getBoolean(uuid + ".dead");
					if(dead) {
						
						players.set(uuid + ".dead", Boolean.valueOf(false));
						for(UUID other_player : plugin.getRegistration().getAllUUIDs()) {
							VaroPlayer other_ip = plugin.getRegistration().loadPlayer(other_player);
							if(other_ip.getKillCount() <= 0) continue;
							other_ip.removeKill(uuid);
						}
						
						if(Bukkit.getPlayer(UUID.fromString(uuid)) != null) {
							
							Player p = Bukkit.getPlayer(UUID.fromString(uuid));
							VaroPlayer ip = plugin.getRegistration().loadPlayer(p);
							PlayerManager.addIngamePlayer(p, ip);
							ip.setupScoreboard();
							plugin.preparePlayer(p);
							PlayerManager.removeSpectator(p);
							p.teleport(plugin.getSettings().getVaroWorld().getSpawnLocation());
							
						} else {
							// Offline Player
							players.set(uuid + ".reviving", Boolean.valueOf(true));
						}
						
						plugin.sendDiscordMessage(VaroMessages.DISCORD_reviveSuccessfull(playerName));
						sender.sendMessage(VaroMessages.reviveSuccessful(playerName));
						
					} else sender.sendMessage(VaroMessages.playerNotDead(playerName));
				} else sender.sendMessage(VaroMessages.playerNotRegistered(playerName));
			} else sender.sendMessage(VaroMessages.playerDoesntExistOrServersDown(playerName));
		} catch (IOException e) {e.printStackTrace();}
				
		plugin.savePlayerConfig(players);
		return true;
	}
	
	public Player getOfflinePlayer(String name, UUID uuid, Location location) {
        Player target = null;
      GameProfile profile = new GameProfile(uuid, name);

      MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
      EntityPlayer entity = new EntityPlayer(server, server.getWorldServer(0), profile, new PlayerInteractManager(server.getWorldServer(0)));
      entity.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
      entity.world = ((CraftWorld) location.getWorld()).getHandle();
      target = entity == null ? null : (Player) entity.getBukkitEntity();
      if (target != null) {
          target.loadData();
          return target;
      }
      return target;
  }

}
