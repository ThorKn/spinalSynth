package synth.uart

import spinal.core._
import spinal.lib._

class UartRx extends Component {

  val io = new Bundle {
    val rx        = in Bool()
    val data      = out Bits(8 bits)
    val dataValid = out Bool()
  }

  // --------------------------------------------------------------------------
  // UART Timing Constants
  // --------------------------------------------------------------------------

  val CLOCK_FREQ     = 24000000
  val BAUD_RATE      = 115200
  val BIT_TICKS      = 208
  val HALF_BIT_TICKS = 104

  // --------------------------------------------------------------------------
  // FSM State Definition
  // --------------------------------------------------------------------------

  object State extends SpinalEnum {
    val IDLE, START, DATA, STOP, DONE = newElement()
  }

  val state = Reg(State()) init(State.IDLE)

  // --------------------------------------------------------------------------
  // Internal Registers
  // --------------------------------------------------------------------------

  val baudCounter  = Reg(UInt(8 bits)) init(0)
  val bitCounter   = Reg(UInt(3 bits)) init(0)

  val shiftReg     = Reg(Bits(8 bits)) init(0)

  val dataReg      = Reg(Bits(8 bits)) init(0)
  val dataValidReg = Reg(Bool()) init(False)

  // --------------------------------------------------------------------------
  // Default Outputs
  // --------------------------------------------------------------------------

  io.data      := dataReg
  io.dataValid := dataValidReg

  // dataValid is a one-clock pulse
  dataValidReg := False

  // --------------------------------------------------------------------------
  // FSM
  // --------------------------------------------------------------------------

  switch(state) {

    // ------------------------------------------------------------------------
    // IDLE
    // ------------------------------------------------------------------------

    is(State.IDLE) {

      baudCounter := 0
      bitCounter  := 0

      when(io.rx === False) {
        state := State.START
      }
    }

    // ------------------------------------------------------------------------
    // START BIT ALIGNMENT
    // ------------------------------------------------------------------------

    is(State.START) {

      when(baudCounter === HALF_BIT_TICKS - 1) {

        baudCounter := 0

        // Verify start bit still valid
        when(io.rx === False) {
          state := State.DATA
        } otherwise {
          state := State.IDLE
        }

      } otherwise {
        baudCounter := baudCounter + 1
      }
    }

    // ------------------------------------------------------------------------
    // DATA BITS
    // ------------------------------------------------------------------------

    is(State.DATA) {

      when(baudCounter === BIT_TICKS - 1) {

        baudCounter := 0

        // UART is LSB first
        shiftReg(bitCounter) := io.rx

        when(bitCounter === 7) {
          bitCounter := 0
          state := State.STOP
        } otherwise {
          bitCounter := bitCounter + 1
        }

      } otherwise {
        baudCounter := baudCounter + 1
      }
    }

    // ------------------------------------------------------------------------
    // STOP BIT
    // ------------------------------------------------------------------------

    is(State.STOP) {

      when(baudCounter === BIT_TICKS - 1) {

        baudCounter := 0

        // Stop bit must be high
        when(io.rx === True) {
          dataReg := shiftReg
          state := State.DONE
        } otherwise {
          state := State.IDLE
        }

      } otherwise {
        baudCounter := baudCounter + 1
      }
    }

    // ------------------------------------------------------------------------
    // DONE
    // ------------------------------------------------------------------------

    is(State.DONE) {

      dataValidReg := True
      state := State.IDLE
    }
  }
}