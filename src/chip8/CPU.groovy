package chip8

/**
 *
 * @author Scott Faria <scott.faria@gmail.com>
 */
class CPU {

    /* Private Members */

    def currentOpcode = 0x0000
    def memory = new byte[4096]
    def vRegister = new byte[16]
    def indexRegister = 0x000
    def programCounter = 0x000
    def graphics = new boolean[64 * 32]
    def delayTimer = 0x00
    def soundTimer = 0x00
    def stack = new short[16]
    def stackPointer = 0x0000

    /* Public Methods */

    def getGraphics() {
        return graphics.clone()
    }

    def init() {

    }

    def loadRom(def romLocation) {

    }

    def setKeyPressState() {

    }

    def emulateCycle(Closure renderCallback) {
        def opcode = memory[programCounter] << 8 | memory[programCounter + 1]
        switch (opcode) {
            case (opcode << 12 == 0x0000):
                // 0NNN - Calls RCA 1802 program at address NNN.
                break
            case (0x00E0):
                // 00E0 - Clear the screen
                graphics = new boolean[64 * 32]
                programCounter += 2
                break
            case (0x00EE):
                // 00EE - Returns from a subroutine
                break
            case (opcode << 12 == 0x1000):
                // 1NNN - Jumps to memory address NNN
                break
            case (opcode << 12 == 0x2000):
                // 2NNN - Jumps to subroutine at NNN
                stack[stackPointer++] = programCounter
                programCounter = opcode & 0x0FFF
                break
            case (opcode << 12 == 0x3000):
                // 3XNN - Skips the next instruction if VX equals NN
                def x = (opcode & 0x0F00) >> 8
                if (vRegister[x] == (opcode & 0x00FF))  {
                    programCounter += 2
                }
                programCounter += 2
                break
            case (opcode << 12 == 0x4000):
                // 4XNN - Skips the next instruction if VX doesn't equal NN
                def x = (opcode & 0x0F00) >> 8
                if (vRegister[x] != (opcode & 0x00FF))  {
                    programCounter += 2
                }
                programCounter += 2
                break
            case (opcode << 12 == 0x5000):
                // 5XY0 - Skips the next instruction if VX equals VY.
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                if (vRegister[x] == vRegister[y]) {
                    programCounter += 2
                }
                programCounter += 2
                break
            case (opcode << 12 == 0x6000):
                // 6XNN - Sets VX to NN
                def x = (opcode & 0x0F00) >> 8
                vRegister[x] = opcode & 0x00FF
                programCounter += 2
                break
            case (opcode << 12 == 0x7000):
                // 7XNN - Adds NN to VX
                def x = (opcode & 0x0F00) >> 8
                vRegister[x] += (opcode & 0x00FF)
                programCounter += 2
                break
            case (opcode << 12 == 0x8000 && opcode << 16 == 0x0000):
                // 8XY0 - Sets VX to the value of VY
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                vRegister[x] = vRegister[y]
                programCounter += 2
                break
            case (opcode << 12 == 0x8000 && opcode << 16 == 0x1000):
                // 8XY1 - Sets VX to VX or VY
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                vRegister[x] = vRegister[x] | vRegister[y]
                programCounter += 2
                break
            case (opcode << 12 == 0x8000 && opcode << 16 == 0x2000):
                // 8XY2 - Sets VX to VX and VY
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                vRegister[x] = vRegister[x] & vRegister[y]
                programCounter += 2
                break
            case (opcode << 12 == 0x8000 && opcode << 16 == 0x3000):
                // 8XY3 - Sets VX to VX xor VY
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                vRegister[x] = vRegister[x] ^ vRegister[y]
                programCounter += 2
                break
            case (opcode << 12 == 0x8000 && opcode << 16 == 0x4000):
                // 8XY4 - Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                if(vRegister[y] > (0x00FF - vRegister[x])) {
                    vRegister[0xF] = 1 //carry
                } else {
                    vRegister[0xF] = 0;
                }
                vRegister[x] += vRegister[y];
                programCounter += 2;
                break
            case (opcode << 12 == 0x8000 && opcode << 16 == 0x5000):
                // 8XY5 - VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                def borrow = (vRegister[x] - vRegister[y]) & (1 << 16)
                if (borrow) {
                    vRegister[0xF] = 0
                } else {
                    vRegister[0xF] = 1
                }
                programCounter += 2
                break
            case (opcode << 12 == 0x8000 && opcode << 16 == 0x6000):
                // 8XY6 - Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift
                def x = (opcode & 0x0F00) >> 8
                vRegister[0xF] = vRegister[x] & (-vRegister[x])
                vRegister[x] = vRegister[x] >> 1
                programCounter += 2
                break
            case (opcode << 12 == 0x8000 && opcode << 16 == 0x7000):
                // 8XY7 - Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                vRegister[x] = vRegister[y] - vRegister[x]
                def borrow = vRegister[x] & (1<<16)
                if (borrow) {
                    vRegister[0xF] = 0
                } else {
                    vRegister[0xF] = 1
                }
                programCounter += 2
                break
            case(opcode << 12 == 0x8000 && opcode << 16 == 0xE000):
                // 8XYE - Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift
                def x = (opcode & 0x0F00) >> 8
                if ((vRegister[x] & 0xF000) == vRegister[0xF]) {
                    vRegister[x] << 1
                }
                programCounter += 2
                break
            case (opcode << 12 == 0x9000):
                // 9XY0 - Skips the next instruction if VX doesn't equal VY
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                if (x != y) {
                    programCounter += 2
                }
                programCounter += 2
                break
            case (opcode << 12 == 0xA000):
                // ANNN - Sets I to the address NNN
                indexRegister = opcode & 0x0FF
                programCounter += 2
                break
            case (opcode << 12 == 0xB000):
                // BNNN - Jumps to the address NNN plus V0
                break
            case (opcode << 12 == 0xC000):
                // CXNN - Sets VX to a random number and NN
                break
            case (opcode << 12 == 0xD000):
                // DXYN - Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels.
                // Each row of 8 pixels is read as bit-coded (with the most significant bit of each byte displayed on the left)
                // starting from memory location I; I value doesn't change after the execution of this instruction. As described
                // above, VF is set to 1 if any screen pixels are flipped from set to unset when the sprite is drawn, and to 0
                // if that doesn't happen
                def x = (opcode & 0x0F00) >> 8
                def y = (opcode & 0x00F0) >> 4
                def height = (opcode & 0x000F) >> 8
                // needs the rest of the implementation
                break
            case (opcode << 12 == 0xE000 && opcode << 16 == 0xE000):
                // EX9E - Skips the next instruction if the key stored in VX is pressed
                break
            case (opcode << 12 == 0xE000 && opcode << 16 == 0x1000):
                // EXA1 - Skips the next instruction if the key stored in VX isn't pressed
                break
            case (opcode << 12 == 0xF000 && opcode << 16 == 0x7000):
                // FX07 - Sets VX to the value of the delay timer
                vRegister[(opcode & 0x0F00) >> 8] = delayTimer
                programCounter += 2
                break
            case (opcode << 12 == 0xF000 && opcode << 16 == 0xA000):
                // FX0A - A key press is awaited, and then stored in VX
                break
            case (opcode << 12 == 0xF000 && opcode << 16 == 0x5000):
                // FX15 - Sets the delay timer to VX
                delayTimer = vRegister[(opcode & 0x0F00 >> 8)]
                programCounter += 2
                break
            case (opcode << 12 == 0xF000 && opcode << 16 == 0x8000):
                // FX18 - Sets the sound timer to VX
                soundTimer = vRegister[(opcode & 0x0F00) >> 8]
                programCounter += 2
                break
            case (opcode << 12 == 0xF000 && opcode << 16 == 0xE000):
                // FX1E - Adds VX to I
                indexRegister += vRegister[(opcode & 0x0F00) >> 8]
                programCounter += 2
                break
            case (opcode << 12 == 0xF000 && opcode << 16 == 0x9000):
                // FX29 - Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are
                // represented by a 4x5 font
                break
            case (opcode << 12 == 0xF000 && opcode << 16 == 0x3000):
                // FX33 - Stores the Binary-coded decimal representation of VX, with the most significant of three digits at
                // the address in I, the middle digit at I plus 1, and the least significant digit at I plus 2. (In other
                // words, take the decimal representation of VX, place the hundreds digit in memory at location in I, the tens
                // digit at location I+1, and the ones digit at location I+2.)
                memory[indexRegister] = vRegister[(opcode & 0x0F00) >> 8] / 100
                memory[indexRegister + 1] = (vRegister[(opcode & 0x0F00) >> 8] / 10) % 10
                memory[indexRegister + 2] = (vRegister[(opcode & 0x0F00) >> 8] % 100) % 10
                programCounter += 2
                break
            case (opcode << 12 == 0xF000 && opcode << 8 == 0x5500):
                // FX55 - Stores V0 to VX in memory starting at address I
                def x = vRegister[(opcode & 0x0F00) >> 8]
                for (offset in 0..x) {
                    memory[indexRegister + offset] = vRegister[offset]
                }
                programCounter += 2
                break
            case (opcode << 12 == 0xF000 && opcode << 8 == 0x6500):
                // FX65 - Fills V0 to VX with values from memory starting at address I
                def x = vRegister[(opcode & 0x0F00) >> 8]
                for (offset in 0..x) {
                    vRegister[offset] = memory[indexRegister + offset]
                }
                programCounter += 2
                break
            default:
                throw new AssertionError("Unknown opcode: ${opcode}")
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

        if (true) {
            renderCallback.call()
        }
    }

}