package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.common.concur.YTTimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.exception.YTCommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTClass;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class CheckRecordTypeStepTest extends TestUtilsFixture {

  @Test
  public void shouldCheckRecordsOfOneType() {
    CommandContext context = new BasicCommandContext();
    context.setDatabase(db);
    String className = createClassInstance().getName();
    CheckRecordTypeStep step = new CheckRecordTypeStep(context, className, false);
    AbstractExecutionStep previous =
        new AbstractExecutionStep(context, false) {
          boolean done = false;

          @Override
          public ExecutionStream internalStart(CommandContext ctx) throws YTTimeoutException {
            List<YTResult> result = new ArrayList<>();
            if (!done) {
              for (int i = 0; i < 10; i++) {
                result.add(new YTResultInternal(ctx.getDatabase(), new EntityImpl(className)));
              }
              done = true;
            }
            return ExecutionStream.resultIterator(result.iterator());
          }
        };

    step.setPrevious(previous);
    ExecutionStream result = step.start(context);
    Assert.assertEquals(10, result.stream(context).count());
    Assert.assertFalse(result.hasNext(context));
  }

  @Test
  public void shouldCheckRecordsOfSubclasses() {
    CommandContext context = new BasicCommandContext();
    context.setDatabase(db);
    YTClass parentClass = createClassInstance();
    YTClass childClass = createChildClassInstance(parentClass);
    CheckRecordTypeStep step = new CheckRecordTypeStep(context, parentClass.getName(), false);
    AbstractExecutionStep previous =
        new AbstractExecutionStep(context, false) {
          boolean done = false;

          @Override
          public ExecutionStream internalStart(CommandContext ctx) throws YTTimeoutException {
            List<YTResult> result = new ArrayList<>();
            if (!done) {
              for (int i = 0; i < 10; i++) {
                result.add(
                    new YTResultInternal(ctx.getDatabase(),
                        new EntityImpl(i % 2 == 0 ? parentClass : childClass)));
              }
              done = true;
            }
            return ExecutionStream.resultIterator(result.iterator());
          }
        };

    step.setPrevious(previous);
    ExecutionStream result = step.start(context);
    Assert.assertEquals(10, result.stream(context).count());
    Assert.assertFalse(result.hasNext(context));
  }

  @Test(expected = YTCommandExecutionException.class)
  public void shouldThrowExceptionWhenTypeIsDifferent() {
    CommandContext context = new BasicCommandContext();
    context.setDatabase(db);
    String firstClassName = createClassInstance().getName();
    String secondClassName = createClassInstance().getName();
    CheckRecordTypeStep step = new CheckRecordTypeStep(context, firstClassName, false);
    AbstractExecutionStep previous =
        new AbstractExecutionStep(context, false) {
          boolean done = false;

          @Override
          public ExecutionStream internalStart(CommandContext ctx) throws YTTimeoutException {
            List<YTResult> result = new ArrayList<>();
            if (!done) {
              for (int i = 0; i < 10; i++) {
                result.add(
                    new YTResultInternal(ctx.getDatabase(),
                        new EntityImpl(i % 2 == 0 ? firstClassName : secondClassName)));
              }
              done = true;
            }
            return ExecutionStream.resultIterator(result.iterator());
          }
        };

    step.setPrevious(previous);
    ExecutionStream result = step.start(context);
    while (result.hasNext(context)) {
      result.next(context);
    }
  }
}
