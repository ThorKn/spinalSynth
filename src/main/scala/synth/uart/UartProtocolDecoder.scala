package synth.uart

import spinal.core._
import spinal.lib._
import synth.RegisterWrite

class UartProtocolDecoder extends Component {

  val io = new Bundle {
    val rxByte   = slave(Flow(Bits(8 bits)))
    val regWrite = master(Flow(RegisterWrite()))
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

  io.regWrite.payload.address := writeAddressReg
  io.regWrite.payload.data    := writeDataReg
  io.regWrite.valid           := writeEnableReg

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

      when(io.rxByte.valid) {

        // Only command 0x01 is supported
        when(io.rxByte.payload === B"8'x01") {
          state := State.WAIT_ADDR
        }
      }
    }

    // ------------------------------------------------------------------------
    // WAIT FOR ADDRESS BYTE
    // ------------------------------------------------------------------------

    is(State.WAIT_ADDR) {

      when(io.rxByte.valid) {

        addressReg := io.rxByte.payload.asUInt

        state := State.WAIT_DATA
      }
    }

    // ------------------------------------------------------------------------
    // WAIT FOR DATA BYTE
    // ------------------------------------------------------------------------

    is(State.WAIT_DATA) {

      when(io.rxByte.valid) {

        writeAddressReg := addressReg
        writeDataReg    := io.rxByte.payload

        writeEnableReg := True

        state := State.WAIT_CMD
      }
    }
  }
}