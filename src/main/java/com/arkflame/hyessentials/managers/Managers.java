package com.arkflame.hyessentials.managers;

import com.arkflame.hyessentials.HyEssentials;
import com.arkflame.hyessentials.data.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

// ==================== PERMISSION MANAGER ====================

public class PermissionManager {
    private final HyEssentials plugin;
    private final Map<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerPermissions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerGroups = new ConcurrentHashMap<>();
    private final File groupsFile;
    
    public PermissionManager(HyEssentials plugin) {
        this.plugin = plugin;
        this.groupsFile = new File(plugin.getDataFolder(), "groups.yml");
    }
    
    public void load() {
        plugin.getTaskRunner().runAsync(() -> {
            // Load groups from file
            if (!groupsFile.exists()) {
                createDefaultGroups();
            }
            // Load logic here using YML parser
        });
    }
    
    private void createDefaultGroups() {
        PermissionGroup defaultGroup = new PermissionGroup("default", 0);
        defaultGroup.addPermission("essentials.help");
        groups.put("default", defaultGroup);
        
        PermissionGroup vip = new PermissionGroup("vip", 10);
        vip.addPermission("essentials.fly");
        vip.setPrefix("&6[VIP]&r ");
        groups.put("vip", vip);
        
        PermissionGroup admin = new PermissionGroup("admin", 100);
        admin.addPermission("essentials.*");
        admin.setPrefix("&c[Admin]&r ");
        groups.put("admin", admin);
    }
    
    public boolean hasPermission(UUID uuid, String permission) {
        // Check player-specific permissions
        Set<String> perms = playerPermissions.getOrDefault(uuid, new HashSet<>());
        if (perms.contains(permission) || perms.contains("*")) {
            return true;
        }
        
        // Check group permissions
        String groupName = playerGroups.get(uuid);
        if (groupName != null) {
            PermissionGroup group = groups.get(groupName);
            if (group != null && group.hasPermission(permission)) {
                return true;
            }
        }
        
        // Check wildcard permissions
        String[] parts = permission.split("\\.");
        StringBuilder wildcard = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            wildcard.append(parts[i]).append(".");
            if (perms.contains(wildcard + "*")) {
                return true;
            }
        }
        
        return false;
    }
    
    public void addPermission(UUID uuid, String permission) {
        playerPermissions.computeIfAbsent(uuid, k -> new HashSet<>()).add(permission);
    }
    
    public void removePermission(UUID uuid, String permission) {
        Set<String> perms = playerPermissions.get(uuid);
        if (perms != null) {
            perms.remove(permission);
        }
    }
    
    public void setGroup(UUID uuid, String groupName) {
        if (groups.containsKey(groupName)) {
            playerGroups.put(uuid, groupName);
        }
    }
    
    public String getGroup(UUID uuid) {
        return playerGroups.getOrDefault(uuid, "default");
    }
    
    public PermissionGroup getGroupObject(String name) {
        return groups.get(name);
    }
    
    public void createGroup(String name, int priority) {
        groups.put(name, new PermissionGroup(name, priority));
    }
    
    public void deleteGroup(String name) {
        groups.remove(name);
    }
    
    public Collection<PermissionGroup> getAllGroups() {
        return groups.values();
    }
}

// ==================== USER DATA MANAGER ====================

public class UserDataManager {
    private final HyEssentials plugin;
    private final File userDataFolder;
    private final Map<UUID, UserData> loadedUsers = new ConcurrentHashMap<>();
    
    public UserDataManager(HyEssentials plugin) {
        this.plugin = plugin;
        this.userDataFolder = new File(plugin.getDataFolder(), "userdata");
        userDataFolder.mkdirs();
    }
    
    public CompletableFuture<UserData> loadUser(UUID uuid) {
        return plugin.getTaskRunner().supplyAsync(() -> {
            if (loadedUsers.containsKey(uuid)) {
                return loadedUsers.get(uuid);
            }
            
            File userFile = new File(userDataFolder, uuid.toString() + ".yml");
            UserData data = new UserData(uuid);
            
            if (userFile.exists()) {
                // Load from file using YML parser
                // data.load(userFile);
            }
            
            loadedUsers.put(uuid, data);
            return data;
        });
    }
    
    public void saveUser(UUID uuid) {
        UserData data = loadedUsers.get(uuid);
        if (data != null) {
            plugin.getTaskRunner().runAsync(() -> {
                File userFile = new File(userDataFolder, uuid.toString() + ".yml");
                // Save to file using YML writer
                // data.save(userFile);
            });
        }
    }
    
    public void saveAll() {
        for (UUID uuid : loadedUsers.keySet()) {
            saveUser(uuid);
        }
    }
    
    public UserData getUserData(UUID uuid) {
        return loadedUsers.get(uuid);
    }
    
    public boolean isFirstJoin(UUID uuid) {
        UserData data = loadedUsers.get(uuid);
        return data == null || data.isFirstJoin();
    }
    
