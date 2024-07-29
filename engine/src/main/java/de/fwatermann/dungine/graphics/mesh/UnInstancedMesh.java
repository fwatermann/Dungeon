package de.fwatermann.dungine.graphics.mesh;

import de.fwatermann.dungine.graphics.GLUsageHint;
import de.fwatermann.dungine.utils.BoundingBox;
import de.fwatermann.dungine.utils.annotations.Nullable;
import java.nio.ByteBuffer;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Represents a mesh that is not instanced, providing basic transformation capabilities such as
 * translation, rotation, and scaling. This abstract class serves as a foundation for meshes that
 * are manipulated individually rather than as part of an instanced group.
 */
public abstract class UnInstancedMesh<T extends UnInstancedMesh<?>>
    extends Mesh<UnInstancedMesh<T>> {

  protected Vector3f translation = new Vector3f(0, 0, 0);
  protected Vector3f scale = new Vector3f(1, 1, 1);
  protected Quaternionf rotation = new Quaternionf();
  protected Matrix4f transformMatrix = new Matrix4f();

  @Nullable protected BoundingBox boundingBox;

  /**
   * Constructs a new UnInstancedMesh with the specified usage hint and attributes.
   *
   * @param vertices the vertices of the mesh
   * @param usageHint the usage hint of the mesh
   * @param attributes the attributes of the mesh
   */
  protected UnInstancedMesh(
      ByteBuffer vertices,
      PrimitiveType primitiveType,
      GLUsageHint usageHint,
      VertexAttributeList attributes) {
    super(vertices, primitiveType, usageHint, attributes);
    this.calcTransformMatrix();
    // this.calcBoundingBox(); //TODO: Fix Buffer Undeflow
  }

  /**
   * Constructs a new UnInstancedMesh with the specified usage hint and attributes.
   *
   * @param vertices the vertices of the mesh
   * @param usageHint the usage hint of the mesh
   * @param attributes the attributes of the mesh
   */
  protected UnInstancedMesh(
      ByteBuffer vertices,
      PrimitiveType primitiveType,
      GLUsageHint usageHint,
      VertexAttribute... attributes) {
    this(vertices, primitiveType, usageHint, new VertexAttributeList(attributes));
  }

  /**
   * Returns the bounding box of the mesh.
   *
   * @return the bounding box of the mesh
   */
  @Nullable
  public BoundingBox getBoundingBox() {
    return this.boundingBox;
  }

  /** Calculates the bounding box of the mesh. */
  private void calcBoundingBox() {
    float minX = Float.MAX_VALUE;
    float minY = Float.MAX_VALUE;
    float minZ = Float.MAX_VALUE;
    float maxX = Float.MIN_VALUE;
    float maxY = Float.MIN_VALUE;
    float maxZ = Float.MIN_VALUE;
    this.vertices.position(0);
    int vertexCount = this.vertices.capacity() / this.attributes.sizeInBytes();
    for (int i = 0; i < vertexCount; i++) {
      float x = this.vertices.get();
      float y = this.vertices.get();
      float z = this.vertices.get();
      this.vertices.position(
          this.vertices.position() + this.attributes.sizeInBytes() - 3 * Float.BYTES);
      minX = Math.min(minX, x);
      minY = Math.min(minY, y);
      minZ = Math.min(minZ, z);
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
      maxZ = Math.max(maxZ, z);
    }
    this.boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
  }

  /**
   * Sets the transformation of the mesh to the specified translation, rotation, and scale.
   *
   * <p>If any of the parameters are null, the corresponding transformation will not be changed.
   *
   * <p>This method only calculates the transformation matrix only once after all parameters are
   * set.
   *
   * @param translation the translation of the mesh
   * @param rotation the rotation of the mesh
   * @param scale the scale of the mesh
   * @return the mesh
   */
  public T transformation(Vector3f translation, Quaternionf rotation, Vector3f scale) {
    if (translation != null) this.translation.set(translation);
    if (rotation != null) this.rotation.set(rotation);
    if (scale != null) this.scale.set(scale);

    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Translates the mesh by the specified amounts along the x, y, and z axes.
   *
   * @param x the amount to translate along the x axis
   * @param y the amount to translate along the y axis
   * @param z the amount to translate along the z axis
   */
  public T translate(float x, float y, float z) {
    this.translation.add(x, y, z);
    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Sets the position of the mesh to the specified coordinates.
   *
   * @param x the x coordinate of the new position
   * @param y the y coordinate of the new position
   * @param z the z coordinate of the new position
   */
  public T translation(float x, float y, float z) {
    this.translation.set(x, y, z);
    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Rotates the mesh around the specified axis by the specified angle.
   *
   * @param x the x coordinate of the axis of rotation
   * @param y the y coordinate of the axis of rotation
   * @param z the z coordinate of the axis of rotation
   * @param angle the angle of rotation, in degrees
   */
  public T rotate(float x, float y, float z, float angle) {
    this.rotation.rotateAxis(Math.toRadians(angle), x, y, z);
    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Scales the mesh by the specified factors along the x, y, and z axes.
   *
   * @param x the scaling factor along the x axis
   * @param y the scaling factor along the y axis
   * @param z the scaling factor along the z axis
   */
  public T scale(float x, float y, float z) {
    this.scale.mul(x, y, z);
    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Sets the scale of the mesh to the specified values.
   *
   * @param x the new scale along the x axis
   * @param y the new scale along the y axis
   * @param z the new scale along the z axis
   */
  public T setScale(float x, float y, float z) {
    this.scale.set(x, y, z);
    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Translates the mesh by the specified vector.
   *
   * @param translation the vector by which to translate the mesh
   */
  public T translate(Vector3f translation) {
    this.translate(translation.x, translation.y, translation.z);
    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Sets the position of the mesh to the specified vector.
   *
   * @param translation the new position of the mesh
   */
  public T translation(Vector3f translation) {
    this.translation(translation.x, translation.y, translation.z);
    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Rotates the mesh around the specified axis by the specified angle.
   *
   * @param axis the axis of rotation
   * @param angle the angle of rotation, in degrees
   */
  public T rotate(Vector3f axis, float angle) {
    this.rotate(axis.x, axis.y, axis.z, angle);
    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Scales the mesh by the specified vector.
   *
   * @param scale the scaling factors along the x, y, and z axes
   */
  public T scale(Vector3f scale) {
    this.scale(scale.x, scale.y, scale.z);
    this.calcTransformMatrix();
    return (T) this;
  }

  /**
   * Sets the scale of the mesh to the specified vector.
   *
   * @param scale the new scale along the x, y, and z axes
   */
  public T setScale(Vector3f scale) {
    return this.setScale(scale.x, scale.y, scale.z);
  }

  /**
   * Sets the transformation matrix of the mesh to the specified matrix.
   *
   * @param matrix the new transformation matrix of the mesh
   */
  public T transformMatrix(Matrix4f matrix) {
    this.transformMatrix = matrix;
    return (T) this;
  }

  /**
   * Returns the transformation matrix of the mesh.
   *
   * @return the transformation matrix of the mesh
   */
  public Matrix4f transformMatrix() {
    return this.transformMatrix;
  }

  /**
   * Returns the translation of the mesh.
   *
   * @return the translation of the mesh
   */
  public Vector3f translation() {
    return new Vector3f(this.translation);
  }

  /**
   * Returns the scale of the mesh.
   *
   * @return the scale of the mesh
   */
  public Vector3f scale() {
    return new Vector3f(this.scale);
  }

  /**
   * Returns the rotation of the mesh.
   *
   * @return the rotation of the mesh
   */
  public Quaternionf rotation() {
    return new Quaternionf(this.rotation);
  }

  /**
   * Calculates the transformation matrix of the mesh. This method is abstract and must be
   * implemented by subclasses to define how the transformation matrix is updated. The
   * transformation matrix is typically used to apply transformations such as translation, rotation,
   * and scaling to the mesh.
   */
  private void calcTransformMatrix() {
    this.transformMatrix =
        new Matrix4f().translationRotateScale(this.translation, this.rotation, this.scale);
  }
}
