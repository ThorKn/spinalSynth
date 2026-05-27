package synth.uart

import spinal.core._
import spinal.lib._
import spinal.lib.com.uart._

class UartRx extends Component {

  val io = new Bundle {
    val rx      = in Bool()
    val byteOut = master(Flow(Bits(8 bits)))
  }

  // 1. Hardware Construction Parameters (Generics)
  val uartCtrlConfig = UartCtrlGenerics(
    dataWidthMax      = 8,
    clockDividerWidth = 20,
    preSamplingSize   = 1,
    samplingSize      = 5,
    postSamplingSize  = 2
  )

  // 2. Instantiate standard UartCtrl
  val uartCtrl = new UartCtrl(uartCtrlConfig)

  // 3. Connect raw physical RX pin (tied to rxd)
  uartCtrl.io.uart.rxd := io.rx

  // 4. Static Protocol Configuration (8N1 Frame)
  val frameConfig = UartCtrlFrameConfig(uartCtrlConfig)
  frameConfig.dataLength := 7 // value + 1 = 8 bits
  frameConfig.parity     := UartParityType.NONE
  frameConfig.stop       := UartStopType.ONE

  uartCtrl.io.config.frame := frameConfig

  // 5. Static Baud Rate Configuration (115200 Baud @ 24MHz clock)
  // Formula: System Clock / (Baud Rate * (preSampling + sampling + postSampling))
  // With generics above, total samplings per bit = 1 + 5 + 2 = 8
  // Divider = 24,000,000 / (115,200 * 8) = 26
  uartCtrl.io.config.clockDivider := 26

  // 6. Connect Flow Output
  io.byteOut.payload     := uartCtrl.io.read.payload
  io.byteOut.valid       := uartCtrl.io.read.valid
  uartCtrl.io.read.ready := True
}