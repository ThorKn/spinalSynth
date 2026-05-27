package synth.mixing

import spinal.core._
import spinal.lib._

class Attenuator extends Component {
  val io = new Bundle {
    val sampleIn  = slave(Flow(SInt(16 bits)))
    val volume    = in UInt(8 bits)
    val sampleOut = master(Flow(SInt(16 bits)))
  }

  // 1. Convert 8-bit unsigned volume to a 9-bit signed integer
  val volumeSigned = io.volume.intoSInt

  // 2. Multiplier (16-bit signed * 9-bit signed = 25-bit signed product)
  val product = io.sampleIn.payload * volumeSigned

  // 3. Scale down (divide by 256) and resize to 16 bits
  val scaledSample = (product >> 8).resize(16 bits)

  // 4. Output registers for high Fmax clock performance (1-cycle latency)
  io.sampleOut.payload := RegNext(scaledSample) init(0)
  io.sampleOut.valid   := RegNext(io.sampleIn.valid) init(False)
}
