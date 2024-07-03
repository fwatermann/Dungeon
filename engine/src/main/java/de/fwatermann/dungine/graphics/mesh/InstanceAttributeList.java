package de.fwatermann.dungine.graphics.mesh;

import de.fwatermann.dungine.graphics.shader.ShaderProgram;
import de.fwatermann.dungine.utils.ReadOnlyIterator;
import java.util.Iterator;
import org.lwjgl.opengl.GL33;

/**
 * The InstanceAttributeList class represents a list of InstanceAttributes. This class implements the
 * Iterable interface, allowing it to be used in enhanced for loops.
 */
public class InstanceAttributeList implements Iterable<InstanceAttribute> {

  /** An array of InstanceAttributes in the list. */
  private final InstanceAttribute[] attributes;

  /** An iterator for the InstanceAttributes in the list. */
  private Iterator<InstanceAttribute> iterator;

  /** The size of the list in bytes. */
  private int sizeInBytes;

  /**
   * Constructs a InstanceAttributeList with the specified InstanceAttributes.
   *
   * @param attributes the InstanceAttributes to be included in the list
   */
  public InstanceAttributeList(InstanceAttribute... attributes) {
    this.attributes = attributes;
    this.calculateOffsets();
  }

  /** Calculates the offsets of the InstanceAttributes in the list. */
  private void calculateOffsets() {
    int offset = 0;
    for (InstanceAttribute attribute : this.attributes) {
      attribute.offset = offset;
      offset += attribute.getSizeInBytes();
    }
    this.sizeInBytes = offset;
  }

  /**
   * Returns the size of the list in bytes.
   *
   * @return the size of the list in bytes
   */
  public int sizeInBytes() {
    return this.sizeInBytes;
  }

  /**
   * Returns the InstanceAttribute at the specified index in the list.
   *
   * @param index the index of the InstanceAttribute to return
   * @return the InstanceAttribute at the specified index in the list
   */
  public InstanceAttribute get(int index) {
    return this.attributes[index];
  }

  /**
   * Returns the count of InstanceAttributes in the list.
   *
   * @return the count of InstanceAttributes in the list
   */
  public int count() {
    return this.attributes.length;
  }

  /**
   * Returns an iterator over the InstanceAttributes in the list.
   *
   * @return an iterator over the InstanceAttributes in the list
   */
  @Override
  public Iterator<InstanceAttribute> iterator() {
    if (this.iterator == null) {
      this.iterator = new ReadOnlyIterator<>(this.attributes);
    }
    return this.iterator;
  }

  /**
   * Binds the attribute pointers of the InstanceAttributes in the list to the specified shader
   * program, vertex array object, and instance data buffer object.
   *
   * @param shaderProgram the shader program to bind the attribute pointers to
   * @param vao the vertex array object to bind the attribute pointers to
   * @param ibo the instance data buffer object to bind the attribute pointers to
   */
  public void bindAttribPointers(ShaderProgram shaderProgram, int vao, int ibo) {
    GL33.glBindVertexArray(vao);
    GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, ibo);
    this.forEach(
      attrib -> {
        int loc = shaderProgram.getAttributeLocation(attrib.name);
        if (loc != -1) {
          int remaining = attrib.numComponents;
          while (remaining > 0) {
            GL33.glEnableVertexAttribArray(loc);
            GL33.glVertexAttribDivisor(loc, 1);
            GL33.glVertexAttribPointer(
              loc,
              Math.min(remaining, 4),
              attrib.glType,
              false,
              this.sizeInBytes(),
              attrib.offset
                + (long) (attrib.numComponents - remaining) * attrib.getSizeInBytes());
            remaining -= Math.min(remaining, 4);
            loc++;
          }
        }
      });
    GL33.glBindVertexArray(0);
    GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
  }

}
