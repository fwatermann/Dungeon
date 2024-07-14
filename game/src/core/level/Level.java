package core.level;

import core.level.tile.Tile;
import core.level.utils.ChunkUtils;
import de.fwatermann.dungine.graphics.camera.CameraPerspective;
import de.fwatermann.dungine.utils.annotations.Null;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Level {

  private final LevelChunkTree chunks = new LevelChunkTree();

  @Null
  public Tile getTileAt(int x, int y, int z) {
    LevelChunk chunk = this.getChunkAtPosition(x, y, z);
    if(chunk == null) return null;
    return chunk.getTileAt(x, y, z);
  }

  public final Tile getTileAt(Vector3i position) {
    return this.getTileAt(position.x, position.y, position.z);
  }

  public final  Tile getTileAt(float x, float y, float z) {
    return this.getTileAt((int) x, (int) y, (int) z);
  }

  public final  Tile getTileAt(Vector3f position) {
    return this.getTileAt(position.x, position.y, position.z);
  }

  public final  LevelChunk getChunkAtPosition(int x, int y, int z) {
    Vector3i chunkCoordinates = ChunkUtils.toChunkCoordinates(x, y, z);
    return this.getChunk(chunkCoordinates.x, chunkCoordinates.y, chunkCoordinates.z);
  }

  public final  LevelChunk getChunk(int chunkX, int chunkY, int chunkZ) {
    return this.chunks.find(chunkX, chunkY, chunkZ);
  }

  public void render(CameraPerspective camera) {
    this.chunks.forEach(chunk -> {
      chunk.render(camera);
    });
  }

}
