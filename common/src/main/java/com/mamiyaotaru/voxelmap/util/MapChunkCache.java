package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.DebugRenderState;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

public class MapChunkCache {
    private final int width;
    private final int height;
    private LevelChunk lastCenterChunk;
    private final MapChunk[] mapChunks;
    private int left;
    private int right;
    private int top;
    private int bottom;
    private boolean loaded;
    private final IChangeObserver changeObserver;

    public MapChunkCache(int width, int height, IChangeObserver changeObserver) {
        this.width = width;
        this.height = height;
        this.mapChunks = new MapChunk[width * height];
        this.changeObserver = changeObserver;
    }

    public void centerChunks(BlockPos blockPos) {
        LevelChunk currentChunk = VoxelConstants.getPlayer().level().getChunkAt(blockPos);
        if (currentChunk != this.lastCenterChunk) {
            if (this.lastCenterChunk == null) {
                this.fillAllChunks(blockPos);
                this.lastCenterChunk = currentChunk;
                return;
            }

            int middleX = this.width / 2;
            int middleZ = this.height / 2;
            int movedX = currentChunk.getPos().x - this.lastCenterChunk.getPos().x;
            int movedZ = currentChunk.getPos().z - this.lastCenterChunk.getPos().z;
            if (Math.abs(movedX) < this.width && Math.abs(movedZ) < this.height && currentChunk.getLevel().equals(this.lastCenterChunk.getLevel())) {
                this.moveX(movedX);
                this.moveZ(movedZ);

                for (int z = movedZ > 0 ? this.height - movedZ : 0; z < (movedZ > 0 ? this.height : -movedZ); ++z) {
                    for (int x = 0; x < this.width; ++x) {
                        this.mapChunks[x + z * this.width] = new MapChunk(currentChunk.getPos().x - (middleX - x), currentChunk.getPos().z - (middleZ - z));
                    }
                }

                for (int z = 0; z < this.height; ++z) {
                    for (int x = movedX > 0 ? this.width - movedX : 0; x < (movedX > 0 ? this.width : -movedX); ++x) {
                        this.mapChunks[x + z * this.width] = new MapChunk(currentChunk.getPos().x - (middleX - x), currentChunk.getPos().z - (middleZ - z));
                    }
                }
            } else {
                this.fillAllChunks(blockPos);
            }

            this.left = this.mapChunks[0].getX();
            this.top = this.mapChunks[0].getZ();
            this.right = this.mapChunks[this.mapChunks.length - 1].getX();
            this.bottom = this.mapChunks[this.mapChunks.length - 1].getZ();
            this.lastCenterChunk = currentChunk;
        }

    }

    private void fillAllChunks(BlockPos blockPos) {
        ChunkAccess currentChunk = VoxelConstants.getPlayer().level().getChunk(blockPos);
        int middleX = this.width / 2;
        int middleZ = this.height / 2;

        for (int z = 0; z < this.height; ++z) {
            for (int x = 0; x < this.width; ++x) {
                this.mapChunks[x + z * this.width] = new MapChunk(currentChunk.getPos().x - (middleX - x), currentChunk.getPos().z - (middleZ - z));
            }
        }

        this.left = this.mapChunks[0].getX();
        this.top = this.mapChunks[0].getZ();
        this.right = this.mapChunks[this.mapChunks.length - 1].getX();
        this.bottom = this.mapChunks[this.mapChunks.length - 1].getZ();
        this.loaded = true;
    }

    private void moveX(int offset) {
        if (offset > 0) {
            System.arraycopy(this.mapChunks, offset, this.mapChunks, 0, this.mapChunks.length - offset);
        } else if (offset < 0) {
            System.arraycopy(this.mapChunks, 0, this.mapChunks, -offset, this.mapChunks.length + offset);
        }

    }

    private void moveZ(int offset) {
        if (offset > 0) {
            System.arraycopy(this.mapChunks, offset * this.width, this.mapChunks, 0, this.mapChunks.length - offset * this.width);
        } else if (offset < 0) {
            System.arraycopy(this.mapChunks, 0, this.mapChunks, -offset * this.width, this.mapChunks.length + offset * this.width);
        }

    }

    public void checkIfChunksChanged() {
        if (this.loaded) {
            DebugRenderState.chunksChanged = 0;
            DebugRenderState.chunksTotal = 0;
            for (int z = this.height - 1; z >= 0; --z) {
                for (int x = 0; x < this.width; ++x) {
                    DebugRenderState.chunksTotal++;
                    this.mapChunks[x + z * this.width].checkIfChunkChanged(this.changeObserver);
                }
            }

        }
    }

    public void checkIfChunksBecameSurroundedByLoaded() {
        if (this.loaded) {
            for (int z = this.height - 1; z >= 0; --z) {
                for (int x = 0; x < this.width; ++x) {
                    this.mapChunks[x + z * this.width].checkIfChunkBecameSurroundedByLoaded(this.changeObserver);
                }
            }

        }
    }

    public void registerChangeAt(int chunkX, int chunkZ) {
        try {
            if (this.lastCenterChunk != null && chunkX >= this.left && chunkX <= this.right && chunkZ >= this.top && chunkZ <= this.bottom) {
                int arrayX = chunkX - this.left;
                int arrayZ = chunkZ - this.top;
                MapChunk mapChunk = this.mapChunks[arrayX + arrayZ * this.width];
                mapChunk.setModified(true);
            }
        } catch (RuntimeException e) {
            VoxelConstants.getLogger().error(e);
        }
    }

    public boolean isChunkSurroundedByLoaded(int chunkX, int chunkZ) {
        if (this.lastCenterChunk != null && chunkX >= this.left && chunkX <= this.right && chunkZ >= this.top && chunkZ <= this.bottom) {
            int arrayX = chunkX - this.left;
            int arrayZ = chunkZ - this.top;
            MapChunk mapChunk = this.mapChunks[arrayX + arrayZ * this.width];
            return mapChunk.isSurroundedByLoaded();
        } else {
            return false;
        }
    }
}
