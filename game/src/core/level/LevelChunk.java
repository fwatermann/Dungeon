package core.level;

import core.level.tile.Tile;
import de.fwatermann.dungine.graphics.camera.CameraFrustum;
import de.fwatermann.dungine.graphics.camera.CameraPerspective;
import de.fwatermann.dungine.graphics.mesh.InstancedArrayMesh;
import de.fwatermann.dungine.graphics.shader.Shader;
import de.fwatermann.dungine.graphics.shader.ShaderProgram;
import de.fwatermann.dungine.utils.ThreadUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33;

public class LevelChunk {

  private static final ShaderProgram SHADER;

  static {
    ThreadUtils.checkMainThread();
    Shader vertexShader = new Shader("/shaders/chunk.vsh", Shader.ShaderType.VERTEX_SHADER);
    Shader fragmentShader = new Shader("/shaders/chunk.fsh", Shader.ShaderType.FRAGMENT_SHADER);
    SHADER = new ShaderProgram(vertexShader, fragmentShader);
  }

  public static final int CHUNK_SIZE_X = 16;
  public static final int CHUNK_SIZE_Y = 16;
  public static final int CHUNK_SIZE_Z = 16;

  private final int x;
  private final int y;
  private final int z;

  private InstancedArrayMesh chunkMesh;

  private final Tile[][][] tiles = new Tile[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];

  public LevelChunk(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public int x() {
    return this.x;
  }

  public int y() {
    return this.y;
  }

  public int z() {
    return this.z;
  }

  public Tile getTileAt(int x, int y, int z) {
    if(x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
      throw new IllegalArgumentException("Inner chunk tile coordinates out of bounds!");
    }
    return this.tiles[x][y][z];
  }

  public void setTile() {

  }

  public void removeTile() {

  }

  public void update() {
    this.updateMesh();
  }

  public void render(CameraPerspective camera) {
    Vector3f min = new Vector3f(this.x * CHUNK_SIZE_X, this.y * CHUNK_SIZE_Y, this.z * CHUNK_SIZE_Z);
    Vector3f max = new Vector3f(min).add(CHUNK_SIZE_X, CHUNK_SIZE_Y, CHUNK_SIZE_Z);
    int frustumCheck = camera.frustum().intersectAab(min, max);
    if(frustumCheck != CameraFrustum.INSIDE && frustumCheck != CameraFrustum.INTERSECT) {
      return;
    }
    if(this.chunkMesh != null) {
      SHADER.bind();
      SHADER.setUniform3iv("u_ChunkSize", CHUNK_SIZE_X, CHUNK_SIZE_Y, CHUNK_SIZE_Z);
      SHADER.setUniform3iv("u_ChunkPosition", this.x, this.y, this.z);
      this.chunkMesh.render(SHADER, GL33.GL_TRIANGLES);
      }
  }

  private void updateMesh() {
    List<Tile> tiles = new ArrayList<>();
    this.forEachTile(tile -> {
      tiles.add(tile);
      return null;
    });

    //

    ByteBuffer instanceData = BufferUtils.createByteBuffer(3 * 4 * tiles.size());
    tiles.forEach(tile -> {
      int x = tile.chunkPosition().x;
      int y = tile.chunkPosition().y;
      int z = tile.chunkPosition().z;
      int textureX = 0;
      int textureY = 0;
      int textureW = 0;
      int textureH = 0;

      int int_1 = (tile.chunkPosition().x << 24) | (tile.chunkPosition().y << 16) | (tile.chunkPosition().z << 8) | 0xFF;
      int int_2 = (textureX << 16) | (textureY & 0xFFFF);
      int int_3 = (textureW << 16) | (textureH & 0xFFFF);

      instanceData.putInt(int_1);
      instanceData.putInt(int_2);
      instanceData.putInt(int_3);


    });

  }

  public void forEachTile(Function<Tile, Void> function) {
    for(int x = 0; x < CHUNK_SIZE_X; x++) {
      for(int y = 0; y < CHUNK_SIZE_Y; y++) {
        for(int z = 0; z < CHUNK_SIZE_Z; z++) {
          Tile tile = this.tiles[x][y][z];
          if(tile != null) {
            function.apply(tile);
          }
        }
      }
    }
  }

}
