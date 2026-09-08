package com.example.customenchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class EnchantCommand implements CommandExecutor {

    // Соответствие зачарования -> подходящий предмет по умолчанию
    private final Map<String, Material> defaultMaterials = new HashMap<>();

    public EnchantCommand() {
        defaultMaterials.put(EnchantListener.BULLDOZER, Material.DIAMOND_PICKAXE);
        defaultMaterials.put(EnchantListener.AUTO_SMELT, Material.DIAMOND_PICKAXE);
        defaultMaterials.put(EnchantListener.TELEKINESIS, Material.DIAMOND_PICKAXE);
        defaultMaterials.put(EnchantListener.WEBBING, Material.DIAMOND_PICKAXE);
        defaultMaterials.put(EnchantListener.EXPERIENCED, Material.DIAMOND_PICKAXE);
        defaultMaterials.put(EnchantListener.BLEEDING, Material.DIAMOND_SWORD);
        defaultMaterials.put(EnchantListener.SMOKE_SCREEN, Material.TRIDENT);
        defaultMaterials.put(EnchantListener.MUDDY_STRIKE, Material.DIAMOND_SWORD);
        defaultMaterials.put(EnchantListener.BLINDING, Material.DIAMOND_SWORD);
        defaultMaterials.put(EnchantListener.AUTO_PLANT, Material.DIAMOND_HOE);
        defaultMaterials.put(EnchantListener.LUMBERJACK, Material.DIAMOND_AXE);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("enchant.give")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав!");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Использование: /enchant give <игрок> <название> [уровень]");
            sender.sendMessage(ChatColor.YELLOW + "Доступные зачарования: " +
                    "Бульдозер, Автоплавка, Телекинез, Паутина, Опытный, Кровотечение, Дымовая завеса, " +
                    "Мутный удар, Ослепление, Автопосев, Лесоруб");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок не найден!");
            return true;
        }

        String enchantName = args[1];
        int level = 1;
        if (args.length >= 3) {
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Уровень должен быть числом!");
                return true;
            }
        }

        // Проверяем, есть ли такое зачарование
        Material defaultMat = defaultMaterials.get(enchantName);
        if (defaultMat == null) {
            sender.sendMessage(ChatColor.RED + "Неизвестное зачарование! Список выше.");
            return true;
        }

        ItemStack item = new ItemStack(defaultMat, 1);
        EnchantUtils.addEnchant(item, enchantName, level);
        target.getInventory().addItem(item);
        sender.sendMessage(ChatColor.GREEN + "Выдали игроку " + target.getName() +
                " предмет с зачарованием " + enchantName + " " + level);
        return true;
    }
}