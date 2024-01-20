package com.usagin.juicecraft.client.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.usagin.juicecraft.friends.Friend;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

import static net.minecraft.world.entity.Pose.SITTING;
import static net.minecraft.world.entity.Pose.SLEEPING;

public abstract class FriendEntityModel<T extends Friend> extends HierarchicalModel<T> implements ArmedModel {
    Logger LOGGER = LogUtils.getLogger();
    public ModelParts parts;
    public Animations animations;

    public FriendEntityModel(ModelPart root) {
        defineParts(root);
        defineAnimations();
    }

    public abstract void defineAnimations();

    public abstract void defineParts(ModelPart root);

    public record Animations(AnimationDefinition idlegrounded, AnimationDefinition idletransition,
                             AnimationDefinition patgrounded, AnimationDefinition sit, AnimationDefinition sitimpatient,
                             AnimationDefinition sitpat, AnimationDefinition sleep, AnimationDefinition death,
                             AnimationDefinition deathstart, AnimationDefinition attackone,
                             AnimationDefinition attacktwo, AnimationDefinition attackthree,
                             AnimationDefinition counter, AnimationDefinition bowdraw,
                             AnimationDefinition standinginspect, AnimationDefinition wet,
                             AnimationDefinition viewflower, AnimationDefinition swim, AnimationDefinition interact,
                             AnimationDefinition swimmove, AnimationDefinition snowballIdle,
                             AnimationDefinition snowballThrow, AnimationDefinition snowballIdleTransition,
                             AnimationDefinition patEmbarrased) {
    }

    public record ModelParts(ModelPart customroot, ModelPart head, ModelPart leftarm, ModelPart rightarm,
                             ModelPart leftleg, ModelPart rightleg, ModelPart chest) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.scale(0.17F, 0.17F, 0.17F);
        poseStack.translate(0, 1.245 / 0.17, 0);
        parts.customroot().render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    public void translateToHand(HumanoidArm pSide, @NotNull PoseStack pPoseStack) {
        ModelPart root = this.root().getChild("hip");
        ModelPart limb;
        ModelPart arm;
        ModelPart hand;
        ModelPart waist = root.getChild("waist");
        ModelPart chest = waist.getChild("chest");

        if (pSide.name().equals("LEFT")) {
            arm = this.parts.leftarm();
            limb = arm.getChild("lowerarm2");
            hand = limb.getChild("grabber2");
        } else {
            arm = this.parts.rightarm();
            limb = arm.getChild("lowerarm");
            hand = limb.getChild("grabber");
        }


        pPoseStack.translate(0, 1.5, 0);

        translateAndRotate(pPoseStack, root);
        translateAndRotate(pPoseStack, waist);
        translateAndRotate(pPoseStack, chest);
        translateAndRotate(pPoseStack, arm);
        translateAndRotate(pPoseStack, limb);
        translateAndRotate(pPoseStack, hand);

        pPoseStack.translate(0, 0.1, 0);

        pPoseStack.scale(0.883F, 0.883F, 0.883F);


    }

    public void translateToBack(@NotNull PoseStack pPoseStack, @Nullable ItemStack pItemStack) {
        ModelPart hip = this.root().getChild("hip");
        ModelPart holster = hip.getChild("weaponholster");

        pPoseStack.translate(0, 1.5, 0);

        translateAndRotate(pPoseStack, hip);
        translateAndRotate(pPoseStack, holster);

        if (pItemStack != null) {
            if (pItemStack.getItem() instanceof BowItem) {
                pPoseStack.translate(0.2, 0, -0.1);
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(70));
            } else if (pItemStack.getItem() instanceof CrossbowItem) {
                pPoseStack.translate(0.35, -0.1, 0);
                pPoseStack.mulPose(Axis.XP.rotationDegrees(0));
                pPoseStack.mulPose(Axis.YP.rotationDegrees(90));
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
        }

        // pPoseStack.translate(-0.3, 1, 0);

        pPoseStack.scale(0.883F, 0.883F, 0.883F);
    }

    public void translateAndRotate(PoseStack pPoseStack, ModelPart part) {
        pPoseStack.translate(part.x * 0.17 / 16.0F, part.y * 0.17 / 16.0F, part.z * 0.17 / 16.0F);
        if (part.xRot != 0.0F || part.yRot != 0.0F || part.zRot != 0.0F) {
            pPoseStack.mulPose((new Quaternionf()).rotationZYX(part.zRot, part.yRot, part.xRot));
        }

        if (part.xScale != 1.0F || part.yScale != 1.0F || part.zScale != 1.0F) {
            pPoseStack.scale(part.xScale, part.yScale, part.zScale);
        }

    }

    @Override
    public ModelPart root() {
        return this.parts.customroot();
    }

    public boolean swimming = false;

