package de.fwatermann.dungine.ui.elements;

import de.fwatermann.dungine.event.input.MouseButtonEvent;
import de.fwatermann.dungine.graphics.GLUsageHint;
import de.fwatermann.dungine.graphics.camera.Camera;
import de.fwatermann.dungine.graphics.mesh.ArrayMesh;
import de.fwatermann.dungine.graphics.mesh.DataType;
import de.fwatermann.dungine.graphics.mesh.PrimitiveType;
import de.fwatermann.dungine.graphics.mesh.VertexAttribute;
import de.fwatermann.dungine.graphics.shader.Shader;
import de.fwatermann.dungine.graphics.shader.ShaderProgram;
import de.fwatermann.dungine.resource.Resource;
import de.fwatermann.dungine.ui.IUIClickable;
import de.fwatermann.dungine.ui.IUIHoverable;
import de.fwatermann.dungine.ui.UIContainer;
import de.fwatermann.dungine.utils.functions.IVoidFunction1Parameter;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

public class UIButton extends UIContainer<UIButton> implements IUIClickable, IUIHoverable {

  private static ShaderProgram SHADER;
  private static ArrayMesh MESH;

  private int fillColor = 0x00000000F; // Default: transparent
  private int borderColor = 0xFFFFFFFF; // Default: white
  private float borderWidth = 1.0f; // Default: 1.0f
  private float borderRadius = 0.0f; // Default: 0.0f

  private IVoidFunction1Parameter<UIButton> onClick;
  private IVoidFunction1Parameter<UIButton> onEnter;
  private IVoidFunction1Parameter<UIButton> onLeave;

  private static void initGL() {

    if (SHADER == null) {
      try {
        Shader vertexShader =
            Shader.loadShader(
                Resource.load("/shaders/ui/ButtonColored.vsh"), Shader.ShaderType.VERTEX_SHADER);
        Shader fragmentShader =
            Shader.loadShader(
                Resource.load("/shaders/ui/ButtonColored.fsh"), Shader.ShaderType.FRAGMENT_SHADER);
        SHADER = new ShaderProgram(vertexShader, fragmentShader);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

    if (MESH == null) {
      ByteBuffer vertices = BufferUtils.createByteBuffer(6 * 3 * 4);
      vertices
          .asFloatBuffer()
          .put(
              new float[] {
                0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f
              })
          .flip();

      MESH =
          new ArrayMesh(
              vertices,
              PrimitiveType.TRIANGLES,
              GLUsageHint.DRAW_STATIC,
              new VertexAttribute(3, DataType.FLOAT, "aPosition"));
    }
  }

  @Override
  public void render(Camera<?> camera) {
    initGL();

    MESH.transformation(this.absolutePosition(), this.rotation, this.size);
    SHADER.bind();
    SHADER.setUniform1i("uFillColor", this.fillColor);
    SHADER.setUniform1i("uBorderColor", this.borderColor);
    SHADER.setUniform1f("uBorderWidth", this.borderWidth);
    SHADER.setUniform1f("uBorderRadius", this.borderRadius);
    SHADER.setUniform2f("uSize", this.size.x, this.size.y);
    MESH.render(camera, SHADER);
    SHADER.unbind();
    super.render(camera);
  }

  @Override
  public void click(int button, MouseButtonEvent.MouseButtonAction action) {
    if (action == MouseButtonEvent.MouseButtonAction.PRESS && this.onClick != null) {
      this.onClick.run(this);
    }
  }

  /**
   * Sets the onClick event handler for the UIButton.
   *
   * @param onClick the function to be called when the button is clicked
   * @return this UIButton instance for method chaining
   */
  public UIButton onClick(IVoidFunction1Parameter<UIButton> onClick) {
    this.onClick = onClick;
    return this;
  }

  /**
   * Sets the onEnter event handler for the UIButton.
   *
   * @param onEnter the function to be called when the mouse enters the button area
   * @return this UIButton instance for method chaining
   */
  public UIButton onEnter(IVoidFunction1Parameter<UIButton> onEnter) {
    this.onEnter = onEnter;
    return this;
  }

  /**
   * Sets the onLeave event handler for the UIButton.
   *
   * @param onLeave the function to be called when the mouse leaves the button area
   * @return this UIButton instance for method chaining
   */
  public UIButton onLeave(IVoidFunction1Parameter<UIButton> onLeave) {
    this.onLeave = onLeave;
    return this;
  }

  /**
   * Gets the border radius of the UIButton.
   *
   * @return the border radius
   */
  public float borderRadius() {
    return this.borderRadius;
  }

  /**
   * Sets the border radius of the UIButton.
   *
   * @param borderRadius the new border radius
   * @return this UIButton instance for method chaining
   */
  public UIButton borderRadius(float borderRadius) {
    this.borderRadius = borderRadius;
    return this;
  }

  /**
   * Gets the border width of the UIButton.
   *
   * @return the border width
   */
  public float borderWidth() {
    return this.borderWidth;
  }

  /**
   * Sets the border width of the UIButton.
   *
   * @param borderWidth the new border width
   * @return this UIButton instance for method chaining
   */
  public UIButton borderWidth(float borderWidth) {
    this.borderWidth = borderWidth;
    return this;
  }

  /**
   * Gets the border color of the UIButton.
   *
   * @return the border color in RGBA format
   */
  public int borderColor() {
    return this.borderColor;
  }

  /**
   * Sets the border color of the UIButton.
   *
   * @param borderColor the new border color in RGBA format
   * @return this UIButton instance for method chaining
   */
  public UIButton borderColor(int borderColor) {
    this.borderColor = borderColor;
    return this;
  }

  /**
   * Gets the fill color of the UIButton.
   *
   * @return the fill color in RGBA format
   */
  public int fillColor() {
    return this.fillColor;
  }

  /**
   * Sets the fill color of the UIButton.
   *
   * @param fillColor the new fill color in RGBA format
   * @return this UIButton instance for method chaining
   */
  public UIButton fillColor(int fillColor) {
    this.fillColor = fillColor;
    return this;
  }

  @Override
  public void enter() {
    if (this.onEnter != null) this.onEnter.run(this);
  }

  @Override
  public void leave() {
    if (this.onLeave != null) this.onLeave.run(this);
  }
}
