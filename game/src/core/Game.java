package core;

import core.game.ECSManagment;
import core.game.GameLoop;
import core.game.PreRunConfiguration;
import core.level.Level;
import core.systems.LevelSystem;
import core.utils.IVoidFunction;
import core.utils.components.path.IPath;
import de.fwatermann.dungine.window.GameWindow;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2i;

/**
 * The Center-Point of the framework.
 *
 * <p>This class is basically an API-Class, it will forward the request to the responsible classes.
 *
 * <p>For Entity management use: {@link #add(Entity)}, {@link #remove(Entity)} or {@link
 * #removeAllEntities()}
 *
 * <p>Use {@link #userOnFrame(IVoidFunction)}, {@link #userOnSetup(IVoidFunction)}, and {@link
 * #userOnLevelLoad(Consumer)} to configure event callbacks. This is the best way to include your
 * own program logic outside a {@link java.lang.System}.
 *
 * <p>For System management use: {@link #add(System)}, {@link #remove(Class)} or {@link
 * #removeAllSystems()}
 *
 * <p>Get access via: {@link #entityStream()}, {@link #systems()}
 *
 * @see PreRunConfiguration
 * @see ECSManagment
 * @see GameLoop
 */
public final class Game extends GameWindow {

  private static final Logger LOGGER = LogManager.getLogger(Game.class);

  private GameLoop gameLoop;

  /** Constructs a new Game. */
  public Game() {
    super(
        "Dungeon",
        new Vector2i(PreRunConfiguration.windowWidth(), PreRunConfiguration.windowHeight()),
        true,
        false);
  }

  @Override
  public void init() {
    this.gameLoop = new GameLoop(this);
    this.setState(this.gameLoop);
  }

  @Override
  public void cleanup() {
    this.gameLoop.dispose();
  }

  /**
   * Loads the configuration from the given path. If the configuration has already been loaded, the
   * cached version will be used.
   *
   * @param path The path to the config file.
   * @param keyboardConfigClass The class where the ConfigKey fields are located.
   * @throws IOException If the file could not be read.
   */
  public static void loadConfig(final IPath path, final Class<?>... keyboardConfigClass)
      throws IOException {
    PreRunConfiguration.loadConfig(path, keyboardConfigClass);
  }

  /**
   * The given entity will be added to the game.
   *
   * <p>For each {@link System}, it will be checked if the {@link System} will process this entity.
   *
   * <p>If necessary, the {@link System} will trigger {@link System#triggerOnAdd(Entity)} .
   *
   * @param entity the entity to add.
   */
  public static void add(final Entity entity) {
    ECSManagment.add(entity);
  }

  /**
   * The given entity will be removed from the game.
   *
   * <p>If necessary, the {@link System}s will trigger {@link System#triggerOnAdd(Entity)} .
   *
   * @param entity the entity to remove
   */
  public static void remove(final Entity entity) {
    ECSManagment.remove(entity);
  }

  /**
   * Add a {@link System} to the game.
   *
   * <p>If a System is added to the game, the {@link System#execute} method will be called every
   * frame.
   *
   * <p>Additionally, the system will be informed about all new, changed, and removed entities.
   *
   * <p>The game can only store one system of each system type.
   *
   * @param system the System to add
   * @return an optional that contains the previous existing system of the given system class, if
   *     one exists
   * @see System
   */
  public static Optional<System> add(final System system) {
    return ECSManagment.add(system);
  }

  /**
   * Get all registered systems.
   *
   * @return a copy of the map that stores all registered {@link System} in the game.
   */
  public static Map<Class<? extends System>, System> systems() {
    return ECSManagment.systems();
  }

  /**
   * Remove all registered systems from the game.
   *
   * <p>Will trigger {@link System#onEntityRemove} for each entity in each system.
   */
  public static void removeAllSystems() {
    ECSManagment.removeAllSystems();
  }

  /**
   * Use this stream if you want to iterate over all currently active entities.
   *
   * @return a stream of all entities currently in the game
   */
  public static Stream<Entity> entityStream() {
    return ECSManagment.entityStream();
  }

  /**
   * Use this stream if you want to iterate over all entities that contain the necessary Components
   * to be processed by the given system.
   *
   * @param system the system to check.
   * @return a stream of all entities currently in the game that should be processed by the given
   *     system.
   */
  public static Stream<Entity> entityStream(final System system) {
    return ECSManagment.entityStream(system);
  }

  /**
   * Use this stream if you want to iterate over all entities that contain the given components.
   *
   * @param filter the components to check.
   * @return a stream of all entities currently in the game that contains the given components.
   */
  public static Stream<Entity> entityStream(final Set<Class<? extends Component>> filter) {
    return ECSManagment.entityStream(filter);
  }

  /**
   * Get the player character.
   *
   * @return the player character, can be null if not initialized
   */
  public static Optional<Entity> hero() {
    return ECSManagment.hero();
  }

  /**
   * Remove the stored system of the given class from the game. If the System is successfully
   * removed, the {@link System#triggerOnRemove(Entity)} method of the System will be called for
   * each existing Entity that was associated with the removed System.
   *
   * @param system the class of the system to remove
   */
  public static void remove(final Class<? extends System> system) {
    ECSManagment.remove(system);
  }

  /**
   * Remove all entities from the game.
   *
   * <p>This will also remove all entities from each system.
   */
  public static void removeAllEntities() {
    ECSManagment.removeAllEntities();
  }

  /**
   * Use this stream if you want to iterate over all active entities.
   *
   * <p>Use {@link #entityStream()} if you want to iterate over all active entities.
   *
   * @return a stream of all entities currently in the game
   */
  public static Stream<Entity> allEntities() {
    return ECSManagment.allEntities();
  }

  /**
   * Find the entity that contains the given component instance.
   *
   * @param component Component instance where the entity is searched for.
   * @return An Optional containing the found Entity, or an empty Optional if not found.
   */
  public static Optional<Entity> find(final Component component) {
    return ECSManagment.find(component);
  }

  /**
   * Get the current level.
   *
   * @return the currently loaded level
   */
  public static Level currentLevel() {
    return LevelSystem.level();
  }

  /**
   * Set the current level.
   *
   * <p>This method is for testing and debugging purposes.
   *
   * @param level New level
   */
  public static void currentLevel(final Level level) {
    LevelSystem levelSystem = (LevelSystem) ECSManagment.systems().get(LevelSystem.class);
    if (levelSystem != null) levelSystem.loadLevel(level);
    else LOGGER.warn("Can not set Level because levelSystem is null.");
  }
}
