package com.Rayne.EnchantGUI;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("EnchantGUIPlugin enabled!");
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

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() != null && event.getItem().getType() == Material.DIAMOND_PICKAXE && event.getAction().toString().contains("RIGHT_CLICK")) {
            openEnchantGUI(player);
        }
    }

    private void openEnchantGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_GREEN + "Enchantments");

        gui.setItem(2, createMenuItem(ChatColor.GOLD + "Fortune", ChatColor.GRAY + "Click to buy or upgrade Fortune"));
        gui.setItem(4, createMenuItem(ChatColor.YELLOW + "Efficiency", ChatColor.GRAY + "Click to buy or upgrade Efficiency"));
        gui.setItem(6, createMenuItem(ChatColor.AQUA + "Unbreaking", ChatColor.GRAY + "Click to buy or upgrade Unbreaking"));

        player.openInventory(gui);
    }

    private ItemStack createMenuItem(String name, String... lore) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Enchantments")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack pickaxe = player.getInventory().getItemInMainHand();

            if (pickaxe.getType() != Material.DIAMOND_PICKAXE) {
                player.sendMessage(ChatColor.RED + "You need to hold a Diamond Pickaxe!");
                return;
            }

            String itemName = Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getDisplayName();
            Enchantment enchantment = null;
            int baseCost = 100; // Default cost

            if (itemName.equals(ChatColor.GOLD + "Fortune")) {
                enchantment = Enchantment.LOOT_BONUS_BLOCKS;
                baseCost = 5000;
            } else if (itemName.equals(ChatColor.YELLOW + "Efficiency")) {
                enchantment = Enchantment.DIG_SPEED;
                baseCost = 500;
            } else if (itemName.equals(ChatColor.AQUA + "Unbreaking")) {
                enchantment = Enchantment.DURABILITY;
                baseCost = 525;
            }


            if (enchantment != null) {
                int currentLevel = pickaxe.getEnchantmentLevel(enchantment);
                int cost = baseCost * (currentLevel + 1);

                if (economy.getBalance(player) >= cost) {
                    economy.withdrawPlayer(player, cost);
                    pickaxe.addUnsafeEnchantment(enchantment, currentLevel + 1);
                    player.sendMessage(ChatColor.GREEN + "Upgraded " + enchantment.getKey().getKey() + " to level " + (currentLevel + 1) + " for " + cost + "!");
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have enough money! Cost: " + cost);
                }
            }
        }
    }
}
