package com.arkflame.hyessentials.managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.arkflame.hyessentials.HyEssentials;
import com.hypixel.hytale.builtin.hytalegenerator.fields.FastNoiseLite.Vector3;

public class WarpManager {
    private final HyEssentials plugin;
    private final Map<String, Warp> warps = new ConcurrentHashMap<>();
    private final File warpsFile;
    
    public WarpManager(HyEssentials plugin) {
        this.plugin = plugin;
        try {
        	// Just to be safe
        	Path load = plugin.getDataDirectory().resolve("warps.yml");
        	if (Files.notExists(load)) { Files.createFile(load); }
            this.warpsFile = load.toFile();
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    public void load() {
        plugin.getTaskRunner().runAsync(() -> {
            if (warpsFile.exists()) {
                // Load warps from file
            }
        });
    }
    
    public void setWarp(String name, Vector3 location) {
        warps.put(name.toLowerCase(), new Warp(name, location));
    }
    
    public Warp getWarp(String name) {
        return warps.get(name.toLowerCase());
    }
    
    public void deleteWarp(String name) {
        warps.remove(name.toLowerCase());
    }
    
    public Set<String> getWarpNames() {
        return warps.keySet();
    }
    
    public boolean hasAccess(UUID uuid, String warpName) {
        return plugin.getPermissionManager().hasPermission(uuid, "essentials.warp." + warpName) ||
               plugin.getPermissionManager().hasPermission(uuid, "essentials.warp.*");
    }
}