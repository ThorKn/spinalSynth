package oscillator

import spinal.core._
import spinal.lib._

class OscillatorTop extends Component {
  val io = new Bundle {
    // External Clock and Reset as defined in Specification Section 2
    val clk        = in Bool()
    val reset      = in Bool()

    // Control Interface
    val freqWord   = in UInt(24 bits)
    val waveSelect = in UInt(3 bits)
    val pwmWidth   = in UInt(8 bits)

    // I2S Interface
    val i2s_bclk   = out Bool()
    val i2s_lrclk  = out Bool()
    val i2s_sdata  = out Bool()
  }

  // Map the external clk and reset pins to the internal ClockDomain logic.
  // This allows the submodules to use standard SpinalHDL registers.
  val coreClockDomain = ClockDomain(
    clock = io.clk,
    reset = io.reset,
    config = ClockDomainConfig(
      resetKind = ASYNC,
      resetActiveLevel = HIGH
    )
  )

  // System Integration Area
  val core = new ClockingArea(coreClockDomain) {
    val timingGen   = new TimingGenerator()
    val oscillator  = new Oscillator()
    val decimator   = new Decimator()
    val transmitter = new I2STransmitter()

    // 1. Timing Distribution
    oscillator.io.phaseTick := timingGen.io.phaseTick
    decimator.io.phaseTick  := timingGen.io.phaseTick
    decimator.io.sampleTick := timingGen.io.sampleTick

    // 2. Control Signal Routing
    oscillator.io.freqWord   := io.freqWord
    oscillator.io.waveSelect := io.waveSelect
    oscillator.io.pwmWidth   := io.pwmWidth

    // 3. Audio Data Path: Oscillator (480kHz) -> Decimator -> Transmitter (48kHz)
    decimator.io.sampleIn    := oscillator.io.sample

    transmitter.io.sampleIn  := decimator.io.sampleOut
    transmitter.io.valid     := decimator.io.valid

    // 4. External I2S Output Mapping
    io.i2s_bclk  := transmitter.io.bclk
    io.i2s_lrclk := transmitter.io.lrclk
    io.i2s_sdata := transmitter.io.sdata
  }
}