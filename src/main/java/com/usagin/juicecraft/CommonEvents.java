package com.usagin.juicecraft;

import com.usagin.juicecraft.Init.BlockEntityInit;
import com.usagin.juicecraft.Init.EntityInit;
import com.usagin.juicecraft.Init.ParticleInit;
import com.usagin.juicecraft.Init.ProjectileInit;
import com.usagin.juicecraft.client.models.alte.AlteEntityModel;
import com.usagin.juicecraft.client.models.harbinger.HarbingerModel;
import com.usagin.juicecraft.client.models.sora.SoraEntityModel;
import com.usagin.juicecraft.client.renderer.blockrenderers.AltePlushieRenderer;
import com.usagin.juicecraft.client.renderer.blockrenderers.FriendBedRenderer;
import com.usagin.juicecraft.client.renderer.entities.AlteEntityRenderer;
import com.usagin.juicecraft.client.renderer.entities.FriendProjectileRenderer;
import com.usagin.juicecraft.client.renderer.entities.HarbingerRenderer;
import com.usagin.juicecraft.client.renderer.entities.SoraEntityRenderer;
import com.usagin.juicecraft.enemies.Harbinger;
import com.usagin.juicecraft.friends.Alte;
import com.usagin.juicecraft.friends.Sora;
import com.usagin.juicecraft.network.*;
import com.usagin.juicecraft.particles.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Collections;
import java.util.List;

import static com.usagin.juicecraft.JuiceCraft.MODID;
import static com.usagin.juicecraft.client.renderer.blockrenderers.PlushieRenderer.PLUSHIE;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonEvents {

    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityInit.SORA.get(), Sora.getSoraAttributes().build());
        event.put(EntityInit.ALTE.get(), Alte.getAlteAttributes().build());
        event.put(EntityInit.HARBINGER.get(), Harbinger.getHarbingerAttributes().build());
    }

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityInit.SORA.get(), SoraEntityRenderer::new);
        event.registerEntityRenderer(EntityInit.ALTE.get(), AlteEntityRenderer::new);
        event.registerEntityRenderer(EntityInit.HARBINGER.get(), HarbingerRenderer::new);
        event.registerEntityRenderer(ProjectileInit.ALTE_MINIGUN_PROJECTILE.get(), FriendProjectileRenderer::new);
        event.registerEntityRenderer(ProjectileInit.ALTE_PANEL_PROJECTILE.get(), FriendProjectileRenderer::new);

    }

    @SubscribeEvent
    public static void blockRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityInit.FRIEND_BED.get(), FriendBedRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityInit.ALTE_PLUSHIE.get(), AltePlushieRenderer::new);
    }

    public static ModelLayerLocation FRIEND_BED_FOOT = new ModelLayerLocation(new ResourceLocation(MODID, "friend_bed_foot"), "main");
    public static ModelLayerLocation FRIEND_BED_HEAD = new ModelLayerLocation(new ResourceLocation(MODID, "friend_bed_head"), "main");

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SoraEntityModel.LAYER_LOCATION, SoraEntityModel::createBodyLayer);
        event.registerLayerDefinition(AlteEntityModel.LAYER_LOCATION, AlteEntityModel::createBodyLayer);
        event.registerLayerDefinition(HarbingerModel.LAYER_LOCATION, HarbingerModel::createBodyLayer);

        event.registerLayerDefinition(FRIEND_BED_FOOT, FriendBedRenderer::createFootLayer);
        event.registerLayerDefinition(FRIEND_BED_HEAD, FriendBedRenderer::createHeadLayer);
        event.registerLayerDefinition(PLUSHIE, AltePlushieRenderer::createLayer);

    }


    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        Minecraft.getInstance().particleEngine.register(ParticleInit.ALTE_IMPACT_PARTICLE.get(), AlteImpactParticle.AlteImpactProvider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.SUGURIVERSE_LARGE.get(), SuguriverseParticleLarge.SugPartProvider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.SLEEPY.get(), SleepyParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.DICEONE.get(), SleepyParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.DICETWO.get(), SleepyParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.DICETHREE.get(), SleepyParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.DICEFOUR.get(), SleepyParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.DICEFIVE.get(), SleepyParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.DICESIX.get(), SleepyParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.GLITCH_PARTICLE.get(), GlitchParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.ALTE_ENERGY_PARTICLE.get(), EnergyParticle.AlteEnergyProvider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.ALTE_LIGHTNING_PARTICLE.get(), AlteLightningParticle.Provider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.ALTE_SELFDESTRUCT_PARTICLE.get(), AlteSelfDestructParticle.SelfDestructProvider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.ALTE_GUNFLASH.get(), AlteGunFlashParticle.GunFlashProvider::new);
        Minecraft.getInstance().particleEngine.register(ParticleInit.RANDOM_ENERGY_PARTICLE.get(), RandomEnergyParticle.RandomEnergyProvider::new);

    }

    @SubscribeEvent
    public static void registerPacketHandler(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CombatSettingsPacketHandler.register();
            DialogueResultPacketHandler.register();
            SpecialDialoguePacketHandler.register();
            SetFarmingPacketHandler.register();
            SetWanderingPacketHandler.register();
            PlaySoundPacketHandler.register();
            UpdateSkillPacketHandler.register();
            ItemPickupPacketHandler.register();
            CircleParticlePacketHandler.register();
        });
    }


}
