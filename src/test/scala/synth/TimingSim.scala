package synth

import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite

class TimingSim extends AnyFunSuite {
  test("TimingGenerator precision check") {
    SimConfig.withWave.compile(new TimingGenerator).doSim { dut =>
      // 1. Initialize clock and assert reset
      dut.clockDomain.forkStimulus(period = 10)
      dut.clockDomain.assertReset()
      
      // Test Case 1: Reset Stability
      println("Verifying Reset Stability:")
      for (_ <- 0 until 20) {
        dut.clockDomain.waitSampling()
        assert(!dut.io.phaseTick.toBoolean, "phaseTick must remain False during reset")
        assert(!dut.io.sampleTick.toBoolean, "sampleTick must remain False during reset")
      }
      dut.clockDomain.deassertReset()
      println("Reset Stability verified successfully.")

      // Test Case 2: Multi-Period Drift Check
      println("Verifying Phase and Sample Tick Intervals (Multi-period):")
      
      // Align and capture initial phaseTick time
      dut.clockDomain.waitSamplingWhere(dut.io.phaseTick.toBoolean)
      var lastPhaseTime = simTime()
      
      // Check 10 consecutive phase ticks (480 kHz)
      for (i <- 1 to 10) {
        dut.clockDomain.waitSamplingWhere(dut.io.phaseTick.toBoolean)
        val current = simTime()
        val interval = (current - lastPhaseTime) / 10
        assert(interval == 50, s"[Phase Tick $i] Expected exactly 50 cycles, got $interval")
        lastPhaseTime = current
      }
      println("10 consecutive phase ticks verified at exactly 50 cycles.")

      // Align and capture initial sampleTick time
      dut.clockDomain.waitSamplingWhere(dut.io.sampleTick.toBoolean)
      var lastSampleTime = simTime()

      // Check 5 consecutive sample ticks (48 kHz)
      for (i <- 1 to 5) {
        dut.clockDomain.waitSamplingWhere(dut.io.sampleTick.toBoolean)
        val current = simTime()
        val interval = (current - lastSampleTime) / 10
        assert(interval == 500, s"[Sample Tick $i] Expected exactly 500 cycles, got $interval")
        lastSampleTime = current
      }
      println("5 consecutive sample ticks verified at exactly 500 cycles.")

      // Test Case 3: Continuous Phase-to-Sample Alignment
      println("Verifying Synchronous Alignment (sampleTick -> phaseTick):")
      var alignmentChecks = 0
      while (alignmentChecks < 5) {
        dut.clockDomain.waitSampling()
        if (dut.io.sampleTick.toBoolean) {
          assert(dut.io.phaseTick.toBoolean, "sampleTick must be aligned with a phaseTick")
          alignmentChecks += 1
        }
      }
      println("Synchronous alignment verified successfully over 5 full cycles.")
    }
  }
}