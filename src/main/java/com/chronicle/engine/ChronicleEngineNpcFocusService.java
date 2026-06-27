package com.chronicle.engine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChronicleEngineNpcFocusService {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private ChronicleEngineNpcFocusService() {
    }

    public static void hold(ServerPlayer player, Entity target, boolean stopMoving, boolean lookAtPlayer, double maxDistance) {
        if (target == null) {
            return;
        }
        SESSIONS.put(player.getUUID(), new Session(
                target.getUUID(),
                target.level().dimension().location().toString(),
                stopMoving,
                lookAtPlayer,
                Math.max(4.0D, maxDistance + 2.0D)
        ));
    }

    public static void release(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!player.level().dimension().location().toString().equals(session.dimension())) {
            release(player);
            return;
        }
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(session.entityId());
        if (entity == null || !entity.isAlive() || player.distanceToSqr(entity) > session.maxDistance() * session.maxDistance()) {
            release(player);
            return;
        }
        if (session.stopMoving()) {
            entity.setDeltaMovement(0.0D, entity.getDeltaMovement().y, 0.0D);
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.setTarget(null);
            }
        }
        if (session.lookAtPlayer()) {
            lookAt(entity, player);
        }
    }

    private static void lookAt(Entity entity, ServerPlayer player) {
        Vec3 delta = player.getEyePosition().subtract(entity.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(delta.y, horizontal) * (180.0F / Math.PI)));
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        if (entity instanceof Mob mob) {
            mob.yHeadRot = yaw;
            mob.yBodyRot = yaw;
            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
    }

    private record Session(UUID entityId, String dimension, boolean stopMoving, boolean lookAtPlayer, double maxDistance) {
    }
}

