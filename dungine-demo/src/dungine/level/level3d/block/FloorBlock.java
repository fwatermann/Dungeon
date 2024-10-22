package dungine.level.level3d.block;

import de.fwatermann.dungine.resource.Resource;
import dungine.level.level3d.Chunk;
import org.joml.Vector3i;

public class FloorBlock extends Block {

  public FloorBlock(Chunk chunk, Vector3i chunkPosition) {
    super(chunk, chunkPosition, Resource.load("/textures/floor_1.png"));
  }

  @Override
  public boolean isSolid() {
    return true;
  }
}
