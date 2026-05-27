package synth.oscillator

import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite

class WaveformSim extends AnyFunSuite {
  test("Waveform Generator mathematical verification") {
    SimConfig.compile(new Generators).doSim { dut =>
      
      // Helper to check waveforms at a specific phase
      def check(phase: Long, expectedSaw: Int, expectedTri: Int, label: String): Unit = {
        dut.io.phase #= phase
        sleep(1) // Combinational settling
        
        val saw = dut.io.waves.saw.toInt
        val tri = dut.io.waves.tri.toInt
        
        println(f"[$label%-15s] Phase: 0x$phase%06X | Saw: $saw%6d | Tri: $tri%6d")
        assert(saw == expectedSaw, s"$label Saw mismatch")
        assert(tri == expectedTri, s"$label Tri mismatch")
      }

      println("Verifying Waveform Peaks and Zero-Crossings:")
      
      // Phase 0: Start of cycle
      // Saw starts at negative peak due to MSB flip.
      // Triangle starts at negative peak.
      check(0x000000, -32768, -32768, "Start (Phase 0)")

      // Quarter Cycle (0x400000)
      // Saw is at -16384 (midway through negative-to-zero ramp)
      // Triangle is at 0 (rising)
      check(0x400000, -16384, 0, "Quarter Cycle")

      // Half Cycle (0x800000)
      // Saw crosses zero
      // Triangle reaches positive peak (32767)
      check(0x800000, 0, 32767, "Half Cycle")

      // Three-Quarter Cycle (0xC00000)
      // Saw is at 16384
      // Triangle is back at -1 (falling)
      check(0xC00000, 16384, -1, "3/4 Cycle")
    }
  }
}