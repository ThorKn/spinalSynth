package synth

import spinal.core._
import spinal.lib._

class I2STransmitter extends Component {
  val io = new Bundle {
    val sampleIn = in SInt(16 bits)
    val valid    = in Bool()
    val bclk     = out Bool()
    val lrclk    = out Bool()
    val sdata    = out Bool()
  }

  // Timing Pattern Table: 16, 16, 15, 16, 16, 15, 16, 15
  // This sequence defines the number of master clock cycles per I2S bit.
  // Sum of sequence = 125. 125 * 4 = 500 cycles per 32-bit frame (48 kHz).
  val patternTable = Vec(U(16, 5 bits), U(16, 5 bits), U(15, 5 bits), U(16, 5 bits),
                         U(16, 5 bits), U(15, 5 bits), U(16, 5 bits), U(15, 5 bits))

  val cycleCounter = Reg(UInt(5 bits)) init(15)
  val patternIndex = Reg(UInt(3 bits)) init(0)
  val bitCounter   = Reg(UInt(5 bits)) init(0)
  val shiftReg     = Reg(UInt(16 bits)) init(0)
  val sampleBuffer = Reg(SInt(16 bits)) init(0)
  val active       = Reg(Bool()) init(False)

  // State machine logic operating at 24 MHz
  // The 'valid' pulse from the Decimator is used as a master synchronization signal
  // to ensure the I2S frame starts exactly when a new sample is ready.
  when(io.valid) {
    // Synchronize frame: Start of Left channel (bit 0)
    sampleBuffer := io.sampleIn
    bitCounter   := 0
    patternIndex := 0
    cycleCounter := patternTable(0) - 1 // Start counting down first bit duration
    shiftReg     := io.sampleIn.asUInt  // Load MSB immediately for serialization
    active       := True
  } elsewhen(active) {
    // Standard serialization state machine
    when(cycleCounter === 0) {
      // Bit Boundary: Occurs every 15 or 16 cycles as defined by the subpattern.
      
      val nextPatternIndex = (patternIndex + 1).resize(3)
      val nextBit = (bitCounter + 1).resize(5)

      patternIndex := nextPatternIndex
      cycleCounter := patternTable(nextPatternIndex) - 1
      bitCounter   := nextBit

      // Serialization: Load buffer into shift register at channel start (bits 0 and 16).
      // Otherwise, shift out the next bit.
      when(nextBit === 0 || nextBit === 16) {
        shiftReg := sampleBuffer.asUInt
      } otherwise {
        shiftReg := (shiftReg << 1).resize(16)
      }
    } otherwise {
      cycleCounter := cycleCounter - 1
    }
  }

  // I2S Signal Assignments

  // BCLK: Approx 50% duty cycle bit clock. High for the first half of the interval.
  // Held low while inactive.
  io.bclk := active && (cycleCounter >= 8)

  // LRCLK: Word Select. Low for Left (0-15), High for Right (16-31).
  // Held High while inactive (standard idle state for I2S).
  io.lrclk := !active || (bitCounter >= 16)

  // SDATA: Serial Data output (MSB first).
  // Held low while inactive.
  io.sdata := active && shiftReg(15)
}