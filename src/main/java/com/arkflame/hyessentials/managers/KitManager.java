package com.arkflame.hyessentials.managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.arkflame.hyessentials.HyEssentials;
import com.hypixel.hytale.server.core.inventory.ItemStack;

public class KitManager {
    private final HyEssentials plugin;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> kitCooldowns = new ConcurrentHashMap<>();
    private final File kitsFile;
    
    public KitManager(HyEssentials plugin) {
        this.plugin = plugin;
        try {
        	// Just to be safe
        	Path load = plugin.getDataDirectory().resolve("kits.yml");
        	if (Files.notExists(load)) { Files.createFile(load); }
            this.kitsFile = load.toFile();
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    public void load() {
        plugin.getTaskRunner().runAsync(() -> {
            if (kitsFile.exists()) {
                // Load kits from file
            }
        });
    }
    
    public void createKit(String name, List<ItemStack> items, long cooldown, double price) {
        kits.put(name.toLowerCase(), new Kit(name, items, cooldown, price));
    }
    
    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }
    
    public void deleteKit(String name) {
        kits.remove(name.toLowerCase());
    }
    
    public Set<String> getKitNames() {
        return kits.keySet();
    }
    
    public boolean canUseKit(UUID uuid, String kitName) {
        return plugin.getPermissionManager().hasPermission(uuid, "essentials.kits." + kitName) ||
               plugin.getPermissionManager().hasPermission(uuid, "essentials.kits.*");
    }
    
    public boolean isOnCooldown(UUID uuid, String kitName) {
        Map<String, Long> cooldowns = kitCooldowns.get(uuid);
        if (cooldowns == null) return false;
        
        Long cooldownEnd = cooldowns.get(kitName);
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }
    
    public void setCooldown(UUID uuid, String kitName, long cooldown) {
        kitCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .put(kitName, System.currentTimeMillis() + cooldown);
    }
}