    public void setFirstJoin(UUID uuid, boolean firstJoin) {
        UserData data = loadedUsers.get(uuid);
        if (data != null) {
            data.setFirstJoin(firstJoin);
        }
    }
}

// ==================== TELEPORT MANAGER ====================

public class TeleportManager {
    private final HyEssentials plugin;
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> tpaRequests = new ConcurrentHashMap<>();
    
    public TeleportManager(HyEssentials plugin) {
        this.plugin = plugin;
    }
    
    public void requestTeleport(Player player, Vector3 destination, TeleportType type) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown
        if (isOnCooldown(uuid)) {
            long remaining = getCooldownRemaining(uuid);
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "teleport_cooldown")
                .replace("{time}", String.valueOf(remaining)));
            return;
        }
        
        int warmup = plugin.getConfigManager().getTeleportWarmup();
        
        if (warmup > 0) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "teleport_warmup")
                .replace("{time}", String.valueOf(warmup)));
            
            PendingTeleport pending = new PendingTeleport(player, destination, type);
            pendingTeleports.put(uuid, pending);
            
            plugin.getTaskRunner().runDelayed(() -> {
                if (pendingTeleports.remove(uuid) != null) {
                    executeTeleport(player, destination);
                    setCooldown(uuid);
                }
            }, warmup, TimeUnit.SECONDS);
        } else {
            executeTeleport(player, destination);
            setCooldown(uuid);
        }
    }
    
    private void executeTeleport(Player player, Vector3 destination) {
        // Store current location for /back
        plugin.getBackManager().addLocation(player.getUniqueId(), player.getPosition());
        
        player.setPosition(destination);
        player.sendMessage(plugin.getLanguageManager().getMessage(player, "teleport_success"));
    }
    
    public void cancelTeleport(UUID uuid) {
        pendingTeleports.remove(uuid);
    }
    
    public boolean hasPendingTeleport(UUID uuid) {
        return pendingTeleports.containsKey(uuid);
    }
    
    public void sendTpaRequest(UUID sender, UUID target) {
        tpaRequests.put(target, sender);
        
        // Auto-expire after 60 seconds
        plugin.getTaskRunner().runAsyncDelayed(() -> {
            tpaRequests.remove(target, sender);
        }, 60, TimeUnit.SECONDS);
    }
    
    public UUID getTpaRequest(UUID target) {
        return tpaRequests.get(target);
    }
    
    public void removeTpaRequest(UUID target) {
        tpaRequests.remove(target);
    }
    
    private boolean isOnCooldown(UUID uuid) {
        Long cooldownEnd = teleportCooldowns.get(uuid);
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }
    
    private long getCooldownRemaining(UUID uuid) {
        Long cooldownEnd = teleportCooldowns.get(uuid);
        if (cooldownEnd == null) return 0;
        return (cooldownEnd - System.currentTimeMillis()) / 1000;
    }
    
    private void setCooldown(UUID uuid) {
        int cooldown = plugin.getConfigManager().getTeleportCooldown();
        if (cooldown > 0) {
            teleportCooldowns.put(uuid, System.currentTimeMillis() + (cooldown * 1000L));
        }
    }
}

// ==================== HOME MANAGER ====================

public class HomeManager {
    private final HyEssentials plugin;
    private final Map<UUID, Map<String, Vector3>> homes = new ConcurrentHashMap<>();
    
    public HomeManager(HyEssentials plugin) {
        this.plugin = plugin;
    }
    
    public void setHome(UUID uuid, String name, Vector3 location) {
        homes.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(name, location);
    }
    
    public Vector3 getHome(UUID uuid, String name) {
        Map<String, Vector3> playerHomes = homes.get(uuid);
        return playerHomes != null ? playerHomes.get(name) : null;
    }
    
    public void deleteHome(UUID uuid, String name) {
        Map<String, Vector3> playerHomes = homes.get(uuid);
        if (playerHomes != null) {
            playerHomes.remove(name);
        }
    }
    
    public Set<String> getHomes(UUID uuid) {
        Map<String, Vector3> playerHomes = homes.get(uuid);
        return playerHomes != null ? playerHomes.keySet() : Collections.emptySet();
    }
    
    public int getHomeCount(UUID uuid) {
        Map<String, Vector3> playerHomes = homes.get(uuid);
        return playerHomes != null ? playerHomes.size() : 0;
    }
    
    public int getMaxHomes(UUID uuid) {
        // Check permissions for max homes
        if (plugin.getPermissionManager().hasPermission(uuid, "essentials.sethome.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        for (int i = 100; i >= 1; i--) {
            if (plugin.getPermissionManager().hasPermission(uuid, "essentials.sethome." + i)) {
                return i;
            }
        }
        
        return plugin.getConfigManager().getDefaultMaxHomes();
    }
}

// ==================== WARP MANAGER ====================

public class WarpManager {
    private final HyEssentials plugin;
    private final Map<String, Warp> warps = new ConcurrentHashMap<>();
    private final File warpsFile;
    
