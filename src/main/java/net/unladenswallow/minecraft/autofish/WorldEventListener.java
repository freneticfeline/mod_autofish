package net.unladenswallow.minecraft.autofish;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;

public class WorldEventListener implements IWorldEventListener {
    
    private AutoFishEventHandler _handler;
    
    public WorldEventListener(AutoFishEventHandler handler) {
        _handler = handler;
    }

    @Override
    public void notifyBlockUpdate(World worldIn, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void notifyLightSet(BlockPos pos) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playSoundToAllNearExcept(EntityPlayer player, SoundEvent soundIn, SoundCategory category, double x,
            double y, double z, float volume, float pitch) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void playRecord(SoundEvent soundIn, BlockPos pos) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord,
            double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        if (particleID == EnumParticleTypes.WATER_WAKE.getParticleID()) {
//            AutoFishLogger.info("Particle WATER_WAKE spawned with id %d at [%f, %f, %f]", particleID, xCoord, yCoord, zCoord);
            _handler.onWaterWakeDetected(xCoord, yCoord, zCoord);
        }
        
    }

    @Override
    public void spawnParticle(int id, boolean ignoreRange, boolean p_190570_3_, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        spawnParticle(id, ignoreRange, x, y, z, xSpeed, ySpeed, zSpeed, parameters);
    }

    @Override
    public void onEntityAdded(Entity entityIn) {
        _handler.onEntityAdded(entityIn);
        
    }

    @Override
    public void onEntityRemoved(Entity entityIn) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void broadcastSound(int soundID, BlockPos pos, int data) {
        AutoFishLogger.info("Sound with id %d broadcast at %s", soundID, pos.toString());
        
    }

    @Override
    public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
        // TODO Auto-generated method stub
        
    }

}
