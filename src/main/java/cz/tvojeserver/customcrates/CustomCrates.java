package cz.tvojeserver.customcrates;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CustomCrates extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private final Set<UUID> openingPlayers = new HashSet<>();
    private final String GUI_TITLE = ChatColor.DARK_PURPLE + "✦ ɢᴀᴍʙᴀ ᴄʀᴀᴛᴇ ✦";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("crate").setExecutor(this);
        getCommand("crate").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("CustomCrates plugin byl uspesne zapnut!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("customcrate.admin") && sender instanceof Player) {
            sender.sendMessage(ChatColor.RED + "Nemáš oprávnění.");
            return true;
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Hráč nebyl nalezen.");
                return true;
            }

            ItemStack key = createKeyItem();
            target.getInventory().addItem(key);
            
            // Zde byla opravena chyba s chybějícími závorkami u target.getName()
            sender.sendMessage(ChatColor.GREEN + "Hráči " + target.getName() + " byl dán gamba klíč!");
            target.sendMessage(ChatColor.GOLD + "Dostal jsi Gamba klíč! Otevři bednu příkazem /crate open.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("open")) {
            if (!(sender instanceof Player player)) return true;
            
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (!isKey(hand)) {
                player.sendMessage(ChatColor.RED + "Musíš držet Gamba klíč v hlavní ruce!");
                return true;
            }

            if (openingPlayers.contains(player.getUniqueId())) return true;

            hand.setAmount(hand.getAmount() - 1);
            startRoulette(player);
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Použití: /crate give <hráč> | /crate open");
        return true;
    }

    private ItemStack createKeyItem() {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ Gamba Klíč ✦");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Otevři s ním bednu a získej",
                    ChatColor.GRAY + "legendární odměnu!",
                    "",
                    ChatColor.YELLOW + "▸ Napiš /crate open"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isKey(ItemStack item) {
        if (item == null || item.getType() != Material.TRIPWIRE_HOOK) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Gamba Klíč");
    }

    private void startRoulette(Player player) {
        openingPlayers.add(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        player.openInventory(inv);

        List<ItemStack> pool = getRewardPool();

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.getOpenInventory().getTitle().equals(GUI_TITLE)) {
                    openingPlayers.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                Random rand = new Random();
                for (int i = 0; i < 27; i++) {
                    inv.setItem(i, pool.get(rand.nextInt(pool.size())));
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);

                step++;
                if (step > 25) {
                    ItemStack reward = pool.get(rand.nextInt(pool.size()));
                    for (int i = 0; i < 27; i++) {
                        inv.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
                    }
                    inv.setItem(13, reward);
                    player.getInventory().addItem(reward);
                    
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVEL_UP, 1f, 1f);
                    String rewardName = (reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()) 
                            ? reward.getItemMeta().getDisplayName() 
                            : reward.getType().name();
                    player.sendTitle(ChatColor.GOLD + "VÝHRA!", ChatColor.YELLOW + rewardName, 10, 40, 10);
                    
                    openingPlayers.remove(player.getUniqueId());
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && player.getOpenInventory().getTitle().equals(GUI_TITLE)) {
                                player.closeInventory();
                            }
                        }
                    }.runTaskLater(CustomCrates.this, 60L);
                    
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    private List<ItemStack> getRewardPool() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(Material.DIAMOND, 16));
        list.add(new ItemStack(Material.NETHERITE_INGOT, 2));
        list.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4));
        list.add(new ItemStack(Material.EMERALD, 32));
        
        ItemStack customSword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = customSword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Gamba Meč");
            customSword.setItemMeta(meta);
        }
        list.add(customSword);
        return list;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("give", "open");
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());
            return players;
        }
        return null;
    }
}
