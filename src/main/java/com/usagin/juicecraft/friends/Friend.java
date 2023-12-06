package com.usagin.juicecraft.friends;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.usagin.juicecraft.FriendMenuProvider;
import com.usagin.juicecraft.Init.ItemInit;
import com.usagin.juicecraft.Seagull;
import com.usagin.juicecraft.data.*;
import com.usagin.juicecraft.goals.FriendFollowGoal;
import com.usagin.juicecraft.goals.FriendLadderClimbGoal;
import com.usagin.juicecraft.goals.FriendWanderGoal;
import com.usagin.juicecraft.goals.navigation.FriendPathNavigation;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import static com.usagin.juicecraft.Init.ParticleInit.SUGURIVERSE_LARGE;
import static com.usagin.juicecraft.Init.UniversalSoundInit.*;
import static net.minecraft.core.particles.ParticleTypes.HEART;
import static net.minecraft.world.item.Items.AIR;

public abstract class Friend extends Wolf implements ContainerListener, MenuProvider {
    int captureDifficulty;
    public final AnimationState idleAnimState = new AnimationState();
    public final AnimationState patAnimState = new AnimationState();
    public final AnimationState idleAnimStartState = new AnimationState();
    int aggression;
    public int mood;

    public boolean isDying = false;
    public int recoveryDifficulty;
    public int deathCounter;
    int time;
    public int blinkCounter = 150;
    public int soundCounter = 40;
    public int patCounter = 20;
    public int idleCounter = 0;
    public int deathTimer = 199;
    int invRows;
    boolean isArmorable;
    boolean isModular;
    String name;
    boolean isShaking;
    public float volume = 0.5F;
    float shakeAnim;
    float shakeAnimO;
    public SimpleContainer inventory = new SimpleContainer(16);
    float[] home;
    Relationships relationships;
    int[] dialogueTree = new int[300];
    CombatSettings combatSettings;
    SumikaMemory oldMemory;

    public String getFriendName() {
        return this.name;
    }

