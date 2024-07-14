package core.level.tile;

import core.level.LevelChunk;
import org.joml.Vector3i;

public abstract class Tile {

  private LevelChunk chunk;
  private Vector3i chunkPosition;

  public Tile(LevelChunk chunk, Vector3i chunkPosition) {
    this.chunkPosition = chunkPosition;
    this.chunk = chunk;
  }

  /**
   * Defines if the tile is solid. Solid tiles can not be walked on.
   * @return True if the tile is solid, false otherwise.
   */
  public abstract boolean isSolid();

  public LevelChunk chunk() {
    return this.chunk;
  }

  public Vector3i chunkPosition() {
    return this.chunkPosition;
  }

}
