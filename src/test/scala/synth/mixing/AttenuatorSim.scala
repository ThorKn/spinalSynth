package synth.mixing

import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite

class AttenuatorSim extends AnyFunSuite {
  test("Attenuator mathematical scaling and pipeline latency") {
    SimConfig.withWave.compile(new Attenuator).doSim { dut =>
      // 1. Fork the clock and reset using standard forkStimulus
      dut.clockDomain.forkStimulus(period = 10)
      
      // 2. Drive active values and check outputs during initial reset (Section 1.1.1)
      println("Verifying Reset Stability during power-on reset:")
      dut.io.sampleIn.valid #= true
      dut.io.sampleIn.payload #= 20000
      dut.io.volume #= 255
      
      // Advance simulation time by 50 units (5 cycles) while reset is active
      sleep(50)
      
      // Assert outputs are strictly quiet/zeroed during reset
      assert(dut.io.sampleOut.payload.toInt == 0, "Output payload must be held at 0 during reset")
      assert(dut.io.sampleOut.valid.toBoolean == false, "Output valid must be held at False during reset")
      
      // 3. Clear inputs and wait for power-on reset to finish deasserting and stabilize
      dut.io.sampleIn.valid #= false
      dut.io.sampleIn.payload #= 0
      dut.io.volume #= 0
      dut.clockDomain.waitSampling(100)
      println("Reset Stability verified successfully.")

      // Helper to check standard mathematical scaling
      def checkScaling(sample: Int, volume: Int, expected: Int): Unit = {
        dut.io.sampleIn.valid #= true
        dut.io.sampleIn.payload #= sample
        dut.io.volume #= volume
        
        dut.clockDomain.waitSampling()
        dut.io.sampleIn.valid #= false
        
        dut.clockDomain.waitSampling()
        // Assert output on the very next cycle (1-cycle pipeline latency)
        assert(dut.io.sampleOut.valid.toBoolean == true, s"Expected output to be valid")
        assert(dut.io.sampleOut.payload.toInt == expected, s"Expected $expected, got ${dut.io.sampleOut.payload.toInt}")
        
        dut.clockDomain.waitSampling()
        assert(dut.io.sampleOut.valid.toBoolean == false, s"Expected output valid to drop to false")
      }

      println("Verifying Attenuator Test Vectors:")
      // Test cases from Testing_specs.md
      checkScaling(20000, 255, 19921)
      checkScaling(20000, 128, 10000)
      checkScaling(-20000, 64, -5000)
      checkScaling(-32768, 0, 0)
      println("Test Vectors passed successfully.")

      // Pipelining throughput test (3 consecutive back-to-back samples)
      println("Verifying Pipelined Throughput (Back-to-Back):")
      
      // Cycle 1: Sample 10000, Vol 255
      dut.io.sampleIn.valid #= true
      dut.io.sampleIn.payload #= 10000
      dut.io.volume #= 255
      dut.clockDomain.waitSampling() // Sample 1 is latched. Output 1 is NOT ready yet.
      assert(dut.io.sampleOut.valid.toBoolean == false)

      // Cycle 2: Sample 20000, Vol 128
      dut.io.sampleIn.payload #= 20000
      dut.io.volume #= 128
      dut.clockDomain.waitSampling() // Sample 2 is latched. Output 1 is READY now!
      assert(dut.io.sampleOut.valid.toBoolean == true)
      assert(dut.io.sampleOut.payload.toInt == 9960)

      // Cycle 3: Sample -10000, Vol 64
      dut.io.sampleIn.payload #= -10000
      dut.io.volume #= 64
      dut.clockDomain.waitSampling() // Sample 3 is latched. Output 2 is READY now!
      assert(dut.io.sampleOut.valid.toBoolean == true)
      assert(dut.io.sampleOut.payload.toInt == 10000)

      // Cycle 4: Idle
      dut.io.sampleIn.valid #= false
      dut.clockDomain.waitSampling() // Output 3 is READY now!
      assert(dut.io.sampleOut.valid.toBoolean == true)
      assert(dut.io.sampleOut.payload.toInt == -2500)

      // Cycle 5: Idle
      dut.clockDomain.waitSampling() // Pipeline is empty!
      assert(dut.io.sampleOut.valid.toBoolean == false)

      println("Pipelined Throughput verified successfully.")
    }
  }
}
