package oscillator

import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite

class I2STransmitterSim extends AnyFunSuite {
  test("I2S Transmitter pattern timing") {
    SimConfig.withWave.compile(new I2STransmitter).doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)
      
      // Initial state
      dut.io.valid #= false
      dut.io.sampleIn #= 12345 // Apply a dummy test sample
      dut.clockDomain.waitSampling(5)

      // Trigger the "Gated Startup": Pulse valid to activate the transmitter
      dut.io.valid #= true
      dut.clockDomain.waitSampling()
      dut.io.valid #= false

      // The transmitter is now active. Wait for the start of the frame.
      dut.clockDomain.waitSamplingWhere(dut.io.lrclk.toBoolean == false)

      // Helper to verify a full BCLK period (falling edge to falling edge)
      def checkBitInterval(expectedCycles: Int) {
        val start = simTime()
        // The interval ends when BCLK completes one high-low cycle
        dut.clockDomain.waitSamplingWhere(dut.io.bclk.toBoolean == true)
        dut.clockDomain.waitSamplingWhere(dut.io.bclk.toBoolean == false)
        dut.clockDomain.waitSamplingWhere(dut.io.bclk.toBoolean == true)
        val stop = simTime()
        
        val interval = (stop - start) / 10

        assert(interval == expectedCycles, s"Expected $expectedCycles cycles for bit, got $interval")
      }

      // Verify the complete 8-step timing pattern
      checkBitInterval(16)
      checkBitInterval(16)
      checkBitInterval(15)
      checkBitInterval(16)
      checkBitInterval(16)
      checkBitInterval(15)
      checkBitInterval(16)
      checkBitInterval(15)

      // Verify full frame duration (4 repeats of the 8-step pattern = 500 cycles)
      dut.clockDomain.waitSamplingWhere(dut.io.lrclk.toBoolean == true)
      dut.clockDomain.waitSamplingWhere(dut.io.lrclk.toBoolean == false)
      val frameStart = simTime()

      dut.clockDomain.waitSamplingWhere(dut.io.lrclk.toBoolean == true)
      dut.clockDomain.waitSamplingWhere(dut.io.lrclk.toBoolean == false)
      val frameDuration = (simTime() - frameStart) / 10
      assert(frameDuration == 500, s"Full frame should be 500 cycles, got $frameDuration")
    }
  }
}