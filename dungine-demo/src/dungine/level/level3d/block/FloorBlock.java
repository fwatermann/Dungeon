package dungine.level.level3d.block;

import de.fwatermann.dungine.resource.Resource;
import dungine.level.level3d.Chunk;
import org.joml.Vector3i;

public abstract class FloorBlock extends Block {

  public FloorBlock(Chunk chunk, Vector3i chunkPosition, Resource resource) {
    super(chunk, chunkPosition, resource);
  }

  @Override
  public boolean isSolid() {
    return true;
  }
}
