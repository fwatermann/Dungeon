package de.fwatermann.dungine.graphics.simple;

import de.fwatermann.dungine.graphics.GLUsageHint;
import de.fwatermann.dungine.graphics.Renderable;
import de.fwatermann.dungine.graphics.camera.Camera;
import de.fwatermann.dungine.graphics.mesh.DataType;
import de.fwatermann.dungine.graphics.mesh.IndexDataType;
import de.fwatermann.dungine.graphics.mesh.IndexedMesh;
import de.fwatermann.dungine.graphics.mesh.PrimitiveType;
import de.fwatermann.dungine.graphics.mesh.VertexAttribute;
import de.fwatermann.dungine.graphics.shader.Shader;
import de.fwatermann.dungine.graphics.shader.ShaderProgram;
import java.nio.ByteBuffer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class Cube extends Renderable<Cube> {

  private static ShaderProgram SHADER;
  protected IndexedMesh mesh;

  /**
   * Constructs a new Cube with the specified position and color.
   *
   * @param position the position of the cube
   */
  public Cube(Vector3f position) {
    super(position, new Vector3f(1.0f), new Quaternionf());
    this.initMesh();
  }

  private void initMesh() {
    ByteBuffer vertices = BufferUtils.createByteBuffer(8 * 3 * 4);
    vertices
        .asFloatBuffer()
        .position(0)
        .put(
            new float[] {
              0.0f, 0.0f, 0.0f, //0
              0.0f, 0.0f, 1.0f, //1
              0.0f, 1.0f, 1.0f, //2
              0.0f, 1.0f, 0.0f, //3
              1.0f, 0.0f, 0.0f, //4
              1.0f, 0.0f, 1.0f, //5
              1.0f, 1.0f, 1.0f, //6
              1.0f, 1.0f, 0.0f  //7
            });
    ByteBuffer indices = BufferUtils.createByteBuffer(36 * 4);
    indices
        .asIntBuffer()
        .position(0)
        .put(
            new int[] {
              3, 2, 6, 6, 7, 3, // TOP
              4, 5, 1, 1, 0, 4, // BOTTOM
              0, 1, 2, 2, 3, 0, // FRONT
              5, 4, 7, 7, 6, 5, // BACK
              4, 0, 3, 3, 7, 4, // LEFT
              1, 5, 6, 6, 2, 1  // RIGHT
            });
    this.mesh =
        new IndexedMesh(
            vertices,
            PrimitiveType.TRIANGLES,
            indices,
            IndexDataType.UNSIGNED_INT,
            GLUsageHint.DRAW_STATIC,
            new VertexAttribute(3, DataType.FLOAT, "a_Position"));
  }

  @Override
  public void render(Camera<?> camera, ShaderProgram shader) {
    if (shader == null) {
      return;
    }
    shader.bind();
    this.mesh.transformation(this.position(), this.rotation(), this.scaling());
    this.mesh.render(camera, shader);
    shader.unbind();
  }

  @Override
  public void render(Camera<?> camera) {
    if (SHADER == null) {
      Shader vertexShader = new Shader(VERTEX_SHADER, Shader.ShaderType.VERTEX_SHADER);
      Shader fragmentShader = new Shader(FRAGMENT_SHADER, Shader.ShaderType.FRAGMENT_SHADER);
      SHADER = new ShaderProgram(vertexShader, fragmentShader);
    }
    this.render(camera, SHADER);
  }

  private static final String VERTEX_SHADER =
      """
#version 330 core

in vec3 a_Position;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

void main() {
    gl_Position = uProjection * uView * uModel * vec4(a_Position, 1.0);
}

""";

  private static final String FRAGMENT_SHADER =
      """
#version 330 core

out vec4 fragColor;

void main() {
   fragColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
}
""";
}
