package synth

import spinal.core._
import spinal.lib._

import synth.uart._

class UartTop extends Component {

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

    val uartRxModule      = new UartRx()
    val protocolDecoder   = new UartProtocolDecoder()
    val registerBank      = new RegisterBank()
    val oscillator        = new OscillatorTop()

    uartRxModule.io.rx := io.uartRx

    protocolDecoder.io.rxData      := uartRxModule.io.data
    protocolDecoder.io.rxDataValid := uartRxModule.io.dataValid

    registerBank.io.writeEnable  := protocolDecoder.io.writeEnable
    registerBank.io.writeAddress := protocolDecoder.io.writeAddress
    registerBank.io.writeData    := protocolDecoder.io.writeData

    // Direct mapping from RegisterBank to OscillatorTop
    oscillator.io.freqWord   := registerBank.io.oscFrequency
    oscillator.io.waveSelect := registerBank.io.oscWaveform(2 downto 0)
    oscillator.io.pwmWidth   := registerBank.io.oscPulseWidth
    // Note: registerBank.io.oscVolume is currently unused by OscillatorTop

    // Map I2S signals to external pins
    io.i2sBclk  := oscillator.io.i2s_bclk
    io.i2sLrclk := oscillator.io.i2s_lrclk
    io.i2sData  := oscillator.io.i2s_sdata
  }
}