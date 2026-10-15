package com.levelupstats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Game-bus events that make the stats actually do things.
 *
 * <p>Luck works by temporarily adding Fortune / Looting levels while a block is being broken or a mob is
 * dying. Because it is added through {@link GetEnchantmentLevelEvent}, it stacks on top of any real
 * Fortune / Looting on the tool, so it is compatible with enchanted gear.
 */
@EventBusSubscriber(modid = LevelUpStats.MODID)
public class StatEvents {
    /** Extra Fortune levels active while the current block break's drops are generated (server thread only). */
    private static final ThreadLocal<Integer> FORTUNE_BONUS = new ThreadLocal<>();
    /** Extra Looting levels active while the current mob death's drops are generated (server thread only). */
    private static final ThreadLocal<Integer> LOOTING_BONUS = new ThreadLocal<>();
    /** True between an XP orb touching a player and the XP being awarded. */
    private static boolean orbPickupInProgress = false;

    // ---- keeping attributes + client in sync ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatManager.applyAndSync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatManager.applyAndSync(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatManager.applyAndSync(player);
        }
    }

    // ---- Luck: Fortune ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            int bonus = StatEffects.fortuneLootingBonus(StatManager.stats(player).get(StatType.LUCK));
            if (bonus > 0) {
                FORTUNE_BONUS.set(bonus);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockDrops(BlockDropsEvent event) {
        // Drops have already been rolled by the time this fires.
        FORTUNE_BONUS.remove();
    }

    // ---- Luck: Looting ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && !(event.getEntity() instanceof Player)) {
            int bonus = StatEffects.fortuneLootingBonus(StatManager.stats(player).get(StatType.LUCK));
            if (bonus > 0) {
                LOOTING_BONUS.set(bonus);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        LOOTING_BONUS.remove();

        // Vanilla only reads Looting from a held item, so with an empty hand the enchantment hook above
        // has nothing to attach to. In that case apply a simplified bonus directly to stackable drops.
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity() instanceof Mob
                && player.getMainHandItem().isEmpty()) {
            int bonus = StatEffects.fortuneLootingBonus(StatManager.stats(player).get(StatType.LUCK));
            if (bonus > 0) {
                for (ItemEntity drop : event.getDrops()) {
                    ItemStack stack = drop.getItem();
                    int room = stack.getMaxStackSize() - stack.getCount();
                    if (stack.getMaxStackSize() > 1 && room > 0) {
                        stack.grow(Math.min(room, player.getRandom().nextInt(bonus + 1)));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEnchantmentLevel(GetEnchantmentLevelEvent event) {
        Integer fortune = FORTUNE_BONUS.get();
        Integer looting = LOOTING_BONUS.get();
        if (fortune == null && looting == null) {
            return;
        }
        ItemEnchantments.Mutable enchantments = event.getEnchantments();
        if (fortune != null) {
            event.getHolder(Enchantments.FORTUNE).ifPresent(holder -> {
                if (event.isTargetting(holder)) {
                    enchantments.set(holder, enchantments.getLevel(holder) + fortune);
                }
            });
        }
        if (looting != null) {
            event.getHolder(Enchantments.LOOTING).ifPresent(holder -> {
                if (event.isTargetting(holder)) {
                    enchantments.set(holder, enchantments.getLevel(holder) + looting);
                }
            });
        }
    }

    // ---- Intelligence: XP orb multiplier ----

    @SubscribeEvent
    public static void onOrbPickup(PlayerXpEvent.PickupXp event) {
        if (event.getEntity() instanceof ServerPlayer) {
            orbPickupInProgress = true;
        }
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (!orbPickupInProgress) {
            return; // only orbs are boosted, not /xp or other sources
        }
        orbPickupInProgress = false;
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0) {
            int level = StatManager.stats(player).get(StatType.INTELLIGENCE);
            if (level > 0) {
                double scaled = event.getAmount() * StatEffects.xpMultiplier(level);
                int whole = (int) scaled;
                if (player.getRandom().nextDouble() < scaled - whole) {
                    whole++;
                }
                event.setAmount(whole);
            }
        }
    }

    // ---- safety net: never leave a temporary flag set ----

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        FORTUNE_BONUS.remove();
        LOOTING_BONUS.remove();
        orbPickupInProgress = false;
    }
}
