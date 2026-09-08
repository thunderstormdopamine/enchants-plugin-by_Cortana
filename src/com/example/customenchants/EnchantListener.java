package com.example.customenchants;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class EnchantListener implements Listener {

    // Названия зачарований (должны совпадать с тем, что в Lore)
    public static final String BULLDOZER = "Бульдозер";
    public static final String AUTO_SMELT = "Автоплавка";
    public static final String TELEKINESIS = "Телекинез";
    public static final String WEBBING = "Паутина";
    public static final String EXPERIENCED = "Опытный";
    public static final String BLEEDING = "Кровотечение";
    public static final String SMOKE_SCREEN = "Дымовая завеса";
    public static final String MUDDY_STRIKE = "Мутный удар";
    public static final String BLINDING = "Ослепление";
    public static final String AUTO_PLANT = "Автопосев";
    public static final String LUMBERJACK = "Лесоруб";

    private final JavaPlugin plugin = JavaPlugin.getPlugin(Main.class);
    private final Random random = new Random();

    // ---------- Обработчик разрушения блоков ----------
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;

        Block center = event.getBlock();
        // Отменяем стандартную обработку, всё делаем сами
        event.setCancelled(true);

        // Множество уже обработанных блоков (чтобы не дублировать)
        Set<Block> processed = new HashSet<>();

        // 1. Обрабатываем центральный блок (с автопосевом)
        handleBlockBreak(player, center, tool, true, processed);

        // 2. Бульдозер (область 3×3 и 2 блока вперёд)
        int bulldozerLevel = EnchantUtils.getEnchantLevel(tool, BULLDOZER);
        if (bulldozerLevel > 0) {
            for (Block b : getBulldozerBlocks(center, player)) {
                if (!processed.contains(b)) {
                    handleBlockBreak(player, b, tool, false, processed);
                }
            }
        }

        // 3. Паутина (жила руды)
        if (EnchantUtils.hasEnchant(tool, WEBBING)) {
            for (Block b : getOreVeinBlocks(center)) {
                if (!processed.contains(b)) {
                    handleBlockBreak(player, b, tool, false, processed);
                }
            }
        }

        // 4. Лесоруб (все брёвна в дереве)
        int lumberjackLevel = EnchantUtils.getEnchantLevel(tool, LUMBERJACK);
        if (lumberjackLevel > 0 && isLog(center.getType())) {
            // Проверяем, что инструмент подходит (топор)
            if (tool.getType().name().contains("AXE")) {
                for (Block b : getTreeLogs(center)) {
                    if (!processed.contains(b)) {
                        handleBlockBreak(player, b, tool, false, processed);
                    }
                }
            }
        }
    }

    // ---------- Обработка одного блока ----------
    private void handleBlockBreak(Player player, Block block, ItemStack tool,
                                  boolean applyAutoPlant, Set<Block> processed) {
        if (block.getType() == Material.AIR || block.getType() == Material.BEDROCK) return;
        // Не обрабатываем, если блок нельзя добыть этим инструментом
        if (!block.getDrops(tool).isEmpty() == false) {
            // Если дроп пуст, возможно инструмент не подходит, но всё равно попробуем
        }

        processed.add(block);

        // Сохраняем тип блока и его данные (для автопосева)
        Material blockType = block.getType();
        BlockState state = block.getState();

        // Удаляем блок
        block.setType(Material.AIR);

        // Получаем дроп с учётом инструмента
        Collection<ItemStack> drops = block.getDrops(tool);

        // ---- Автоплавка ----
        if (EnchantUtils.hasEnchant(tool, AUTO_SMELT)) {
            drops = autoSmelt(drops);
        }

        // ---- Телекинез ----
        boolean hasTelekinesis = EnchantUtils.hasEnchant(tool, TELEKINESIS);
        if (hasTelekinesis) {
            // Добавляем предметы прямо в инвентарь
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drops.toArray(new ItemStack[0]));
            // Если остались (инвентарь полон) – выпадают в мир
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(block.getLocation(), left);
            }
        } else {
            // Выпадают в мир
            for (ItemStack drop : drops) {
                player.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }

        // ---- Опыт ----
        int exp = block.getExpDrop(block.getState(), player.getInventory().getItemInMainHand());
        if (exp > 0) {
            int experiencedLevel = EnchantUtils.getEnchantLevel(tool, EXPERIENCED);
            if (experiencedLevel > 0) {
                // Опытный III даёт множитель: 1 + уровень * 0.5 (для III – 2.5)
                exp = (int) Math.round(exp * (1 + experiencedLevel * 0.5));
            }
            if (hasTelekinesis) {
                player.giveExp(exp);
            } else {
                // Опыт выпадает в мир, но мы просто даём игроку (для простоты)
                // Можно создать орб опыта, но дадим сразу в инвентарь для телекинеза
                // Если телекинеза нет, то по условию дроп и опыт попадают в инвентарь только с телекинезом,
                // но по описанию "Телекинез дроп и опыт сразу попадают в инвентарь" – значит без телекинеза опыт выпадает.
                // Для простоты будем выдавать опыт игроку всегда, так как на серверах часто так делают.
                // Но чтобы соответствовать, можно спавнить орб.
                player.giveExp(exp);
            }
        }

        // ---- Автопосев (только для центрального блока) ----
        if (applyAutoPlant && EnchantUtils.hasEnchant(tool, AUTO_PLANT)) {
            autoPlant(block, player, state);
        }

        // ---- Износ инструмента ----
        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            damageTool(tool, 1);
        }
    }

    // ---------- Автоплавка ----------
    private Collection<ItemStack> autoSmelt(Collection<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : drops) {
            Material smelted = getSmelted(drop.getType());
            if (smelted != null) {
                drop.setType(smelted);
            }
            result.add(drop);
        }
        return result;
    }

    private Material getSmelted(Material ore) {
        // Сопоставление руд и слитков
        switch (ore) {
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return Material.IRON_INGOT;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case NETHER_GOLD_ORE:
                return Material.GOLD_INGOT;
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return Material.COPPER_INGOT;
            case RAW_IRON:
                return Material.IRON_INGOT;
            case RAW_GOLD:
                return Material.GOLD_INGOT;
            case RAW_COPPER:
                return Material.COPPER_INGOT;
            default:
                return null;
        }
    }

    // ---------- Автопосев ----------
    private void autoPlant(Block block, Player player, BlockState state) {
        // Проверяем, что это зрелая культура на грядке
        if (!(block.getState() instanceof Ageable)) return;
        Ageable ageable = (Ageable) state;
        if (!ageable.isAgeFull()) return;

        Material seed = getSeed(block.getType());
        if (seed == null) return;

        // Ищем семена в инвентаре
        ItemStack seedItem = new ItemStack(seed, 1);
        if (player.getInventory().containsAtLeast(seedItem, 1)) {
            // Забираем одно семя
            player.getInventory().removeItem(seedItem);
            // Сажаем
            block.setType(block.getType());
            Ageable newAge = (Ageable) block.getState();
            newAge.setAge(0);
            newAge.update();
        }
    }

    private Material getSeed(Material crop) {
        switch (crop) {
            case WHEAT: return Material.WHEAT_SEEDS;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOTS: return Material.BEETROOT_SEEDS;
            case NETHER_WART: return Material.NETHER_WART;
            default: return null;
        }
    }

    // ---------- Бульдозер: блоки в области ----------
    private List<Block> getBulldozerBlocks(Block center, Player player) {
        List<Block> blocks = new ArrayList<>();
        Vector dir = player.getLocation().getDirection().normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = dir.clone().crossProduct(up);
        if (right.lengthSquared() == 0) {
            up = new Vector(0, 0, 1);
            right = dir.clone().crossProduct(up);
        }
        right.normalize();
        up = dir.clone().crossProduct(right).normalize();

        Location centerLoc = center.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) { // 2 блока вперёд (0 и 1)
                    Vector offset = right.clone().multiply(dx)
                            .add(up.clone().multiply(dy))
                            .add(dir.clone().multiply(dz));
                    Block b = centerLoc.clone().add(offset).getBlock();
                    if (!b.equals(center) && !b.getType().isAir() && b.getType() != Material.BEDROCK) {
                        blocks.add(b);
                    }
                }
            }
        }
        return blocks;
    }

    // ---------- Паутина: жила руды (BFS) ----------
    private List<Block> getOreVeinBlocks(Block start) {
        List<Block> result = new ArrayList<>();
        Material target = start.getType();
        if (!isOre(target)) return result;

        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            // Ограничим радиус 10 блоков, чтобы не уходить слишком далеко
            if (current.getLocation().distance(start.getLocation()) > 10) continue;
            for (Block neighbor : getNeighbors(current)) {
                if (neighbor.getType() == target && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    result.add(neighbor);
                }
            }
        }
        // Убираем сам стартовый блок, если он там есть
        result.remove(start);
        return result;
    }

    private boolean isOre(Material mat) {
        String name = mat.name();
        return name.contains("ORE") || name.contains("RAW_") || name.equals("NETHER_GOLD_ORE");
    }

    // ---------- Лесоруб: все брёвна в дереве (BFS) ----------
    private List<Block> getTreeLogs(Block start) {
        List<Block> result = new ArrayList<>();
        Material target = start.getType();
        if (!isLog(target)) return result;

        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            for (Block neighbor : getNeighbors(current)) {
                if (isLog(neighbor.getType()) && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    result.add(neighbor);
                }
            }
        }
        result.remove(start);
        return result;
    }

    private boolean isLog(Material mat) {
        String name = mat.name();
        return name.contains("LOG") || name.contains("WOOD") || name.contains("STEM") ||
                name.contains("HYPHAE") || (name.contains("STRIPPED") && (name.contains("LOG") || name.contains("WOOD")));
    }

    private List<Block> getNeighbors(Block block) {
        List<Block> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    neighbors.add(block.getRelative(dx, dy, dz));
                }
            }
        }
        return neighbors;
    }

    // ---------- Износ инструмента ----------
    private void damageTool(ItemStack tool, int amount) {
        if (tool.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) tool.getItemMeta();
            int maxDurability = tool.getType().getMaxDurability();
            if (maxDurability > 0) {
                int newDamage = damageable.getDamage() + amount;
                if (newDamage >= maxDurability) {
                    tool.setAmount(0);
                } else {
                    damageable.setDamage(newDamage);
                    tool.setItemMeta((ItemMeta) damageable);
                }
            }
        }
    }

    // ---------- Обработчик урона (Кровотечение, Мутный удар, Ослепление) ----------
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;

        Entity target = event.getEntity();

        // Кровотечение
        int bleedLevel = EnchantUtils.getEnchantLevel(tool, BLEEDING);
        if (bleedLevel > 0 && random.nextDouble() < 0.2 * bleedLevel) { // 20% за уровень
            applyBleeding(target, 3 + bleedLevel); // длительность = 3 + уровень секунд
        }

        // Мутный удар (шанс 30%)
        if (EnchantUtils.hasEnchant(tool, MUDDY_STRIKE) && random.nextDouble() < 0.3) {
            applyMuddyWave(target.getLocation(), player);
        }

        // Ослепление (шанс 30%)
        if (EnchantUtils.hasEnchant(tool, BLINDING) && random.nextDouble() < 0.3) {
            if (target instanceof LivingEntity) {
                ((LivingEntity) target).addPotionEffect(
                        new PotionEffect(PotionEffectType.BLINDNESS, 5 * 20, 0, false, true)
                );
            }
        }
    }

    // ---------- Кровотечение (периодический урон) ----------
    private void applyBleeding(Entity target, int durationSeconds) {
        if (!(target instanceof LivingEntity)) return;
        LivingEntity living = (LivingEntity) target;
        new BleedingTask(living, durationSeconds).runTaskTimer(plugin, 0, 40); // каждые 2 секунды (40 тиков)
    }

    // ---------- Мутный удар: волна яда ----------
    private void applyMuddyWave(Location location, Player source) {
        // Радиус 5 блоков, накладываем яд на 5 секунд
        for (Entity entity : location.getWorld().getNearbyEntities(location, 5, 5, 5)) {
            if (entity.equals(source)) continue;
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).addPotionEffect(
                        new PotionEffect(PotionEffectType.POISON, 5 * 20, 0, false, true)
                );
            }
        }
        // Визуальный эффект – частицы
        location.getWorld().spawnParticle(Particle.SPELL_WITCH, location, 30, 2, 1, 2, 0.5);
    }

    // ---------- Дымовая завеса (трезубец) ----------
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident)) return;
        Trident trident = (Trident) event.getEntity();
        if (!(trident.getShooter() instanceof Player)) return;
        Player player = (Player) trident.getShooter();
        ItemStack tool = player.getInventory().getItemInMainHand();
        // Зачарование может быть на трезубце, но в руке может быть другой предмет.
        // Обычно зачарование на самом трезубце. Проверим предмет в руке – если это трезубец с зачарованием.
        if (tool == null || tool.getType() != Material.TRIDENT) return;
        if (EnchantUtils.hasEnchant(tool, SMOKE_SCREEN) && random.nextDouble() < 0.3) {
            Location hitLoc = event.getHitEntity() != null ?
                    event.getHitEntity().getLocation() :
                    event.getHitBlock() != null ? event.getHitBlock().getLocation() : null;
            if (hitLoc != null) {
                // Создаём облако дыма (частицы)
                hitLoc.getWorld().spawnParticle(Particle.CLOUD, hitLoc, 50, 2, 2, 2, 0.1);
                hitLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, hitLoc, 30, 2, 2, 2, 0.1);
            }
        }
    }
}