    @Override
    public void setupAnim(Friend pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        if (!pEntity.getIsDying()) {
            if (pEntity.getPose() != SITTING) {
                if (pEntity.getPose() == SLEEPING) {
                    animate(pEntity.sleepAnimState, this.animations.sleep(), pAgeInTicks);
                } else if (pEntity.getAttackCounter() <= 0) {
                    if (pEntity.shakeAnimO > 0) {
                        animate(pEntity.wetAnimState, this.animations.wet(), pAgeInTicks);
                    } else {
                        animate(pEntity.viewFlowerAnimState, this.animations.viewflower(), pAgeInTicks);
                        if (!pEntity.viewFlowerAnimState.isStarted()) {
                            animate(pEntity.idleAnimState, this.animations.idlegrounded(), pAgeInTicks);
                            animate(pEntity.inspectAnimState, this.animations.standinginspect(), pAgeInTicks);
                            animate(pEntity.idleAnimStartState, this.animations.idletransition(), pAgeInTicks);
                            animate(pEntity.snowballIdleAnimState, this.animations.snowballIdle(), pAgeInTicks);
                            animate(pEntity.snowballIdleTransitionAnimState, this.animations.snowballIdleTransition(), pAgeInTicks);
                            if (pLimbSwingAmount > 0.1) {
                                this.swimming = true;
                                animate(pEntity.swimAnimState, this.animations.swimmove(), pAgeInTicks);
                                //LOGGER.info(pEntity.getDeltaMovement().y +"");
                                //this.root().getChild("hip").offsetRotation(new Vector3f((float)pEntity.getDeltaMovement().y*(float)pEntity.getDeltaMovement().y/4*25 ,0,0));
                            } else {
                                this.swimming = false;
                                animate(pEntity.swimAnimState, this.animations.swim(), pAgeInTicks);
                            }

                        }
                    }
                    if(pEntity.isembarassed>0){
                        //LOGGER.info(pEntity.patAnimState.isStarted() +"");
                        animate(pEntity.patAnimState, this.animations.patEmbarrased(), pAgeInTicks);
                    }else{
                    animate(pEntity.patAnimState, this.animations.patgrounded(), pAgeInTicks);}
                }

            } else {
                animate(pEntity.sitPatAnimState, this.animations.sitpat(), pAgeInTicks);
                animate(pEntity.sitAnimState, this.animations.sit(), pAgeInTicks);
                animate(pEntity.sitImpatientAnimState, this.animations.sitimpatient(), pAgeInTicks);
            }
            if (pEntity.getAttackType() == 50) {
                if (pEntity.getAttackCounter() >= pEntity.getCounterMax() - 1) {
                    pEntity.attackCounterAnimState.stop();
                }
                animate(pEntity.attackCounterAnimState, this.animations.counter(), pAgeInTicks, (float) pEntity.getAttackSpeed());
            } else if (pEntity.getAttackType() == 40) {
                animate(pEntity.attackAnimState, this.animations.attackone(), pAgeInTicks, (float) pEntity.getAttackSpeed());
            } else if (pEntity.getAttackType() == 20) {
                animate(pEntity.attackAnimState, this.animations.attacktwo(), pAgeInTicks, (float) pEntity.getAttackSpeed());
            } else if (pEntity.getAttackType() == 10) {
                animate(pEntity.attackAnimState, this.animations.attackthree(), pAgeInTicks, (float) pEntity.getAttackSpeed());
            } else if (pEntity.getAttackType() == 60) {
                animate(pEntity.snowballThrowAnimState, this.animations.snowballThrow(), pAgeInTicks, (float) pEntity.getAttackSpeed());
            }

            if (!pEntity.getInSittingPose()) {
                if (!pEntity.isSprinting() && !pEntity.isSwimming() && !pEntity.idle() && pEntity.walkAnimation.isMoving()) {
                    this.parts.rightleg().xRot = (float) (Math.cos(pLimbSwing * 0.6662F) * 1.4F * pLimbSwingAmount);
                    this.parts.leftleg().xRot = (float) ((Math.cos(pLimbSwing * 0.6662F + (float) Math.PI)) * 1.4F * pLimbSwingAmount);
                    if (pEntity.getAttackCounter() <= 0 && !pEntity.drawBowAnimationState.isStarted() && !pEntity.swimAnimState.isStarted() && pEntity.shakeAnimO == 0 && !pEntity.snowballIdle()) {
                        this.parts.leftarm().xRot = (float) (Math.cos(pLimbSwing * 0.6662F) * 1.4F * pLimbSwingAmount);
                        this.parts.rightarm().xRot = (float) ((Math.cos(pLimbSwing * 0.6662F + (float) Math.PI)) * 1.4F * pLimbSwingAmount);
                    }else if(pEntity.getAttackCounter() <= 0 && !pEntity.drawBowAnimationState.isStarted() && !pEntity.swimAnimState.isStarted() && pEntity.shakeAnimO == 0 && pEntity.snowballIdle()){
                        this.parts.leftarm().resetPose();
                        for(ModelPart part: this.parts.leftarm().getAllParts().toList()){
                            part.resetPose();
                        }
                        this.parts.leftarm().xRot = (float) (Math.cos(pLimbSwing * 0.6662F) * 1.4F * pLimbSwingAmount);
                        //LOGGER.info("HIT");

                    }
                }
                animate(pEntity.drawBowAnimationState, this.animations.bowdraw(), pAgeInTicks);
            }
            if (!pEntity.sitImpatientAnimState.isStarted() && pEntity.getPose() != SLEEPING && pEntity.animatestandingtimer <= 0) {
                if ((pEntity.sleepAnimState.isStarted() && pLimbSwingAmount > 0.1)) {
                    this.parts.head().yRot = (pNetHeadYaw * (float) Math.PI / 180f);
                } else {
                    this.parts.head().yRot = (pNetHeadYaw * (float) Math.PI / 180f);
                    this.parts.head().xRot = (pHeadPitch * (float) Math.PI / 180f);
                }

            }
            if (!pEntity.attackAnimState.isStarted()) {
                animate(pEntity.interactAnimState, this.animations.interact(), pAgeInTicks);
            }
        } else {
            animate(pEntity.deathStartAnimState, this.animations.deathstart(), pAgeInTicks);
            animate(pEntity.deathAnimState, this.animations.death(), pAgeInTicks);
        }

    }
}
