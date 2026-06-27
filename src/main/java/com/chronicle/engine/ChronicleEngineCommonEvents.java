package com.chronicle.engine;

import com.chronicle.engine.data.NpcSpec;
import com.chronicle.engine.data.ChronicleEngineData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ChronicleEngineCommonEvents {
    private static final String DAMAGE_CREDIT_TAG = "chronicleDamageCredit";
    private static final int DAMAGE_CREDIT_TICKS = 20 * 60 * 10;
    private static final float MIN_CREDIT_DAMAGE = 1.0F;

    private ChronicleEngineCommonEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Entity target = event.getTarget();
        ResourceLocation entityType = EntityType.getKey(target.getType());
        List<NpcSpec> specs = ChronicleEngineData.npcsForEntityType(entityType.toString());
        if (specs.isEmpty()) {
            return;
        }

        for (NpcSpec spec : specs) {
            if (player.distanceToSqr(target) > spec.dialogueDistance() * spec.dialogueDistance()) {
                continue;
            }
            for (NpcSpec.Binding binding : spec.bindings()) {
                String dialogueId = resolveDialogueId(target, binding);
                if (dialogueId.isBlank()) {
                    continue;
                }
                if (!ChronicleEngineConditions.test(player, target, binding.condition())) {
                    continue;
                }
                if (target instanceof Mob mob && spec.shouldStopMoving()) {
                    mob.getNavigation().stop();
                }
                ChronicleEngineNpcFocusService.hold(player, target, spec.shouldStopMoving(), spec.shouldLookAtPlayer(), spec.dialogueDistance());
                ChronicleEngineDialogueService.open(player, dialogueId, target.getDisplayName());
                if (spec.cancelVanillaInteract()) {
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        ServerPlayer player = sourcePlayer(event.getSource());
        if (player == null || event.getAmount() < MIN_CREDIT_DAMAGE || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer targetPlayer && ChronicleEngineTeamService.shouldBlockFriendlyFire(player, targetPlayer)) {
            event.setAmount(0.0F);
            return;
        }
        LivingEntity target = event.getEntity();
        CompoundTag credit = target.getPersistentData().getCompound(DAMAGE_CREDIT_TAG);
        long now = target.level().getGameTime();
        String key = player.getUUID().toString();
        CompoundTag entry = credit.getCompound(key);
        entry.putUUID("player", player.getUUID());
        entry.putLong("lastTick", now);
        entry.putFloat("damage", entry.getFloat("damage") + event.getAmount());
        credit.put(key, entry);
        pruneDamageCredit(credit, now);
        target.getPersistentData().put(DAMAGE_CREDIT_TAG, credit);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        String entityType = EntityType.getKey(event.getEntity().getType()).toString();
        Set<ServerPlayer> players = creditedPlayers(event.getEntity(), event.getSource());
        for (ServerPlayer player : players) {
            ChronicleEngineQuestService.notifyObjective(player, "KILL", entityType, 1);
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAdvancement().getId() != null) {
            ChronicleEngineQuestService.notifyAdvancement(player, event.getAdvancement().getId().toString());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            ChronicleEngineNpcFocusService.tick(player);
            if (event.player.tickCount % 20 == 0) {
                ChronicleEngineQuestService.updateCollectedItems(player);
                ChronicleEngineQuestService.updateAdvancementObjectives(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChronicleEngineQuestService.syncMarkers(player);
            ChronicleEngineQuestService.syncTracker(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        ChronicleEnginePlayerData.copyOnClone(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChronicleEngineNpcFocusService.release(player);
        }
    }

    private static String resolveDialogueId(Entity target, NpcSpec.Binding binding) {
        if (!binding.dialogueIdFromNbt().isBlank()) {
            CompoundTag persistent = target.getPersistentData();
            if (persistent.contains(binding.dialogueIdFromNbt())) {
                return persistent.getString(binding.dialogueIdFromNbt());
            }
            CompoundTag save = new CompoundTag();
            target.saveWithoutId(save);
            if (save.contains(binding.dialogueIdFromNbt())) {
                return save.getString(binding.dialogueIdFromNbt());
            }
        }
        return binding.dialogueId();
    }

    private static ServerPlayer sourcePlayer(DamageSource source) {
        return source.getEntity() instanceof ServerPlayer player ? player : null;
    }

    private static Set<ServerPlayer> creditedPlayers(LivingEntity entity, DamageSource source) {
        Set<ServerPlayer> players = new LinkedHashSet<>();
        ServerPlayer direct = sourcePlayer(source);
        if (direct != null) {
            players.add(direct);
        }
        if (entity.getServer() == null || !entity.getPersistentData().contains(DAMAGE_CREDIT_TAG)) {
            return players;
        }
        CompoundTag credit = entity.getPersistentData().getCompound(DAMAGE_CREDIT_TAG);
        long now = entity.level().getGameTime();
        for (String key : credit.getAllKeys()) {
            CompoundTag entry = credit.getCompound(key);
            if (now - entry.getLong("lastTick") > DAMAGE_CREDIT_TICKS || entry.getFloat("damage") < MIN_CREDIT_DAMAGE) {
                continue;
            }
            UUID uuid;
            try {
                uuid = entry.getUUID("player");
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ServerPlayer player = entity.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private static void pruneDamageCredit(CompoundTag credit, long now) {
        List<String> staleKeys = credit.getAllKeys().stream()
                .filter(key -> now - credit.getCompound(key).getLong("lastTick") > DAMAGE_CREDIT_TICKS)
                .toList();
        for (String key : staleKeys) {
            credit.remove(key);
        }
    }
}

