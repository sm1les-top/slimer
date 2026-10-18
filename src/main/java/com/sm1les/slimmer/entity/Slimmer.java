package com.sm1les.slimmer.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Слиммер - зелёный крипер-диверсант.
 * Подкрадывается к игроку со спины, запрыгивает на спину, держится и взрывается.
 */
public class Slimmer extends Monster {
    // Состояния
    public static final int NORMAL = 0; // стоит / идёт
    public static final int SWELL = 1;  // обычный фитиль (как у крипера)
    public static final int STALK = 2;  // крадётся пригнувшись
    public static final int RUSH = 3;   // бежит к спине игрока
    public static final int LEAP = 4;   // прыжок на спину
    public static final int CLING = 5;  // висит на спине, фитиль горит

    public static final int LEAP_TICKS = 10;
    public static final int CLING_FUSE = 40;
    public static final int SWELL_FUSE = 30;
    public static final float EXPLOSION_POWER = 3.0F; // как у обычного крипера

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(Slimmer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOST =
            SynchedEntityData.defineId(Slimmer.class, EntityDataSerializers.INT);

    /** Тики в текущем состоянии (и на сервере, и на клиенте). */
    public int stateTicks;
    /** Сколько тиков Слиммер уже охотится за целью. */
    public int stalkTicks;

    // Плавные значения для анимации (текущее и прошлое)
    public float crouch, crouchO, run, runO, leap, leapO, cling, clingO;

    private Vec3 leapStart = Vec3.ZERO;

    public Slimmer(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SlimmerSwellGoal(this));
        this.goalSelector.addGoal(3, new SlimmerSneakGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, NORMAL);
        builder.define(DATA_HOST, -1);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_STATE.equals(key)) {
            this.stateTicks = 0;
        }
    }

    // ---------- состояние ----------

    public int getState() {
        return this.entityData.get(DATA_STATE);
    }

    public void setState(int state) {
        if (getState() != state) {
            this.entityData.set(DATA_STATE, state);
            this.stateTicks = 0;
        }
    }

    public Player getHost() {
        int id = this.entityData.get(DATA_HOST);
        if (id < 0) return null;
        return this.level().getEntity(id) instanceof Player p ? p : null;
    }

    private void setHost(int id) {
        this.entityData.set(DATA_HOST, id);
    }

    public boolean isOnBack() {
        int s = getState();
        return s == LEAP || s == CLING;
    }

    /** Точка на спине игрока, где сидит Слиммер. */
    public static Vec3 backPoint(Vec3 playerPos, float playerBodyYaw) {
        float r = playerBodyYaw * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(r);
        double fz = Mth.cos(r);
        return new Vec3(playerPos.x - fx * 0.55D, playerPos.y + 0.2D, playerPos.z - fz * 0.55D);
    }

    /** Прогресс фитиля 0..1 (для раздувания и вспышки). */
    public float getSwell(float partialTick) {
        int s = getState();
        if (s == SWELL) return Mth.clamp((stateTicks + partialTick) / (float) SWELL_FUSE, 0.0F, 1.0F);
        if (s == CLING) return Mth.clamp((stateTicks + partialTick) / (float) CLING_FUSE, 0.0F, 1.0F);
        return 0.0F;
    }

    public void beginLeap(Player host) {
        this.leapStart = this.position();
        setHost(host.getId());
        this.getNavigation().stop();
        setState(LEAP);
        this.playSound(SoundEvents.CREEPER_HURT, 1.0F, 1.6F);
    }

    private void detach() {
        setHost(-1);
        setState(NORMAL);
        this.stalkTicks = 0;
    }

    private void explodeNow() {
        if (!this.level().isClientSide) {
            this.dead = true;
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_POWER,
                    Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    // ---------- тик ----------

    @Override
    public void tick() {
        int st = getState();
        this.noPhysics = (st == LEAP || st == CLING);

        // плавные значения для анимации
        crouchO = crouch;
        runO = run;
        leapO = leap;
        clingO = cling;
        crouch += ((st == STALK ? 1.0F : 0.0F) - crouch) * 0.3F;
        run += ((st == RUSH ? 1.0F : 0.0F) - run) * 0.3F;
        leap += ((st == LEAP ? 1.0F : 0.0F) - leap) * 0.5F;
        cling += ((st == CLING ? 1.0F : 0.0F) - cling) * 0.35F;

        super.tick();
        this.stateTicks++;

        if (this.level().isClientSide) return;

        st = getState();
        if (st == LEAP || st == CLING) {
            Player host = getHost();
            if (host == null || !host.isAlive() || host.isSpectator() || host.level() != this.level()) {
                detach();
                return;
            }
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            this.fallDistance = 0.0F;
            Vec3 end = backPoint(host.position(), host.yBodyRot);

            if (st == LEAP) {
                float t = Math.min(1.0F, this.stateTicks / (float) LEAP_TICKS);
                Vec3 p = leapStart.lerp(end, t).add(0.0D, Math.sin(Math.PI * t) * 0.9D, 0.0D);
                this.setPos(p.x, p.y, p.z);
                faceYaw(host.yBodyRot);
                if (this.stateTicks >= LEAP_TICKS) {
                    setState(CLING);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0F, 0.5F);
                }
            } else { // CLING
                this.setPos(end.x, end.y, end.z);
                faceYaw(host.yBodyRot);
                if (this.stateTicks >= CLING_FUSE) {
                    explodeNow();
                }
            }
        } else if (st == SWELL) {
            this.getNavigation().stop();
            if (this.stateTicks == 1) {
                this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            }
            if (this.stateTicks >= SWELL_FUSE) {
                explodeNow();
            }
        }
    }

    private void faceYaw(float yaw) {
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    @Override
    protected void serverAiStep() {
        // на спине и в прыжке ИИ отключён - всем управляет tick()
        if (isOnBack()) return;
        super.serverAiStep();
    }

    @Override
    public boolean isNoGravity() {
        return isOnBack() || super.isNoGravity();
    }

    @Override
    public boolean isPushable() {
        return !isOnBack() && super.isPushable();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ---------- звуки ----------

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CREEPER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_DEATH;
    }
}
