package com.example.customenchants;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EnchantUtils {

    // Получить уровень зачарования по названию (римские цифры)
    public static int getEnchantLevel(ItemStack item, String enchantName) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        for (String line : meta.getLore()) {
            if (line.startsWith(enchantName)) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    return romanToInt(parts[1]);
                }
            }
        }
        return 0;
    }

    public static boolean hasEnchant(ItemStack item, String enchantName) {
        return getEnchantLevel(item, enchantName) > 0;
    }

    // Добавить зачарование в Lore предмета
    public static ItemStack addEnchant(ItemStack item, String enchantName, int level) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : meta.getLore();
        lore.removeIf(line -> line.startsWith(enchantName));
        lore.add(enchantName + " " + intToRoman(level));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static int romanToInt(String roman) {
        switch (roman) {
            case "I": return 1;
            case "II": return 2;
            case "III": return 3;
            case "IV": return 4;
            case "V": return 5;
            default: return 0;
        }
    }

    private static String intToRoman(int num) {
        switch (num) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return "";
        }
    }
}