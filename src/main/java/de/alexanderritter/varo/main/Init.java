package de.alexanderritter.varo.main;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;

import de.alexanderritter.varo.commands.CMDbordersize;
import de.alexanderritter.varo.commands.CMDcoordinates;
import de.alexanderritter.varo.commands.CMDdelete;
import de.alexanderritter.varo.commands.CMDmaintainence;
import de.alexanderritter.varo.commands.CMDmodifyconfig;
import de.alexanderritter.varo.commands.CMDregister;
import de.alexanderritter.varo.commands.CMDrevive;
import de.alexanderritter.varo.commands.CMDsetlobby;
import de.alexanderritter.varo.commands.CMDspawn;
import de.alexanderritter.varo.commands.CMDspectators;
import de.alexanderritter.varo.commands.CMDstart;
import de.alexanderritter.varo.commands.CMDstop;
import de.alexanderritter.varo.commands.Overwrite;
import de.alexanderritter.varo.config.BorderMode;
import de.alexanderritter.varo.config.Settings;
import de.alexanderritter.varo.events.BeforeVaroListener;
import de.alexanderritter.varo.events.IngameEvents;
import de.alexanderritter.varo.events.SpectatorListener;
import de.alexanderritter.varo.ingame.ChestManager;
import de.alexanderritter.varo.ingame.TeamChest;

public class Init {
	
	Varo plugin;
	String path;
	
	public Init(Varo plugin, String path) {
		this.plugin = plugin;
		this.path = path;
	}
	
	public boolean init() {		
		String varoworld = plugin.getConfig().getString("varoworld");		
		if(Bukkit.getWorld(varoworld) != null) {	
			plugin.getLogger().info("Die Welt mit dem Namen " + varoworld + " wurde erfolgreich geladen");
			plugin.getSettings().setVaroWorld(Bukkit.getWorld(varoworld));		
		} else {		
			plugin.getLogger().severe("Eine Welt mit dem Namen " + varoworld + " existiert nicht!");
			Bukkit.getPluginManager().disablePlugin(plugin);
			return false;
		}
		registerCommands();
		registerEvents();
		loadChests();
		return true;
	}
	
	public void registerEvents() {
		PluginManager pm = Bukkit.getPluginManager();		
		if(plugin.getSettings().isRunning()) {
			pm.registerEvents(new IngameEvents(plugin), plugin);
			pm.registerEvents(new TeamChest(plugin), plugin);
			pm.registerEvents(new SpectatorListener(plugin), plugin);
		} else {
			pm.registerEvents(new BeforeVaroListener(plugin), plugin);
		}
	}
	
	private void registerCommands() {
		((PluginCommand) plugin.getCommand("varo.start").setUsage(ChatColor.RED + "Syntax: /varo.start <Sekunden>")).setExecutor(new CMDstart(plugin));
		((PluginCommand) plugin.getCommand("varo.stop").setUsage(ChatColor.RED + "Syntax: /varo.stop")).setExecutor(new CMDstop(plugin));
		((PluginCommand) plugin.getCommand("varo.register").setUsage(ChatColor.RED + "Syntax: /varo.register <Team> <Teamfarbe> <Spieler1> <Spieler2> <...>")).setExecutor(new CMDregister(plugin));
		((PluginCommand) plugin.getCommand("varo.delete").setUsage(ChatColor.RED + "Syntax: /varo.delete <Team>")).setExecutor(new CMDdelete(plugin));
		((PluginCommand) plugin.getCommand("varo.spawn").setUsage(ChatColor.RED + "Syntax: /varo.spawn <Id>")).setExecutor(new CMDspawn(plugin));
		((PluginCommand) plugin.getCommand("varo.revive").setUsage(ChatColor.RED + "Syntax: /varo.revive <Spieler>")).setExecutor(new CMDrevive(plugin));
		((PluginCommand) plugin.getCommand("varo.setlobby").setUsage(ChatColor.RED + "Syntax: /varo.setlobby")).setExecutor(new CMDsetlobby(plugin));
		((PluginCommand) plugin.getCommand("coordinates").setUsage(ChatColor.RED + "Syntax: /coordinates")).setExecutor(new CMDcoordinates(plugin));
		((PluginCommand) plugin.getCommand("bordersize").setUsage(ChatColor.RED + "Syntax: /bordersize")).setExecutor(new CMDbordersize(plugin));		
		((PluginCommand) plugin.getCommand("spectators").setUsage(ChatColor.RED + "Syntax: /spectators")).setExecutor(new CMDspectators());
		((PluginCommand) plugin.getCommand("modifyconfig").setUsage(ChatColor.RED + "Syntax: /modifyconfig <attribute> <value>")).setExecutor(new CMDmodifyconfig(plugin));
		((PluginCommand) plugin.getCommand("maintainence").setUsage(ChatColor.RED + "Syntax: /maintainence <player>\nFür Admins, die nicht am Varo teilnehmen: /maintainence <player> forever")).setExecutor(new CMDmaintainence(plugin));
		plugin.getCommand("overwrite").setExecutor(new Overwrite(plugin));
	}
	
	public Settings loadSettings() {
		FileConfiguration config = plugin.getConfig();
		BorderMode bordermode = BorderMode.valueOf(config.getString("bordermode").toUpperCase());
		int sessions_length = config.getInt("sessions_length");
		int sessions_per_week = config.getInt("sessions_per_week");
		int login_protection = config.getInt("login_protection");
		int start_protection = config.getInt("start_protection");
		int min_logout_distance = config.getInt("min_logout_distance");
		int current_week = config.getInt("plugin.current_week");
		
		int endsize = plugin.getConfig().getInt("border.end-radius")*2;
		int size = plugin.getConfig().getInt("border.radius")*2;
		int time = plugin.getConfig().getInt("border.shrink-time"); // in hours
		double borderShrinkPerHour = (double) (size-endsize)/time;
		
		boolean isAllowedToSpectateIfTeamAlive = config.getBoolean("spectate-allowed-if-team-alive");
		boolean friendlyfire = config.getBoolean("friendlyfire");
		boolean running = config.getBoolean("plugin.running");
		String discordid = config.getString("discord-id");
		int daytopost = config.getInt("coord_post_day");
		Location lobby = null;
		if(config.get("lobby") != null) {
			lobby = new Location(Bukkit.getWorld(config.getString("lobby.world")), config.getInt("lobby.x"), config.getInt("lobby.y"), config.getInt("lobby.z"));
		}
		return new Settings(plugin, bordermode, sessions_per_week, sessions_length, start_protection, login_protection, 
				min_logout_distance, current_week, isAllowedToSpectateIfTeamAlive, friendlyfire, running, discordid, daytopost, borderShrinkPerHour, lobby);
	}
	
	public void loadChests() {
		YamlConfiguration chests = plugin.getChestConfig();
		for(String team : chests.getKeys(false)) {
			Location chest1 = new Location(Bukkit.getWorld(chests.getString(team + ".1.world")),
					chests.getInt(team + ".1.x"), chests.getInt(team + ".1.y"), chests.getInt(team + ".1.z"));
			Location chest2 = new Location(Bukkit.getWorld(chests.getString(team + ".2.world")),
					chests.getInt(team + ".2.x"), chests.getInt(team + ".2.y"), chests.getInt(team + ".2.z"));
			ChestManager.addChest(team, chest1, chest2);
		}
	}
	
}