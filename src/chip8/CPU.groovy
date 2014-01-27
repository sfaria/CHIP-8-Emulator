package chip8

/**
 *
 * @author Scott Faria <scott.faria@gmail.com>
 */
class CPU {

    // registers and memory
    def currentOpcode = 0x0000
    def memory = new byte[4096]
    def vRegister = new byte[16]
    def indexRegister = 0x000
    def programCounter = 0x000

    // graphics context
    def graphics = new boolean[64 * 32]

    // timers
    def delayTimer = 0x00
    def soundTimer = 0x00

    // stack variables
    def stack = new short[16]
    def stackPointer = 0x000

    // 0x0 - 0xF, 16 keys
    def currentKeyPress = null

    // rng
    def rng = new Random()

    // system font set
    def fontset = [
        0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
        0x20, 0x60, 0x20, 0x20, 0x70, // 1
        0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
        0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
        0x90, 0x90, 0xF0, 0x10, 0x10, // 4
        0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
        0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
        0xF0, 0x10, 0x20, 0x40, 0x40, // 7
        0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
        0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
        0xF0, 0x90, 0xF0, 0x90, 0x90, // A
        0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
        0xF0, 0x80, 0x80, 0x80, 0xF0, // C
        0xE0, 0x90, 0x90, 0x90, 0xE0, // D
        0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
        0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    ]

    def getGraphics() {
        return graphics.clone()
    }

    def init() {
        currentOpcode = 0x0000
        programCounter = 0x000
        indexRegister = 0x000
        stackPointer = 0x000
        stack = new short[16]
        memory = new byte[4096]
        vRegister = new byte[16]
        graphics = new boolean[64 * 32]
        delayTimer = 0x00
        soundTimer = 0x00

        // load the system font set
        fontset.eachWithIndex { int font, int index ->
            memory[index] = font
        }

        delayTimer = 0
        soundTimer = 0
    }

    def loadRom(def romLocation) {
        new File(romLocation as String).readBytes().eachWithIndex { byte instruction, int i ->
            memory[i + 512] = instruction
        }
    }

    def setKeyPressState() {
       currentKeyPress = null // TODO implement key press
    }

