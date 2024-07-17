package core.level.block;

public enum BlockFace {

  TOP,
  BOTTOM,
  FRONT,
  BACK,
  LEFT,
  RIGHT;

  public BlockFace opposite() {
    return switch (this) {
      case TOP -> BOTTOM;
      case BOTTOM -> TOP;
      case FRONT -> BACK;
      case BACK -> FRONT;
      case LEFT -> RIGHT;
      case RIGHT -> LEFT;
      default -> throw new IllegalStateException("Unexpected value: " + this);
    };
  }

  public byte bitMask() {
    return switch (this) {
      case TOP -> 0b00100000;
      case BOTTOM -> 0b00010000;
      case FRONT -> 0b00001000;
      case BACK -> 0b00000100;
      case LEFT -> 0b00000010;
      case RIGHT -> 0b00000001;
    };
  }

  public static BlockFace fromBitMask(byte bitMask) {
    return switch (bitMask) {
      case 0b00100000 -> TOP;
      case 0b00010000 -> BOTTOM;
      case 0b00001000 -> FRONT;
      case 0b00000100 -> BACK;
      case 0b00000010 -> LEFT;
      case 0b00000001 -> RIGHT;
      default -> throw new IllegalArgumentException("Invalid bit mask: " + bitMask);
    };
  }

}
