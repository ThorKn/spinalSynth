# spinalSynth: Testing Specification

This document provides the formal specifications for the simulation test suite in `spinalSynth`. It defines the verification logic, simulated environments, stimulus parameters, and assertion constraints for all unit and integration testbenches.

---

## 1. Unit Tests

This chapter contains the specifications for testing individual, isolated hardware modules.

### 1.1 Attenuator Unit Test (`AttenuatorSim`)

#### Purpose
Verifies the custom DSP volume attenuator module (`Attenuator`) for mathematical precision, pipeline latency, and reset stability.

#### Simulated Environment
* **Component Under Test**: `Attenuator`
* **Clock Domain**: 24 MHz synchronous master clock (simulation period = 10 units).
* **Reset**: Asynchronous, Active-High.

#### Input Stimulus & Signals
* `io.sampleIn`: `Flow[SInt]` (16 bits)
* `io.volume`: `UInt` (8 bits)
* `io.sampleOut`: `Flow[SInt]` (16 bits)

#### Test Cases

##### 1.1.1 Reset Stability
* **Action**: Assert `reset` high for 5 clock cycles while driving random values on `io.sampleIn.payload` and `io.volume`.
* **Assertion**: Verify that `io.sampleOut.payload` is strictly held at `0` and `io.sampleOut.valid` is strictly `False`.

##### 1.1.2 Mathematical Scaling
* **Action**: Pulse `io.sampleIn.valid` high for 1 clock cycle with a specific payload and volume, then return it to `False`. Wait 1 clock cycle for the pipeline register.
* **Assertion**: Verify that `io.sampleOut.valid` is `True` during the second cycle and its payload matches the expected fractional attenuation exactly:
  $$\text{expected} = \frac{\text{sampleIn.payload} \times \text{volume}}{256}$$
* **Test Vectors**:
  | Input Sample | Volume | Expected Output | Notes |
  |---|---|---|---|
  | `20000` | `255` | `19921` | Full scale fractional ($19921.8$ truncated) |
  | `20000` | `128` | `10000` | Half scale (Exact) |
  | `-20000` | `64` | `-5000` | Quarter scale negative |
  | `-32768` | `0` | `0` | Silent (Mute) |

##### 1.1.3 Cycle-Accurate Latency & Pipelining
* **Action**: Pulse `io.sampleIn.valid` back-to-back for 3 consecutive clock cycles with distinct samples:
  * Cycle 1: Sample = `10000`, Volume = `255`
  * Cycle 2: Sample = `20000`, Volume = `128`
  * Cycle 3: Sample = `-10000`, Volume = `64`
* **Assertion**: Verify that:
  * Cycle 1: `sampleOut.valid` is `False`.
  * Cycle 2: `sampleOut.valid` is `True`, payload = `9960`.
  * Cycle 3: `sampleOut.valid` is `True`, payload = `10000`.
  * Cycle 4: `sampleOut.valid` is `True`, payload = `-2500`.
  * Cycle 5: `sampleOut.valid` is `False`.
  * This confirms the 1-cycle pipeline throughput operates continuously without stalls or register leakage.

---

### 1.2 Register Bank Unit Test (`RegisterBankSim`)

#### Purpose
Verifies the parameter storage register bank module (`RegisterBank`) for reset defaults, single-byte register updates, and atomic 24-bit frequency word commitment.

#### Simulated Environment
* **Component Under Test**: `RegisterBank`
* **Clock Domain**: 24 MHz master clock (simulation period = 10 units).
* **Reset**: Asynchronous, Active-High.

#### Input Stimulus & Signals
* `io.regWrite`: `Flow[RegisterWrite]` (containing 8-bit `address` and 8-bit `data`)
* `io.config`: `OscillatorConfig` (output bundle containing `freqWord`, `waveSelect`, `pwmWidth`, `volume`)

#### Test Cases

##### 1.2.1 Reset Defaults
* **Action**: Start simulation with reset asserted.
* **Assertion**: Verify that all output configuration fields in `io.config` are held strictly at `0`.

##### 1.2.2 Single-Byte Direct Updates
* **Action**: Write individually to non-atomic registers using the `io.regWrite` port:
  - Write `0x03` (address `0x03` - `WAVE_SEL`)
  - Write `0xA5` (address `0x04` - `PWM_WIDTH`)
  - Write `0x7F` (address `0x05` - `VOLUME`)
* **Assertion**: Verify that the corresponding output fields (`config.waveSelect`, `config.pwmWidth`, and `config.volume`) are updated to the written values on the next clock cycle.

##### 1.2.3 Atomic 24-Bit Frequency Commitment
* **Action**: Perform a sequential write sequence to verify shadow staging and atomic trigger commitment:
  1. Write `0x55` to `0x00` (`FREQ_LOW`).
     * *Assertion*: Verify that `config.freqWord` remains unchanged (stages in shadow register).
  2. Write `0xAA` to `0x01` (`FREQ_MID`).
     * *Assertion*: Verify that `config.freqWord` remains unchanged (stages in shadow register).
  3. Write `0x0C` to `0x02` (`FREQ_HIGH`).
     * *Assertion*: Verify that on the next clock cycle, the active output `config.freqWord` updates atomically to `0x0CAA55` (`830037` in decimal) all at once.

---

### 1.3 UART Protocol Decoder Unit Test (`UartProtocolDecoderSim`)

#### Purpose
Verifies the FSM protocol parser (`UartProtocolDecoder`) for reset safety, successful command framing, byte-spacing delay tolerance, and command-valid bounds.

#### Simulated Environment
* **Component Under Test**: `UartProtocolDecoder`
* **Clock Domain**: 24 MHz master clock (simulation period = 10 units).
* **Reset**: Asynchronous, Active-High.

#### Input Stimulus & Signals
* `io.rxByte`: `Flow[Bits]` (input stream representing incoming serial bytes)
* `io.regWrite`: `Flow[RegisterWrite]` (output transaction flow)

#### Test Cases

##### 1.3.1 Reset Safety
* **Action**: Assert reset while pulsing `rxByte.valid` with random bytes.
* **Assertion**: Verify that `io.regWrite.valid` remains strictly `False`.

##### 1.3.2 Valid Command Framing (3-Byte Stream)
* **Action**: Push three bytes back-to-back:
  - Byte 1: `0x01` (WriteRegister command)
  - Byte 2: `0x02` (`FREQ_HIGH` address)
  - Byte 3: `0xAB` (Register data payload)
* **Assertion**: Verify that:
  - `regWrite.valid` is `False` after Byte 1 and Byte 2.
  - `regWrite.valid` is `True` in the exact cycle Byte 3 is pushed, and its payload contains `address = 0x02` and `data = 0xAB`.
  - `regWrite.valid` drops back to `False` in the following cycle.

##### 1.3.3 Byte-Spacing Delay Tolerance
* **Action**: Push a 3-byte transaction with arbitrary timing spacing to simulate slow UART transmissions:
  - Push Byte 1 (`0x01`), then wait 25 clock cycles.
  - Push Byte 2 (`0x05` - `VOLUME`), then wait 50 clock cycles.
  - Push Byte 3 (`0x7F`), then wait 1 clock cycle.
* **Assertion**: Verify that the FSM maintains state synchronization, and atomically asserts `regWrite.valid = True` with `address = 0x05` and `data = 0x7F` exactly when Byte 3 is pushed.
