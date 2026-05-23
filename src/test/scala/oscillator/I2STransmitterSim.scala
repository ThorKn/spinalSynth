package oscillator

import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite

class I2STransmitterSim extends AnyFunSuite {
  test("I2S Transmitter pattern timing") {
    SimConfig.withWave.compile(new I2STransmitter).doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)
      
      // Initial state
      dut.io.valid #= false
      dut.io.sampleIn #= 0
      dut.clockDomain.waitSampling(5)

      // Align to LRCLK transition (start of frame)
      waitUntil(dut.io.lrclk.toBoolean == true)
      waitUntil(dut.io.lrclk.toBoolean == false)
      val frameStart = simTime()
      
      // Verify the first few bit intervals (Expected: 16, 16, 15...)
      def checkBitInterval(expectedCycles: Int) {
        val start = simTime()
        // Wait for BCLK rising edge (middle of bit) then falling (end of bit)
        waitUntil(dut.io.bclk.toBoolean == true)
        waitUntil(dut.io.bclk.toBoolean == false)
        val interval = (simTime() - start) / 10
        assert(interval == expectedCycles, s"Expected $expectedCycles cycles for bit, got $interval")
      }

      checkBitInterval(16)
      checkBitInterval(16)
      checkBitInterval(15)
      checkBitInterval(16)

      // Verify full frame duration
      waitUntil(dut.io.lrclk.toBoolean == true)
      waitUntil(dut.io.lrclk.toBoolean == false)
      val frameDuration = (simTime() - frameStart) / 10
      assert(frameDuration == 500, s"Full frame should be 500 cycles, got $frameDuration")
    }
  }
}