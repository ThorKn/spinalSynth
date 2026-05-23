package oscillator.components

import spinal.core._
import spinal.lib._

class Mux extends Component {
  val io = new Bundle {
    val waveSelect = in UInt(3 bits)
    val sawWave    = in SInt(16 bits)
    val squareWave = in SInt(16 bits)
    val pwmWave    = in SInt(16 bits)
    val triWave    = in SInt(16 bits)
    val noiseWave  = in SInt(16 bits)
    val sample     = out SInt(16 bits)
  }

  // Combinational selection logic based on the 3-bit waveSelect input.
  // Using a switch statement ensures clarity and priority-free multiplexing.
  switch(io.waveSelect) {
    is(0)   { io.sample := io.sawWave }
    is(1)   { io.sample := io.squareWave }
    is(2)   { io.sample := io.pwmWave }
    is(3)   { io.sample := io.triWave }
    is(4)   { io.sample := io.noiseWave }
    // Default case for safety: Output silence if an undefined index is selected.
    default { io.sample := S(0, 16 bits) }
  }
}