package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.internal.core.exception.SequenceException;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.DBSequence;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.DBSequence.SEQUENCE_TYPE;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.SequenceLibrary;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @since 3/2/2015
 */
@Test(groups = "sequence")
public class DBSequenceTest extends BaseDBTest {

  private static final int CACHE_SIZE = 40;
  private static final long FIRST_START = DBSequence.DEFAULT_START;
  private static final long SECOND_START = 31;

  @Parameters(value = "remote")
  public DBSequenceTest(boolean remote) {
    super(remote);
  }

  @Test
  public void trivialTest() throws ExecutionException, InterruptedException {
    testSequence("seq1", SEQUENCE_TYPE.ORDERED);
    testSequence("seq2", SEQUENCE_TYPE.CACHED);
  }

  private void testSequence(String sequenceName, SEQUENCE_TYPE sequenceType)
      throws ExecutionException, InterruptedException {
    SequenceLibrary sequenceLibrary = database.getMetadata().getSequenceLibrary();

    DBSequence seq = sequenceLibrary.createSequence(sequenceName, sequenceType, null);

    SequenceException err = null;
    try {
      sequenceLibrary.createSequence(sequenceName, sequenceType, null);
    } catch (SequenceException se) {
      err = se;
    }
    Assert.assertTrue(
        err == null || err.getMessage().toLowerCase(Locale.ENGLISH).contains("already exists"),
        "Creating a second "
            + sequenceType.toString()
            + " sequences with same name doesn't throw an exception");

    DBSequence seqSame = sequenceLibrary.getSequence(sequenceName);
    Assert.assertEquals(seqSame, seq);

    // Doing it twice to check everything works after reset
    for (int i = 0; i < 2; ++i) {
      Assert.assertEquals(seq.next(), 1L);
      Assert.assertEquals(seq.current(), 1L);
      Assert.assertEquals(seq.next(), 2L);
      Assert.assertEquals(seq.next(), 3L);
      Assert.assertEquals(seq.next(), 4L);
      Assert.assertEquals(seq.current(), 4L);
      Assert.assertEquals(seq.reset(), 0L);
    }
  }

  @Test
  public void testOrdered() throws ExecutionException, InterruptedException {
    SequenceLibrary sequenceManager = database.getMetadata().getSequenceLibrary();

    DBSequence seq = sequenceManager.createSequence("seqOrdered", SEQUENCE_TYPE.ORDERED, null);

    SequenceException err = null;
    try {
      sequenceManager.createSequence("seqOrdered", SEQUENCE_TYPE.ORDERED, null);
    } catch (SequenceException se) {
      err = se;
    }
    Assert.assertTrue(
        err == null || err.getMessage().toLowerCase(Locale.ENGLISH).contains("already exists"),
        "Creating two ordered sequences with same name doesn't throw an exception");

    DBSequence seqSame = sequenceManager.getSequence("seqOrdered");
    Assert.assertEquals(seqSame, seq);

    testUsage(seq, FIRST_START);

    //
    database.begin();
    seq.updateParams(new DBSequence.CreateParams().setStart(SECOND_START).setCacheSize(13));
    database.commit();

    testUsage(seq, SECOND_START);
  }

  private void testUsage(DBSequence seq, long reset)
      throws ExecutionException, InterruptedException {
    for (int i = 0; i < 2; ++i) {
      Assert.assertEquals(seq.reset(), reset);
      Assert.assertEquals(seq.current(), reset);
      Assert.assertEquals(seq.next(), reset + 1L);
      Assert.assertEquals(seq.current(), reset + 1L);
      Assert.assertEquals(seq.next(), reset + 2L);
      Assert.assertEquals(seq.next(), reset + 3L);
      Assert.assertEquals(seq.next(), reset + 4L);
      Assert.assertEquals(seq.current(), reset + 4L);
      Assert.assertEquals(seq.reset(), reset);
    }
  }
}