    public WarpManager(HyEssentials plugin) {
        this.plugin = plugin;
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");
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

// ==================== SPAWN MANAGER ====================

public class SpawnManager {
    private final HyEssentials plugin;
    private Vector3 spawn;
    private final File spawnFile;
    
    public SpawnManager(HyEssentials plugin) {
        this.plugin = plugin;
        this.spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
    }
    
    public void load() {
        plugin.getTaskRunner().runAsync(() -> {
            if (spawnFile.exists()) {
                // Load spawn from file
            }
        });
    }
    
    public void setSpawn(Vector3 location) {
        this.spawn = location;
    }
    
    public Vector3 getSpawn() {
        return spawn;
    }
}

// ==================== BACK MANAGER ====================

public class BackManager {
    private final HyEssentials plugin;
    private final Map<UUID, LinkedList<Vector3>> locationHistory = new ConcurrentHashMap<>();
    private final int maxHistory = 5;
    
    public BackManager(HyEssentials plugin) {
        this.plugin = plugin;
    }
    
    public void addLocation(UUID uuid, Vector3 location) {
        LinkedList<Vector3> history = locationHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
        history.addFirst(location);
        
        if (history.size() > maxHistory) {
            history.removeLast();
        }
    }
    
    public Vector3 getLastLocation(UUID uuid) {
        LinkedList<Vector3> history = locationHistory.get(uuid);
        return history != null && !history.isEmpty() ? history.removeFirst() : null;
    }
    
    public boolean hasHistory(UUID uuid) {
        LinkedList<Vector3> history = locationHistory.get(uuid);
        return history != null && !history.isEmpty();
    }
}

// ==================== CHAT MANAGER ====================

public class ChatManager {
    private final HyEssentials plugin;
    
    public ChatManager(HyEssentials plugin) {
        this.plugin = plugin;
    }
    
    public String formatChat(Player player, String message) {
        UUID uuid = player.getUniqueId();
        
        // Get player's group
        String groupName = plugin.getPermissionManager().getGroup(uuid);
        PermissionGroup group = plugin.getPermissionManager().getGroupObject(groupName);
        
        String format = plugin.getConfigManager().getChatFormat();
        
        if (group != null) {
            String prefix = group.getPrefix() != null ? group.getPrefix() : "";
            String suffix = group.getSuffix() != null ? group.getSuffix() : "";
            
            format = format.replace("{prefix}", prefix)
                           .replace("{suffix}", suffix);
        } else {
            format = format.replace("{prefix}", "").replace("{suffix}", "");
        }
        
        format = format.replace("{player}", player.getName())
                       .replace("{message}", message);
        
        // Handle color permissions
        if (!plugin.getPermissionManager().hasPermission(uuid, "essentials.chat.color")) {
            message = stripColors(message);
        }
        
        return colorize(format);
    }
    
    private String colorize(String text) {
        return text.replace("&", "§");
    }
    
    private String stripColors(String text) {
        return text.replaceAll("&[0-9a-fk-or]", "");
    }
}

// ==================== MUTE MANAGER ====================

public class MuteManager {
    private final Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();
    
    public void mute(UUID uuid, long duration) {
        if (duration > 0) {
            mutedPlayers.put(uuid, System.currentTimeMillis() + duration);
        } else {
            mutedPlayers.put(uuid, Long.MAX_VALUE);
        }
    }
    
    public void unmute(UUID uuid) {
        mutedPlayers.remove(uuid);
    }
    
    public boolean isMuted(UUID uuid) {
        Long muteEnd = mutedPlayers.get(uuid);
        if (muteEnd == null) return false;
        
        if (muteEnd != Long.MAX_VALUE && System.currentTimeMillis() > muteEnd) {
            mutedPlayers.remove(uuid);
            return false;
        }
        
        return true;
    }
}

// ==================== IGNORE MANAGER ====================

public class IgnoreManager {
    private final Map<UUID, Set<UUID>> ignoredPlayers = new ConcurrentHashMap<>();
    
    public void ignore(UUID uuid, UUID target) {
        ignoredPlayers.computeIfAbsent(uuid, k -> new HashSet<>()).add(target);
    }
    
    public void unignore(UUID uuid, UUID target) {
        Set<UUID> ignored = ignoredPlayers.get(uuid);
        if (ignored != null) {
            ignored.remove(target);
        }
    }
    
    public boolean isIgnoring(UUID uuid, UUID target) {
        Set<UUID> ignored = ignoredPlayers.get(uuid);
        return ignored != null && ignored.contains(target);
    }
}

// ==================== KIT MANAGER ====================

public class KitManager {
    private final HyEssentials plugin;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> kitCooldowns = new ConcurrentHashMap<>();
    private final File kitsFile;
    
    public KitManager(HyEssentials plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
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