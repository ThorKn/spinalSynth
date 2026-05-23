# SpinalHDL Audio Oscillator Implementation Specification

## Table of Contents

1. RTL Hierarchy
2. OscillatorTop Module
3. TimingGenerator
   - IO Bundle
   - Internal Structure
   - Tick Behaviour
4. Oscillator
   - IO Bundle
   - Internal structure of submodules
   - 4.1 Oscillator Submodules
     - Accumulator
     - Noise
     - Generators
     - Mux
   - 4.2 Oscillator signal flow
5. Decimator
6. I2STransmitter

## 1. RTL Hierarchy

The RTL implementation shall use the following module hierarchy.

```text
OscillatorTop
 ├── TimingGenerator
 ├── Oscillator
 ├── Decimator
 └── I2STransmitter
```

## 2. OscillatorTop Module

### Purpose

The OscillatorTop module is the top-level integration entity of the complete oscillator system.

The module shall:

- instantiate all submodules
- connect subsystem interfaces
- expose the external hardware interface
- contain no DSP or protocol implementation details

This keeps the top-level RTL clean and structurally simple.

### IO Bundle

```
val io = new Bundle {
    val clk        = in Bool()
    val reset      = in Bool()

    val freqWord   = in UInt(24 bits)
    val waveSelect = in UInt(3 bits)
    val pwmWidth   = in UInt(8 bits)

    val i2s_bclk   = out Bool()
    val i2s_lrclk  = out Bool()
    val i2s_sdata  = out Bool()
}
```

## 3. TimingGenerator

### IO Bundle

```scala
val io = new Bundle {

    val phaseTick  = out Bool()
    val sampleTick = out Bool()
}
```

### Internal Structure

| Signal          | Width | Purpose            |
| --------------- | ----- | ------------------ |
| `phaseCounter`  | 6 bit | modulo-50 counter  |
| `sampleCounter` | 9 bit | modulo-500 counter |

Both counters:

- operate directly from the 24 MHz master clock
- run independently
- are free-running

### Tick Behaviour

Both tick outputs:

- are registered
- synchronous
- one clock cycle wide
- default to `False`

## 4. Oscillator

The Oscillator is a module that connects the four submodules, as shown in the following internal submodule structure. It serves as an abstraction layer above the generation of the oscillating audio signals.

### Purpose

Core DDS synthesis engine.

Responsibilities:

- phase accumulation
- waveform generation
- noise generation
- mux between waveforms and noise
- oversampled sample generation

### IO Bundle

```scala
val io = new Bundle {
    val phaseTick = in Bool()
    val freqWord  = in UInt(24 bits)
    val waveSelect = in UInt(3 bits)
    val pwmWidth  = in UInt(8 bits)
    val sample    = out SInt(16 bits)
}
```

### Internal structure of submodules:

```text
Oscillator
 ├── Accumulator
 ├── Noise
 ├── Generators
 └── Mux
```

### 4.1 Oscillator Submodules

#### Accumulator

Responsible for:

- phase register
- phase accumulation
- frequency addition

```scala
val io = new Bundle {
    val phaseTick = in Bool()
    val freqWord  = in UInt(24 bits)
    val phase     = out UInt(24 bits)
}
```

#### Noise

Responsible for:

- LFSR register
- feedback logic
- shift/update logic
- noise sample generation

```scala
val io = new Bundle {
    val phaseTick = in Bool()
    val sample    = out SInt(16 bits)
}
```

#### Generators

Responsible for:

- saw generation
- square generation
- PWM generation
- triangle generation

```scala
val io = new Bundle {
    val phase      = in UInt(24 bits)
    val pwmWidth   = in UInt(8 bits)
    val sawWave    = out SInt(16 bits)
    val squareWave = out SInt(16 bits)
    val pwmWave    = out SInt(16 bits)
    val triWave    = out SInt(16 bits)
}
```

The module shall contain only combinational logic.

#### Mux

Responsible for:

- waveform or noise selection
- final waveform routing

```scala
val io = new Bundle {

    val waveSelect = in UInt(3 bits)
    val sawWave    = in SInt(16 bits)
    val squareWave = in SInt(16 bits)
    val pwmWave    = in SInt(16 bits)
    val triWave    = in SInt(16 bits)
    val noiseWave  = in SInt(16 bits)
    val sample     = out SInt(16 bits)
}
```

### 4.2 Oscillator signal flow

```text
Accumulator ── phase ──┐
                        │
                        ↓
                   Generators
                        │
                        ├── sawWave
                        ├── squareWave
                        ├── pwmWave
                        └── triangleWave

Noise ── noiseSample ───┘
                        ↓
                       Mux
                        ↓
                     sample
```

## 5. Decimator

### IO Bundle

```scala
val io = new Bundle {
    val phaseTick  = in Bool()
    val sampleTick = in Bool()
    val sampleIn   = in SInt(16 bits)
    val sampleOut  = out SInt(16 bits) 
    val valid      = out Bool()
}
```

Responsible for converting the 480 kHz oversampled waveform stream into 48 kHz audio samples. The decimator captures every 10th oscillator sample.

sampleOut is:

- registered
- stable
- updated only at 48 kHz

valid communicates that a new 48 kHz sample is available now.

## 6. I2STransmitter

### IO Bundle

```scala
val io = new Bundle {
    val sampleIn  = in SInt(16 bits)
    val valid     = in Bool()
    val bclk      = out Bool()
    val lrclk     = out Bool()
    val sdata     = out Bool()
}
```

Self-contained serializer and protocol engine.

Responsibilities:

- BCLK generation
- LRCLK generation
- shift register control
- stereo frame timing
- serial audio output

The module operates directly from the 24 MHz master clock.

The serializer uses the scheduled timing subpattern:

```text
16,16,15,16,16,15,16,15
```

to generate the required average I²S bit timing.

##

