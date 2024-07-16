package core.level.block;

import core.level.LevelChunk;
import de.fwatermann.dungine.resource.Resource;
import org.joml.Vector3i;

public abstract class Block {

  private LevelChunk chunk;
  private Vector3i chunkPosition;
  private Resource currentTexture;

  public Block(LevelChunk chunk, Vector3i chunkPosition) {
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

  public Resource currentTexture() {
    return this.currentTexture;
  }

}