    def emulateCycle(Closure renderCallback) {
        def render = false

        currentOpcode = memory[programCounter] << 8 | memory[programCounter + 1]
        switch (currentOpcode) {
            case ((currentOpcode & 0x0FFF) == 0x0000):
                // 0NNN - Calls RCA 1802 program at address NNN.
                break
            case (0x00E0):
                // 00E0 - Clear the screen
                graphics = new boolean[64 * 32]
                programCounter += 2
                render = true
                break
            case (0x00EE):
                // 00EE - Returns from a subroutine
                programCounter = stack[--stackPointer]
                break
            case ((currentOpcode & 0x0FFF) == 0x1000):
                // 1NNN - Jumps to memory address NNN
                programCounter = currentOpcode & 0x0FFF
                break
            case ((currentOpcode & 0x0FFF) == 0x2000):
                // 2NNN - Jumps to subroutine at NNN
                stack[stackPointer++] = programCounter
                programCounter = currentOpcode & 0x0FFF
                break
            case ((currentOpcode & 0x0FFF) == 0x3000):
                // 3XNN - Skips the next instruction if VX equals NN
                def x = (currentOpcode & 0x0F00) >> 8
                if (vRegister[x] == (currentOpcode & 0x00FF))  {
                    programCounter += 2
                }
                programCounter += 2
                break
            case ((currentOpcode & 0x0FFF) == 0x4000):
                // 4XNN - Skips the next instruction if VX doesn't equal NN
                def x = (currentOpcode & 0x0F00) >> 8
                if (vRegister[x] != (currentOpcode & 0x00FF))  {
                    programCounter += 2
                }
                programCounter += 2
                break
            case ((currentOpcode & 0x0FFF) == 0x5000):
                // 5XY0 - Skips the next instruction if VX equals VY.
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                if (vRegister[x] == vRegister[y]) {
                    programCounter += 2
                }
                programCounter += 2
                break
            case ((currentOpcode & 0x0FFF) == 0x6000):
                // 6XNN - Sets VX to NN
                def x = (currentOpcode & 0x0F00) >> 8
                vRegister[x] = currentOpcode & 0x00FF
                programCounter += 2
                break
            case ((currentOpcode & 0x0FFF) == 0x7000):
                // 7XNN - Adds NN to VX
                def x = (currentOpcode & 0x0F00) >> 8
                vRegister[x] += (currentOpcode & 0x00FF)
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0x8000):
                // 8XY0 - Sets VX to the value of VY
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                vRegister[x] = vRegister[y]
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0x8001):
                // 8XY1 - Sets VX to VX or VY
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                vRegister[x] = vRegister[x] | vRegister[y]
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0x8002):
                // 8XY2 - Sets VX to VX and VY
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                vRegister[x] = vRegister[x] & vRegister[y]
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0x8003):
                // 8XY3 - Sets VX to VX xor VY
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                vRegister[x] = vRegister[x] ^ vRegister[y]
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0x8004):
                // 8XY4 - Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                if(vRegister[y] > (0x00FF - vRegister[x])) {
                    vRegister[0xF] = 1 //carry
                } else {
                    vRegister[0xF] = 0;
                }
                vRegister[x] += vRegister[y];
                programCounter += 2;
                break
            case ((currentOpcode & 0x0FF0) == 0x8005):
                // 8XY5 - VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                def borrow = (vRegister[x] - vRegister[y]) & (1 << 16)
                if (borrow) {
                    vRegister[0xF] = 0
                } else {
                    vRegister[0xF] = 1
                }
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0x8006):
                // 8XY6 - Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift
                def x = (currentOpcode & 0x0F00) >> 8
                vRegister[0xF] = vRegister[x] & (-vRegister[x])
                vRegister[x] = vRegister[x] >> 1
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0x8007):
                // 8XY7 - Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                vRegister[x] = vRegister[y] - vRegister[x]
                def borrow = vRegister[x] & (1<<16)
                if (borrow) {
                    vRegister[0xF] = 0
                } else {
                    vRegister[0xF] = 1
                }
                programCounter += 2
                break
            case((currentOpcode & 0x0FF0) == 0x800E):
                // 8XYE - Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift
                def x = (currentOpcode & 0x0F00) >> 8
                if ((vRegister[x] & 0xF000) == vRegister[0xF]) {
                    vRegister[x] << 1
                }
                programCounter += 2
                break
            case ((currentOpcode & 0x0FFF) == 0x9000):
                // 9XY0 - Skips the next instruction if VX doesn't equal VY
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                if (x != y) {
                    programCounter += 2
                }
                programCounter += 2
                break
            case ((currentOpcode & 0x0FFF) == 0xA000):
                // ANNN - Sets I to the address NNN
                indexRegister = currentOpcode & 0x0FF
                programCounter += 2
                break
            case ((currentOpcode & 0x0FFF) == 0xB000):
                // BNNN - Jumps to the address NNN plus V0
                programCounter = (currentOpcode & 0x0FFF) + vRegister[0x0]
                break
            case ((currentOpcode & 0x0FFF) == 0xC000):
                // CXNN - Sets VX to a random number and NN
                def x = (currentOpcode & 0x0F00) >> 8
                def nn = (currentOpcode & 0x00FF)
                def randomByte = new byte[1]
                rng.nextBytes(randomByte)
                vRegister[x] = randomByte[1] & nn
                programCounter += 2
                break
            case ((currentOpcode & 0x0FFF) == 0xD000):
                // DXYN - Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels.
                // Each row of 8 pixels is read as bit-coded (with the most significant bit of each byte displayed on the left)
                // starting from memory location I; I value doesn't change after the execution of this instruction. As described
                // above, VF is set to 1 if any screen pixels are flipped from set to unset when the sprite is drawn, and to 0
                // if that doesn't happen
                def x = (currentOpcode & 0x0F00) >> 8
                def y = (currentOpcode & 0x00F0) >> 4
                def height = (currentOpcode & 0x000F) >> 8
                // needs the rest of the implementation

                programCounter += 2
                render = true
                break
            case ((currentOpcode & 0x0FF0) == 0xE00E):
                // EX9E - Skips the next instruction if the key stored in VX is pressed
                def x = (currentOpcode & 0x0F00) >> 8
                if (vRegister[x] == currentKeyPress) {
                    programCounter += 2
                }
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0xE001):
                // EXA1 - Skips the next instruction if the key stored in VX isn't pressed
                def x = (currentOpcode & 0x0F00) >> 8
                if (vRegister[x] != currentKeyPress) {
                    programCounter += 2
                }
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0xF007):
                // FX07 - Sets VX to the value of the delay timer
                vRegister[(currentOpcode & 0x0F00) >> 8] = delayTimer
                programCounter += 2
                break
            case ((currentOpcode & 0x0FF0) == 0xF00A):
                // FX0A - A key press is awaited, and then stored in VX
                setKeyPressState()
                def x = (currentOpcode & 0x0F00) >> 8
                vRegister[x] = currentKeyPress
                programCounter += 2
                break
            case ((currentOpcode & 0x0F00) == 0xF015):
                // FX15 - Sets the delay timer to VX
                delayTimer = vRegister[(currentOpcode & 0x0F00 >> 8)]
                programCounter += 2
                break
            case ((currentOpcode & 0x0F00) == 0xF018):
                // FX18 - Sets the sound timer to VX
                soundTimer = vRegister[(currentOpcode & 0x0F00) >> 8]
                programCounter += 2
                break
            case ((currentOpcode & 0x0F00) == 0xF01E):
                // FX1E - Adds VX to I
                indexRegister += vRegister[(currentOpcode & 0x0F00) >> 8]
                programCounter += 2
                break
            case ((currentOpcode & 0x0F00) == 0xF029):
                // FX29 - Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are
                // represented by a 4x5 font
                // TODO implement fully
                programCounter += 2
                break
            case ((currentOpcode & 0x0F00) == 0xF033):
                // FX33 - Stores the Binary-coded decimal representation of VX, with the most significant of three digits at
                // the address in I, the middle digit at I plus 1, and the least significant digit at I plus 2. (In other
                // words, take the decimal representation of VX, place the hundreds digit in memory at location in I, the tens
                // digit at location I+1, and the ones digit at location I+2.)
                memory[indexRegister] = vRegister[(currentOpcode & 0x0F00) >> 8] / 100
                memory[indexRegister + 1] = (vRegister[(currentOpcode & 0x0F00) >> 8] / 10) % 10
                memory[indexRegister + 2] = (vRegister[(currentOpcode & 0x0F00) >> 8] % 100) % 10
                programCounter += 2
                break
            case ((currentOpcode & 0x0F00) == 0xF055):
                // FX55 - Stores V0 to VX in memory starting at address I
                def x = vRegister[(currentOpcode & 0x0F00) >> 8]
                for (offset in 0..x) {
                    memory[indexRegister + offset] = vRegister[offset]
                }
                programCounter += 2
                break
            case ((currentOpcode & 0x0F00) == 0xF065):
                // FX65 - Fills V0 to VX with values from memory starting at address I
                def x = vRegister[(currentOpcode & 0x0F00) >> 8]
                for (offset in 0..x) {
                    vRegister[offset] = memory[indexRegister + offset]
                }
                programCounter += 2
                break
            default:
                throw new AssertionError("Unknown currentOpcode: ${currentOpcode}")
        }

        if (delayTimer > 0) {
            delayTimer--
        }

        if (soundTimer > 0) {
            if (soundTimer == 1) {
                println("Beep!")
            }
            soundTimer--
        }

        if (render) {
            renderCallback.call()
        }
    }

}