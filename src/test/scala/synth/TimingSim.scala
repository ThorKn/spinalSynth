package synth

import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite

class TimingSim extends AnyFunSuite {
  test("TimingGenerator precision check") {
    SimConfig.withWave.compile(new TimingGenerator).doSim { dut =>
      // Initialize clock
      dut.clockDomain.forkStimulus(period = 10)
      
      // 1. Verify phaseTick (480 kHz / every 50 cycles)
      // Wait for first tick to align
      dut.clockDomain.waitSamplingWhere(dut.io.phaseTick.toBoolean)
      val firstPhaseTick = simTime()
      
      // Wait for second tick
      dut.clockDomain.waitSamplingWhere(dut.io.phaseTick.toBoolean)
      val secondPhaseTick = simTime()
      
      val phaseInterval = (secondPhaseTick - firstPhaseTick) / 10
      println(s"Phase Tick Interval: $phaseInterval cycles")
      assert(phaseInterval == 50, s"phaseTick should occur every 50 cycles, got $phaseInterval")

      // 2. Verify sampleTick (48 kHz / every 500 cycles)
      // Wait for first tick to align
      dut.clockDomain.waitSamplingWhere(dut.io.sampleTick.toBoolean)
      val firstSampleTick = simTime()
      
      // Wait for second tick
      dut.clockDomain.waitSamplingWhere(dut.io.sampleTick.toBoolean)
      val secondSampleTick = simTime()
      
      val sampleInterval = (secondSampleTick - firstSampleTick) / 10
      println(s"Sample Tick Interval: $sampleInterval cycles")
      assert(sampleInterval == 500, s"sampleTick should occur every 500 cycles, got $sampleInterval")

      // 3. Verify synchronous alignment
      // Every 10th phaseTick should coincide with a sampleTick
      waitUntil(dut.io.sampleTick.toBoolean)
      assert(dut.io.phaseTick.toBoolean, "sampleTick must be aligned with a phaseTick")
      
      println("TimingGenerator simulation successful: ticks are cycle-accurate.")
    }
  }
}