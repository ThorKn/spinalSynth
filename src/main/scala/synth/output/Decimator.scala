package synth.output

import spinal.core._
import spinal.lib._

class Decimator extends Component {
  val io = new Bundle {
    val sampleTick = in Bool()
    val sampleIn   = slave(Flow(SInt(16 bits)))
    val sampleOut  = master(Flow(SInt(16 bits)))
  }

  // Register to hold the decimated sample at 48 kHz.
  // By latching only when sampleTick is high, we achieve 10x downsampling.
  val sampleReg = Reg(SInt(16 bits)) init(0)

  when(io.sampleIn.valid && io.sampleTick) {
    sampleReg := io.sampleIn.payload
  }

  io.sampleOut.payload := sampleReg

  // The valid pulse indicates that a new 48 kHz sample is available in sampleOut.
  // We register the gated tick so that 'valid' is high exactly when the data is ready in the register.
  io.sampleOut.valid := RegNext(io.sampleIn.valid && io.sampleTick) init(False)
}