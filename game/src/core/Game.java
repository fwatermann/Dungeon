package core;

import com.badlogic.gdx.ai.pfa.GraphPath;
import core.components.PositionComponent;
import core.game.ECSManagment;
import core.game.GameLoop;
import core.game.PreRunConfiguration;
import core.level.Tile;
import core.level.elements.ILevel;
import core.level.utils.Coordinate;
import core.level.utils.LevelElement;
import core.level.utils.LevelSize;
import core.systems.LevelSystem;
import core.utils.IVoidFunction;
import core.utils.Point;
import core.utils.components.MissingComponentException;
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
  public static ILevel currentLevel() {
    return LevelSystem.level();
  }

  /**
   * Get the tile at the given point in the level.
   *
   * <p>{@link Point#toCoordinate} will be used, to convert the point into a coordinate.
   *
   * @param point Point from where to get the tile
   * @return the tile at the given point.
   */
  public static Tile tileAT(final Point point) {
    return currentLevel().tileAt(point);
  }

  /**
   * Get the tile at the given coordinate in the level.
   *
   * @param coordinate Coordinate from where to get the tile
   * @return the tile at the given coordinate.
   */
  public static Tile tileAT(final Coordinate coordinate) {
    return currentLevel().tileAt(coordinate);
  }

  /**
   * Get a random tile in the level.
   *
   * @return a random Tile in the Level
   */
  public static Tile randomTile() {
    return currentLevel().randomTile();
  }

  /**
   * Get the end tile.
   *
   * @return The end tile.
   */
  public static Tile endTile() {
    return currentLevel().endTile();
  }

  /**
   * Get the start tile.
   *
   * @return The start tile.
   */
  public static Tile startTile() {
    return currentLevel().startTile();
  }

  /**
   * Returns the tile the given entity is standing on.
   *
   * @param entity entity to check for.
   * @return tile at the coordinate of the entity
   */
  public static Tile tileAtEntity(final Entity entity) {
    return currentLevel().tileAtEntity(entity);
  }

  /**
   * Returns the entities on the given tile. If the tile is null, an empty stream will be returned.
   *
   * @param check Tile to check for.
   * @return Stream of all entities on the given tile
   */
  public static Stream<Entity> entityAtTile(final Tile check) {
    Tile tile = Game.tileAT(check.position());
    if (tile == null) return Stream.empty();

    return ECSManagment.entityStream(Set.of(PositionComponent.class))
        .filter(
            e ->
                tile.equals(
                    tileAT(
                        e.fetch(PositionComponent.class)
                            .orElseThrow(
                                () -> MissingComponentException.build(e, PositionComponent.class))
                            .position())));
  }

  /**
   * Get a random tile of the given type.
   *
   * @param elementType Type of the tile.
   * @return A random tile of the given type.
   */
  public static Tile randomTile(final LevelElement elementType) {
    return currentLevel().randomTile(elementType);
  }

  /**
   * Get the position of a random Tile as Point.
   *
   * @return Position of the Tile as Point.
   */
  public static Point randomTilePoint() {
    return currentLevel().randomTilePoint();
  }

  /**
   * Get the position of a random Tile as Point.
   *
   * @param elementTyp Type of the Tile.
   * @return Position of the Tile as Point.
   */
  public static Point randomTilePoint(final LevelElement elementTyp) {
    return currentLevel().randomTilePoint(elementTyp);
  }

  /**
   * Starts the indexed A* pathfinding algorithm a returns a path
   *
   * <p>Throws an IllegalArgumentException if start or end is non-accessible.
   *
   * @param start Start tile
   * @param end End tile
   * @return Generated path
   */
  public static GraphPath<Tile> findPath(final Tile start, final Tile end) {
    return currentLevel().findPath(start, end);
  }

  /**
   * Get the Position of the given entity in the level.
   *
   * @param entity Entity to get the current position from (needs a {@link PositionComponent}
   * @return Position of the given entity.
   */
  public static Point positionOf(final Entity entity) {
    return currentLevel().positionOf(entity);
  }

  /**
   * Set the current level.
   *
   * <p>This method is for testing and debugging purposes.
   *
   * @param level New level
   */
  public static void currentLevel(final ILevel level) {
    LevelSystem levelSystem = (LevelSystem) ECSManagment.systems().get(LevelSystem.class);
    if (levelSystem != null) levelSystem.loadLevel(level);
    else LOGGER.warn("Can not set Level because levelSystem is null.");
  }

  /**
   * The currently set level-Size.
   *
   * <p>This value is used for the generation of the next level.
   *
   * <p>The currently active level can have a different size.
   *
   * @return currently set level-Size.
   */
  public static LevelSize levelSize() {
    return LevelSystem.levelSize();
  }

  /**
   * Set the {@link LevelSize} of the next level.
   *
   * @param levelSize Size of the next level.
   */
  public static void levelSize(final LevelSize levelSize) {
    LevelSystem.levelSize(levelSize);
  }
}
