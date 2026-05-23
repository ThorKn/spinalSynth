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

      dut.clockDomain.waitSamplingWhere(dut.io.lrclk.toBoolean == true)
      dut.clockDomain.waitSamplingWhere(dut.io.lrclk.toBoolean == false)

      // Verify the first few bit intervals (Expected: 16, 16, 15...)
      def checkBitInterval(expectedCycles: Int) {
        val start = simTime()
        // Wait for BCLK rising edge (middle of bit) then falling (end of bit)
        dut.clockDomain.waitSamplingWhere(dut.io.bclk.toBoolean == true)
        dut.clockDomain.waitSamplingWhere(dut.io.bclk.toBoolean == false)
        dut.clockDomain.waitSamplingWhere(dut.io.bclk.toBoolean == true)
        val stop = simTime()
        print("start:", start)
        print("stop:", stop)
        val interval = (stop - start) / 10
        assert(interval == expectedCycles, s"Expected $expectedCycles cycles for bit, got $interval")
      }

      checkBitInterval(16)
      checkBitInterval(16)
      checkBitInterval(15)
      checkBitInterval(16)

      // Verify full frame duration
       // Align to LRCLK transition (start of frame)
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