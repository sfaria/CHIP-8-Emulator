package chip8

/**
 *
 * @author Scott Faria <scott.faria@gmail.com>
 */
class CPU {

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
        def draw = true
        renderCallback.call()
    }

}