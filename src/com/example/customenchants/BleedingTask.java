package com.example.customenchants;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class BleedingTask extends BukkitRunnable {
    private final LivingEntity target;
    private int ticksLeft;

    public BleedingTask(LivingEntity target, int durationSeconds) {
        this.target = target;
        this.ticksLeft = durationSeconds * 20; // 20 тиков в секунде
    }

    @Override
    public void run() {
        if (target.isDead() || !target.isValid()) {
            this.cancel();
            return;
        }
        // Наносим урон (1 сердце = 2 хп)
        target.damage(2);
        // Каждые 40 тиков (2 секунды) срабатывает, пока не кончится время
        ticksLeft -= 40;
        if (ticksLeft <= 0) {
            this.cancel();
        }
    }
}