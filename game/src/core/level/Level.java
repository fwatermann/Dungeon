package core.level;

import core.level.block.Block;
import core.level.generator.IGenerator;
import core.level.utils.ChunkUtils;
import de.fwatermann.dungine.graphics.camera.CameraPerspective;
import de.fwatermann.dungine.graphics.texture.atlas.TextureAtlas;
import de.fwatermann.dungine.utils.annotations.Null;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Level {

  private final LevelChunkTree chunks = new LevelChunkTree();
  protected TextureAtlas textureAtlas;
  protected IGenerator generator;

  public Level() {
    this.textureAtlas = new TextureAtlas();
  }

  @Null
  public Block getTileAt(int x, int y, int z) {
    LevelChunk chunk = this.getChunkAtPosition(x, y, z);
    if(chunk == null) return null;
    return chunk.getBlockAt(x, y, z);
  }

  public final Block getTileAt(Vector3i position) {
    return this.getTileAt(position.x, position.y, position.z);
  }

  public final Block getTileAt(float x, float y, float z) {
    return this.getTileAt((int) x, (int) y, (int) z);
  }

  public final Block getTileAt(Vector3f position) {
    return this.getTileAt(position.x, position.y, position.z);
  }

  public final  LevelChunk getChunkAtPosition(int x, int y, int z) {
    Vector3i chunkCoordinates = ChunkUtils.toChunkCoordinates(x, y, z);
    return this.getChunk(chunkCoordinates.x, chunkCoordinates.y, chunkCoordinates.z);
  }

  public final  LevelChunk getChunk(int chunkX, int chunkY, int chunkZ) {
    LevelChunk chunk = this.chunks.find(chunkX, chunkY, chunkZ);
    if(chunk == null) {
      chunk = this.generator.generateChunk(chunkX, chunkY, chunkZ);
      this.chunks.insert(chunk);
    }
    return chunk;
  }

  public void render(CameraPerspective camera) {
    this.chunks.forEach(chunk -> {
      chunk.render(camera);
    });
  }

}
