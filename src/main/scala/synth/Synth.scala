package synth

import spinal.core._
import spinal.lib._

import synth.uart._
import synth.oscillator.Oscillator
import synth.output._

class Synth extends Component {

  val io = new Bundle {
    val clk24MHz = in Bool()
    val reset    = in Bool()

    val uartRx   = in Bool()

    val i2sBclk  = out Bool()
    val i2sLrclk = out Bool()
    val i2sData  = out Bool()
  }

  // Map the external clk and reset pins to the internal ClockDomain logic.
  // This allows the submodules to use standard SpinalHDL registers.
  val coreClockDomain = ClockDomain(
    clock = io.clk24MHz,
    reset = io.reset,
    config = ClockDomainConfig(
      resetKind = ASYNC,
      resetActiveLevel = HIGH
    )
  )

  // System Integration Area
  val core = new ClockingArea(coreClockDomain) {

    // UART and RegisterBank Modules
    val uartRxModule      = new UartRx()
    val protocolDecoder   = new UartProtocolDecoder()
    val registerBank      = new RegisterBank()
    
    // Synthesis and Output Modules
    val timingGen         = new TimingGenerator()
    val oscillator        = new Oscillator()
    val decimator         = new Decimator()
    val transmitter       = new I2STransmitter()

    // --- UART Control Path ---
    uartRxModule.io.rx             := io.uartRx
    protocolDecoder.io.rxData      := uartRxModule.io.data
    protocolDecoder.io.rxDataValid := uartRxModule.io.dataValid
    registerBank.io.writeEnable    := protocolDecoder.io.writeEnable
    registerBank.io.writeAddress   := protocolDecoder.io.writeAddress
    registerBank.io.writeData      := protocolDecoder.io.writeData

    // --- Synthesis Engine Wiring ---

    // 1. Tick Distribution
    oscillator.io.phaseTick        := timingGen.io.phaseTick
    decimator.io.sampleTick        := timingGen.io.sampleTick

    // 2. Control Signals (Register Bank -> Oscillator)
    oscillator.io.freqWord         := registerBank.io.oscFrequency
    oscillator.io.waveSelect       := registerBank.io.oscWaveform(2 downto 0)
    oscillator.io.pwmWidth         := registerBank.io.oscPulseWidth

    // 3. Audio Data Path
    // Oscillator (480kHz) -> Decimator -> I2S Transmitter (48kHz)
    oscillator.io.sample           >> decimator.io.sampleIn
    decimator.io.sampleOut         >> transmitter.io.sampleIn

    // --- External Output Mapping ---
    io.i2sBclk  := transmitter.io.bclk
    io.i2sLrclk := transmitter.io.lrclk
    io.i2sData  := transmitter.io.sdata
  }
}
