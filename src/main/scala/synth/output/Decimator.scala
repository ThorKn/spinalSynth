package synth.output

import spinal.core._
import spinal.lib._

class Decimator extends Component {
  val io = new Bundle {
    val phaseTick  = in Bool()
    val sampleTick = in Bool()
    val sampleIn   = in SInt(16 bits)
    val sampleOut  = out SInt(16 bits) 
    val valid      = out Bool()
  }

  // Register to hold the decimated sample at 48 kHz.
  // By latching only when sampleTick is high, we achieve 10x downsampling.
  val sampleReg = Reg(SInt(16 bits)) init(0)

  when(io.sampleTick) {
    sampleReg := io.sampleIn
  }

  io.sampleOut := sampleReg

  // The valid pulse indicates that a new 48 kHz sample is available in sampleOut.
  // We register the sampleTick so that 'valid' is high exactly when the data is ready.
  io.valid := RegNext(io.sampleTick) init(False)
}