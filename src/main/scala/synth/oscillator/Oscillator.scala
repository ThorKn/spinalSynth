package synth.oscillator

import spinal.core._
import spinal.lib._

class Oscillator extends Component {
  val io = new Bundle {
    val phaseTick  = in Bool()
    val freqWord   = in UInt(24 bits)
    val waveSelect = in UInt(3 bits)
    val pwmWidth   = in UInt(8 bits)
    val sample     = master(Flow(SInt(16 bits)))
  }

  // Instantiate submodules within the same package
  val accumulator = new Accumulator()
  val generators  = new Generators()
  val noise       = new Noise()
  val mux         = new Mux()

  // Wire up the Accumulator: Phase advances only on phaseTick (480 kHz)
  accumulator.io.phaseTick := io.phaseTick
  accumulator.io.freqWord  := io.freqWord

  // Wire up the Generators: Purely combinational transformation of phase
  generators.io.phase    := accumulator.io.phase
  generators.io.pwmWidth := io.pwmWidth

  // Wire up the Noise source: Updates its LFSR state on phaseTick (480 kHz)
  noise.io.phaseTick := io.phaseTick

  // Wire up the Mux for waveform selection
  mux.io.waveSelect := io.waveSelect
  mux.io.sawWave    := generators.io.sawWave
  mux.io.squareWave := generators.io.squareWave
  mux.io.pwmWave    := generators.io.pwmWave
  mux.io.triWave    := generators.io.triWave
  mux.io.noiseWave  := noise.io.sample

  // Packaging the output into a Flow. 
  // The sample is valid only during the phaseTick (480 kHz heartbeat).
  io.sample.valid   := io.phaseTick
  io.sample.payload := mux.io.sample
}