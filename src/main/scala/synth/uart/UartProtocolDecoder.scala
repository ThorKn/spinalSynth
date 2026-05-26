package synth.uart

import spinal.core._
import spinal.lib._

class UartProtocolDecoder extends Component {

  val io = new Bundle {

    val rxData      = in Bits(8 bits)
    val rxDataValid = in Bool()

    val writeEnable  = out Bool()
    val writeAddress = out UInt(8 bits)
    val writeData    = out Bits(8 bits)
  }

  // --------------------------------------------------------------------------
  // FSM State Definition
  // --------------------------------------------------------------------------

  object State extends SpinalEnum {
    val WAIT_CMD, WAIT_ADDR, WAIT_DATA = newElement()
  }

  val state = Reg(State()) init(State.WAIT_CMD)

  // --------------------------------------------------------------------------
  // Internal Registers
  // --------------------------------------------------------------------------

  val addressReg = Reg(UInt(8 bits)) init(0)

  val writeEnableReg  = Reg(Bool()) init(False)
  val writeAddressReg = Reg(UInt(8 bits)) init(0)
  val writeDataReg    = Reg(Bits(8 bits)) init(0)

  // --------------------------------------------------------------------------
  // Default Outputs
  // --------------------------------------------------------------------------

  io.writeEnable  := writeEnableReg
  io.writeAddress := writeAddressReg
  io.writeData    := writeDataReg

  // writeEnable is a one-clock pulse
  writeEnableReg := False

  // --------------------------------------------------------------------------
  // FSM
  // --------------------------------------------------------------------------

  switch(state) {

    // ------------------------------------------------------------------------
    // WAIT FOR COMMAND BYTE
    // ------------------------------------------------------------------------

    is(State.WAIT_CMD) {

      when(io.rxDataValid) {

        // Only command 0x01 is supported
        when(io.rxData === B"8'x01") {
          state := State.WAIT_ADDR
        }
      }
    }

    // ------------------------------------------------------------------------
    // WAIT FOR ADDRESS BYTE
    // ------------------------------------------------------------------------

    is(State.WAIT_ADDR) {

      when(io.rxDataValid) {

        addressReg := io.rxData.asUInt

        state := State.WAIT_DATA
      }
    }

    // ------------------------------------------------------------------------
    // WAIT FOR DATA BYTE
    // ------------------------------------------------------------------------

    is(State.WAIT_DATA) {

      when(io.rxDataValid) {

        writeAddressReg := addressReg
        writeDataReg    := io.rxData

        writeEnableReg := True

        state := State.WAIT_CMD
      }
    }
  }
}