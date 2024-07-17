package core.level;

import de.fwatermann.dungine.utils.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LevelChunkTree implements Iterable<LevelChunk> {

  private final LevelChunkTree[] subTrees;
  private LevelChunk chunk;
  private int x, y, z;
  boolean deleted = false;
  private LevelChunkTreeIterator iterator;

  public LevelChunkTree(@NotNull LevelChunk chunk) {
    this.subTrees = new LevelChunkTree[8];
    this.chunk = chunk;
    this.x = chunk.x();
    this.y = chunk.y();
    this.z = chunk.z();
  }

  public LevelChunkTree() {
    this.subTrees = new LevelChunkTree[8];
  }

  public void insert(LevelChunk insert) {
    if (!this.deleted) {
      if (this.chunk != null) {
        if (this.chunk.x() == insert.x()
            && this.chunk.y() == insert.y()
            && this.chunk.z() == insert.z()) {
          return; // Already inserted
        }
      } else {
        this.chunk = insert;
        this.x = insert.x();
        this.y = insert.y();
        this.z = insert.z();
        return;
      }
    }
    int index = this.index(insert.x(), insert.y(), insert.z(), this.x, this.y, this.z);
    if (this.subTrees[index] == null) {
      this.subTrees[index] = new LevelChunkTree(insert);
    } else {
      this.subTrees[index].insert(insert);
    }
  }

  public LevelChunk find(int x, int y, int z) {
    if (!this.deleted) {
      if (this.chunk != null) {
        if (this.chunk.x() == x && this.chunk.y() == y && this.chunk.z() == z) {
          return this.chunk;
        }
      } else {
        return null; // Reached leaf -> Not in tree.
      }
    }
    int index = this.index(x, y, z, this.x, this.y, this.z);
    if (this.subTrees[index] != null) {
      return this.subTrees[index].find(x, y, z);
    }
    return null; // Not in tree
  }

  public LevelChunk delete(LevelChunk chunk) {
    return this.delete(chunk.x(), chunk.y(), chunk.z());
  }

  public LevelChunk delete(int x, int y, int z) {
    if (!this.deleted) {
      if (this.chunk != null) {
        if (this.chunk.x() == x && this.chunk.y() == y && this.chunk.z() == z) {
          this.deleted = true;
          LevelChunk ret = this.chunk;
          this.chunk = null;
          return ret;
        }
      } else {
        return null; // Reached leaf -> Not in tree.
      }
    }
    int index = this.index(x, y, z, this.x, this.y, this.z);
    if (this.subTrees[index] != null) {
      return this.subTrees[index].delete(x, y, z);
    }
    return null; // Not in tree
  }

  public void rebuild() {
    //TODO: Implement
  }

  private int index(int aX, int aY, int aZ, int bX, int bY, int bZ) {
    if (aX >= bX) {
      if (aY >= bY) {
        if (aZ >= bZ) {
          return 0;
        } else {
          return 1;
        }
      } else {
        if (aZ >= bZ) {
          return 2;
        } else {
          return 3;
        }
      }
    } else {
      if (aY >= bY) {
        if (aZ >= bZ) {
          return 4;
        } else {
          return 5;
        }
      } else {
        if (aZ >= bZ) {
          return 6;
        } else {
          return 7;
        }
      }
    }
  }

  @Override
  public Iterator<LevelChunk> iterator() {
    if(this.iterator == null) {
      this.iterator = new LevelChunkTreeIterator(this);
    }
    return this.iterator;
  }

  private static class LevelChunkTreeIterator implements Iterator<LevelChunk> {

    private final List<LevelChunkTree> stack = new ArrayList<>();

    public LevelChunkTreeIterator(LevelChunkTree root) {
      this.stack.add(root);
    }

    @Override
    public boolean hasNext() {
      return !this.stack.isEmpty() && this.stack.stream().anyMatch(tree -> !tree.deleted);
    }

    @Override
    public LevelChunk next() {
      if(this.stack.isEmpty()) return null;
      LevelChunkTree current = this.stack.removeLast();
      for(int i = 0; i < current.subTrees.length; i ++) {
        if(current.subTrees[i] != null) {
          this.stack.add(current.subTrees[i]);
        }
      }
      if(current.deleted) {
        return this.next();
      }
      if(current.chunk != null) {
        return current.chunk;
      }
      return this.next();
    }

  }

}
