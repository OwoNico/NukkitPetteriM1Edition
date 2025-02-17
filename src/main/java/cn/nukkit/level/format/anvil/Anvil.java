package cn.nukkit.level.format.anvil;

import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.format.generic.BaseLevelProvider;
import cn.nukkit.level.format.generic.BaseRegionLoader;
import cn.nukkit.level.format.generic.serializer.NetworkChunkSerializer;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.ChunkException;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public class Anvil extends BaseLevelProvider {

    public Anvil(Level level, String path) throws IOException {
        super(level, path);
    }

    public static String getProviderName() {
        return "anvil";
    }

    public static byte getProviderOrder() {
        return ORDER_YZX;
    }

    public static boolean usesChunkSection() {
        return true;
    }

    public static boolean isValid(String path) {
        boolean isValid = (new File(path + "/level.dat").exists()) && new File(path + "/region/").isDirectory();
        if (isValid) {
            for (File file : new File(path + "/region/").listFiles((dir, name) -> Pattern.matches("^.+\\.mc[r|a]$", name))) {
                if (!file.getName().endsWith(".mca")) {
                    isValid = false;
                    break;
                }
            }
        }
        return isValid;
    }

    public static void generate(String path, String name, long seed, Class<? extends Generator> generator) throws IOException {
        generate(path, name, seed, generator, new HashMap<>());
    }

    public static void generate(String path, String name, long seed, Class<? extends Generator> generator, Map<String, String> options) throws IOException {
        if (!new File(path + "/region").exists()) {
            new File(path + "/region").mkdirs();
        }

        CompoundTag levelData = new CompoundTag("Data")
                .putCompound("GameRules", new CompoundTag())
                .putLong("DayTime", 0)
                .putInt("GameType", 0)
                .putString("generatorName", Generator.getGeneratorName(generator))
                .putString("generatorOptions", options.getOrDefault("preset", ""))
                .putInt("generatorVersion", 1)
                .putBoolean("hardcore", false)
                .putBoolean("initialized", true)
                .putLong("LastPlayed", System.currentTimeMillis() / 1000)
                .putString("LevelName", name)
                .putBoolean("raining", false)
                .putInt("rainTime", 0)
                .putLong("RandomSeed", seed)
                .putInt("SpawnX", 128)
                .putInt("SpawnY", 70)
                .putInt("SpawnZ", 128)
                .putBoolean("thundering", false)
                .putInt("thunderTime", 0)
                .putInt("version", 19133)
                .putLong("Time", 0)
                .putLong("SizeOnDisk", 0);

        NBTIO.writeGZIPCompressed(new CompoundTag().putCompound("Data", levelData), new FileOutputStream(path + "level.dat"), ByteOrder.BIG_ENDIAN);
    }

    @Override
    public Chunk getEmptyChunk(int chunkX, int chunkZ) {
        return Chunk.getEmptyChunk(chunkX, chunkZ, this);
    }

    @Override
    public void requestChunkTask(IntSet protocols, int x, int z) throws ChunkException {
        Chunk chunk = (Chunk) this.getChunk(x, z, false);
        if (chunk == null) {
            throw new ChunkException("Invalid Chunk Set");
        }

        long timestamp = chunk.getChanges();

        /*byte[] blockEntities = new byte[0];

        if (!chunk.getBlockEntities().isEmpty()) {
            List<CompoundTag> tagList = new ArrayList<>();

            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity instanceof BlockEntitySpawnable) {
                    tagList.add(((BlockEntitySpawnable) blockEntity).getSpawnCompound());
                }
            }

            try {
                blockEntities = NBTIO.write(tagList, ByteOrder.LITTLE_ENDIAN, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Map<Integer, Integer> extra = chunk.getBlockExtraDataArray();
        BinaryStream extraData;
        if (!extra.isEmpty()) {
            extraData = new BinaryStream();
            extraData.putVarInt(extra.size());
            for (Map.Entry<Integer, Integer> entry : extra.entrySet()) {
                extraData.putVarInt(entry.getKey());
                extraData.putLShort(entry.getValue());
            }
        } else {
            extraData = null;
        }

        int subChunkCount = 0;
        cn.nukkit.level.format.ChunkSection[] sections = chunk.getSections();
        for (int i = sections.length - 1; i >= 0; i--) {
            if (!sections[i].isEmpty()) {
                subChunkCount = i + 1;
                break;
            }
        }

        for (int protocolId : protocols) {
            BinaryStream stream = ThreadCache.binaryStream.get().reset();
            if (protocolId < ProtocolInfo.v1_12_0) {
                stream.putByte((byte) subChunkCount);
            }

            //1.18.0开始主世界支持384世界高度
            byte[] biomePalettes = null;
            if (protocolId >= ProtocolInfo.v1_18_0) {
                // In 1.18 3D biome palettes were introduced. However, current world format
                // used internally doesn't support them, so we need to convert from legacy 2D
                biomePalettes = this.convert2DBiomesTo3D(protocolId, chunk);

                stream = ThreadCache.binaryStream.get().reset();

                if (this.level.getDimension() == 0) {
                    // Build up 4 SubChunks for the extended negative height
                    for (int i = 0; i < EXTENDED_NEGATIVE_SUB_CHUNKS; i++) {
                        stream.putByte((byte) 8); // SubChunk version
                        stream.putByte((byte) 0); // 0 layers
                    }
                }
            }

            for (int i = 0; i < subChunkCount; i++) {
                if (protocolId < ProtocolInfo.v1_13_0) {
                    stream.putByte((byte) 0);
                    stream.put(sections[i].getBytes(protocolId));
                }else {
                    sections[i].writeTo(protocolId, stream);
                }
            }
            if (protocolId < ProtocolInfo.v1_12_0) {
                for (byte height : chunk.getHeightMapArray()) {
                    stream.putByte(height);
                }
                stream.put(PAD_256);
            }
            if (protocolId >= ProtocolInfo.v1_18_0) {
                stream.put(biomePalettes);
            }else {
                stream.put(chunk.getBiomeIdArray());
            }
            stream.putByte((byte) 0);// Border blocks
            if (protocolId < ProtocolInfo.v1_16_100) {
                stream.putVarInt(0);// There is no extra data anymore but idk when it was removed
            }
            stream.put(blockEntities);

            int count = subChunkCount;
            if (protocolId >= ProtocolInfo.v1_18_0 && this.level.getDimension() == 0) {
                count += EXTENDED_NEGATIVE_SUB_CHUNKS;
            }
            this.getLevel().chunkRequestCallback(protocolId, timestamp, x, z, count, stream.getBuffer());
        }*/

        if (this.getServer().asyncChunkSending) {
            final Chunk chunkClone = chunk.fullClone();
            this.level.getAsyncChuckExecutor().execute(() -> {
                NetworkChunkSerializer.serialize(protocols, chunkClone, networkChunkSerializerCallback -> {
                    getLevel().asyncChunkRequestCallback(networkChunkSerializerCallback.getProtocolId(),
                            timestamp,
                            x,
                            z,
                            networkChunkSerializerCallback.getSubchunks(),
                            networkChunkSerializerCallback.getStream().getBuffer()
                    );
                }, getLevel().getDimensionData());
            });
        }else {
            NetworkChunkSerializer.serialize(protocols, chunk, networkChunkSerializerCallback -> {
                this.getLevel().chunkRequestCallback(networkChunkSerializerCallback.getProtocolId(),
                        timestamp,
                        x,
                        z,
                        networkChunkSerializerCallback.getSubchunks(),
                        networkChunkSerializerCallback.getStream().getBuffer()
                );
            }, this.level.getDimensionData());
        }
    }

/*    private byte[] convert2DBiomesTo3D(int protocolId, BaseFullChunk chunk) {
        PalettedBlockStorage palette = PalettedBlockStorage.createWithDefaultState(Biome.getBiomeIdOrCorrect(protocolId, chunk.getBiomeId(0, 0)));
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int biomeId = Biome.getBiomeIdOrCorrect(protocolId, chunk.getBiomeId(x, z));
                for (int y = 0; y < 16; y++) {
                    palette.setBlock(x, y, z, biomeId);
                }
            }
        }

        BinaryStream stream = ThreadCache.binaryStream.get().reset();
        palette.writeTo(protocolId, stream);
        byte[] bytes = stream.getBuffer();
        stream.reset();

        for (int i = 0; i < 25; i++) {
            stream.put(bytes);
        }
        return stream.getBuffer();
    }*/

    private int lastPosition = 0;

    @Override
    public void doGarbageCollection(long time) {
        long start = System.currentTimeMillis();
        int maxIterations = size();
        if (lastPosition > maxIterations) lastPosition = 0;
        int i;
        synchronized (chunks) {
            ObjectIterator<BaseFullChunk> iter = chunks.values().iterator();
            if (lastPosition != 0) iter.skip(lastPosition);
            for (i = 0; i < maxIterations; i++) {
                if (!iter.hasNext()) {
                    iter = chunks.values().iterator();
                }
                if (!iter.hasNext()) break;
                BaseFullChunk chunk = iter.next();
                if (chunk == null) continue;
                if (chunk.isGenerated() && chunk.isPopulated() && chunk instanceof Chunk) {
                    chunk.compress();
                    if (System.currentTimeMillis() - start >= time) break;
                }
            }
        }
        lastPosition += i;
    }

    @Override
    public synchronized BaseFullChunk loadChunk(long index, int chunkX, int chunkZ, boolean create) {
        int regionX = getRegionIndexX(chunkX);
        int regionZ = getRegionIndexZ(chunkZ);
        BaseRegionLoader region = this.loadRegion(regionX, regionZ);
        if (this.level.timings.syncChunkLoadDataTimer != null) this.level.timings.syncChunkLoadDataTimer.startTiming();
        BaseFullChunk chunk;
        try {
            chunk = region.readChunk(chunkX - (regionX << 5), chunkZ - (regionZ << 5));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (chunk == null) {
            if (create) {
                chunk = this.getEmptyChunk(chunkX, chunkZ);
                putChunk(index, chunk);
            }
        } else {
            putChunk(index, chunk);
        }
        if (this.level.timings.syncChunkLoadDataTimer != null) this.level.timings.syncChunkLoadDataTimer.stopTiming();
        return chunk;
    }

    @Override
    public synchronized void saveChunk(int X, int Z) {
        BaseFullChunk chunk = this.getChunk(X, Z);
        if (chunk != null) {
            try {
                this.loadRegion(X >> 5, Z >> 5).writeChunk(chunk);
            } catch (Exception e) {
                throw new ChunkException("Error saving chunk (" + X + ", " + Z + ')', e);
            }
        }
    }


    @Override
    public synchronized void saveChunk(int x, int z, FullChunk chunk) {
        if (!(chunk instanceof Chunk)) {
            throw new ChunkException("Invalid Chunk class");
        }
        int regionX = x >> 5;
        int regionZ = z >> 5;
        this.loadRegion(regionX, regionZ);
        chunk.setX(x);
        chunk.setZ(z);
        try {
            this.getRegion(regionX, regionZ).writeChunk(chunk);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ChunkSection createChunkSection(int y) {
        ChunkSection cs = new ChunkSection(y);
        cs.hasSkyLight = true;
        return cs;
    }

    protected synchronized BaseRegionLoader loadRegion(int x, int z) {
        BaseRegionLoader tmp = lastRegion.get();
        if (tmp != null && x == tmp.getX() && z == tmp.getZ()) {
            return tmp;
        }
        long index = Level.chunkHash(x, z);
        synchronized (regions) {
            BaseRegionLoader region = this.regions.get(index);
            if (region == null) {
                try {
                    region = new RegionLoader(this, x, z);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                this.regions.put(index, region);
            }
            lastRegion.set(region);
            return region;
        }
    }

    @Override
    public int getMaximumLayer() {
        return 1;
    }
}