package synth.uart

import spinal.core._
import spinal.lib._
import synth.{RegisterWrite, OscillatorConfig}

class RegisterBank extends Component {

  val io = new Bundle {
    val regWrite = slave(Flow(RegisterWrite()))
    val config   = out(OscillatorConfig())
  }

  // --------------------------------------------------------------------------
  // Raw Register Storage
  // --------------------------------------------------------------------------

  val freqLowReg      = Reg(Bits(8 bits)) init(0)
  val freqMidReg      = Reg(Bits(8 bits)) init(0)
  val freqHighReg     = Reg(Bits(8 bits)) init(0)
  
  // Staging registers for atomic commitment
  val freqLowShadow   = Reg(Bits(8 bits)) init(0)
  val freqMidShadow   = Reg(Bits(8 bits)) init(0)

  val waveformReg     = Reg(Bits(8 bits)) init(0)
  val pulseWidthReg   = Reg(Bits(8 bits)) init(0)
  val volumeReg       = Reg(Bits(8 bits)) init(0)

  // --------------------------------------------------------------------------
  // Register Write Logic
  // --------------------------------------------------------------------------

  when(io.regWrite.valid) {

    switch(io.regWrite.payload.address) {

      // Frequency Low (Stage in shadow register)
      is(U"8'x00") {
        freqLowShadow := io.regWrite.payload.data
      }

      // Frequency Mid (Stage in shadow register)
      is(U"8'x01") {
        freqMidShadow := io.regWrite.payload.data
      }

      // Frequency High (Trigger simultaneous atomic commit of High, Mid, and Low)
      is(U"8'x02") {
        freqHighReg := io.regWrite.payload.data
        freqMidReg  := freqMidShadow
        freqLowReg  := freqLowShadow
      }

      // Waveform
      is(U"8'x03") {
        waveformReg := io.regWrite.payload.data
      }

      // Pulse Width
      is(U"8'x04") {
        pulseWidthReg := io.regWrite.payload.data
      }

      // Volume
      is(U"8'x05") {
        volumeReg := io.regWrite.payload.data
      }
    }
  }

  // --------------------------------------------------------------------------
  // Frequency Assembly
  // --------------------------------------------------------------------------

  val frequencyCombined =
    (freqHighReg ## freqMidReg ## freqLowReg).asUInt

  // --------------------------------------------------------------------------
  // One-Cycle Synchronization Stage
  // --------------------------------------------------------------------------

  val oscFrequencyReg =
    RegNext(frequencyCombined) init(0)

  val oscWaveformReg =
    RegNext(waveformReg.asUInt) init(0)

  val oscPulseWidthReg =
    RegNext(pulseWidthReg.asUInt) init(0)

  val oscVolumeReg =
    RegNext(volumeReg.asUInt) init(0)

  // --------------------------------------------------------------------------
  // Outputs
  // --------------------------------------------------------------------------

  io.config.freqWord   := oscFrequencyReg
  io.config.waveSelect := oscWaveformReg(2 downto 0)
  io.config.pwmWidth   := oscPulseWidthReg
  io.config.volume     := oscVolumeReg
}