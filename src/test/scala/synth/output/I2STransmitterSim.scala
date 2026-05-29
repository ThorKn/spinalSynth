package synth.output

import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite

class I2STransmitterSim extends AnyFunSuite {
  test("I2S Transmitter pattern timing and serialization verification") {
    SimConfig.withWave.compile(new I2STransmitter).doSim { dut =>
      // 1. Initialize clock and assert reset
      dut.clockDomain.forkStimulus(period = 10)
      dut.io.sampleIn.valid #= false
      dut.io.sampleIn.payload #= 0
      dut.clockDomain.assertReset()

      // Test Case 1: Reset Stability & Inactive Idle Verification
      println("Verifying Reset Stability and Inactive Idle:")
      for (_ <- 0 until 20) {
        dut.clockDomain.waitSampling()
        assert(!dut.io.bclk.toBoolean, "bclk must remain False during reset")
        assert(dut.io.lrclk.toBoolean, "lrclk must remain True (idle WS state) during reset")
        assert(!dut.io.sdata.toBoolean, "sdata must remain False during reset")
      }
      dut.clockDomain.deassertReset()

      // Verify that it remains idle after deasserting reset, before first valid pulse
      for (_ <- 0 until 20) {
        dut.clockDomain.waitSampling()
        assert(!dut.io.bclk.toBoolean, "bclk must remain False while inactive")
        assert(dut.io.lrclk.toBoolean, "lrclk must remain True (idle WS state) while inactive")
        assert(!dut.io.sdata.toBoolean, "sdata must remain False while inactive")
      }
      println("Reset stability and inactive idle states verified successfully.")

      // Test Case 2: Bit-by-Bit Serialization Verification
      println("Verifying dynamic serialization:")
      
      // We will feed the test sample 0xA5A5 (binary 1010 0101 1010 0101)
      val testSample = 0xA5A5
      dut.io.sampleIn.payload #= testSample.toShort.toInt
      dut.io.sampleIn.valid #= true
      dut.clockDomain.waitSampling()
      dut.io.sampleIn.valid #= false

      // Wait for the exact start of Left-channel serialization (lrclk goes Low)
      dut.clockDomain.waitSamplingWhere(!dut.io.lrclk.toBoolean)

      // The serialization outputs bits MSB first (bit 15 to 0)
      for (bitIndex <- 15 to 0 by -1) {
        // Wait until BCLK goes High (middle of the bit interval) to sample the data cleanly
        dut.clockDomain.waitSamplingWhere(dut.io.bclk.toBoolean)
        
        val expectedBit = (testSample >> bitIndex) & 1
        val actualBit = if (dut.io.sdata.toBoolean) 1 else 0
        
        println(f" [Bit $bitIndex%2d] Expected: $expectedBit | Actual: $actualBit")
        assert(actualBit == expectedBit, s"Serialization mismatch at bit index $bitIndex")
        
        // Wait for BCLK to go Low to complete this bit cycle
        dut.clockDomain.waitSamplingWhere(!dut.io.bclk.toBoolean)
      }
      println("Serialization verified perfectly for all 16 bits of 0xA5A5.")

      // Test Case 3: Exact Cycle-Accurate Timing Pattern Table
      println("Verifying bit timing interval pattern:")

      // Trigger gated startup again to ensure full cycle alignment
      dut.io.sampleIn.valid #= true
      dut.clockDomain.waitSampling()
      dut.io.sampleIn.valid #= false

      // Wait for the start of Left channel frame
      dut.clockDomain.waitSamplingWhere(!dut.io.lrclk.toBoolean)

      // Helper to measure and assert a single bit-clock period (falling edge to falling edge)
      def checkBitInterval(expectedCycles: Int, bitLabel: String): Unit = {
        val start = simTime()
        dut.clockDomain.waitSamplingWhere(dut.io.bclk.toBoolean)
        dut.clockDomain.waitSamplingWhere(!dut.io.bclk.toBoolean)
        dut.clockDomain.waitSamplingWhere(dut.io.bclk.toBoolean)
        val stop = simTime()
        
        val interval = (stop - start) / 10
        println(f" [$bitLabel%-6s] Expected cycles: $expectedCycles | Actual: $interval")
        assert(interval == expectedCycles, s"Timing pattern error at $bitLabel: expected $expectedCycles cycles, got $interval")
      }

      // Verify the 8-step cycles per bit pattern: 16, 16, 15, 16, 16, 15, 16, 15
      checkBitInterval(16, "Bit 0")
      checkBitInterval(16, "Bit 1")
      checkBitInterval(15, "Bit 2")
      checkBitInterval(16, "Bit 3")
      checkBitInterval(16, "Bit 4")
      checkBitInterval(15, "Bit 5")
      checkBitInterval(16, "Bit 6")
      checkBitInterval(15, "Bit 7")

      // Verify full frame duration (Left + Right channel complete period = 500 cycles)
      dut.clockDomain.waitSamplingWhere(dut.io.lrclk.toBoolean)
      dut.clockDomain.waitSamplingWhere(!dut.io.lrclk.toBoolean)
      val frameStart = simTime()

      dut.clockDomain.waitSamplingWhere(dut.io.lrclk.toBoolean)
      dut.clockDomain.waitSamplingWhere(!dut.io.lrclk.toBoolean)
      val frameDuration = (simTime() - frameStart) / 10
      println(s"Full frame period: $frameDuration cycles")
      assert(frameDuration == 500, s"Full frame timing error: expected 500 cycles, got $frameDuration")
      
      println("Timing patterns and full frame bounds verified successfully.")
    }
  }
}