package com.example.mixin.planthitbox.mixin;

import com.jandbdavenport.cobblestonehelper.ModConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class PlantHitboxMixin {
    private boolean isTargetPlant(Block block) {
        return block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM ||
               block == Blocks.CRIMSON_FUNGUS || block == Blocks.WARPED_FUNGUS ||
               block == Blocks.CACTUS_FLOWER || block == Blocks.TALL_DRY_GRASS;
    }

    private boolean isWildflower(Block block) {
        return block == Blocks.WILDFLOWERS;
    }

    private VoxelShape getFullCube() {
        return VoxelShapes.fullCube();
    }

    private VoxelShape getSlabShape() {
        return Block.createCuboidShape(0, 0, 0, 16, 8, 16);
    }

    // Inject into all getRaycastShape overloads
    @Inject(method = "getRaycastShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("RETURN"), cancellable = true)
    private void enlargeRaycastShape1(BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (!ModConfig.plantHitboxEnabled) return;
        BlockState state = (BlockState) (Object) this;
        if (isTargetPlant(state.getBlock())) {
            cir.setReturnValue(getFullCube());
        } else if (isWildflower(state.getBlock())) {
            cir.setReturnValue(getSlabShape());
        }
    }

    // Inject into all getOutlineShape overloads
    @Inject(method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("RETURN"), cancellable = true)
    private void enlargeOutlineShape1(BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (!ModConfig.plantHitboxEnabled) return;
        BlockState state = (BlockState) (Object) this;
        if (isTargetPlant(state.getBlock())) {
            cir.setReturnValue(getFullCube());
        } else if (isWildflower(state.getBlock())) {
            cir.setReturnValue(getSlabShape());
        }
    }

    @Inject(method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("RETURN"), cancellable = true)
    private void enlargeOutlineShape2(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!ModConfig.plantHitboxEnabled) return;
        BlockState state = (BlockState) (Object) this;
        if (isTargetPlant(state.getBlock())) {
            cir.setReturnValue(getFullCube());
        } else if (isWildflower(state.getBlock())) {
            cir.setReturnValue(getSlabShape());
        }
    }

}
