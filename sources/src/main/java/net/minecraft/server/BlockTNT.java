package net.minecraft.server;

public class BlockTNT extends Block {

    public static final BlockStateBoolean EXPLODE = BlockStateBoolean.of("explode");

    public BlockTNT() {
        super(Material.TNT);
        this.y(this.blockStateList.getBlockData().set(BlockTNT.EXPLODE, Boolean.valueOf(false)));
        this.a(CreativeModeTab.d);
    }

    @Override
    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        super.onPlace(world, blockposition, iblockdata);
        if (world.isBlockIndirectlyPowered(blockposition)) {
            this.postBreak(world, blockposition, iblockdata.set(BlockTNT.EXPLODE, Boolean.valueOf(true)));
            world.setAir(blockposition);
        }

    }

    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, BlockPosition blockposition1) {
        if (world.isBlockIndirectlyPowered(blockposition)) {
            this.postBreak(world, blockposition, iblockdata.set(BlockTNT.EXPLODE, Boolean.valueOf(true)));
            world.setAir(blockposition);
        }

    }

    @Override
    public void wasExploded(World world, BlockPosition blockposition, Explosion explosion) {
        // Paper start - Old TNT cannon behaviors
        double y = blockposition.getY();
        if (!world.paperConfig.oldCannonBehaviors) y += 0.5;
        EntityTNTPrimed entitytntprimed = new EntityTNTPrimed(world, blockposition.getX() + 0.5F, y, blockposition.getZ() + 0.5F, explosion.getSource());
        // Paper end

        entitytntprimed.setFuseTicks((short) (world.random.nextInt(entitytntprimed.getFuseTicks() / 4) + entitytntprimed.getFuseTicks() / 8));
        world.addEntity(entitytntprimed);
    }

    @Override
    public void postBreak(World world, BlockPosition blockposition, IBlockData iblockdata) {
        this.a(world, blockposition, iblockdata, (EntityLiving) null);
    }

    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, EntityLiving entityliving) {
        if (iblockdata.get(BlockTNT.EXPLODE).booleanValue()) {
            // Paper start - Old TNT cannon behaviors
            double y = blockposition.getY();
            if (!world.paperConfig.oldCannonBehaviors) y += 0.5;
            EntityTNTPrimed entitytntprimed = new EntityTNTPrimed(world, blockposition.getX() + 0.5F, y, blockposition.getZ() + 0.5F, entityliving);
            // Paper end

            world.addEntity(entitytntprimed);
            world.a((EntityHuman) null, entitytntprimed.locX, entitytntprimed.locY, entitytntprimed.locZ, SoundEffects.gV, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean interact(World world, BlockPosition blockposition, IBlockData iblockdata, EntityHuman entityhuman, EnumHand enumhand, EnumDirection enumdirection, float f, float f1, float f2) {
        ItemStack itemstack = entityhuman.b(enumhand);

        if (!itemstack.isEmpty() && (itemstack.getItem() == Items.FLINT_AND_STEEL || itemstack.getItem() == Items.FIRE_CHARGE)) {
            this.a(world, blockposition, iblockdata.set(BlockTNT.EXPLODE, Boolean.valueOf(true)), (EntityLiving) entityhuman);
            world.setTypeAndData(blockposition, Blocks.AIR.getBlockData(), 11);
            if (itemstack.getItem() == Items.FLINT_AND_STEEL) {
                itemstack.damage(1, entityhuman);
            } else if (!entityhuman.abilities.canInstantlyBuild) {
                itemstack.subtract(1);
            }

            return true;
        } else {
            return super.interact(world, blockposition, iblockdata, entityhuman, enumhand, enumdirection, f, f1, f2);
        }
    }

    @Override
    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, Entity entity) {
        if (entity instanceof EntityArrow) {
            EntityArrow entityarrow = (EntityArrow) entity;

            if (entityarrow.isBurning()) {
                // CraftBukkit start
                if (org.bukkit.craftbukkit.event.CraftEventFactory.callEntityChangeBlockEvent(entityarrow, blockposition, Blocks.AIR, 0).isCancelled()) {
                    return;
                }
                // CraftBukkit end
                this.a(world, blockposition, world.getType(blockposition).set(BlockTNT.EXPLODE, Boolean.valueOf(true)), entityarrow.shooter instanceof EntityLiving ? (EntityLiving) entityarrow.shooter : null);
                world.setAir(blockposition);
            }
        }

    }

    @Override
    public boolean a(Explosion explosion) {
        return false;
    }

    @Override
    public IBlockData fromLegacyData(int i) {
        return this.getBlockData().set(BlockTNT.EXPLODE, Boolean.valueOf((i & 1) > 0));
    }

    @Override
    public int toLegacyData(IBlockData iblockdata) {
        return iblockdata.get(BlockTNT.EXPLODE).booleanValue() ? 1 : 0;
    }

    @Override
    protected BlockStateList getStateList() {
        return new BlockStateList(this, new IBlockState[] { BlockTNT.EXPLODE});
    }
}
