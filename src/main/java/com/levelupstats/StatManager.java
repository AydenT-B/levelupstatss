package com.levelupstats;

import com.levelupstats.network.SyncStatsPayload;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side logic: spending levels, applying attribute modifiers and syncing to the client.
 */
public final class StatManager {
    private StatManager() {
    }

    public static PlayerStats stats(ServerPlayer player) {
        return player.getData(ModAttachments.STATS.get());
    }

    public static void tryUpgrade(ServerPlayer player, int statIndex) {
        StatType[] types = StatType.values();
        if (statIndex < 0 || statIndex >= types.length) {
            return;
        }
        StatType type = types[statIndex];
        PlayerStats stats = stats(player);

        if (stats.isMaxed(type)) {
            player.displayClientMessage(Component.translatable("message.levelupstats.maxed"), true);
            return;
        }

        int cost = stats.costFor(type);
        if (player.experienceLevel < cost) {
            player.displayClientMessage(Component.translatable("message.levelupstats.not_enough", cost), true);
            return;
        }

        player.giveExperienceLevels(-cost);
        stats.set(type, stats.get(type) + 1);
        player.setData(ModAttachments.STATS.get(), stats);

        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.3F);
        applyAndSync(player);
    }

    public static void tryPrestige(ServerPlayer player) {
        PlayerStats stats = stats(player);
        if (!stats.allMaxed()) {
            player.displayClientMessage(Component.translatable("message.levelupstats.prestige_locked", stats.cap()), true);
            return;
        }
        stats.prestige();
        player.setData(ModAttachments.STATS.get(), stats);
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable("message.levelupstats.prestiged", stats.cap()), true);
        applyAndSync(player);
    }

    public static void applyAndSync(ServerPlayer player) {
        applyAll(player);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncStatsPayload(stats(player).copy()));
    }

    /** (Re)applies every attribute modifier from the player's current stat levels. Safe to call repeatedly. */
    public static void applyAll(ServerPlayer player) {
        PlayerStats s = stats(player);
        float oldMaxHealth = player.getMaxHealth();

        setModifier(player, Attributes.MOVEMENT_SPEED, StatType.SPEED.modifierId(),
                s.get(StatType.SPEED) * StatEffects.SPEED_PER_LEVEL, Operation.ADD_MULTIPLIED_BASE);

        setModifier(player, Attributes.JUMP_STRENGTH, StatType.JUMP.modifierId(),
                s.get(StatType.JUMP) * StatEffects.JUMP_PER_LEVEL, Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, StatType.JUMP.modifierId(),
                s.get(StatType.JUMP) * StatEffects.SAFE_FALL_PER_LEVEL, Operation.ADD_VALUE);

        setModifier(player, Attributes.ATTACK_DAMAGE, StatType.STRENGTH.modifierId(),
                s.get(StatType.STRENGTH) * StatEffects.STRENGTH_PER_LEVEL, Operation.ADD_VALUE);

        setModifier(player, Attributes.LUCK, StatType.LUCK.modifierId(),
                s.get(StatType.LUCK) * StatEffects.LUCK_ATTRIBUTE_PER_LEVEL, Operation.ADD_VALUE);

        setModifier(player, Attributes.MAX_HEALTH, StatType.CONSTITUTION.modifierId(),
                s.get(StatType.CONSTITUTION) * StatEffects.HEALTH_PER_LEVEL, Operation.ADD_VALUE);

        float newMaxHealth = player.getMaxHealth();
        if (newMaxHealth > oldMaxHealth) {
            player.heal(newMaxHealth - oldMaxHealth);
        } else if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        }
    }

    private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
                                    double amount, Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
