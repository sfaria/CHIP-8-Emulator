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
                break
            case (0x00EE):
                // 00EE - Returns from a subroutine
                break
            case (opcode << 12 == 0x1000):
                // 1NNN - Jumps to memory address NNN
                break
            case (opcode << 12 == 0x2000):
                // 2NNN - Jumps to subroutine at NNN
                break
            case (opcode << 12 == 0x3000):
                // 3XNN - Skips the next instruction if VX equals NN
                break
            case (opcode << 12 == 0x4000):
                // 4XNN - Skips the next instruction if VX doesn't equal NN
                break
            case (opcode << 12 == 0x5000):
                // 5XY0 - Skips the next instruction if VX equals VY.
                break
            case (opcode << 12 == 0x6000):
                // 6XNN - Sets VX to NN
                break
            case (opcode << 12 == 0x7000):
                // 7XNN - Adds NN to VX
                break
            case (opcode << 12 == 0x8000):
                // 8XY0 - Sets VX to the value of VY
            default:
                throw new AssertionError("Unknown opcode: ${opcode}")
        }

        if (true) {
            renderCallback.call()
        }
    }

}