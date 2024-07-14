package core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2i;

import de.fwatermann.dungine.window.GameWindow;

public class DungeonGame extends GameWindow {

  private static Logger LOGGER = LogManager.getLogger();

  private static DungeonGame INSTANCE;

  public static DungeonGame getInstance() {
    if(INSTANCE == null)
      INSTANCE = new DungeonGame();
    return INSTANCE;
  }

  private DungeonGame() {
    super("Dungeon Game", new Vector2i(1280, 720), true, true);
  }

  @Override
  public void init() {
    LOGGER.info("Game init...");
    LOGGER.info("Game init done!");
  }

  @Override
  public void cleanup() {
  }

}
