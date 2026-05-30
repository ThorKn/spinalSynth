package synth.uart

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import synth.common.RegisterWrite

class UartProtocolDecoder extends Component {

  val io = new Bundle {
    val rxByte   = slave(Flow(Bits(8 bits)))
    val regWrite = master(Flow(RegisterWrite()))
  }

  // 1. Transaction Registers
  val writeAddressReg = Reg(UInt(8 bits)) init(0)
  val writeDataReg    = Reg(Bits(8 bits)) init(0)
  val writeEnableReg  = Reg(Bool()) init(False)

  // 2. Drive Outputs
  io.regWrite.payload.address := writeAddressReg
  io.regWrite.payload.data    := writeDataReg
  io.regWrite.valid           := writeEnableReg

  // Keep writeEnable as a one-clock pulse by default
  writeEnableReg := False

  // 3. Declarative State Machine
  val fsm = new StateMachine {
    val WAIT_CMD  = new State with EntryPoint
    val WAIT_ADDR = new State
    val WAIT_DATA = new State

    val addressBuffer = Reg(UInt(8 bits)) init(0)

    WAIT_CMD.whenIsActive {
      when(io.rxByte.valid) {
        when(io.rxByte.payload === B"8'x01") {
          goto(WAIT_ADDR)
        }
      }
    }

    WAIT_ADDR.whenIsActive {
      when(io.rxByte.valid) {
        addressBuffer := io.rxByte.payload.asUInt
        goto(WAIT_DATA)
      }
    }

    WAIT_DATA.whenIsActive {
      when(io.rxByte.valid) {
        writeAddressReg := addressBuffer
        writeDataReg    := io.rxByte.payload
        writeEnableReg  := True
        goto(WAIT_CMD)
      }
    }
  }
}