package com.usagin.juicecraft.ai.goals;

import com.mojang.logging.LogUtils;
import com.usagin.juicecraft.friends.Friend;
import net.minecraft.client.particle.HeartParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public class FriendHitAndRunGoal extends Goal {
    Friend friend;
    LivingEntity target;
    public FriendHitAndRunGoal(Friend friend){
        this.friend=friend;
        this.pathNav=friend.getNavigation();
    }
    @Nullable
    protected Path path;
    protected final PathNavigation pathNav;
    private static final Logger LOGGER = LogUtils.getLogger();
    @Override
    public boolean canUse() {
        if(friend.getTarget()!=null && this.friend.canDoThings() && this.friend.runTimer>30){
            this.target = this.friend.getTarget();
                Vec3 vec3 = DefaultRandomPos.getPosAway(this.friend, 16, 7, this.target.position());
                if (vec3 == null) {
                    return false;
                } else if (this.friend.distanceToSqr(vec3.x, vec3.y, vec3.z) < this.target.distanceToSqr(this.friend)) {
                    return false;
                } else {
                    this.path = this.pathNav.createPath(vec3.x, vec3.y, vec3.z, 0);
                    return this.path != null;
                }
        }
        return false;
    }
    @Override
    public boolean canContinueToUse(){
        return friend.getTarget()!=null && this.friend.canDoThings() && this.friend.runTimer>0;
    }
    public boolean isInterruptable() {
        return false;
    }
    @Override
    public void start(){
        this.pathNav.stop();
        this.pathNav.moveTo(this.path, this.friend.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }
    @Override
    public void tick(){
        if (this.friend.distanceToSqr(this.target) < 5.0D) {
            this.friend.getNavigation().setSpeedModifier(1.3);
        } else {
            this.friend.getNavigation().setSpeedModifier(1);
        }
    }
    @Override
    public void stop(){
        this.target=null;
    }
}
