package synth

import spinal.core._
import spinal.lib._

import synth.uart._

class UartTop extends Component {

  val io = new Bundle {

    // ------------------------------------------------------------------------
    // System Clock and Reset
    // ------------------------------------------------------------------------

    val clk24MHz = in Bool()
    val reset    = in Bool()

    // ------------------------------------------------------------------------
    // UART Interface
    // ------------------------------------------------------------------------

    val uartRx   = in Bool()

    // ------------------------------------------------------------------------
    // I2S Outputs
    // ------------------------------------------------------------------------

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
    val oscillatorWrapper = new OscillatorWrapper()

    uartRxModule.io.rx := io.uartRx

    protocolDecoder.io.rxData      := uartRxModule.io.data
    protocolDecoder.io.rxDataValid := uartRxModule.io.dataValid

    registerBank.io.writeEnable  := protocolDecoder.io.writeEnable
    registerBank.io.writeAddress := protocolDecoder.io.writeAddress
    registerBank.io.writeData    := protocolDecoder.io.writeData

    oscillatorWrapper.io.oscFrequency  := registerBank.io.oscFrequency
    oscillatorWrapper.io.oscWaveform   := registerBank.io.oscWaveform
    oscillatorWrapper.io.oscPulseWidth := registerBank.io.oscPulseWidth
    oscillatorWrapper.io.oscVolume     := registerBank.io.oscVolume

    io.i2sBclk  := oscillatorWrapper.io.i2s_bclk
    io.i2sLrclk := oscillatorWrapper.io.i2s_lrclk
    io.i2sData  := oscillatorWrapper.io.i2s_sdata
  }
}