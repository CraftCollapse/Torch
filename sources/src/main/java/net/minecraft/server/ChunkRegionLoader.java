package net.minecraft.server;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentLinkedQueue; // Paper
import java.util.concurrent.atomic.LongAdder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.torch.server.TorchIOThread;

import com.google.common.collect.Maps;

public class ChunkRegionLoader implements IChunkLoader, IAsyncChunkSaver {

    private ConcurrentLinkedQueue<QueuedChunk> queue = new com.destroystokyo.paper.utils.CachedSizeConcurrentLinkedQueue<>(); // Paper - Chunk queue improvements
    private final Object lock = new Object(); // Paper - Chunk queue improvements
    private static final Logger a = LogManager.getLogger();
    private final Map<ChunkCoordIntPair, NBTTagCompound> b = Maps.newConcurrentMap();
    //private final Set<ChunkCoordIntPair> c = Collections.newSetFromMap(new ConcurrentHashMap()); // Paper - Chunk queue improvements
    private final File d;
    private final DataConverterManager e;
    private boolean f;

    public ChunkRegionLoader(File file, DataConverterManager dataconvertermanager) {
        this.d = file;
        this.e = dataconvertermanager;
    }

    // CraftBukkit start
    // Paper start
    private long queuedSaves = 0;
    private final LongAdder processedSaves = new LongAdder();
    public int getQueueSize() { return queue.size(); }
    public long getQueuedSaves() { return queuedSaves; }
    public long getProcessedSaves() { return processedSaves.longValue(); }
    // Paper end
    // CraftBukkit end

    // CraftBukkit start - Add async variant, provide compatibility
    @Override @Nullable
    public Chunk a(World world, int i, int j) throws IOException {
        world.timings.syncChunkLoadDataTimer.startTiming(); // Spigot
        Object[] data = loadChunk(world, i, j);
        world.timings.syncChunkLoadDataTimer.stopTiming(); // Spigot
        if (data != null) {
            Chunk chunk = (Chunk) data[0];
            NBTTagCompound nbttagcompound = (NBTTagCompound) data[1];
            loadEntities(chunk, nbttagcompound.getCompound("Level"), world);
            return chunk;
        }

        return null;
    }

    public Object[] loadChunk(World world, int i, int j) throws IOException {
        // CraftBukkit end
        ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i, j);
        NBTTagCompound nbttagcompound = this.b.get(chunkcoordintpair);

        if (nbttagcompound == null) {
            // CraftBukkit start
            nbttagcompound = RegionFileCache.d(this.d, i, j);

            if (nbttagcompound == null) {
                return null;
            }

            nbttagcompound = this.e.a(DataConverterTypes.CHUNK, nbttagcompound);
            // CraftBukkit end
        }

