package synth

import spinal.core._
import spinal.lib._

class TimingGenerator extends Component {
  val io = new Bundle {
    val phaseTick  = out Bool()
    val sampleTick = out Bool()
  }

  // phaseCounter: modulo-50 (24MHz / 50 = 480kHz)
  // SpinalHDL Counter(50) will automatically use a 6-bit UInt
  val phaseCounter = Counter(50)
  phaseCounter.increment()

  // sampleCounter: modulo-500 (24MHz / 500 = 48kHz)
  // SpinalHDL Counter(500) will automatically use a 9-bit UInt
  val sampleCounter = Counter(500)
  sampleCounter.increment()

  // Ticks are registered to ensure they are synchronous, one cycle wide,
  // and provide a clean timing boundary for downstream logic.
  // They default to False via the init(False) on the register.
  io.phaseTick  := RegNext(phaseCounter.willOverflow) init(False)
  io.sampleTick := RegNext(sampleCounter.willOverflow) init(False)
}