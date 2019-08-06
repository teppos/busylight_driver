package com.fyayc.essen.busylight.core.protocol;

import com.fyayc.essen.busylight.core.BlDriver;
import com.fyayc.essen.busylight.core.protocol.bytes.Color;
import com.fyayc.essen.busylight.core.protocol.bytes.Command;
import com.fyayc.essen.busylight.core.protocol.bytes.Repeat;
import com.fyayc.essen.busylight.core.protocol.bytes.StepByte;
import com.fyayc.essen.busylight.core.protocol.bytes.Time;
import com.fyayc.essen.busylight.core.protocol.bytes.Tone;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import java.util.Arrays;
import java.util.stream.Stream;

public class ProtocolSpec {
  public static final int BYTE_LENGTH = 64;
  private static final int SPEC_STEPS = 8;
  private final ProtocolStep[] steps;

  public ProtocolSpec(ProtocolStep[] steps) {
    this.steps = steps;
  }

  public static void main(String[] args) throws InterruptedException {
    ProtocolSpec protocol =
        ProtocolSpec.builder()
            .addStep(
                ProtocolStep.builder()
                    .color( Color.EMPTY, Color.EMPTY,Color.ofIntensity(20))
                    .light(Time.forDuration(0.5), Time.forDuration(0.5))
                    .repeatStep(Repeat.ofTimes(3))
                    .tone(Tone.TURN_OFF_TONE)
                    .command(Command.nextStep(1))
                    .build())
            .addStep(
                ProtocolStep.builder()
                    .color(Color.ofIntensity(80), Color.EMPTY, Color.EMPTY)
                    .light(Time.forDuration(1.5), Time.forDuration(0.5))
                    .repeatStep(Repeat.ofTimes(5))
                    .tone(Tone.noSettings())
                    .command(Command.nextStep(2))
                    .build())
            .addStep(
                ProtocolStep.builder()
                    .color(Color.EMPTY, Color.EMPTY,Color.ofIntensity(100))
                    .light(Time.forDuration(10), Time.forDuration(0))
                    .repeatStep(Repeat.ofTimes(1))
                    .tone(Tone.forSettings(13, 7))
                    .command(Command.nextStep(0))
                    .build())
            .build();
    System.out.println(protocol.toHexString());
    BlDriver driver = BlDriver.tryAndAcquire();
    driver.sendBuffer(protocol.toBytes());
  }

  public static SpecBuilder builder() {
    return new SpecBuilder();
  }

  public byte[] toBytes() {
    byte[] protocolBytes = new byte[BYTE_LENGTH];
    int ix=0;
    for (int currStep = 0; currStep < steps.length; currStep++) {
      byte[] currentStepBytes = steps[currStep].getBytes();
      for (int currentStepByteIx = 0;currentStepByteIx < currentStepBytes.length;currentStepByteIx++) {
        protocolBytes[ix++] = currentStepBytes[currentStepByteIx];
      }
    }
    return protocolBytes;
  }

  public String toHexString() {
    return Stream.of(steps).map(stp -> stp.toHexString() + "\n").reduce(" ", (a, b) -> a + b);
  }

  public static class SpecBuilder {
    int currentStep = 0;
    private ProtocolStep[] steps;

    public SpecBuilder() {
      steps = new ProtocolStep[ProtocolSpec.SPEC_STEPS];
    }

    public SpecBuilder addStep(ProtocolStep step) {
      if (currentStep > 7) {
        throw new IllegalArgumentException("max steps reached, currentstep: " + currentStep);
      }
      steps[currentStep++] = step;
      return this;
    }

    public SpecBuilder addStep(ProtocolStep step, int stepIndex) {
      if (stepIndex > 7 || stepIndex < 0) {
        throw new IllegalArgumentException("expecting stepIndex between 0 and 7 ");
      }
      steps[stepIndex] = step;
      return this;
    }

    public ProtocolSpec build() {
      if (currentStep < 8) {
        for (int i = 0; i < steps.length; i++) {
          ProtocolStep step = steps[i];
          if (step == null) {
            steps[i] = ProtocolStep.empty();
          }
        }
      }
      steps[7] = generateMetaStep();
      StepByte[] checksumBytes = calculateChecksum(steps);
      steps[7].setByte(checksumBytes[1], 6);
      steps[7].setByte(checksumBytes[0], 7);

      return new ProtocolSpec(steps);
    }

    private ProtocolStep generateMetaStep() {
      return ProtocolStep.builder()
          .add(0, StepByte.ofUnsignedInt("sensitivity", 0)) // 0-31
          .add(1, StepByte.ofUnsignedInt("timeout", 31)) // 1-31s
          .add(2, StepByte.ofUnsignedInt("trigger_time", 250)) // 1=250
          .add(3, StepByte.ofUnsignedInt("no_use", 255)) // not used
          .add(4, StepByte.ofUnsignedInt("no_use", 255)) // not used
          .add(5, StepByte.ofUnsignedInt("no_use", 255)) // not used
          .add(6, StepByte.EMPTY) // msb
          .add(7, StepByte.EMPTY) // lsb
          .build();
    }

    private StepByte[] calculateChecksum(ProtocolStep[] stepsToCalc) {
      int stepTotalSum =
          Arrays.stream(stepsToCalc)
              .map(ProtocolStep::checkSum)
              .reduce(0, (s1cs, s2cs) -> s1cs + s2cs);

      // checksum is 16 bits
      BitStore checksumStore = Bits.toStore(stepTotalSum, 16);
      return new StepByte[] {
        new StepByte("checksum_lsb", checksumStore.range(0, 8).toString(2)),
        new StepByte("checksum_msb", checksumStore.range(8, 16).toString(2))
      };
    }
  }
}