        return this.a(world, i, j, nbttagcompound);
    }

    public boolean chunkExists(int x, int z) { return this.a(x, z); } // OBFHELPER (also in IChunkLoader)
    @Override public boolean a(int i, int j) {
        ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i, j);
        NBTTagCompound nbttagcompound = this.b.get(chunkcoordintpair);

        return nbttagcompound != null ? true : RegionFileCache.f(this.d, i, j);
    }

    @Nullable
    protected Object[] a(World world, int i, int j, NBTTagCompound nbttagcompound) { // CraftBukkit - return Chunk -> Object[]
        if (!nbttagcompound.hasKeyOfType("Level", 10)) {
            ChunkRegionLoader.a.error("Chunk file at {},{} is missing level data, skipping", new Object[] { Integer.valueOf(i), Integer.valueOf(j)});
            return null;
        } else {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("Level");

            if (!nbttagcompound1.hasKeyOfType("Sections", 9)) {
                ChunkRegionLoader.a.error("Chunk file at {},{} is missing block data, skipping", new Object[] { Integer.valueOf(i), Integer.valueOf(j)});
                return null;
            } else {
                Chunk chunk = this.a(world, nbttagcompound1);

                if (!chunk.a(i, j)) {
                    ChunkRegionLoader.a.error("Chunk file at {},{} is in the wrong location; relocating. (Expected {}, {}, got {}, {})", new Object[] { Integer.valueOf(i), Integer.valueOf(j), Integer.valueOf(i), Integer.valueOf(j), Integer.valueOf(chunk.locX), Integer.valueOf(chunk.locZ)});
                    nbttagcompound1.setInt("xPos", i);
                    nbttagcompound1.setInt("zPos", j);

                    // CraftBukkit start - Have to move tile entities since we don't load them at this stage
                    NBTTagList tileEntities = nbttagcompound.getCompound("Level").getList("TileEntities", 10);
                    if (tileEntities != null) {
                        for (int te = 0; te < tileEntities.size(); te++) {
                            NBTTagCompound tileEntity = tileEntities.get(te);
                            int x = tileEntity.getInt("x") - chunk.locX * 16;
                            int z = tileEntity.getInt("z") - chunk.locZ * 16;
                            tileEntity.setInt("x", i * 16 + x);
                            tileEntity.setInt("z", j * 16 + z);
                        }
                    }
                    // CraftBukkit end
                    chunk = this.a(world, nbttagcompound1);
                }

                // CraftBukkit start
                Object[] data = new Object[2];
                data[0] = chunk;
                data[1] = nbttagcompound;
                return data;
                // CraftBukkit end
            }
        }
    }

    @Override
	public void a(World world, Chunk chunk) throws IOException, ExceptionWorldConflict {
        world.checkSession();

        try {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();

            nbttagcompound.set("Level", nbttagcompound1);
            nbttagcompound.setInt("DataVersion", 922);
            this.a(chunk, world, nbttagcompound1);
            this.a(chunk.k(), nbttagcompound);
        } catch (Exception exception) {
            ChunkRegionLoader.a.error("Failed to save chunk", exception);
        }

    }

    protected void a(ChunkCoordIntPair chunkcoordintpair, NBTTagCompound nbttagcompound) {
        synchronized (lock) {  // Paper - Chunk queue improvements
            this.b.put(chunkcoordintpair, nbttagcompound);
        }
        queuedSaves++; // Paper
        queue.add(new QueuedChunk(chunkcoordintpair, nbttagcompound)); // Paper - Chunk queue improvements
        
        TorchIOThread.saveChunk(this);
    }

    @Override
	public boolean c() {
        // Paper start - Chunk queue improvements
        QueuedChunk chunk = queue.poll();
        if (chunk == null) {
            // Paper - end
            if (this.f) {
                ChunkRegionLoader.a.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", new Object[] { this.d.getName()});
            }

            return false;
        } else {
            ChunkCoordIntPair chunkcoordintpair = chunk.coords; // Paper - Chunk queue improvements
            processedSaves.increment(); // Paper

            boolean flag;

            try {
                //this.c.add(chunkcoordintpair);
                NBTTagCompound nbttagcompound = chunk.compound; // Paper - Chunk queue improvements

                if (nbttagcompound != null) {
                    int attempts = 0; Exception laste = null; while (attempts++ < 5) { // Paper
                    try {
                        this.b(chunkcoordintpair, nbttagcompound);
                        laste = null; break; // Paper
                    } catch (Exception exception) {
                        //ChunkRegionLoader.a.error("Failed to save chunk", exception); // Paper
                        laste = exception; // Paper
                    }
                    try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();} } // Paper
                    if (laste != null) { com.destroystokyo.paper.exception.ServerInternalException.reportInternalException(laste); MinecraftServer.LOGGER.error("Failed to save chunk", laste); } // Paper
                }
                synchronized (lock) { if (this.b.get(chunkcoordintpair) == nbttagcompound) { this.b.remove(chunkcoordintpair); } }// Paper - This will not equal if a newer version is still pending

                flag = true;
            } finally {
                //this.c.remove(chunkcoordintpair); // Paper
            }

            return flag;
        }
    }

    private void b(ChunkCoordIntPair chunkcoordintpair, NBTTagCompound nbttagcompound) throws IOException {
        // CraftBukkit start
        RegionFileCache.e(this.d, chunkcoordintpair.x, chunkcoordintpair.z, nbttagcompound);

        /*
        NBTCompressedStreamTools.a(nbttagcompound, (DataOutput) dataoutputstream);
        dataoutputstream.close();
        */
        // CraftBukkit end
    }

    @Override
	public void b(World world, Chunk chunk) throws IOException {}

    @Override
	public void a() {}

    @Override
	public void b() {
        try {
            this.f = true;

            while (true) {
                if (this.c()) {
                    continue;
                }
                break; // CraftBukkit - Fix infinite loop when saving chunks
            }
        } finally {
            this.f = false;
        }

    }

    public static void a(DataConverterManager dataconvertermanager) {
        dataconvertermanager.a(DataConverterTypes.CHUNK, new DataInspector() {
            @Override
			public NBTTagCompound a(DataConverter dataconverter, NBTTagCompound nbttagcompound, int i) {
                if (nbttagcompound.hasKeyOfType("Level", 10)) {
                    NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("Level");
                    NBTTagList nbttaglist;
                    int j;

                    if (nbttagcompound1.hasKeyOfType("Entities", 9)) {
                        nbttaglist = nbttagcompound1.getList("Entities", 10);

                        for (j = 0; j < nbttaglist.size(); ++j) {
                            nbttaglist.a(j, dataconverter.a(DataConverterTypes.ENTITY, (NBTTagCompound) nbttaglist.h(j), i));
                        }
                    }

                    if (nbttagcompound1.hasKeyOfType("TileEntities", 9)) {
                        nbttaglist = nbttagcompound1.getList("TileEntities", 10);

                        for (j = 0; j < nbttaglist.size(); ++j) {
                            nbttaglist.a(j, dataconverter.a(DataConverterTypes.BLOCK_ENTITY, (NBTTagCompound) nbttaglist.h(j), i));
                        }
                    }
                }

                return nbttagcompound;
            }
        });
    }

    private void a(Chunk chunk, World world, NBTTagCompound nbttagcompound) {
        nbttagcompound.setInt("xPos", chunk.locX);
        nbttagcompound.setInt("zPos", chunk.locZ);
        nbttagcompound.setLong("LastUpdate", world.getTime());
        nbttagcompound.setIntArray("HeightMap", chunk.r());
        nbttagcompound.setBoolean("TerrainPopulated", chunk.isDone());
        nbttagcompound.setBoolean("LightPopulated", chunk.v());
        nbttagcompound.setLong("InhabitedTime", chunk.x());
        ChunkSection[] achunksection = chunk.getSections();
        NBTTagList nbttaglist = new NBTTagList();
        boolean flag = world.worldProvider.m();
        ChunkSection[] achunksection1 = achunksection;
        int i = achunksection.length;

        NBTTagCompound nbttagcompound1;

        for (int j = 0; j < i; ++j) {
            ChunkSection chunksection = achunksection1[j];

            if (chunksection != Chunk.a) {
                nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Y", (byte) (chunksection.getYPosition() >> 4 & 255));
                byte[] abyte = new byte[4096];
                NibbleArray nibblearray = new NibbleArray();
                NibbleArray nibblearray1 = chunksection.getBlocks().exportData(abyte, nibblearray);

                nbttagcompound1.setByteArray("Blocks", abyte);
                nbttagcompound1.setByteArray("Data", nibblearray.asBytes());
                if (nibblearray1 != null) {
                    nbttagcompound1.setByteArray("Add", nibblearray1.asBytes());
                }

                nbttagcompound1.setByteArray("BlockLight", chunksection.getEmittedLightArray().asBytes());
                if (flag) {
                    nbttagcompound1.setByteArray("SkyLight", chunksection.getSkyLightArray().asBytes());
                } else {
                    nbttagcompound1.setByteArray("SkyLight", new byte[chunksection.getEmittedLightArray().asBytes().length]);
                }

                nbttaglist.add(nbttagcompound1);
            }
        }

        nbttagcompound.set("Sections", nbttaglist);
        nbttagcompound.setByteArray("Biomes", chunk.getBiomeIndex());
        chunk.g(false);
        NBTTagList nbttaglist1 = new NBTTagList();

        Iterator iterator;

        for (i = 0; i < chunk.getEntitySlices().length; ++i) {
            iterator = chunk.getEntitySlices()[i].iterator();

            while (iterator.hasNext()) {
                Entity entity = (Entity) iterator.next();

                nbttagcompound1 = new NBTTagCompound();
                if (entity.d(nbttagcompound1)) {
                    chunk.g(true);
                    nbttaglist1.add(nbttagcompound1);
                }
            }
        }

        nbttagcompound.set("Entities", nbttaglist1);
        NBTTagList nbttaglist2 = new NBTTagList();

        iterator = chunk.getTileEntities().values().iterator();

        while (iterator.hasNext()) {
            TileEntity tileentity = (TileEntity) iterator.next();

            nbttagcompound1 = tileentity.save(new NBTTagCompound());
            nbttaglist2.add(nbttagcompound1);
        }

        nbttagcompound.set("TileEntities", nbttaglist2);
        List list = world.a(chunk, false);

        if (list != null) {
            long k = world.getTime();
            NBTTagList nbttaglist3 = new NBTTagList();
            Iterator iterator1 = list.iterator();

            while (iterator1.hasNext()) {
                NextTickListEntry nextticklistentry = (NextTickListEntry) iterator1.next();
                NBTTagCompound nbttagcompound2 = new NBTTagCompound();
                MinecraftKey minecraftkey = Block.REGISTRY.b(nextticklistentry.a());

                nbttagcompound2.setString("i", minecraftkey == null ? "" : minecraftkey.toString());
                nbttagcompound2.setInt("x", nextticklistentry.a.getX());
                nbttagcompound2.setInt("y", nextticklistentry.a.getY());
                nbttagcompound2.setInt("z", nextticklistentry.a.getZ());
                nbttagcompound2.setInt("t", (int) (nextticklistentry.b - k));
                nbttagcompound2.setInt("p", nextticklistentry.c);
                nbttaglist3.add(nbttagcompound2);
            }

            nbttagcompound.set("TileTicks", nbttaglist3);
        }

    }

    private Chunk a(World world, NBTTagCompound nbttagcompound) {
        int i = nbttagcompound.getInt("xPos");
        int j = nbttagcompound.getInt("zPos");
        Chunk chunk = new Chunk(world, i, j);

        chunk.a(nbttagcompound.getIntArray("HeightMap"));
        chunk.d(nbttagcompound.getBoolean("TerrainPopulated"));
        chunk.e(nbttagcompound.getBoolean("LightPopulated"));
        chunk.c(nbttagcompound.getLong("InhabitedTime"));
        NBTTagList nbttaglist = nbttagcompound.getList("Sections", 10);
        boolean flag = true;
        ChunkSection[] achunksection = new ChunkSection[16];
        boolean flag1 = world.worldProvider.m();

        for (int k = 0; k < nbttaglist.size(); ++k) {
            NBTTagCompound nbttagcompound1 = nbttaglist.get(k);
            byte b0 = nbttagcompound1.getByte("Y");
            ChunkSection chunksection = new ChunkSection(b0 << 4, flag1);
            byte[] abyte = nbttagcompound1.getByteArray("Blocks");
            NibbleArray nibblearray = new NibbleArray(nbttagcompound1.getByteArray("Data"));
            NibbleArray nibblearray1 = nbttagcompound1.hasKeyOfType("Add", 7) ? new NibbleArray(nbttagcompound1.getByteArray("Add")) : null;

            chunksection.getBlocks().a(abyte, nibblearray, nibblearray1);
            chunksection.a(new NibbleArray(nbttagcompound1.getByteArray("BlockLight")));
            if (flag1) {
                chunksection.b(new NibbleArray(nbttagcompound1.getByteArray("SkyLight")));
            }

            chunksection.recalcBlockCounts();
            achunksection[b0] = chunksection;
        }

        chunk.a(achunksection);
        if (nbttagcompound.hasKeyOfType("Biomes", 7)) {
            chunk.a(nbttagcompound.getByteArray("Biomes"));
        }

        // CraftBukkit start - End this method here and split off entity loading to another method
        return chunk;
    }

    public void loadEntities(Chunk chunk, NBTTagCompound nbttagcompound, World world) {
        // CraftBukkit end
        world.timings.syncChunkLoadNBTTimer.startTiming(); // Spigot
        NBTTagList nbttaglist1 = nbttagcompound.getList("Entities", 10);

        for (int l = 0; l < nbttaglist1.size(); ++l) {
            NBTTagCompound nbttagcompound2 = nbttaglist1.get(l);

            a(nbttagcompound2, world, chunk);
            chunk.g(true);
        }
        NBTTagList nbttaglist2 = nbttagcompound.getList("TileEntities", 10);

        for (int i1 = 0; i1 < nbttaglist2.size(); ++i1) {
            NBTTagCompound nbttagcompound3 = nbttaglist2.get(i1);
            TileEntity tileentity = TileEntity.a(world, nbttagcompound3);

            if (tileentity != null) {
                chunk.a(tileentity);
            }
        }

        if (nbttagcompound.hasKeyOfType("TileTicks", 9)) {
            NBTTagList nbttaglist3 = nbttagcompound.getList("TileTicks", 10);

            for (int j1 = 0; j1 < nbttaglist3.size(); ++j1) {
                NBTTagCompound nbttagcompound4 = nbttaglist3.get(j1);
                Block block;

                if (nbttagcompound4.hasKeyOfType("i", 8)) {
                    block = Block.getByName(nbttagcompound4.getString("i"));
                } else {
                    block = Block.getById(nbttagcompound4.getInt("i"));
                }

                world.b(new BlockPosition(nbttagcompound4.getInt("x"), nbttagcompound4.getInt("y"), nbttagcompound4.getInt("z")), block, nbttagcompound4.getInt("t"), nbttagcompound4.getInt("p"));
            }
        }
        world.timings.syncChunkLoadNBTTimer.stopTiming(); // Spigot

        // return chunk; // CraftBukkit
    }

    @Nullable
    public static Entity a(NBTTagCompound nbttagcompound, World world, Chunk chunk) {
        Entity entity = a(nbttagcompound, world);

        if (entity == null) {
            return null;
        } else {
            chunk.a(entity);
            if (nbttagcompound.hasKeyOfType("Passengers", 9)) {
                NBTTagList nbttaglist = nbttagcompound.getList("Passengers", 10);

                for (int i = 0; i < nbttaglist.size(); ++i) {
                    Entity entity1 = a(nbttaglist.get(i), world, chunk);

                    if (entity1 != null) {
                        entity1.a(entity, true);
                    }
                }
            }

            return entity;
        }
    }

    @Nullable
    // CraftBukkit start
    public static Entity a(NBTTagCompound nbttagcompound, World world, double d0, double d1, double d2, boolean flag) {
        return spawnEntity(nbttagcompound, world, d0, d1, d2, flag, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DEFAULT);
    }

    public static Entity spawnEntity(NBTTagCompound nbttagcompound, World world, double d0, double d1, double d2, boolean flag, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason spawnReason) {
        // CraftBukkit end
        Entity entity = a(nbttagcompound, world);

        if (entity == null) {
            return null;
        } else {
            entity.setPositionRotation(d0, d1, d2, entity.yaw, entity.pitch);
            if (flag && !world.addEntity(entity, spawnReason)) { // CraftBukkit
                return null;
            } else {
                if (nbttagcompound.hasKeyOfType("Passengers", 9)) {
                    NBTTagList nbttaglist = nbttagcompound.getList("Passengers", 10);

                    for (int i = 0; i < nbttaglist.size(); ++i) {
                        Entity entity1 = a(nbttaglist.get(i), world, d0, d1, d2, flag);

                        if (entity1 != null) {
                            entity1.a(entity, true);
                        }
                    }
                }

                return entity;
            }
        }
    }

    @Nullable
    protected static Entity a(NBTTagCompound nbttagcompound, World world) {
        try {
            return EntityTypes.a(nbttagcompound, world);
        } catch (RuntimeException runtimeexception) {
            return null;
        }
    }

    // CraftBukkit start
    public static void a(Entity entity, World world) {
        a(entity, world, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DEFAULT);
    }

    public static void a(Entity entity, World world, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason) {
        if (!entity.valid && world.addEntity(entity, reason) && entity.isVehicle()) { // Paper
            // CraftBukkit end
            Iterator iterator = entity.bx().iterator();

            while (iterator.hasNext()) {
                Entity entity1 = (Entity) iterator.next();

                a(entity1, world);
            }
        }

    }

    @Nullable
    public static Entity a(NBTTagCompound nbttagcompound, World world, boolean flag) {
        Entity entity = a(nbttagcompound, world);

        if (entity == null) {
            return null;
        } else if (flag && !world.addEntity(entity)) {
            return null;
        } else {
            if (nbttagcompound.hasKeyOfType("Passengers", 9)) {
                NBTTagList nbttaglist = nbttagcompound.getList("Passengers", 10);

                for (int i = 0; i < nbttaglist.size(); ++i) {
                    Entity entity1 = a(nbttaglist.get(i), world, flag);

                    if (entity1 != null) {
                        entity1.a(entity, true);
                    }
                }
            }

            return entity;
        }
    }

    // Paper start - Chunk queue improvements
    private static class QueuedChunk {
        public ChunkCoordIntPair coords;
        public NBTTagCompound compound;

        public QueuedChunk(ChunkCoordIntPair coords, NBTTagCompound compound) {
            this.coords = coords;
            this.compound = compound;
        }
    }
    // Paper end
}
