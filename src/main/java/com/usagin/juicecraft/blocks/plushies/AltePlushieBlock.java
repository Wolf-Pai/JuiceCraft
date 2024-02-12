package com.usagin.juicecraft.blocks.plushies;

import com.usagin.juicecraft.JuiceCraft;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AltePlushieBlock extends PlushieBlock {

    public AltePlushieBlock(Properties pProperties) {
        super(pProperties);
    }

    public static ResourceLocation ATLAS = new ResourceLocation(JuiceCraft.MODID,"textures/entities/plushies/alte.png");
    @Override
    public Material getMaterial() {
        return new Material(ATLAS,ATLAS);
    }
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return PLUSHIESHAPE;
    }
}
