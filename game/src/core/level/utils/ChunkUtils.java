package core.level.utils;

import core.level.LevelChunk;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class ChunkUtils {

  /**
   * Get the coordinates of the chunk that contains the given position.
   * @param x X coordinate
   * @param y Y coordinate
   * @param z Z coordinate
   * @return Chunk coordinates
   */
  public static Vector3i toChunkCoordinates(int x, int y, int z) {
    int chunkX = (int) Math.floor((float) x / LevelChunk.CHUNK_SIZE_X);
    int chunkY = (int) Math.floor((float) y / LevelChunk.CHUNK_SIZE_Y);
    int chunkZ = (int) Math.floor((float) z / LevelChunk.CHUNK_SIZE_Z);
    return new Vector3i(chunkX, chunkY, chunkZ);
  }

  /**
   * Get the coordinates of the chunk that contains the given position.
   * @param position Position
   * @return Chunk coordinates
   */
  public static Vector3i toChunkCoordinates(Vector3f position) {
    return toChunkCoordinates(position.x, position.y, position.z);
  }

  /**
   * Get the coordinates of the chunk that contains the given position.
   * @param position Position
   * @return Chunk coordinates
   */
  public static Vector3i toChunkCoordinates(Vector3i position) {
    return toChunkCoordinates(position.x, position.y, position.z);
  }

  /**
   * Get the coordinates of the chunk that contains the given position.
   * @param x X coordinate
   * @param y Y coordinate
   * @param z Z coordinate
   * @return Chunk coordinates
   */
  public static Vector3i toChunkCoordinates(float x, float y, float z) {
    return toChunkCoordinates((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
  }

  /**
   * Get the coordinates of the chunk that contains the given position.
   * @param x X coordinate
   * @param y Y coordinate
   * @param z Z coordinate
   * @return Chunk coordinates
   */
  public static Vector3i toInnerChunkCoordinates(int x, int y, int z) {
    int chunkX = (x % LevelChunk.CHUNK_SIZE_X) + (x < 0 ? LevelChunk.CHUNK_SIZE_X : 0);
    int chunkY = (y % LevelChunk.CHUNK_SIZE_Y) + (y < 0 ? LevelChunk.CHUNK_SIZE_Y : 0);
    int chunkZ = (z % LevelChunk.CHUNK_SIZE_Z) + (z < 0 ? LevelChunk.CHUNK_SIZE_Z : 0);
    return new Vector3i(chunkX, chunkY, chunkZ);
  }

  /**
   * Get the coordinates of the chunk that contains the given position.
   * @param x X coordinate
   * @param y Y coordinate
   * @param z Z coordinate
   * @return Chunk coordinates
   */
  public static Vector3i toInnerChunkCoordinates(float x, float y, float z) {
    return toInnerChunkCoordinates((int) x, (int) y, (int) z);
  }

  /**
   * Get the coordinates of the chunk that contains the given position.
   * @param position Position
   * @return Chunk coordinates
   */
  public static Vector3i toInnerChunkCoordinates(Vector3i position) {
    return toInnerChunkCoordinates(position.x, position.y, position.z);
  }

  /**
   * Get the coordinates of the chunk that contains the given position.
   * @param position Position
   * @return Chunk coordinates
   */
  public static Vector3i toInnerChunkCoordinates(Vector3f position) {
    return toInnerChunkCoordinates(position.x, position.y, position.z);
  }



}
