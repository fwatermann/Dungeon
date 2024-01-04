package core.level.elements.tile;

import core.level.Tile;
import core.level.utils.Coordinate;
import core.level.utils.DesignLabel;
import core.level.utils.LevelElement;
import core.utils.components.path.IPath;

/** Represents an empty void in the dungeon. */
public class SkipTile extends Tile {

  /**
   * Creates a new Tile.
   *
   * @param texturePath Path to the texture of the tile.
   * @param globalPosition Position of the tile in the global system.
   * @param designLabel Design of the Tile
   */
  public SkipTile(
      final IPath texturePath, final Coordinate globalPosition, final DesignLabel designLabel) {
    super(texturePath, globalPosition, designLabel);
    levelElement = LevelElement.SKIP;
  }
}
