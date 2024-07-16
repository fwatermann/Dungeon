package core.level;

import core.DungeonGame;
import core.level.block.Block;
import de.fwatermann.dungine.graphics.GLUsageHint;
import de.fwatermann.dungine.graphics.camera.CameraFrustum;
import de.fwatermann.dungine.graphics.camera.CameraPerspective;
import de.fwatermann.dungine.graphics.mesh.InstanceAttribute;
import de.fwatermann.dungine.graphics.mesh.InstanceAttributeList;
import de.fwatermann.dungine.graphics.mesh.InstancedIndexedMesh;
import de.fwatermann.dungine.graphics.mesh.VertexAttribute;
import de.fwatermann.dungine.graphics.mesh.VertexAttributeList;
import de.fwatermann.dungine.graphics.shader.Shader;
import de.fwatermann.dungine.graphics.shader.ShaderProgram;
import de.fwatermann.dungine.utils.ThreadUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.function.Function;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33;

public class LevelChunk {

  private static final ShaderProgram SHADER;

  static {
    ThreadUtils.checkMainThread();
    Shader vertexShader = new Shader("/shaders/level/chunk.vsh", Shader.ShaderType.VERTEX_SHADER);
    Shader fragmentShader =
        new Shader("/shaders/level/chunk.fsh", Shader.ShaderType.FRAGMENT_SHADER);
    SHADER = new ShaderProgram(vertexShader, fragmentShader);
  }

  public static final int CHUNK_SIZE_X = 16;
  public static final int CHUNK_SIZE_Y = 16;
  public static final int CHUNK_SIZE_Z = 16;

  private final int x;
  private final int y;
  private final int z;

  private final Level level;

  private InstancedIndexedMesh chunkMesh;

  private final Block[][][] blocks = new Block[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];

  public LevelChunk(Level level, int x, int y, int z) {
    this.level = level;
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

  public Block getBlockAt(int x, int y, int z) {
    if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
      throw new IllegalArgumentException("Inner chunk tile coordinates out of bounds!");
    }
    return this.blocks[x][y][z];
  }

