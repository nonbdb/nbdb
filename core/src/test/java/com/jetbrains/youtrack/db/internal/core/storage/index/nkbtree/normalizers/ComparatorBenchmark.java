package com.jetbrains.youtrack.db.internal.core.storage.index.nkbtree.normalizers;

import com.jetbrains.youtrack.db.internal.common.comparator.OByteArrayComparator;
import com.jetbrains.youtrack.db.internal.common.comparator.OUnsafeByteArrayComparator;
import com.jetbrains.youtrack.db.internal.core.index.OCompositeKey;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTType;
import java.text.Collator;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 1, batchSize = 1)
@Warmup(iterations = 1, batchSize = 1)
@Fork(1)
public class ComparatorBenchmark {

  KeyNormalizer keyNormalizer;

  public static void main(String[] args) throws RunnerException {
    final Options opt =
        new OptionsBuilder()
            .include("ComparatorBenchmark.*")
            .addProfiler(StackProfiler.class, "detailLine=true;excludePackages=true;period=1")
            .jvmArgs("-server", "-XX:+UseConcMarkSweepGC", "-Xmx4G", "-Xms1G")
            // .result("target" + "/" + "results.csv")
            // .param("offHeapMessages", "true""
            // .resultFormat(ResultFormatType.CSV)
            .build();
    new Runner(opt).run();
  }

  final OByteArrayComparator arrayComparator = new OByteArrayComparator();
  final OUnsafeByteArrayComparator byteArrayComparator = new OUnsafeByteArrayComparator();

  byte[] negative;
  byte[] zero;
  byte[] positive;

  @Setup(Level.Iteration)
  public void setup() {
    keyNormalizer = new KeyNormalizer();

    negative = getNormalizedKeySingle(-62, YTType.INTEGER);
    zero = getNormalizedKeySingle(0, YTType.INTEGER);
    positive = getNormalizedKeySingle(5, YTType.INTEGER);
  }

  @Benchmark
  public void comparatorByteArrayNegative() throws Exception {
    byteArrayComparator.compare(negative, zero);
  }

  @Benchmark
  public void comparatorByteArrayPositive() throws Exception {
    byteArrayComparator.compare(positive, zero);
  }

  @Benchmark
  public void comparatorByteArrayEqual() throws Exception {
    byteArrayComparator.compare(zero, zero);
  }

  @Benchmark
  public void comparatorUnsafeByteArrayNegative() throws Exception {
    arrayComparator.compare(negative, zero);
  }

  @Benchmark
  public void comparatorUnsafeByteArrayPositive() throws Exception {
    arrayComparator.compare(positive, zero);
  }

  @Benchmark
  public void comparatorUnsafeByteArrayEqual() throws Exception {
    arrayComparator.compare(zero, zero);
  }

  private byte[] getNormalizedKeySingle(final int keyValue, final YTType type) {
    final OCompositeKey compositeKey = new OCompositeKey();
    compositeKey.addKey(keyValue);
    Assert.assertEquals(1, compositeKey.getKeys().size());

    final YTType[] types = new YTType[1];
    types[0] = type;

    return keyNormalizer.normalize(compositeKey, types, Collator.NO_DECOMPOSITION);
  }
}