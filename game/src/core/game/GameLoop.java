package core.game;

import core.Entity;
import core.Game;
import core.System;
import core.components.PositionComponent;
import core.level.generator.postGeneration.WallGenerator;
import core.level.generator.randomwalk.RandomWalkGenerator;
import core.systems.CameraSystem;
import core.systems.DrawSystem;
import core.systems.LevelSystem;
import core.systems.PlayerSystem;
import core.systems.PositionSystem;
import core.systems.VelocitySystem;
import core.utils.IVoidFunction;
import core.utils.components.MissingComponentException;
import de.fwatermann.dungine.event.EventHandler;
import de.fwatermann.dungine.event.EventListener;
import de.fwatermann.dungine.event.EventManager;
import de.fwatermann.dungine.event.input.KeyboardEvent;
import de.fwatermann.dungine.event.window.WindowResizeEvent;
import de.fwatermann.dungine.state.GameState;
import de.fwatermann.dungine.state.LoadStepper;
import de.fwatermann.dungine.window.GameWindow;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

/**
 * The Dungeon-GameLoop.
 *
 * <p>This class contains the game loop method that is connected with libGDX. It controls the system
 * flow, will execute the Systems, and triggers the event callbacks configured in the {@link
 * PreRunConfiguration}.
 *
 * <p>All API methods can also be accessed via the {@link core.Game} class.
 */
public final class GameLoop extends GameState implements EventListener {

  private static final Logger LOGGER = LogManager.getLogger(GameLoop.class);
  private boolean newLevelWasLoadedInThisLoop = false;

  /**
   * Sets {@link Game#currentLevel} to the new level and changes the currently active entity
   * storage.
   *
   * <p>Will remove all Systems using {@link ECSManagment#removeAllSystems()} from the Game. This
   * will trigger {@link System#onEntityRemove} for the old level. Then, it will readd all Systems
   * using {@link ECSManagment#add(System)}, triggering {@link System#onEntityAdd} for the new
   * level.
   *
   * <p>Will re-add the hero if they exist.
   */
  private final IVoidFunction onLevelLoad =
      () -> {
        this.newLevelWasLoadedInThisLoop = true;
        Optional<Entity> hero = ECSManagment.hero();
        boolean firstLoad = !ECSManagment.levelStorageMap().containsKey(Game.currentLevel());
        hero.ifPresent(ECSManagment::remove);
        // Remove the systems so that each triggerOnRemove(entity) will be called (basically
        // cleanup).
        Map<Class<? extends System>, System> s = ECSManagment.systems();
        ECSManagment.removeAllSystems();
        ECSManagment.activeEntityStorage(
            ECSManagment.levelStorageMap()
                .computeIfAbsent(Game.currentLevel(), k -> new HashSet<>()));
        // readd the systems so that each triggerOnAdd(entity) will be called (basically
        // setup). This will also create new EntitySystemMapper if needed.
        s.values().forEach(ECSManagment::add);

        try {
          hero.ifPresent(this::placeOnLevelStart);
        } catch (MissingComponentException e) {
          LOGGER.warn(e.getMessage());
        }
        hero.ifPresent(ECSManagment::add);
        Game.currentLevel().onLoad();
        PreRunConfiguration.userOnLevelLoad().accept(firstLoad);
      };

  // for singleton
  public GameLoop(GameWindow window) {
    super(window);
    EventManager.getInstance().registerListener(this);
  }

  /**
   * Main game loop.
   *
   * <p>Triggers the execution of the systems and the event callbacks.
   *
   * <p>Will trigger {@link PreRunConfiguration#userOnFrame()}.
   *
   * @param deltaTime The time since the last loop.
   */
  @Override
  public void render(float deltaTime) {
    LOGGER.trace("GameLoop.render({})", deltaTime);
    DrawSystem.batch().setProjectionMatrix(CameraSystem.camera().combined);
    PreRunConfiguration.userOnFrame().execute();
  }

  @Override
  public void update(float deltaTime) {
    for (System system : ECSManagment.systems().values()) {
      // if a new level was loaded, stop this loop-run
      if (this.newLevelWasLoadedInThisLoop) break;
      system.lastExecuteInFrames(system.lastExecuteInFrames() + 1);
      if (system.isRunning() && system.lastExecuteInFrames() >= system.executeEveryXFrames()) {
        system.execute();
        system.lastExecuteInFrames(0);
      }
    }
    this.newLevelWasLoadedInThisLoop = false;
  }

  /**
   * Called once at the beginning of the game.
   *
   * <p>Will perform some setup.
   */
  @Override
  public void init() {

    LoadStepper stepper = new LoadStepper(this.window);
    stepper.step(
        "createSystems",
        (results) -> {
          this.createSystems();
          return null;
        });

    PreRunConfiguration.userOnSetup().execute();
  }

  @Override
  public boolean loaded() {
    return true;
  }

  @EventHandler
  private void onKey(KeyboardEvent event) {
    if (event.key == GLFW.GLFW_KEY_F11 && event.action == KeyboardEvent.KeyAction.PRESS) {
      this.window.fullscreen(!this.window.fullscreen());
    }
  }

  @EventHandler
  private void onResize(WindowResizeEvent event) {
    LOGGER.debug("Window Resize: {} -> {}", event.from, event.to);
  }

  /**
   * Set the position of the given entity to the position of the level-start.
   *
   * <p>A {@link PositionComponent} is needed.
   *
   * @param entity entity to set on the start of the level, normally this is the hero.
   */
  private void placeOnLevelStart(final Entity entity) {
    ECSManagment.add(entity);
    PositionComponent pc =
        entity
            .fetch(PositionComponent.class)
            .orElseThrow(() -> MissingComponentException.build(entity, PositionComponent.class));
    pc.position(Game.startTile());
  }

  /** Create the systems. */
  private void createSystems() {
    ECSManagment.add(new PositionSystem());
    ECSManagment.add(new CameraSystem());
    ECSManagment.add(
        new LevelSystem(
            DrawSystem.painter(), new WallGenerator(new RandomWalkGenerator()), this.onLevelLoad));
    ECSManagment.add(new DrawSystem());
    ECSManagment.add(new VelocitySystem());
    ECSManagment.add(new PlayerSystem());
  }

  @Override
  public void dispose() {}
}
