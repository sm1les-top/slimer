package com.sm1les.slimmer.entity;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Подкрадывание со спины:
 *  - если игрок смотрит на Слиммера - тот замирает пригнувшись;
 *  - если Слиммер не в поле зрения - обходит и заходит за спину;
 *  - за спиной бежит и запрыгивает;
 *  - если крался слишком долго (15 сек) - просто бежит и взрывается как крипер.
 */
public class SlimmerSneakGoal extends Goal {
    public static final int FALLBACK_TICKS = 300;

    private final Slimmer slimmer;
    private int repath;

    public SlimmerSneakGoal(Slimmer slimmer) {
        this.slimmer = slimmer;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private Player target() {
        LivingEntity t = slimmer.getTarget();
        if (t instanceof Player p && p.isAlive() && !p.isSpectator() && !p.getAbilities().invulnerable) {
            return p;
        }
        return null;
    }

    private boolean stateOk() {
        int s = slimmer.getState();
        return s == Slimmer.NORMAL || s == Slimmer.STALK || s == Slimmer.RUSH;
    }

    @Override
    public boolean canUse() {
        Player p = target();
        return p != null && stateOk() && slimmer.distanceToSqr(p) < 32.0D * 32.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        slimmer.getNavigation().stop();
        int s = slimmer.getState();
        if (s == Slimmer.STALK || s == Slimmer.RUSH) {
            slimmer.setState(Slimmer.NORMAL);
        }
        if (slimmer.getTarget() == null) {
            slimmer.stalkTicks = 0;
        }
    }

    @Override
    public void tick() {
        Player p = target();
        if (p == null) return;

        slimmer.stalkTicks++;
        slimmer.getLookControl().setLookAt(p, 30.0F, 30.0F);

        float r = p.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(r), 0.0D, Mth.cos(r));
        Vec3 flat = new Vec3(slimmer.getX() - p.getX(), 0.0D, slimmer.getZ() - p.getZ());
        double dist = flat.length();
        Vec3 dir = dist > 1.0E-4D ? flat.scale(1.0D / dist) : fwd.scale(-1.0D);
        double dot = fwd.dot(dir); // -1 = ровно за спиной
        Vec3 look = p.getLookAngle();
        Vec3 lookFlat = new Vec3(look.x, 0.0D, look.z);
        if (lookFlat.lengthSqr() > 1.0E-6D) lookFlat = lookFlat.normalize();
        boolean seen = lookFlat.dot(dir) > 0.3D;
        boolean behind = dot < -0.3D;
        boolean repathNow = ++repath >= 3;
        if (repathNow) repath = 0;

        // 0) крался слишком долго - просто бежит на игрока
        if (slimmer.stalkTicks >= FALLBACK_TICKS) {
            slimmer.setState(Slimmer.RUSH);
            if (repathNow) slimmer.getNavigation().moveTo(p, 1.3D);
            return;
        }

        // 1) игрок смотрит на Слиммера - замирает пригнувшись
        if (seen && dist > 2.5D && dist < 14.0D) {
            slimmer.getNavigation().stop();
            slimmer.setState(Slimmer.STALK);
            return;
        }

        // 2) за спиной - бежит и прыгает
        if (behind && dist < 10.0D) {
            slimmer.setState(Slimmer.RUSH);
            Vec3 back = p.position().subtract(fwd.scale(0.9D));
            if (repathNow) slimmer.getNavigation().moveTo(back.x, back.y, back.z, 1.3D);
            if (dist < 1.8D && Math.abs(slimmer.getY() - p.getY()) < 1.6D) {
                slimmer.beginLeap(p);
            }
            return;
        }

        // 3) обходит: идёт в точку позади игрока, пригнувшись
        slimmer.setState(Slimmer.STALK);
        if (repathNow) {
            Vec3 side = new Vec3(-fwd.z, 0.0D, fwd.x);
            double sign = dir.dot(side) >= 0.0D ? 1.0D : -1.0D;
            Vec3 wp = p.position().subtract(fwd.scale(5.0D)).add(side.scale(sign * 3.0D));
            slimmer.getNavigation().moveTo(wp.x, wp.y, wp.z, 0.95D);
        }
    }
}
