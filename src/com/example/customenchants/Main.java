package com.example.customenchants;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new EnchantListener(), this);
        getCommand("enchant").setExecutor(new EnchantCommand());
        getLogger().info("Custom Enchants enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Custom Enchants disabled!");
    }
}