    public Friend(EntityType<? extends Wolf> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        initializeNew();
        if(!pLevel.isClientSide()){
            registerCustomGoals();
        }
    }
    void initializeNew() {
        this.setRecoveryDifficulty();
        this.setCaptureDifficulty();
        this.setCaptureDifficulty();
        this.deathCounter = 7 - this.recoveryDifficulty;
        this.setAggression();
        this.mood = 100;
        this.time = 0;
        this.socialInteraction = 100;
        this.setPersistenceRequired();
        this.setName();
        this.home = new float[4];
        this.initializeDialogueSettings();
        this.relationships = new Relationships();
        this.combatSettings = new CombatSettings();
        this.oldMemory = new SumikaMemory();
        this.setInventoryRows();
        this.setArmorableModular();
        ((FriendPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.createInventory();
    }
    @Override
    protected @NotNull PathNavigation createNavigation(Level pLevel){
        return new FriendPathNavigation(this, pLevel);
    }
    private net.minecraftforge.common.util.LazyOptional<?> itemHandler = null;

    protected void createInventory() {
        SimpleContainer simplecontainer = this.inventory;
        this.inventory = new SimpleContainer(7 + this.getInventoryRows() * 5);
        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
            int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = simplecontainer.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }
        this.inventory.addListener(this);
        this.updateContainerEquipment();
        this.itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this.inventory));
    }

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.core.Direction facing) {
        if (this.isAlive() && capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && itemHandler != null)
            return itemHandler.cast();
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (itemHandler != null) {
            net.minecraftforge.common.util.LazyOptional<?> oldHandler = itemHandler;
            itemHandler = null;
            oldHandler.invalidate();
        }
    }

    abstract void setInventoryRows();

    abstract void setArmorableModular();

    abstract void initializeDialogueSettings();

    abstract void setName();

    abstract void setAggression();

    abstract void setCaptureDifficulty();

    abstract void setRecoveryDifficulty();

    abstract void indicateTamed();

    void spawnHorizontalParticles() {
        if (this.level() instanceof ServerLevel pLevel) {
            Vec3 vec3 = this.position().add(0.0D, (double) 1.6F, 0.0D);
            Vec3 vec31 = this.getEyePosition().subtract(vec3);
            Vec3 vec32 = vec31.normalize();
            for (int i = 1; i < Mth.floor(vec31.length()) + 7; ++i) {
                Vec3 vec33 = vec3.add(vec32.scale((double) i));
                pLevel.sendParticles(SUGURIVERSE_LARGE.get(), vec33.x, vec33.y, vec33.z, 1, 0.0D, 0.0D, 0.0D, 1);
            }
        }
    }

    void petEvent() {
        if (this.soundCounter >= 150) {
            this.playSound(this.getPat(), volume, 1);
            soundCounter = 0;
        }
        patCounter = 20;
    }

    void doRecoveryEvent() {
        this.setHealth(this.getMaxHealth() / 2);
        this.deathCounter = 7 - recoveryDifficulty;
        this.soundCounter = 0;
        this.getEntityData().set(FRIEND_ISDYING, false);
        this.isDying=false;
        this.playSound(getRecovery(), volume, 1);
        this.playSound(RECOVERY.get(), volume, 1);
        this.spawnHorizontalParticles();
        //this.level().spawn
    }

    void doDyingEvent() {
        this.soundCounter = 0;
        this.playSound(getRecoveryFail(), volume, 1);
        if (this.deathCounter == 0) {
            this.doDeathEvent();
        }
    }

    public void doDeathEvent(){
        this.spawnHorizontalParticles();
        this.playSound(FRIEND_DEATH.get(),volume,2);
        this.setRemoved(RemovalReason.KILLED);
    }

    abstract SoundEvent getIdle();

    abstract SoundEvent getInjured();

    abstract SoundEvent getInteract();

    abstract SoundEvent getPat();

    public abstract SoundEvent getHurt(float dmg);

    public abstract SoundEvent getAttack();

    abstract SoundEvent getEvade();

    abstract SoundEvent getBattle();

    abstract public SoundEvent getHyperEquip();

    abstract SoundEvent getHyperUse();

    abstract SoundEvent getRecovery();

    abstract SoundEvent getOnHeal();

    abstract SoundEvent getRecoveryFail();

    abstract SoundEvent getWarning();

    abstract SoundEvent getEquip();

    abstract SoundEvent getModuleEquip();


    abstract Relationships parseRelationships(int[] relations);

    abstract CombatSettings parseCombatSettings(int[] combatSettings);

    abstract int[] convertRelationships(Relationships relations);

    abstract int[] convertCombatSettings(CombatSettings combatSettings);

    public boolean isArmorable() {
        return this.isArmorable;
    }

    public boolean hasInventoryChanged(Container pInventory) {
        return this.inventory != pInventory;
    }

    public boolean isModular() {
        return this.isModular;
    }

    public boolean isModule(ItemStack pStack) {
        return pStack.getItem() instanceof ModuleItem;
    }

    public boolean isLivingTame() {
        return this.isAlive() && this.isTame();
    }

    boolean testMood(LivingEntity a) {
        return this.mood < 15;
    }

    public int getInventoryRows() {
        return this.invRows;
    }

    void saveMemory() {
        this.oldMemory.saveDialogue(dialogueTree);
        this.oldMemory.saveCombatSettings(combatSettings);
        this.oldMemory.saveRelationships(relationships);
        this.oldMemory.saveHome(home);
    }

    void loadMemory() {
        this.dialogueTree = this.oldMemory.getDialogueTree();
        this.relationships = this.oldMemory.getRelationships();
        this.combatSettings = this.oldMemory.getCombatSettings();
        this.home = this.oldMemory.getHome();
    }

    protected void dropEquipment() {
        super.dropEquipment();
        if (this.inventory != null) {
            for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.inventory.getItem(i);
                if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack)) {
                    this.spawnAtLocation(itemstack);
                }
            }

        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putIntArray("juicecraft.home", new int[]{(int) this.home[0], (int) this.home[1], (int) this.home[2], (int) this.home[3]});
        pCompound.putIntArray("juicecraft.dialogue", this.dialogueTree);
        pCompound.putIntArray("juicecraft.relationships", convertRelationships(this.relationships));
        pCompound.putIntArray("juicecraft.csettings", convertCombatSettings(this.combatSettings));
        pCompound.putInt("juicecraft.social", this.socialInteraction);
        pCompound.putInt("juicecraft.mood", this.mood);
        pCompound.putInt("juicecraft.existed", 1);
        pCompound.putBoolean("Tame", this.isTame());
        pCompound.putBoolean("juicecraft.isdying", this.isDying);
        pCompound.putFloat("Health", this.getHealth());
        pCompound.putInt("juicecraft.deathcounter", this.deathCounter);
        ListTag listtag = new ListTag();

        for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte) i);
                itemstack.save(compoundtag);
                listtag.add(compoundtag);
            }
        }
        pCompound.put("juicecraft.inventory", listtag);
        if (this.getOwnerUUID() != null) {
            pCompound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        if (pCompound.getInt("juicecraft.existed") == 0) {
            this.initializeNew();
            return;
        }
        super.readAdditionalSaveData(pCompound);
        int[] temp = pCompound.getIntArray("juicecraft.home");
        if (temp.length != 0) {
            this.home = new float[]{temp[0], temp[1], temp[2], temp[3]};
        }
        this.dialogueTree = pCompound.getIntArray("juicecraft.dialogue");
        this.relationships = this.parseRelationships((pCompound.getIntArray("juicecraft.relationships")));
        this.combatSettings = this.parseCombatSettings((pCompound.getIntArray("juicecraft.csettings")));
        this.socialInteraction = (pCompound.getInt("juicecraft.social"));
        this.mood = (pCompound.getInt("juicecraft.mood"));
        this.setHealth(pCompound.getFloat("Health"));
        this.isDying=(pCompound.getBoolean("juicecraft.isdying"));
        this.deathCounter=(pCompound.getInt("juicecraft.deathcounter"));

        this.setTame(pCompound.getBoolean("Tame"));
        this.createInventory();
        ListTag listtag = pCompound.getList("juicecraft.inventory", 10);
        for (int i = 0; i < listtag.size(); ++i) {
            CompoundTag compoundtag = listtag.getCompound(i);
            int j = compoundtag.getByte("Slot") & 255;
            if (j < this.inventory.getContainerSize()) {
                this.inventory.setItem(j, ItemStack.of(compoundtag));
            }
        }
        UUID uuid;
        if (pCompound.hasUUID("Owner")) {
            uuid = pCompound.getUUID("Owner");
        } else {
            String s = pCompound.getString("Owner");
            uuid = OldUsersConverter.convertMobOwnerIfNecessary(Objects.requireNonNull(this.getServer()), s);
        }

        if (uuid != null) {
            this.setOwnerUUID(uuid);
        }

        if (pCompound.contains("SaddleItem", 10)) {
            ItemStack itemstack = ItemStack.of(pCompound.getCompound("SaddleItem"));
            if (itemstack.is(Items.SADDLE)) {
                this.inventory.setItem(0, itemstack);
            }
        }

        this.updateContainerEquipment();
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ID_FLAGS, (byte) 0);
        this.entityData.define(FRIEND_ISDYING, isDying);
    }

    public void updateGear(){
        if(!this.inventory.getItem(1).isEmpty()){
            this.setItemSlot(EquipmentSlot.MAINHAND, this.inventory.getItem(1));
        }
        else{
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(AIR));
        }
        if(!this.inventory.getItem(3).isEmpty()){
            this.setItemSlot(EquipmentSlot.HEAD, this.inventory.getItem(1));
        }
        else{
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AIR));
        }
        if(!this.inventory.getItem(4).isEmpty()){
            this.setItemSlot(EquipmentSlot.CHEST, this.inventory.getItem(1));
        }
        else{
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AIR));
        }
        if(!this.inventory.getItem(5).isEmpty()){
            this.setItemSlot(EquipmentSlot.LEGS, this.inventory.getItem(1));
        }
        else{
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(AIR));
        }
        if(!this.inventory.getItem(6).isEmpty()){
            this.setItemSlot(EquipmentSlot.FEET, this.inventory.getItem(1));
        }
        else{
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(AIR));
        }

    }

    private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(Friend.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Boolean> FRIEND_ISDYING = SynchedEntityData.defineId(Friend.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> FRIEND_NUMBER_DATA = SynchedEntityData.defineId(Friend.class, EntityDataSerializers.FLOAT);

    protected void setFlag(int pFlagId, boolean pValue) {
        byte b0 = this.entityData.get(DATA_ID_FLAGS);

        if (pValue) {
            this.entityData.set(DATA_ID_FLAGS, (byte) (b0 | pFlagId));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte) (b0 & ~pFlagId));
        }

    }

    private SlotAccess createEquipmentSlotAccess(final int pSlot, final Predicate<ItemStack> pStackFilter) {
        return new SlotAccess() {
            public ItemStack get() {
                return inventory.getItem(pSlot);
            }

            public boolean set(ItemStack p_149528_) {
                if (!pStackFilter.test(p_149528_)) {
                    return false;
                } else {
                    inventory.setItem(pSlot, p_149528_);
                    updateContainerEquipment();
                    return true;
                }
            }
        };
    }

    protected void updateContainerEquipment() {
        if (!this.level().isClientSide) {
            this.setFlag(4, !this.inventory.getItem(0).isEmpty());
        }
    }

    int socialInteraction;
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new OpenDoorGoal(this,true));
        this.goalSelector.addGoal(4, new FriendLadderClimbGoal(this));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        //add Friend wandering/idle ai goal
        this.goalSelector.addGoal(7, new FriendFollowGoal(this, 1D, 10.0F, 2.0F, false));
        //this.goalSelector.addGoal(7, new BreedGoal(this, 1.0D)); //...maybe in the future...
        this.goalSelector.addGoal(9, new FriendWanderGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new BegGoal(this, 8.0F));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(8, new NearestAttackableTargetGoal<>(this, Seagull.class, true));
        this.targetSelector.addGoal(9, new ResetUniversalAngerTargetGoal<>(this, true));
    }
    void registerCustomGoals(){
        if (this.aggression > 75) {
            this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::testMood));
            this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this,
                    Mob.class, 5, false, false,
                    (p_28879_) -> p_28879_ instanceof Enemy));
        }
        else if(this.aggression > 49) {
            this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this,
                    Mob.class, 5, false, false,
                    (p_28879_) -> p_28879_ instanceof Enemy && !(p_28879_ instanceof Creeper)));
        }
        else{
            this.targetSelector.addGoal(7, new AvoidEntityGoal<>(this,
                    Mob.class, 5, 1, 1,
                    (p_28879_) -> p_28879_ instanceof Enemy && !(p_28879_ instanceof Creeper)));
        }
    }
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        Item item = itemstack.getItem();
        if (this.level().isClientSide) {
            boolean flag = this.isOwnedBy(pPlayer) || this.isTame() || itemstack.is(ItemInit.ORANGE.get()) || itemstack.is(ItemInit.GOLDEN_ORANGE.get()) && !this.isTame() && !this.isAngry();
            if (this.isOwnedBy(pPlayer) || this.isTame()) {
                if (itemstack.isEmpty() && !pPlayer.isCrouching()) {
                    this.patCounter = 20;
                }
            }
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else if (this.isTame()) {
            if (itemstack.is(Items.COOKIE) && this.getHealth() < this.getMaxHealth()) {
                this.heal(1);
                this.mood++;
                if (!pPlayer.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                this.gameEvent(GameEvent.EAT, this);
                this.socialInteraction++;
                if (this.soundCounter >= 150) {
                    this.playSound(this.getOnHeal(), volume, 1);
                    soundCounter = 0;
                }
                return InteractionResult.SUCCESS;
            } else if (itemstack.isEdible()) {
                this.mood++;
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, itemstack.getFoodProperties(this).getNutrition()));
                if (!pPlayer.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                this.socialInteraction++;
                this.gameEvent(GameEvent.EAT, this);
                if (this.soundCounter >= 150) {
                    this.playSound(this.getOnHeal(), volume, 1);
                    soundCounter = 0;
                }
                return InteractionResult.SUCCESS;
            } else if (itemstack.is(ItemInit.SUMIKA_MEMORY.get())) {
                this.loadMemory();
                this.doRecoveryEvent(); //recovery animation
                return InteractionResult.SUCCESS;
            } else {
                if (itemstack.isEmpty() && this.isOwnedBy(pPlayer) && pPlayer.isCrouching()) {
                    if (pPlayer instanceof ServerPlayer serverPlayer) {
                        this.playSound(this.getInteract(), volume, 1);
                        soundCounter = 0;

                        serverPlayer.openMenu(new FriendMenuProvider(this), buffer -> buffer.writeVarInt(this.getId()));
                    }
                    return InteractionResult.SUCCESS;
                } else if (itemstack.isEmpty() && this.isOwnedBy(pPlayer)) {
                    if (this.mood > 50) {
                        petEvent();
                        if (this.random.nextInt(20) == 6) {
                            if (this.mood <= 80) {
                                this.mood += 20;
                                if (this.level() instanceof ServerLevel sLevel) {
                                    for (int i = 0; i < 5; i++) {
                                        sLevel.sendParticles(HEART, this.getX(), this.getY() + 1, this.getZ(), 1, this.random.nextInt(-1, 2), this.random.nextInt(-1, 2), this.random.nextInt(-1, 2), 0.5);
                                    }

                                }
                            } else {
                                this.mood = 100;
                                if (this.level() instanceof ServerLevel sLevel) {
                                    for (int i = 0; i < 5; i++) {
                                        sLevel.sendParticles(HEART, this.getX(), this.getY() + 1, this.getZ(), 1, this.random.nextInt(-1, 2), this.random.nextInt(-1, 2), this.random.nextInt(-1, 2), 0.5);
                                    }
                                }
                            }
                        }
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6);
                        this.mood--;
                    }
                    this.socialInteraction++;
                    return InteractionResult.SUCCESS;
                }
            }
        } else if ((itemstack.is(ItemInit.ORANGE.get()) || itemstack.is(ItemInit.GOLDEN_ORANGE.get())) && !this.isAngry() && this.mood == 100) {
            if (!pPlayer.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            if (captureDifficulty > 1 && itemstack.is(ItemInit.GOLDEN_ORANGE.get())) {
                captureDifficulty--;
            }
            if (this.random.nextInt(captureDifficulty) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, pPlayer)) {
                this.tame(pPlayer);
                this.navigation.stop();
                this.setTarget((LivingEntity) null);
                this.indicateTamed();
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
                if (aggression < 50) {
                    this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 50, 1, 1));
                } else if (aggression > 75) {
                    this.setRemainingPersistentAngerTime(24000);
                }
                this.mood = 0;
            }
            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(pPlayer, pHand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        //shaking anim, work on later
        if (!this.level().isClientSide && this.isWet() && !this.isShaking && !this.isPathFinding() && this.onGround()) {
            this.isShaking = true;
            this.shakeAnim = 0.0F;
            this.shakeAnimO = 0.0F;
            this.level().broadcastEntityEvent(this, (byte) 8);
        }
        if (this.aggression < 50) {
            if (this.time % 100 == 0) {
                this.time = 0;
                if (mood != 100) {
                    this.mood++;
                }
                if (mood != 1) {
                    if (this.isTame()) {
                        this.socialInteraction--;
                        mood -= 2;
                    }
                }
            }
        } else if (this.aggression < 75) {
            if (this.time % 200 == 0) {
                this.time = 0;
                if (this.mood != 100) {
                    this.mood++;
                }
                if (mood != 1) {
                    if (this.isTame()) {
                        this.socialInteraction--;
                        mood -= 2;
                    }
                }
            }
        } else {
            if (this.time % 300 == 0) {
                this.time = 0;
                if (this.mood != 100) {
                    this.mood++;
                }
                if (mood != 1) {
                    if (this.isTame()) {
                        this.socialInteraction--;
                        mood -= 2;
                    }
                }
            }
        }
        if (socialInteraction == 0) {
            this.setTame(false);
            this.setOwnerUUID(null);
            this.socialInteraction = 100;
            this.mood = 0;
            this.captureDifficulty *= 2;
        }
        if (!this.level().isClientSide) {
            this.updatePersistentAnger((ServerLevel) this.level(), true);
        }
        this.time++;
        if (blinkCounter == 0) {
            blinkCounter = 150;
            this.ambientSoundTime -= 50;
        }
        if (soundCounter < 150) {
            soundCounter++;
        }
        blinkCounter--;
        if (!this.level().isClientSide()) {
            if (isDying) {
                if (deathTimer == 80 && this.random.nextInt(7) >= this.recoveryDifficulty) {
                    doRecoveryEvent();
                } else if (deathTimer == 80) {
                    deathCounter--;
                    this.doDyingEvent();
                    LOGGER.info(this.name + " is dying.");
                }
                deathTimer--;
                if (deathTimer == 0) {
                    deathTimer = 80;
                }

            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.getHealth() < this.getAttributeValue(Attributes.MAX_HEALTH) / 2) {
            return this.getInjured();
        } else {
            return this.getIdle();
        }
    }

    @Override
    protected float getSoundVolume() {
        return this.volume;
    }

    @Override
    public float getVoicePitch() {
        return 1F;
    }
    @Override
    public void tick() {
        if (level().isClientSide()) {
            boolean idle = !this.walkAnimation.isMoving() && !this.isDescending();
            if (idle && idleCounter < 20) {
                this.idleCounter++;
            }
            if (!idle && idleCounter > 0) {
                this.idleCounter = 0;
            }
            this.idleAnimState.animateWhen(idle && idleCounter == 20 && patCounter == 0, this.tickCount);
            this.idleAnimStartState.animateWhen(idle && idleCounter < 20 && patCounter == 0, this.tickCount);
            this.patAnimState.animateWhen(this.patCounter > 0 && !this.walkAnimation.isMoving() && !this.isDescending(), this.tickCount);
        }
        if (this.patCounter > 0) {
            this.idleCounter = 0;
            this.patCounter--;
        }
        super.tick();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource dmgsrc) {
        return SoundEvents.PLAYER_HURT;

    }
}
