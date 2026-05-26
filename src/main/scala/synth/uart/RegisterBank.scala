package synth.uart

import spinal.core._
import spinal.lib._

class RegisterBank extends Component {

  val io = new Bundle {

    val writeEnable  = in Bool()
    val writeAddress = in UInt(8 bits)
    val writeData    = in Bits(8 bits)

    val oscFrequency  = out UInt(24 bits)
    val oscWaveform   = out UInt(8 bits)
    val oscPulseWidth = out UInt(8 bits)
    val oscVolume     = out UInt(8 bits)
  }

  // --------------------------------------------------------------------------
  // Raw Register Storage
  // --------------------------------------------------------------------------

  val freqLowReg    = Reg(Bits(8 bits)) init(0)
  val freqMidReg    = Reg(Bits(8 bits)) init(0)
  val freqHighReg   = Reg(Bits(8 bits)) init(0)

  val waveformReg   = Reg(Bits(8 bits)) init(0)
  val pulseWidthReg = Reg(Bits(8 bits)) init(0)
  val volumeReg     = Reg(Bits(8 bits)) init(0)

  // --------------------------------------------------------------------------
  // Register Write Logic
  // --------------------------------------------------------------------------

  when(io.writeEnable) {

    switch(io.writeAddress) {

      // Frequency Low
      is(U"8'x00") {
        freqLowReg := io.writeData
      }

      // Frequency Mid
      is(U"8'x01") {
        freqMidReg := io.writeData
      }

      // Frequency High
      is(U"8'x02") {
        freqHighReg := io.writeData
      }

      // Waveform
      is(U"8'x03") {
        waveformReg := io.writeData
      }

      // Pulse Width
      is(U"8'x04") {
        pulseWidthReg := io.writeData
      }

      // Volume
      is(U"8'x05") {
        volumeReg := io.writeData
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

  io.oscFrequency  := oscFrequencyReg
  io.oscWaveform   := oscWaveformReg
  io.oscPulseWidth := oscPulseWidthReg
  io.oscVolume     := oscVolumeReg
}