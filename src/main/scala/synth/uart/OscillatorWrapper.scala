package synth.uart

import spinal.core._
import spinal.lib._

import synth.OscillatorTop

class OscillatorWrapper extends Component {

  val io = new Bundle {

    // ------------------------------------------------------------------------
    // Control Signals from Register Bank
    // ------------------------------------------------------------------------

    val oscFrequency  = in UInt(24 bits)
    val oscWaveform   = in UInt(8 bits)
    val oscPulseWidth = in UInt(8 bits)
    val oscVolume     = in UInt(8 bits)

    // ------------------------------------------------------------------------
    // I2S Outputs
    // ------------------------------------------------------------------------

    val i2s_bclk  = out Bool()
    val i2s_lrclk = out Bool()
    val i2s_sdata = out Bool()
  }

  // --------------------------------------------------------------------------
  // Oscillator Core Instance
  // --------------------------------------------------------------------------

  val oscillatorCore = new OscillatorTop()

  // --------------------------------------------------------------------------
  // Control Signal Routing
  // --------------------------------------------------------------------------

  oscillatorCore.io.freqWord := io.oscFrequency

  // OscillatorTop expects 3-bit waveform select
  oscillatorCore.io.waveSelect := io.oscWaveform(2 downto 0)

  oscillatorCore.io.pwmWidth := io.oscPulseWidth

  // --------------------------------------------------------------------------
  // Volume Register Handling
  // --------------------------------------------------------------------------

  // Currently unused by OscillatorTop.
  // Reserved for future expansion.
  val unusedVolume = io.oscVolume

  // --------------------------------------------------------------------------
  // I2S Output Routing
  // --------------------------------------------------------------------------

  io.i2s_bclk  := oscillatorCore.io.i2s_bclk
  io.i2s_lrclk := oscillatorCore.io.i2s_lrclk
  io.i2s_sdata := oscillatorCore.io.i2s_sdata
}