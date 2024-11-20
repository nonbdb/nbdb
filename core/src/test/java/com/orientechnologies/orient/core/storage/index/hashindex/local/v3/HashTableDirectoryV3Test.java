package com.orientechnologies.orient.core.storage.index.hashindex.local.v3;

import com.orientechnologies.BaseMemoryInternalDatabase;
import com.orientechnologies.orient.core.exception.OCommandInterruptedException;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperationsManager;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Andrey Lomakin (a.lomakin-at-orientdb.com)
 * @since 5/15/14
 */
public class HashTableDirectoryV3Test extends BaseMemoryInternalDatabase {

  private static OHashTableDirectory directory;

  public void beforeTest() {
    super.beforeTest();
    OAbstractPaginatedStorage storage = (OAbstractPaginatedStorage) db.getStorage();
    directory =
        new OHashTableDirectory(
            ".tsc", "hashTableDirectoryTest", "hashTableDirectoryTest", storage);

    try {
      final OAtomicOperation atomicOperation = startTx();
      directory.create(atomicOperation);
      completeTx();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void afterTest() {
    try {
      final OAtomicOperation atomicOperation = startTx();
      directory.clear(atomicOperation);
      completeTx();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    super.afterTest();
  }

  private OAtomicOperation startTx() throws IOException {
    OAbstractPaginatedStorage storage = (OAbstractPaginatedStorage) db.getStorage();
    OAtomicOperationsManager manager = storage.getAtomicOperationsManager();
    Assert.assertNull(manager.getCurrentOperation());
    return manager.startAtomicOperation(null);
  }

  private void rollbackTx() throws IOException {
    OAbstractPaginatedStorage storage = (OAbstractPaginatedStorage) db.getStorage();
    OAtomicOperationsManager manager = storage.getAtomicOperationsManager();
    manager.endAtomicOperation(new OCommandInterruptedException(""));
    Assert.assertNull(manager.getCurrentOperation());
  }

  private void completeTx() throws IOException {
    OAbstractPaginatedStorage storage = (OAbstractPaginatedStorage) db.getStorage();
    OAtomicOperationsManager manager = storage.getAtomicOperationsManager();
    manager.endAtomicOperation(null);
    Assert.assertNull(manager.getCurrentOperation());
  }

  @Test
  public void addFirstLevel() throws IOException {
    OAtomicOperation atomicOperation = startTx();

    long[] level = new long[OLocalHashTableV3.MAX_LEVEL_SIZE];
    for (int i = 0; i < level.length; i++) {
      level[i] = i;
    }

    int index = directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);

    Assert.assertEquals(index, 0);
    Assert.assertEquals(directory.getMaxLeftChildDepth(0, atomicOperation), 2);
    Assert.assertEquals(directory.getMaxRightChildDepth(0, atomicOperation), 3);
    Assert.assertEquals(directory.getNodeLocalDepth(0, atomicOperation), 4);

    Assertions.assertThat(directory.getNode(0, atomicOperation)).isEqualTo(level);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(0, i, atomicOperation), i);
    }
    rollbackTx();
  }

  @Test
  public void changeFirstLevel() throws IOException {
    OAtomicOperation atomicOperation = startTx();
    long[] level = new long[OLocalHashTableV3.MAX_LEVEL_SIZE];
    for (int i = 0; i < level.length; i++) {
      level[i] = i;
    }

    directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);

    for (int i = 0; i < level.length; i++) {
      directory.setNodePointer(0, i, i + 100, atomicOperation);
    }

