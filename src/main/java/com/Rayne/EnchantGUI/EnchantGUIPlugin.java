package com.Rayne.EnchantGUI;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public class EnchantGUIPlugin extends JavaPlugin implements Listener {

    private static Economy economy = null;
    private long lastClickTime = 0;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("EnchantGUIPlugin enabled!");
        getCommand("enchantgui").setExecutor(this);
        getCommand("version").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info("EnchantGUIPlugin disabled!");
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    public static Economy getEconomy() {
        return economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("enchantgui")) {

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("enchantgui.reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reload the plugin.");
                    return false;
                }

                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "EnchantGUI plugin reloaded successfully!");
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
                sender.sendMessage(ChatColor.AQUA + "Version: " + ChatColor.WHITE + getDescription().getVersion() + "\n" + ChatColor.AQUA + "Made by: " + ChatColor.WHITE + "Rayne" + "\n" + ChatColor.AQUA + "Discord: " + ChatColor.WHITE + "https://discord.gg/nrFSp8VugF");
                return true;
            }

            return false;
        }
        return false;
    }


    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() != null && event.getItem().getType() == Material.DIAMOND_PICKAXE && event.getAction().toString().contains("RIGHT_CLICK")) {
            openEnchantGUI(player);
        }
    }

    private void openEnchantGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.AQUA + "GGMC " + ChatColor.BLUE + "Enchantments");

        // Get the current item (Diamond Pickaxe) in the player's hand
        ItemStack pickaxe = player.getInventory().getItemInMainHand();

        // Fetch the enchantments and base costs from config
        int fortuneBaseCost = getConfig().getInt("enchantments.fortune.basecost");
        int efficiencyBaseCost = getConfig().getInt("enchantments.efficiency.basecost");
        int unbreakingBaseCost = getConfig().getInt("enchantments.unbreaking.basecost");

        // Get the current level of each enchantment on the player's pickaxe
        int fortuneCurrentLevel = pickaxe.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
        int efficiencyCurrentLevel = pickaxe.getEnchantmentLevel(Enchantment.DIG_SPEED);
        int unbreakingCurrentLevel = pickaxe.getEnchantmentLevel(Enchantment.DURABILITY);

        // Calculate the cost for each enchantment based on the current level and multi factor
        int fortuneCost = getConfig().getInt("enchantments.multi") * (fortuneCurrentLevel + 1) * fortuneBaseCost;
        int efficiencyCost = getConfig().getInt("enchantments.multi") * (efficiencyCurrentLevel + 1) * efficiencyBaseCost;
        int unbreakingCost = getConfig().getInt("enchantments.multi") * (unbreakingCurrentLevel + 1) * unbreakingBaseCost;

        // Set the items in the inventory with the correct dynamic cost
        gui.setItem(2, createMenuItem(ChatColor.GOLD + "Fortune", ChatColor.GRAY + "Click to buy or upgrade Fortune", fortuneCost));
        gui.setItem(4, createMenuItem(ChatColor.YELLOW + "Efficiency", ChatColor.GRAY + "Click to buy or upgrade Efficiency", efficiencyCost));
        gui.setItem(6, createMenuItem(ChatColor.AQUA + "Unbreaking", ChatColor.GRAY + "Click to buy or upgrade Unbreaking", unbreakingCost));

        player.openInventory(gui);
    }



    private ItemStack createMenuItem(String name, String lore, int cost) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        // Set the display name
        meta.setDisplayName(name);

        // Add lore, including the cost
        meta.setLore(List.of(lore, ChatColor.GREEN + "Cost: " + "$" + ChatColor.WHITE + cost));

        item.setItemMeta(meta);
        return item;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the inventory is the custom GUI
        if (event.getView().getTitle().equals(ChatColor.AQUA + "GGMC " + ChatColor.BLUE + "Enchantments")) {
            event.setCancelled(true); // Prevent all interactions

            // Ensure the click was inside the GUI itself
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }

            // Ensure the clicked item is valid
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack pickaxe = player.getInventory().getItemInMainHand();

            // Ensure the player is holding a Diamond Pickaxe
            if (pickaxe.getType() != Material.DIAMOND_PICKAXE) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You need to hold a Diamond Pickaxe!");
                return;
            }

            if (System.currentTimeMillis() - lastClickTime < 500) { // 500ms cooldown
                return;
            }
            lastClickTime = System.currentTimeMillis();

            String itemName = Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getDisplayName();
            Enchantment enchantment = null;
            int baseCost = 0;

            // Determine enchantment and cost
            if (itemName.equals(ChatColor.GOLD + "Fortune") && getConfig().getBoolean("enchantments.fortune.enabled")) {
                enchantment = Enchantment.LOOT_BONUS_BLOCKS;
                baseCost = getConfig().getInt("enchantments.fortune.basecost");
            } else if (itemName.equals(ChatColor.YELLOW + "Efficiency") && getConfig().getBoolean("enchantments.efficiency.enabled")) {
                enchantment = Enchantment.DIG_SPEED;
                baseCost = getConfig().getInt("enchantments.efficiency.basecost");
            } else if (itemName.equals(ChatColor.AQUA + "Unbreaking") && getConfig().getBoolean("enchantments.unbreaking.enabled")) {
                enchantment = Enchantment.DURABILITY;
                baseCost = getConfig().getInt("enchantments.unbreaking.basecost");
            } else {
                player.sendMessage(ChatColor.RED + "Unknown enchantment or not enabled.");
                return;
            }

            // Apply enchantment if valid
            if (enchantment != null) {
                int currentLevel = pickaxe.getEnchantmentLevel(enchantment);
                int cost = (currentLevel + 1) * baseCost * getConfig().getInt("enchantments.multi");

                if (currentLevel >= getConfig().getInt("enchantments.maxlevel")) {
                    player.sendMessage(ChatColor.RED + getConfig().getString("messages.max_enchant"));
                    return;
                }

                if (getEconomy().getBalance(player) >= cost) {
                    getEconomy().withdrawPlayer(player, cost);
                    pickaxe.addUnsafeEnchantment(enchantment, currentLevel + 1);
                    player.sendMessage(ChatColor.GREEN + "Upgraded " + enchantment.getKey().getKey() + " to level " + (currentLevel + 1) + " for " + cost + "!");
                    player.closeInventory();
                    openEnchantGUI(player); // Reopen GUI
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have enough money! Cost: " + cost);
                }
            }
        }
    }
}