package de.fwatermann.dungine.graphics.scene.model;

import de.fwatermann.dungine.graphics.mesh.Mesh;
import de.fwatermann.dungine.graphics.texture.animation.Animation;
import de.fwatermann.dungine.utils.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector4f;

public class Material {

  public static final int MATERIAL_FLAG_HAS_DIFFUSE_TEXTURE = 0x01;
  public static final int MATERIAL_FLAG_HAS_NORMAL_TEXTURE = 0x02;
  public static final int MATERIAL_FLAG_HAS_SPECULAR_TEXTURE = 0x04;
  public static final int MATERIAL_FLAG_HAS_AMBIENT_TEXTURE = 0x08;

  public static final Vector4f DEFAULT_COLOR = new Vector4f(1.0f);

  public final List<MeshEntry> meshes = new ArrayList<>();

  public final Vector4f diffuseColor = new Vector4f(DEFAULT_COLOR);
  public final Vector4f ambientColor = new Vector4f(DEFAULT_COLOR);
  public final Vector4f specularColor = new Vector4f(DEFAULT_COLOR);

  @Nullable
  public Animation diffuseTexture = null;

  @Nullable
  public Animation ambientTexture = null;

  @Nullable
  public Animation normalTexture = null;

  @Nullable
  public Animation specularTexture = null;

  public boolean transparent = false;
  public float reflectance = 0.0f;
  public int flags = 0;

  public record MeshEntry(Mesh<?> mesh, int offset, int count) {}

}