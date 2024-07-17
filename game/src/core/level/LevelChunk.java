package core.level;

import core.level.block.Block;
import core.level.block.BlockFace;
import de.fwatermann.dungine.graphics.GLUsageHint;
import de.fwatermann.dungine.graphics.camera.CameraFrustum;
import de.fwatermann.dungine.graphics.camera.CameraPerspective;
import de.fwatermann.dungine.graphics.mesh.ArrayMesh;
import de.fwatermann.dungine.graphics.mesh.VertexAttribute;
import de.fwatermann.dungine.graphics.mesh.VertexAttributeList;
import de.fwatermann.dungine.graphics.shader.Shader;
import de.fwatermann.dungine.graphics.shader.ShaderProgram;
import de.fwatermann.dungine.graphics.texture.atlas.TextureAtlas;
import de.fwatermann.dungine.utils.ThreadUtils;
import java.nio.ByteBuffer;
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

  private ArrayMesh chunkMesh;

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
    this.updateBlockAt(x, y, z, false);
  }

  public void removeBlock(int x, int y, int z) {
    if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
      throw new IllegalArgumentException("Inner chunk tile coordinates out of bounds!");
    }
    this.blocks[x][y][z] = null;
    this.updateBlockAt(x, y, z, false);
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
    SHADER.useCamera(camera);
    this.level.textureAtlas.use(SHADER);
    SHADER.setUniform3iv("uChunkSize", CHUNK_SIZE_X, CHUNK_SIZE_Y, CHUNK_SIZE_Z);
    SHADER.setUniform3iv("uChunkPosition", this.x, this.y, this.z);
    this.chunkMesh.render(SHADER, GL33.GL_POINTS, 0, CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z, false);
    SHADER.unbind();
  }

  private void initMesh() {
    VertexAttributeList vertexAttributes =
        new VertexAttributeList(
            new VertexAttribute(VertexAttribute.Usage.POSITION, 3, GL33.GL_UNSIGNED_BYTE, "a_Position"),
            new VertexAttribute(VertexAttribute.Usage.POSITION, 1, GL33.GL_UNSIGNED_BYTE, "a_Faces"),
            new VertexAttribute(VertexAttribute.Usage.POSITION, 3, GL33.GL_UNSIGNED_INT, "a_FaceAtlasEntries"));

    ByteBuffer vertices = BufferUtils.createByteBuffer(16 * CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z);

    for (int x = 0; x < CHUNK_SIZE_X; x++) {
      for (int y = 0; y < CHUNK_SIZE_Y; y++) {
        for (int z = 0; z < CHUNK_SIZE_Z; z++) {
          Block block = this.blocks[x][y][z];
          vertices.put((byte) x).put((byte) y).put((byte) z).put((byte) 0);
          if(block == null) {
            vertices.putInt(0).putInt(0).putInt(0);
            continue;
          }
          TextureAtlas atlas = this.level.textureAtlas;
          vertices.putShort((short) atlas.getIndex(block.getFaceResource(BlockFace.TOP)));
          vertices.putShort((short) atlas.getIndex(block.getFaceResource(BlockFace.BOTTOM)));
          vertices.putShort((short) atlas.getIndex(block.getFaceResource(BlockFace.FRONT)));
          vertices.putShort((short) atlas.getIndex(block.getFaceResource(BlockFace.BACK)));
          vertices.putShort((short) atlas.getIndex(block.getFaceResource(BlockFace.LEFT)));
          vertices.putShort((short) atlas.getIndex(block.getFaceResource(BlockFace.RIGHT)));
        }
      }
    }

    this.chunkMesh = new ArrayMesh(vertices, GLUsageHint.DRAW_STATIC, vertexAttributes);
  }

  /**
   * Updates the tile at the given coordinates in the instance data of the chunk mesh. Call this method
   * after changing anything rendering related of a tile.
   *
   * @param x The x coordinate in chunk of the tile.
   * @param y The y coordinate in chunk of the tile.
   * @param z The z coordinate in chunk of the tile.
   * @param updateNeighbours If true, the neighbouring blocks will be updated as well.
   */
  public void updateBlockAt(int x, int y, int z, boolean updateNeighbours) {
    if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
      throw new IllegalArgumentException("Inner chunk tile coordinates out of bounds!");
    }

    Block block = this.blocks[x][y][z];
    if (block == null) {
      if(this.chunkMesh == null) return;
      int index = ((x * CHUNK_SIZE_Y + y) * CHUNK_SIZE_Z + z) * 16;
      this.chunkMesh.getVertexBuffer().position(index + 3).put((byte) 0x00);
      this.chunkMesh.markVerticesDirty();
      return;
    }

    byte faces = 0;

    Block top;
    if(y < CHUNK_SIZE_Y - 1) top = this.blocks[x][y + 1][z];
    else top = this.level.getChunk(this.x, this.y + 1, this.z).getBlockAt(x, 0, z);
    if(top == null || !top.isSolid()) faces |= BlockFace.TOP.bitMask();

    Block bottom;
    if(y > 0) bottom = this.blocks[x][y - 1][z];
    else bottom = this.level.getChunk(this.x, this.y - 1, this.z).getBlockAt(x, CHUNK_SIZE_Y - 1, z);
    if(bottom == null || !bottom.isSolid()) faces |= BlockFace.BOTTOM.bitMask();

    Block front;
    if(x > 0) front = this.blocks[x - 1][y][z];
    else front = this.level.getChunk(this.x - 1, this.y, this.z).getBlockAt(CHUNK_SIZE_X - 1, y, z);
    if(front == null || !front.isSolid()) faces |= BlockFace.FRONT.bitMask();

    Block back;
    if(x < CHUNK_SIZE_X - 1) back = this.blocks[x + 1][y][z];
    else back = this.level.getChunk(this.x + 1, this.y, this.z).getBlockAt(0, y, z);
    if(back == null || !back.isSolid()) faces |= BlockFace.BACK.bitMask();

    Block left;
    if(z > 0) left = this.blocks[x][y][z - 1];
    else left = this.level.getChunk(this.x, this.y, this.z - 1).getBlockAt(x, y, CHUNK_SIZE_Z - 1);
    if(left == null || !left.isSolid()) faces |= BlockFace.LEFT.bitMask();

    Block right;
    if(z < CHUNK_SIZE_Z - 1) right = this.blocks[x][y][z + 1];
    else right = this.level.getChunk(this.x, this.y, this.z + 1).getBlockAt(x, y, 0);
    if(right == null || !right.isSolid()) faces |= BlockFace.RIGHT.bitMask();

    int index = ((x * CHUNK_SIZE_Y + y) * CHUNK_SIZE_Z + z) * 16;
    TextureAtlas atlas = this.level.textureAtlas;
    this.chunkMesh.getVertexBuffer().position(index + 3)
      .put(faces)
      .putShort((short)atlas.getIndex(block.getFaceResource(BlockFace.TOP)))
      .putShort((short)atlas.getIndex(block.getFaceResource(BlockFace.BOTTOM)))
      .putShort((short)atlas.getIndex(block.getFaceResource(BlockFace.FRONT)))
      .putShort((short)atlas.getIndex(block.getFaceResource(BlockFace.BACK)))
      .putShort((short)atlas.getIndex(block.getFaceResource(BlockFace.LEFT)))
      .putShort((short)atlas.getIndex(block.getFaceResource(BlockFace.RIGHT)))
      .position(0);
    this.chunkMesh.markVerticesDirty();

    if(updateNeighbours) {
      if(top != null) top.update(false);
      if(bottom != null) bottom.update(false);
      if(front != null) front.update(false);
      if(back != null) back.update(false);
      if(left != null) left.update(false);
      if(right != null) right.update(false);
    }
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
