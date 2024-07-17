package core.level.generator;

import core.level.LevelChunk;

public interface IGenerator {

  public LevelChunk generateChunk(int chunkX, int chunkY, int chunkZ);

}