  public void setBlock(Block tile, int x, int y, int z) {
    if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
      throw new IllegalArgumentException("Inner chunk tile coordinates out of bounds!");
    }
    if(tile == null) {
      this.removeBlock(x, y, z);
      return;
    }
    this.blocks[x][y][z] = tile;
    if (ThreadUtils.isMainThread()) {
      this.updateBlockAt(x, y, z);
    } else {
      DungeonGame.getInstance().runOnMainThread(() -> this.updateBlockAt(x, y, z));
    }
  }

  public void removeBlock(int x, int y, int z) {
    if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
      throw new IllegalArgumentException("Inner chunk tile coordinates out of bounds!");
    }
    this.blocks[x][y][z] = null;
    if (ThreadUtils.isMainThread()) {
      this.updateBlockAt(x, y, z);
    } else {
      DungeonGame.getInstance().runOnMainThread(() -> this.updateBlockAt(x, y, z));
    }
  }

  public void render(CameraPerspective camera) {
    Vector3f min =
        new Vector3f(this.x * CHUNK_SIZE_X, this.y * CHUNK_SIZE_Y, this.z * CHUNK_SIZE_Z);
    Vector3f max = new Vector3f(min).add(CHUNK_SIZE_X, CHUNK_SIZE_Y, CHUNK_SIZE_Z);
    int frustumCheck = camera.frustum().intersectAab(min, max);
    if (frustumCheck != CameraFrustum.INSIDE && frustumCheck != CameraFrustum.INTERSECT) {
      return;
    }
    if (this.chunkMesh == null) {
      this.initMesh();
    }
    SHADER.bind();
    SHADER.setUniform3iv("uChunkSize", CHUNK_SIZE_X, CHUNK_SIZE_Y, CHUNK_SIZE_Z);
    SHADER.setUniform3iv("uChunkPosition", this.x, this.y, this.z);
    this.chunkMesh.render(SHADER, GL33.GL_TRIANGLES, 0, 36, false);
    SHADER.unbind();
  }

  private void initMesh() {
    FloatBuffer vertices = BufferUtils.createFloatBuffer(3 * 4);
    vertices
        .put(
            new float[] {
              0.0f, 1.0f, 0.0f, 0.0f, 0.0f, //Top
              0.0f, 1.0f, 1.0f, 1.0f, 0.0f,
              1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
              1.0f, 1.0f, 0.0f, 0.0f, 1.0f,

              1.0f, 0.0f, 0.0f, 0.0f, 0.0f, //Bottom
              1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
              0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
              0.0f, 0.0f, 0.0f, 0.0f, 1.0f,

              0.0f, 0.0f, 0.0f, 0.0f, 0.0f, //Front
              0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
              0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
              0.0f, 1.0f, 0.0f, 0.0f, 1.0f,

              1.0f, 0.0f, 1.0f, 0.0f, 0.0f, //Back
              1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
              1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
              1.0f, 1.0f, 1.0f, 0.0f, 1.0f,

              1.0f, 0.0f, 0.0f, 0.0f, 0.0f, //Left
              0.0f, 0.0f, 0.0f, 1.0f, 0.0f,
              0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
              1.0f, 1.0f, 0.0f, 0.0f, 1.0f,

              0.0f, 0.0f, 1.0f, 0.0f, 0.0f, //Right
              1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
              1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
              0.0f, 1.0f, 1.0f, 0.0f, 1.0f
            })
        .flip();
    IntBuffer indices = BufferUtils.createIntBuffer(6);
    indices.put(new int[] {
      0,   1,  2,  2,  3,  0,
      4,   5,  6,  6,  7,  4,
      8,   9, 10, 10, 11,  8,
      12, 13, 14, 14, 15, 12,
      16, 17, 18, 18, 19, 16,
      20, 21, 22, 22, 23, 20
    }).flip();

    VertexAttributeList vertexAttributes =
        new VertexAttributeList(
            new VertexAttribute(VertexAttribute.Usage.POSITION, 3, GL33.GL_FLOAT, "a_Position"),
            new VertexAttribute(
                VertexAttribute.Usage.POSITION, 2, GL33.GL_FLOAT, "a_TextureCoords"));
    InstanceAttributeList instanceAttributes =
        new InstanceAttributeList(
            new InstanceAttribute(0, 3, GL33.GL_UNSIGNED_BYTE, "i_TilePosition"),
            new InstanceAttribute(1, 1, GL33.GL_UNSIGNED_SHORT, "i_AtlasEntry"));

    ByteBuffer instanceData0 =
        BufferUtils.createByteBuffer(
            3 * CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z); // 3 Bytes per instance/tile -> 3xuByte
    ByteBuffer instanceData1 =
        BufferUtils.createByteBuffer(
            2
                * CHUNK_SIZE_X
                * CHUNK_SIZE_Y
                * CHUNK_SIZE_Z); // 2 Bytes per instance/tile -> 1xuShort

    this.chunkMesh =
        new InstancedIndexedMesh(
            vertices,
            indices,
            List.of(instanceData0, instanceData1),
            CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z,
            GLUsageHint.DRAW_STATIC,
            vertexAttributes,
            instanceAttributes);

    for (int x = 0; x < CHUNK_SIZE_X; x++) {
      for (int y = 0; y < CHUNK_SIZE_Y; y++) {
        for (int z = 0; z < CHUNK_SIZE_Z; z++) {
          instanceData0.put((byte) x).put((byte) y).put((byte) z);
          this.updateBlockAt(x, y, z);
        }
      }
    }
  }

  /**
   * Updates the tile at the given coordinates in the instance data of the chunk mesh. Call this method
   * after changing anything rendering related of a tile.
   *
   * @param x The x coordinate in chunk of the tile.
   * @param y The y coordinate in chunk of the tile.
   * @param z The z coordinate in chunk of the tile.
   */
  public void updateBlockAt(int x, int y, int z) {
    if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
      throw new IllegalArgumentException("Inner chunk tile coordinates out of bounds!");
    }

    Block tile = this.blocks[x][y][z];
    ByteBuffer instanceData1 = this.chunkMesh.getInstanceData(1);
    int index = (x * CHUNK_SIZE_Y + y) * CHUNK_SIZE_Z + z;
    if (tile == null) {
      instanceData1.position(index * 2).putShort((short) 0).position(0);
      this.chunkMesh.markInstanceDataDirty(1);
      return;
    }
    instanceData1
        .position(index * 2)
        .putShort((short) this.level.textureAtlas.getIndex(tile.currentTexture()))
        .position(0);
    this.chunkMesh.markInstanceDataDirty(1);
  }

  public void forEachBlock(Function<Block, Void> function) {
    for (int x = 0; x < CHUNK_SIZE_X; x++) {
      for (int y = 0; y < CHUNK_SIZE_Y; y++) {
        for (int z = 0; z < CHUNK_SIZE_Z; z++) {
          Block tile = this.blocks[x][y][z];
          if (tile != null) {
            function.apply(tile);
          }
        }
      }
    }
  }
}
