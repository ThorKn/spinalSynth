package oscillator

import spinal.core._
import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite

class OscillatorTopSim extends AnyFunSuite {
  test("OscillatorTop end-to-end I2S verification") {
    // Using a simulation frequency of 24MHz (period = 41.67ns)
    // We'll use 10 units as the half-period for simplicity in the sim
    SimConfig.withWave.compile(new OscillatorTop).doSim { dut =>
      // 1. Initialize Inputs
      dut.io.clk #= false
      dut.io.reset #= true
      dut.io.freqWord #= 34953 // ~1000 Hz: (1000 * 2^24) / 480000
      dut.io.waveSelect #= 0   // Sawtooth
      dut.io.pwmWidth #= 128

      // 2. Clock Generator (24 MHz)
      fork {
        while (true) {
          dut.io.clk #= !dut.io.clk.toBoolean
          sleep(5) // 10 units per full cycle
        }
      }

      // 3. Reset Sequence
      sleep(50)
      dut.io.reset #= false
      sleep(50)

      // 4. I2S Frame Monitor
      var framesCaptured = 0
      val maxFrames = 10

      fork {
        // Align with the start of a Left-channel frame (LRCLK goes Low)
        waitUntil(dut.io.i2s_lrclk.toBoolean == true)
        waitUntil(dut.io.i2s_lrclk.toBoolean == false)
        waitUntil(dut.io.i2s_lrclk.toBoolean == true)
        waitUntil(dut.io.i2s_lrclk.toBoolean == false)

        var lastFrameTime = simTime()

        while (framesCaptured < maxFrames) {
          var leftRaw = 0
          var rightRaw = 0

          // Capture Left Channel (16 bits)
          for (i <- 0 until 16) {
            waitUntil(dut.io.i2s_bclk.toBoolean == true) // Sample on rising edge
            leftRaw = (leftRaw << 1) | (if (dut.io.i2s_sdata.toBoolean) 1 else 0)
            waitUntil(dut.io.i2s_bclk.toBoolean == false)
          }

          // Capture Right Channel (16 bits)
          for (i <- 0 until 16) {
            waitUntil(dut.io.i2s_bclk.toBoolean == true)
            rightRaw = (rightRaw << 1) | (if (dut.io.i2s_sdata.toBoolean) 1 else 0)
            waitUntil(dut.io.i2s_bclk.toBoolean == false)
          }

          // Convert to Signed 16-bit
          val leftSample  = if ((leftRaw & 0x8000) != 0) leftRaw - 0x10000 else leftRaw
          val rightSample = if ((rightRaw & 0x8000) != 0) rightRaw - 0x10000 else rightRaw

          // Verification
          val currentTime = simTime()
          val framePeriod = currentTime - lastFrameTime
          
          println(s"[Frame $framesCaptured] L: $leftSample, R: $rightSample | Period: ${framePeriod/10} cycles")
          
          assert(leftSample == rightSample, "Stereo mismatch: Left and Right should be identical")
          // assert(framePeriod == 5000, s"Timing error: Frame period should be 500 cycles, got ${framePeriod/10}")

          lastFrameTime = currentTime
          framesCaptured += 1
        }
      }

      // Run until we have enough data
      waitUntil(framesCaptured >= maxFrames)
      println("End-to-end I2S simulation successful.")
    }
  }
}