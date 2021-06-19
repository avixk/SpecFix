package me.avixk.SpecFix;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
    public static Plugin plugin;
    public static HashMap<UUID,UUID> waitingForRespawn = new HashMap<UUID, UUID>();

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this,this);
        if(Main.plugin.getConfig().getBoolean("teleport_spectator_to_target_constantly"))
            init();
    }

    public static int task = 0;
    public static void init(){
        stopTask();
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.plugin, new Runnable() {
            @Override
            public void run() {
                for(Player player : Bukkit.getOnlinePlayers()){
                    if(!player.getGameMode().equals(GameMode.SPECTATOR))continue;
                    if(player.getSpectatorTarget() == null)continue;
                    //if(!player.hasPermission("redbannercore.admin"))continue;
                    player.teleport(player.getSpectatorTarget());
                }
            }
        },40, 40);
    }
    public static void stopTask(){
        if(task != 0)
            Bukkit.getScheduler().cancelTask(task);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e){
        if(!Main.plugin.getConfig().getBoolean("teleport_spectator_to_target_on_teleports"))return;
        for(Player player : Bukkit.getOnlinePlayers()){
            if(player.getGameMode() == GameMode.SPECTATOR
                    && player.getSpectatorTarget() != null
                    && player.getSpectatorTarget().equals(e.getPlayer())){
                player.setSpectatorTarget(null);
                Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable() {
                    @Override
                    public void run() {
                        player.teleport(e.getPlayer().getEyeLocation());
                    }
                },1);
                Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable() {
                    @Override
                    public void run() {
                        player.setSpectatorTarget(e.getPlayer());
                    }
                },2);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        if(!Main.plugin.getConfig().getBoolean("teleport_spectator_to_target_on_respawn"))return;
        for(Player player : Bukkit.getOnlinePlayers()){
            if(player.getGameMode() == GameMode.SPECTATOR
                    && player.getSpectatorTarget() != null
                    && player.getSpectatorTarget().equals(e.getEntity())){
                waitingForRespawn.put(e.getEntity().getUniqueId(),player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e){
        if(!Main.plugin.getConfig().getBoolean("teleport_spectator_to_target_on_respawn"))return;
        if(waitingForRespawn.containsKey(e.getPlayer().getUniqueId())){
            OfflinePlayer waitingForRespawnPlayer = Bukkit.getOfflinePlayer(waitingForRespawn.get(e.getPlayer().getUniqueId()));
            waitingForRespawn.remove(e.getPlayer().getUniqueId());
            if(waitingForRespawnPlayer.getPlayer() != null && waitingForRespawnPlayer.getPlayer().getGameMode() == GameMode.SPECTATOR){

                Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable() {
                    @Override
                    public void run() {
                        waitingForRespawnPlayer.getPlayer().teleport(e.getPlayer().getEyeLocation());
                    }
                },1);
                Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable() {
                    @Override
                    public void run() {
                        waitingForRespawnPlayer.getPlayer().setSpectatorTarget(e.getPlayer());
                    }
                },2);
            }
        }
    }
}