    directory.setMaxLeftChildDepth(0, (byte) 100, atomicOperation);
    directory.setMaxRightChildDepth(0, (byte) 101, atomicOperation);
    directory.setNodeLocalDepth(0, (byte) 102, atomicOperation);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(0, i, atomicOperation), i + 100);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(0, atomicOperation), 100);
    Assert.assertEquals(directory.getMaxRightChildDepth(0, atomicOperation), 101);
    Assert.assertEquals(directory.getNodeLocalDepth(0, atomicOperation), 102);

    rollbackTx();
  }

  @Test
  public void addThreeRemoveSecondAddNewAndChange() throws IOException {
    OAtomicOperation atomicOperation = startTx();

    long[] level = new long[OLocalHashTableV3.MAX_LEVEL_SIZE];
    for (int i = 0; i < level.length; i++) {
      level[i] = i;
    }

    int index = directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);
    Assert.assertEquals(index, 0);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 100;
    }

    index = directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);
    Assert.assertEquals(index, 1);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 200;
    }

    index = directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);
    Assert.assertEquals(index, 2);

    directory.deleteNode(1, atomicOperation);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 300;
    }

    index = directory.addNewNode((byte) 5, (byte) 6, (byte) 7, level, atomicOperation);
    Assert.assertEquals(index, 1);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(1, i, atomicOperation), i + 300);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(1, atomicOperation), 5);
    Assert.assertEquals(directory.getMaxRightChildDepth(1, atomicOperation), 6);
    Assert.assertEquals(directory.getNodeLocalDepth(1, atomicOperation), 7);

    rollbackTx();
  }

  @Test
  public void addRemoveChangeMix() throws IOException {
    OAtomicOperation atomicOperation = startTx();

    long[] level = new long[OLocalHashTableV3.MAX_LEVEL_SIZE];
    for (int i = 0; i < level.length; i++) {
      level[i] = i;
    }

    int index = directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);
    Assert.assertEquals(index, 0);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 100;
    }

    index = directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);
    Assert.assertEquals(index, 1);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 200;
    }

    index = directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);
    Assert.assertEquals(index, 2);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 300;
    }

    index = directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);
    Assert.assertEquals(index, 3);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 400;
    }

    index = directory.addNewNode((byte) 2, (byte) 3, (byte) 4, level, atomicOperation);
    Assert.assertEquals(index, 4);

    directory.deleteNode(1, atomicOperation);
    directory.deleteNode(3, atomicOperation);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 500;
    }

    index = directory.addNewNode((byte) 5, (byte) 6, (byte) 7, level, atomicOperation);
    Assert.assertEquals(index, 3);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 600;
    }

    index = directory.addNewNode((byte) 8, (byte) 9, (byte) 10, level, atomicOperation);
    Assert.assertEquals(index, 1);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 700;
    }

    index = directory.addNewNode((byte) 11, (byte) 12, (byte) 13, level, atomicOperation);
    Assert.assertEquals(index, 5);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(3, i, atomicOperation), i + 500);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(3, atomicOperation), 5);
    Assert.assertEquals(directory.getMaxRightChildDepth(3, atomicOperation), 6);
    Assert.assertEquals(directory.getNodeLocalDepth(3, atomicOperation), 7);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(1, i, atomicOperation), i + 600);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(1, atomicOperation), 8);
    Assert.assertEquals(directory.getMaxRightChildDepth(1, atomicOperation), 9);
    Assert.assertEquals(directory.getNodeLocalDepth(1, atomicOperation), 10);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(5, i, atomicOperation), i + 700);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(5, atomicOperation), 11);
    Assert.assertEquals(directory.getMaxRightChildDepth(5, atomicOperation), 12);
    Assert.assertEquals(directory.getNodeLocalDepth(5, atomicOperation), 13);

    rollbackTx();
  }

  @Test
  public void addThreePages() throws IOException {
    OAtomicOperation atomicOperation = startTx();

    int firsIndex = -1;
    int secondIndex = -1;
    int thirdIndex = -1;

    long[] level = new long[OLocalHashTableV3.MAX_LEVEL_SIZE];

    for (int n = 0; n < ODirectoryFirstPage.NODES_PER_PAGE; n++) {
      for (int i = 0; i < level.length; i++) {
        level[i] = i + n * 100L;
      }

      int index = directory.addNewNode((byte) 5, (byte) 6, (byte) 7, level, atomicOperation);
      if (firsIndex < 0) {
        firsIndex = index;
      }
    }

    for (int n = 0; n < ODirectoryPage.NODES_PER_PAGE; n++) {
      for (int i = 0; i < level.length; i++) {
        level[i] = i + n * 100L;
      }

      int index = directory.addNewNode((byte) 5, (byte) 6, (byte) 7, level, atomicOperation);
      if (secondIndex < 0) {
        secondIndex = index;
      }
    }

    for (int n = 0; n < ODirectoryPage.NODES_PER_PAGE; n++) {
      for (int i = 0; i < level.length; i++) {
        level[i] = i + n * 100L;
      }

      int index = directory.addNewNode((byte) 5, (byte) 6, (byte) 7, level, atomicOperation);
      if (thirdIndex < 0) {
        thirdIndex = index;
      }
    }

    Assert.assertEquals(firsIndex, 0);
    Assert.assertEquals(secondIndex, ODirectoryFirstPage.NODES_PER_PAGE);
    Assert.assertEquals(
        thirdIndex, ODirectoryFirstPage.NODES_PER_PAGE + ODirectoryPage.NODES_PER_PAGE);

    directory.deleteNode(secondIndex, atomicOperation);
    directory.deleteNode(firsIndex, atomicOperation);
    directory.deleteNode(thirdIndex, atomicOperation);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 1000;
    }

    int index = directory.addNewNode((byte) 8, (byte) 9, (byte) 10, level, atomicOperation);
    Assert.assertEquals(index, thirdIndex);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 2000;
    }

    index = directory.addNewNode((byte) 11, (byte) 12, (byte) 13, level, atomicOperation);
    Assert.assertEquals(index, firsIndex);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 3000;
    }

    index = directory.addNewNode((byte) 14, (byte) 15, (byte) 16, level, atomicOperation);
    Assert.assertEquals(index, secondIndex);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 4000;
    }

    index = directory.addNewNode((byte) 17, (byte) 18, (byte) 19, level, atomicOperation);
    Assert.assertEquals(
        index, ODirectoryFirstPage.NODES_PER_PAGE + 2L * ODirectoryPage.NODES_PER_PAGE);

    Assert.assertEquals(directory.getMaxLeftChildDepth(thirdIndex, atomicOperation), 8);
    Assert.assertEquals(directory.getMaxRightChildDepth(thirdIndex, atomicOperation), 9);
    Assert.assertEquals(directory.getNodeLocalDepth(thirdIndex, atomicOperation), 10);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(thirdIndex, i, atomicOperation), i + 1000);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(firsIndex, atomicOperation), 11);
    Assert.assertEquals(directory.getMaxRightChildDepth(firsIndex, atomicOperation), 12);
    Assert.assertEquals(directory.getNodeLocalDepth(firsIndex, atomicOperation), 13);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(firsIndex, i, atomicOperation), i + 2000);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(secondIndex, atomicOperation), 14);
    Assert.assertEquals(directory.getMaxRightChildDepth(secondIndex, atomicOperation), 15);
    Assert.assertEquals(directory.getNodeLocalDepth(secondIndex, atomicOperation), 16);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(secondIndex, i, atomicOperation), i + 3000);
    }

    final int lastIndex = ODirectoryFirstPage.NODES_PER_PAGE + 2 * ODirectoryPage.NODES_PER_PAGE;

    Assert.assertEquals(directory.getMaxLeftChildDepth(lastIndex, atomicOperation), 17);
    Assert.assertEquals(directory.getMaxRightChildDepth(lastIndex, atomicOperation), 18);
    Assert.assertEquals(directory.getNodeLocalDepth(lastIndex, atomicOperation), 19);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(lastIndex, i, atomicOperation), i + 4000);
    }
    rollbackTx();
  }

  @Test
  public void changeLastNodeSecondPage() throws IOException {
    OAtomicOperation atomicOperation = startTx();

    long[] level = new long[OLocalHashTableV3.MAX_LEVEL_SIZE];

    for (int n = 0; n < ODirectoryFirstPage.NODES_PER_PAGE; n++) {
      for (int i = 0; i < level.length; i++) {
        level[i] = i + n * 100L;
      }

      directory.addNewNode((byte) 5, (byte) 6, (byte) 7, level, atomicOperation);
    }

    for (int n = 0; n < ODirectoryPage.NODES_PER_PAGE; n++) {
      for (int i = 0; i < level.length; i++) {
        level[i] = i + n * 100L;
      }

      directory.addNewNode((byte) 5, (byte) 6, (byte) 7, level, atomicOperation);
    }

    for (int n = 0; n < ODirectoryPage.NODES_PER_PAGE; n++) {
      for (int i = 0; i < level.length; i++) {
        level[i] = i + n * 100L;
      }

      directory.addNewNode((byte) 5, (byte) 6, (byte) 7, level, atomicOperation);
    }

    directory.deleteNode(
        ODirectoryFirstPage.NODES_PER_PAGE + ODirectoryPage.NODES_PER_PAGE - 1, atomicOperation);

    for (int i = 0; i < level.length; i++) {
      level[i] = i + 1000;
    }

    int index = directory.addNewNode((byte) 8, (byte) 9, (byte) 10, level, atomicOperation);
    Assert.assertEquals(
        index, ODirectoryFirstPage.NODES_PER_PAGE + ODirectoryPage.NODES_PER_PAGE - 1);

    directory.setMaxLeftChildDepth(index - 1, (byte) 10, atomicOperation);
    directory.setMaxRightChildDepth(index - 1, (byte) 11, atomicOperation);
    directory.setNodeLocalDepth(index - 1, (byte) 12, atomicOperation);

    for (int i = 0; i < level.length; i++) {
      directory.setNodePointer(index - 1, i, i + 2000, atomicOperation);
    }

    directory.setMaxLeftChildDepth(index + 1, (byte) 13, atomicOperation);
    directory.setMaxRightChildDepth(index + 1, (byte) 14, atomicOperation);
    directory.setNodeLocalDepth(index + 1, (byte) 15, atomicOperation);

    for (int i = 0; i < level.length; i++) {
      directory.setNodePointer(index + 1, i, i + 3000, atomicOperation);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(index - 1, atomicOperation), 10);
    Assert.assertEquals(directory.getMaxRightChildDepth(index - 1, atomicOperation), 11);
    Assert.assertEquals(directory.getNodeLocalDepth(index - 1, atomicOperation), 12);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(index - 1, i, atomicOperation), i + 2000);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(index, atomicOperation), 8);
    Assert.assertEquals(directory.getMaxRightChildDepth(index, atomicOperation), 9);
    Assert.assertEquals(directory.getNodeLocalDepth(index, atomicOperation), 10);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(index, i, atomicOperation), i + 1000);
    }

    Assert.assertEquals(directory.getMaxLeftChildDepth(index + 1, atomicOperation), 13);
    Assert.assertEquals(directory.getMaxRightChildDepth(index + 1, atomicOperation), 14);
    Assert.assertEquals(directory.getNodeLocalDepth(index + 1, atomicOperation), 15);

    for (int i = 0; i < level.length; i++) {
      Assert.assertEquals(directory.getNodePointer(index + 1, i, atomicOperation), i + 3000);
    }
    rollbackTx();
  }
}
