package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.event.block.BlockRedstoneEvent; // CraftBukkit

public class BlockRedstoneTorch extends BlockTorch {

    private static final Map<World, List<BlockRedstoneTorch.RedstoneUpdateInfo>> g = new java.util.WeakHashMap(); // Spigot
    private final boolean isOn;

    private boolean a(World world, BlockPosition blockposition, boolean flag) {
        if (!BlockRedstoneTorch.g.containsKey(world)) {
            BlockRedstoneTorch.g.put(world, Lists.<BlockRedstoneTorch.RedstoneUpdateInfo>newArrayList()); // CraftBukkit - fix decompile error
        }

        List list = BlockRedstoneTorch.g.get(world);

        if (flag) {
            list.add(new BlockRedstoneTorch.RedstoneUpdateInfo(blockposition, world.getTime()));
        }

        int i = 0;

        for (int j = 0; j < list.size(); ++j) {
            BlockRedstoneTorch.RedstoneUpdateInfo blockredstonetorch_redstoneupdateinfo = (BlockRedstoneTorch.RedstoneUpdateInfo) list.get(j);

            if (blockredstonetorch_redstoneupdateinfo.a.equals(blockposition)) {
                ++i;
                if (i >= 8) {
                    return true;
                }
            }
        }

        return false;
    }

    protected BlockRedstoneTorch(boolean flag) {
        this.isOn = flag;
        this.a(true);
        this.a((CreativeModeTab) null);
    }

    @Override
    public int a(World world) {
        return 2;
    }

    @Override
    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if (this.isOn) {
            // Paper start - Old TNT cannon behaviors
            if (world.paperConfig.oldCannonBehaviors) {
                this.shiftPositions(world, blockposition);
                return;
            }
            // Paper end
            EnumDirection[] aenumdirection = EnumDirection.values();
            int i = aenumdirection.length;

            for (int j = 0; j < i; ++j) {
                EnumDirection enumdirection = aenumdirection[j];

                world.applyPhysics(blockposition.shift(enumdirection), this, false);
            }
        }

    }

    @Override
    public void remove(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if (this.isOn) {
            // Paper start - Old TNT cannon behaviors
            if (world.paperConfig.oldCannonBehaviors) {
                this.shiftPositions(world, blockposition);
                return;
            }
            // Paper end
            EnumDirection[] aenumdirection = EnumDirection.values();
            int i = aenumdirection.length;

            for (int j = 0; j < i; ++j) {
                EnumDirection enumdirection = aenumdirection[j];

                world.applyPhysics(blockposition.shift(enumdirection), this, false);
            }
        }

    }

    // Paper start - Old TNT cannon behaviors
    private void shiftPositions(World world, BlockPosition blockposition) {
        world.applyPhysics(blockposition.shift(EnumDirection.DOWN), this, false);
        world.applyPhysics(blockposition.shift(EnumDirection.UP), this, false);
        world.applyPhysics(blockposition.shift(EnumDirection.WEST), this, false);
        world.applyPhysics(blockposition.shift(EnumDirection.EAST), this, false);
        world.applyPhysics(blockposition.shift(EnumDirection.SOUTH), this, false);
        world.applyPhysics(blockposition.shift(EnumDirection.NORTH), this, false);
    }
    // Paper end

    @Override
    public int b(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        return this.isOn && iblockdata.get(BlockRedstoneTorch.FACING) != enumdirection ? 15 : 0;
    }

    private boolean g(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = iblockdata.get(BlockRedstoneTorch.FACING).opposite();

        return world.isBlockFacePowered(blockposition.shift(enumdirection), enumdirection);
    }

    @Override
    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {}

    @Override
    public void b(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {
        boolean flag = this.g(world, blockposition, iblockdata);
        List list = BlockRedstoneTorch.g.get(world);

        // Paper start
        if (list != null) {
            int index = 0;
            while (index < list.size() && world.getTime() - ((BlockRedstoneTorch.RedstoneUpdateInfo) list.get(index)).getTime() > 60L) {
                index++;
            }
            if (index > 0) {
                list.subList(0, index).clear();
            }
        }
        // Paper end

        // CraftBukkit start
        org.bukkit.plugin.PluginManager manager = world.getServer().getPluginManager();
        org.bukkit.block.Block block = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
        int oldCurrent = this.isOn ? 15 : 0;

        BlockRedstoneEvent event = BlockRedstoneEvent.requestMutable(block, oldCurrent, oldCurrent);
        // CraftBukkit end

        if (this.isOn) {
            if (flag) {
                // CraftBukkit start
                if (oldCurrent != 0) {
                    event.setNewCurrent(0);
                    manager.callEvent(event);
                    if (event.getNewCurrent() != 0) {
                        return;
                    }
                }
                // CraftBukkit end
                world.setTypeAndData(blockposition, Blocks.UNLIT_REDSTONE_TORCH.getBlockData().set(BlockRedstoneTorch.FACING, iblockdata.get(BlockRedstoneTorch.FACING)), 3);
                if (this.a(world, blockposition, true)) {
                    world.a((EntityHuman) null, blockposition, SoundEffects.fl, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

                    for (int i = 0; i < 5; ++i) {
                        double d0 = blockposition.getX() + random.nextDouble() * 0.6D + 0.2D;
                        double d1 = blockposition.getY() + random.nextDouble() * 0.6D + 0.2D;
                        double d2 = blockposition.getZ() + random.nextDouble() * 0.6D + 0.2D;

                        world.addParticle(EnumParticle.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D, new int[0]);
                    }

                    world.a(blockposition, world.getType(blockposition).getBlock(), 160);
                }
            }
        } else if (!flag && !this.a(world, blockposition, false)) {
            // CraftBukkit start
            if (oldCurrent != 15) {
                event.setNewCurrent(15);
                manager.callEvent(event);
                if (event.getNewCurrent() != 15) {
                    return;
                }
            }
            // CraftBukkit end
            world.setTypeAndData(blockposition, Blocks.REDSTONE_TORCH.getBlockData().set(BlockRedstoneTorch.FACING, iblockdata.get(BlockRedstoneTorch.FACING)), 3);
        }

    }

    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, BlockPosition blockposition1) {
        if (!this.e(world, blockposition, iblockdata)) {
            if (this.isOn == this.g(world, blockposition, iblockdata)) {
                world.a(blockposition, this, this.a(world));
            }

        }
    }

    @Override
    public int c(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        return enumdirection == EnumDirection.DOWN ? iblockdata.a(iblockaccess, blockposition, enumdirection) : 0;
    }

    @Override
    public Item getDropType(IBlockData iblockdata, Random random, int i) {
        return Item.getItemOf(Blocks.REDSTONE_TORCH);
    }

    @Override
    public boolean isPowerSource(IBlockData iblockdata) {
        return true;
    }

    @Override
    public ItemStack a(World world, BlockPosition blockposition, IBlockData iblockdata) {
        return new ItemStack(Blocks.REDSTONE_TORCH);
    }

    @Override
    public boolean b(Block block) {
        return block == Blocks.UNLIT_REDSTONE_TORCH || block == Blocks.REDSTONE_TORCH;
    }

    static class RedstoneUpdateInfo {

        BlockPosition a;
        long b; final long getTime() { return this.b; } // Paper - OBFHELPER

        public RedstoneUpdateInfo(BlockPosition blockposition, long i) {
            this.a = blockposition;
            this.b = i;
        }
    }
}
