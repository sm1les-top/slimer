package com.sm1les.slimmer.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** Запасной вариант: если со спины зайти не вышло - раздувается и взрывается как обычный крипер. */
public class SlimmerSwellGoal extends Goal {
    private final Slimmer slimmer;

    public SlimmerSwellGoal(Slimmer slimmer) {
        this.slimmer = slimmer;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private Player target() {
        LivingEntity t = slimmer.getTarget();
        return t instanceof Player p && p.isAlive() ? p : null;
    }

    @Override
    public boolean canUse() {
        Player p = target();
        int s = slimmer.getState();
        boolean free = s == Slimmer.NORMAL || s == Slimmer.STALK || s == Slimmer.RUSH;
        return p != null && free
                && slimmer.stalkTicks >= SlimmerSneakGoal.FALLBACK_TICKS
                && slimmer.distanceToSqr(p) < 9.0D;
    }

    @Override
    public boolean canContinueToUse() {
        Player p = target();
        return p != null && slimmer.getState() == Slimmer.SWELL && slimmer.distanceToSqr(p) < 49.0D;
    }

    @Override
    public void start() {
        slimmer.getNavigation().stop();
        slimmer.setState(Slimmer.SWELL);
    }

    @Override
    public void stop() {
        if (slimmer.getState() == Slimmer.SWELL) {
            slimmer.setState(Slimmer.NORMAL);
        }
        slimmer.stalkTicks = 150;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        slimmer.getNavigation().stop();
        Player p = target();
        if (p != null) slimmer.getLookControl().setLookAt(p, 30.0F, 30.0F);
    }
}
