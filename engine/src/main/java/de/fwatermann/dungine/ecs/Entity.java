package de.fwatermann.dungine.ecs;

import java.util.*;
import java.util.stream.Stream;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Entity {

  private Vector3f position;
  private Vector3f size;
  private Quaternionf rotation;

  private final Map<Class<? extends Component>, List<Component>> components = new HashMap<>();

  public Entity(Vector3f position, Quaternionf rotation, Vector3f scaling) {
    this.position = position;
    this.rotation = rotation;
    this.size = scaling;
  }

  public Entity() {
    this(new Vector3f(), new Quaternionf(), new Vector3f(1.0f));
  }

  /**
   * Create a new entity with the given components.
   *
   * @param components the components to add
   */
  public Entity(Component... components) {
    this();
    Arrays.stream(components)
        .forEach(
            c -> {
              this.components
                  .computeIfAbsent(
                      c.getClass(), (Class<? extends Component> clazz) -> new ArrayList<>())
                  .add(c);
            });
  }

  /**
   * Get all components.
   *
   * @return a stream of components
   */
  public Stream<Component> components() {
    return this.components.values().stream().flatMap(List::stream);
  }

  /**
   * Get all components of a specific type.
   *
   * @param clazz the class of the component
   * @return a stream of components
   */
  public <T extends Component> Stream<T> components(Class<T> clazz) {
    return (Stream<T>) this.components.getOrDefault(clazz, Collections.emptyList()).stream();
  }

  /**
   * Add a component to this entity.
   *
   * @param component the component to add
   * @return this entity
   */
  public Entity addComponent(Component component) {
    List<Component> list =
        this.components.computeIfAbsent(
            component.getClass(), (Class<? extends Component> clazz) -> new ArrayList<>());
    if (!component.canHaveMultiple()) {
      if (list.isEmpty()) {
        list.add(component);
      } else {
        list.set(0, component);
      }
    } else {
      list.add(component);
    }
    return this;
  }

  /**
   * Get the first component of a specific type.
   *
   * @param clazz the class of the component
   * @return the component
   * @param <T> the type of the component
   */
  public <T extends Component> Optional<T> component(Class<T> clazz) {
    return Optional.ofNullable(
        (T) this.components.getOrDefault(clazz, Collections.emptyList()).get(0));
  }

  /**
   * Remove a component from this entity.
   *
   * @param component the component to remove
   * @return true if the component was removed, false if the component was not present
   */
  public boolean removeComponent(Component component) {
    return this.components
        .getOrDefault(component.getClass(), Collections.emptyList())
        .remove(component);
  }

  /**
   * Check if this entity has a specific component.
   *
   * @param component the component to check
   * @return true if the component is present, false otherwise
   */
  public boolean hasComponents(Component component) {
    return this.components
        .getOrDefault(component.getClass(), Collections.emptyList())
        .contains(component);
  }

  /**
   * Check if this entity has a specific component of a specific class.
   *
   * @param componentClass the class of the component to check
   * @return true if the component is present, false otherwise
   */
  @SafeVarargs
  public final boolean hasComponents(Class<? extends Component>... componentClass) {
    return Arrays.stream(componentClass).allMatch(this.components::containsKey);
  }

  public final boolean hasComponents(Set<Class<? extends Component>> componentClasses) {
    return componentClasses.stream().allMatch(this.components::containsKey);
  }

  public Vector3f position() {
    return this.position;
  }

  public Entity position(Vector3f position) {
    this.position = position;
    return this;
  }

  public Vector3f size() {
    return this.size;
  }

  public Entity size(Vector3f size) {
    this.size = size;
    return this;
  }

  public Quaternionf rotation() {
    return this.rotation;
  }

  public Entity rotation(Quaternionf rotation) {
    this.rotation = rotation;
    return this;
  }
}
