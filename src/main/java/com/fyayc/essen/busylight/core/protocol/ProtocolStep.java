package com.fyayc.essen.busylight.core.protocol;

import com.fyayc.essen.busylight.core.protocol.SpecConstants.Light;
import com.fyayc.essen.busylight.core.protocol.bytes.StepByte;
import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

/** a protocol step */
public class ProtocolStep {
  public static final int BYTE_LENGTH = 8;
  protected static final Logger logger = LoggerFactory.getLogger(ProtocolStep.class);
  private final StepByte[] stepBytes;

  public ProtocolStep(StepByte[] stepBytes) {
    this.stepBytes = stepBytes;
  }

  public static ProtocolStep empty() {
    StepByte[] steps = new StepByte[BYTE_LENGTH];

    for (int i = 0; i < BYTE_LENGTH; i++) {
      steps[i] = StepByte.EMPTY;
    }

    return new ProtocolStep(steps);
  }

  public static StepBuilder builder() {
    return new StepBuilder();
  }

  void setByte(StepByte stepByte, int index) {
    if (index < 0 || index > BYTE_LENGTH - 1) {
      throw new IllegalArgumentException("index must be between 0 and 7");
    }

    stepBytes[index] = stepByte;
  }

  public byte[] getBytes() {
    byte[] bytes = new byte[stepBytes.length];
    for (int i = 0; i < stepBytes.length; i++) {
      bytes[i] = stepBytes[i].getCurrentByte();
    }
    return bytes;
  }

  public int checkSum() {
    return Stream.of(stepBytes).map(StepByte::toInt).reduce(0, (a, b) -> a + b);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("stepBytes", stepBytes).toString();
  }

  public String toHexString() {
    return Stream.of(stepBytes).map(b -> b.hex() + " ").reduce(" ", (a, b) -> a + b);
  }

  public static class StepBuilder {
    private StepByte[] steps;

    public StepBuilder() {
      this.steps = new StepByte[ProtocolStep.BYTE_LENGTH];
      for (int i = 0; i < steps.length; i++) {
        steps[i] = StepByte.EMPTY;
      }
    }

    public StepBuilder add(int index, StepByte sbyte) {
      if (index < 0 || index > BYTE_LENGTH - 1) {
        throw new IllegalArgumentException("index must be between 0 and 7");
      }

      steps[index] = sbyte;
      return this;
    }

    public StepBuilder command(StepByte command) {
      return add(0, command);
    }

    public StepBuilder repeat(StepByte repeat) {
      return add(1, repeat);
    }

    public StepBuilder light(StepByte red, StepByte green, StepByte blue) {
      return add(2, red).add(3, green).add(4, blue);
    }

    public StepBuilder light(Light status) {
      return light(status.rgbBytes[0], status.rgbBytes[1], status.rgbBytes[2]);
    }

    public StepBuilder lightDuration(StepByte on, StepByte off) {
      return add(5, on).add(6, off);
    }

    public StepBuilder tone(StepByte tone) {
      return add(7, tone);
    }

    public ProtocolStep build() {
      return new ProtocolStep(steps);
    }
  }
}
