package synth.common

import spinal.core._

// Represents an atomic register write transaction
case class RegisterWrite() extends Bundle {
  val address = UInt(8 bits)
  val data    = Bits(8 bits)
}

// Unified configuration bundle sent from Register Bank to the Oscillator
case class OscillatorConfig() extends Bundle {
  val freqWord   = UInt(24 bits)
  val waveSelect = UInt(3 bits)
  val pwmWidth   = UInt(8 bits)
  val volume     = UInt(8 bits)
}

// Unified waveforms bundle sent from Generators to Mux
case class Waveforms() extends Bundle {
  val saw    = SInt(16 bits)
  val square = SInt(16 bits)
  val pwm    = SInt(16 bits)
  val tri    = SInt(16 bits)
}
