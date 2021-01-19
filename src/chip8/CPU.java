package chip8;

import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.IntStream;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class CPU {

    // -------------------- Private Statics --------------------

    // system font set
    private static final short[] FONT_SET = new short[] {
            0x00F0, 0x0090, 0x0090, 0x0090, 0x00F0, // 0
            0x0020, 0x0060, 0x0020, 0x0020, 0x0070, // 1
            0x00F0, 0x0010, 0x00F0, 0x0080, 0x00F0, // 2
            0x00F0, 0x0010, 0x00F0, 0x0010, 0x00F0, // 3
            0x0090, 0x0090, 0x00F0, 0x0010, 0x0010, // 4
            0x00F0, 0x0080, 0x00F0, 0x0010, 0x00F0, // 5
            0x00F0, 0x0080, 0x00F0, 0x0090, 0x00F0, // 6
            0x00F0, 0x0010, 0x0020, 0x0040, 0x0040, // 7
            0x00F0, 0x0090, 0x00F0, 0x0090, 0x00F0, // 8
            0x00F0, 0x0090, 0x00F0, 0x0010, 0x00F0, // 9
            0x00F0, 0x0090, 0x00F0, 0x0090, 0x0090, // A
            0x00E0, 0x0090, 0x00E0, 0x0090, 0x00E0, // B
            0x00F0, 0x0080, 0x0080, 0x0080, 0x00F0, // C
            0x00E0, 0x0090, 0x0090, 0x0090, 0x00E0, // D
            0x00F0, 0x0080, 0x00F0, 0x0080, 0x00F0, // E
            0x00F0, 0x0080, 0x00F0, 0x0080, 0x0080  // F
    };

    // -------------------- Private Methods --------------------

    // registers and memory
    private int currentOpcode = 0x0000;                 // two bytes used
    private short[] memory = new short[4096];           // 1 byte * 4096
    private short[] vRegister = new short[16];          // two bytes each
    private short indexRegister = 0x0000;                 // two bytes
    private short programCounter = 0x2000;                // two bytes

    // stack variables
    private short[] stack = new short[16];                  // two bytes each
    private short stackPointer = 0x0000;                  // two byte pointer into stack

    // graphics "memory"
    private boolean[] graphics = new boolean[64 * 32];  // matrix of "bits"

    // timers
    private short delayTimer = 0x0000;                    // 2 bytes
    private short soundTimer = 0x0000;                    // 2 bytes

    // rng
    private final Random rng = new Random();
    private Keyboard keyboard = null;

    // general stuff
    private final EventListenerList ll = new EventListenerList();

    // -------------------- Constructors --------------------

    CPU() { }

    // -------------------- Default Methods --------------------

    final void addDebuggerListener(DebuggerListener l) {
        Objects.requireNonNull(l);
        ll.add(DebuggerListener.class, l);
    }

    final void addRenderListener(RenderListener l) {
        Objects.requireNonNull(l);
        ll.add(RenderListener.class, l);
    }

    final void init(Keyboard keyboard) {
        this.keyboard = keyboard;
        this.currentOpcode = 0x0000;
        this.programCounter = 0x200;
        this.indexRegister = 0x000;
        this.stackPointer = 0x000;
        this.stack = new short[16];
        this.memory = new short[4096];
        this.vRegister = new short[16];
        this.graphics = new boolean[64 * 32];
        this.delayTimer = 0x00;
        this.soundTimer = 0x00;
        this.delayTimer = 0x0000;
        this.soundTimer = 0x0000;

        // load the system font set
        IntStream.range(0, FONT_SET.length)
                .forEach(index -> this.memory[index] = FONT_SET[index]);
    }

    final void loadRom(String romLocation) throws IOException {
        File romFile = new File(romLocation);
        byte[] fileBytes = Files.readAllBytes(romFile.toPath());
        for (int fileIndex = 0, memIndex = 512; fileIndex < fileBytes.length; fileIndex++, memIndex++) {
            memory[memIndex] = (short) Byte.toUnsignedInt(fileBytes[fileIndex]);
        }
        fireInit();
    }

    final void emulateCycle() {
        fireExecuteStateChanged();

        boolean render = false;

        short highByte = memory[programCounter];
        short lowByte = memory[programCounter + 1];
        currentOpcode = (short) (highByte << 8 | lowByte);
        short nnn = (short) (currentOpcode & 0x0FFF);
        short n = (short) (currentOpcode & 0x000F);
        short x = (short) (highByte & 0x000F);
        short y = (short) ((lowByte & 0xF000) >> 2);
        short nn = lowByte;

        programCounter += 2;
        short topByte = (short) ((currentOpcode & 0xF000) >> 12);
        switch (topByte) {
            case 0x0:
                render = do0X();
                break;
            case 0x1:
                // 1NNN - Jumps to memory address NNN
                programCounter = nnn;
                break;
            case 0x2:
                // 2NNN - Jumps to subroutine at NNN
                stack[stackPointer++] = programCounter;
                programCounter = nnn;
                break;
            case 0x3:
                // 3XNN - Skips the next instruction if VX equals NN
                if (vRegister[x] == nn) {
                    programCounter += 2;
                }
                break;
            case 0x4:
                // 4XNN - Skips the next instruction if VX doesn't equal NN
                if (vRegister[x] != nn) {
                    programCounter += 2;
                }
                break;
            case 0x5:
                // 5XY0 - Skips the next instruction if VX equals VY.
                if (vRegister[x] == vRegister[y]) {
                    programCounter += 2;
                }
                break;
            case 0x6:
                // 6XNN - Sets VX to NN
                vRegister[x] = nn;
                break;
            case 0x7:
                // 7XNN - Adds NN to VX
                vRegister[x] += nn;
                break;
            case 0x8:
                do8XY(n, x, y);
                break;
            case 0x9:
                // 9XY0 - Skips the next instruction if VX doesn't equal VY
                if (x != y) {
                    programCounter += 2;
                }
                break;
            case 0xB:
                // BNNN - Jumps to the address NNN plus V0
                programCounter = (short) (nnn + vRegister[0x0]);
                break;
            case 0xC:
                // CXNN - Sets VX to a random number and NN
                byte[] randomByte = new byte[1];
                rng.nextBytes(randomByte);
                vRegister[x] = (byte) (randomByte[0] & nn);
                break;
            case 0xD:
                render = do0XD(n, x, y);
                break;
            case 0xE:
                doEX(n, x);
                break;
            case 0xF:
                doFX(lowByte, x);
                break;
            default:
                throw new IllegalArgumentException();
        }

        if (delayTimer > 0) {
            delayTimer--;
        }

        if (soundTimer > 0) {
            if (soundTimer == 1) {
                System.out.println("Beep!");
            }
            soundTimer--;
        }

        if (render) {
            fireRenderNeeded();
        }
    }

    private boolean do0XD(short n, short x, short y) {
        // DXYN - Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels.
        // The interpreter reads n bytes from memory, starting at the address stored in I. These bytes are then displayed as sprites
        // on screen at coordinates (Vx, Vy). Sprites are XORed onto the existing screen. If this causes any pixels to
        // be erased, VF is set to 1, otherwise it is set to 0. If the sprite is positioned so part of it is outside the
        // coordinates of the display, it wraps around to the opposite side of the screen. See instruction 8xy3 for more
        // information on XOR, and section 2.4, Display, for more information on the Chip-8 screen and sprites.
        short[] sprite = new short[n];
        for (int i = 0; i < n; i++) {
            sprite[i] = memory[indexRegister + i];
        }

        short xCoord = vRegister[x];
        short yCoord = vRegister[y];

        return true;
    }

    // -------------------- Private Methods --------------------

    private boolean do0X() {
        if (currentOpcode == 0x00E0) {
            // 00E0 - Clear the screen
            graphics = new boolean[64 * 32];
            return true;
        } else if (currentOpcode == 0x00EE) {
            // 00EE - Returns from a subroutine
            programCounter = stack[--stackPointer];
        } else {
            // 0NNN - Calls RCA 1802 program at address NNN. Ignored by modern interpreters.
            System.out.println("0NNN called: Ignoring");
        }
        return false;
    }

    private void do8XY(short n, short x, short y) {
        switch (n) {
            case 0x0:
                // 8XY0 - Sets VX to the value of VY
                vRegister[x] = vRegister[y];
                break;
            case 0x1:
                // 8XY1 - Sets VX to VX or VY
                vRegister[x] = (short) (vRegister[x] | vRegister[y]);
                break;
            case 0x2:
                // 8XY2 - Sets VX to VX and VY
                vRegister[x] = (short) (vRegister[x] & vRegister[y]);
                break;
            case 0x3:
                // 8XY3 - Sets VX to VX xor VY
                vRegister[x] = (short) (vRegister[x] ^ vRegister[y]);
                break;
            case 0x4:
                // 8XY4 - Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't
                short result = (short) (vRegister[x] + vRegister[y]);
                boolean carry = result > 255;
                if (carry) {
                    vRegister[0xF] = 1;
                } else {
                    vRegister[0xF] = 0;
                }
                vRegister[x] = (short)(result & 0x00FF);
                break;
            case 0x5:
                // 8XY5 - VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't
                if (vRegister[x] > vRegister[y]) {
                    vRegister[0xF] = 1;
                } else {
                    vRegister[0xF] = 0;
                }
                vRegister[x] = (short) (vRegister[x] - vRegister[y]);
                break;
            case 0x6:
                // 8XY6 - Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift
                vRegister[0xF] = (byte) (vRegister[x] & 0b0001);
                vRegister[x] = (byte) (vRegister[x] >> 1);
                break;
            case 0x7:
                // 8XY7 - Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't
                if (vRegister[y] > vRegister[x]) {
                    vRegister[0xF] = 1;
                } else {
                    vRegister[0xF] = 0;
                }
                vRegister[x] = (short) (vRegister[y] - vRegister[x]);
                break;
            case 0xE:
                // 8XYE - Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift
                vRegister[0xF] = (short) ((vRegister[x] >> 4) & 0b00000001);
                vRegister[x] = (short) (vRegister[x] << 1);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void doEX(short n, short x) {
        switch (n) {
            case 0xE:
                // EX9E - Skips the next instruction if the key stored in VX is pressed
                if (keyboard.isPressed(vRegister[x])) {
                    programCounter += 2;
                }
                break;
            case 0x1:
                // EXA1 - Skips the next instruction if the key stored in VX isn't pressed
                if (!keyboard.isPressed(vRegister[x])) {
                    programCounter += 2;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void doFX(short lowByte, short x) {
        switch (lowByte) {
            case 0x07:
                // FX07 - Sets VX to the value of the delay timer
                vRegister[x] = delayTimer;
                break;
            case 0x0A:
                // FX0A - A key press is awaited, and then stored in VX
                vRegister[x] = (byte) keyboard.waitForKeyPress();
                break;
            case 0x15:
                // FX15 - Sets the delay timer to VX
                delayTimer = vRegister[x];
                break;
            case 0x18:
                // FX18 - Sets the sound timer to VX
                soundTimer = vRegister[x];
                break;
            case 0x1E:
                // FX1E - Adds VX to I
                indexRegister += vRegister[x];
                break;
            case 0x29:
                // FX29 - Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are
                // represented by a 4x5 font
                indexRegister = (short) (vRegister[x] * 5);
                break;
            case 0x33:
                // FX33 - Stores the Binary-coded decimal representation of VX, with the most significant of three digits at
                // the address in I, the middle digit at I plus 1, and the least significant digit at I plus 2. (In other
                // words, take the decimal representation of VX, place the hundreds digit in memory at location in I, the tens
                // digit at location I+1, and the ones digit at location I+2.)
                memory[indexRegister] = (byte) (vRegister[x] / 100);
                memory[indexRegister + 1] = (byte) ((vRegister[x] / 10) % 10);
                memory[indexRegister + 2] = (byte) ((vRegister[x] % 100) % 10);
                break;
            case 0x55:
                // FX55 - Stores V0 to VX in memory starting at address I
                System.arraycopy(vRegister, 0, memory, indexRegister, x);
                break;
            case 0x65:
                // FX65 - Fills V0 to VX with values from memory starting at address I
                System.arraycopy(memory, indexRegister, vRegister, 0, x);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void fireInit() {
        List<OperationInfo> operations = new ArrayList<>();
        for (int memIndex = programCounter; memIndex < memory.length; memIndex = memIndex + 2) {
            short highByte = memory[memIndex];
            short lowByte = memory[memIndex + 1];
            short opcode = (short) (highByte << 8 | lowByte);
            operations.add(new OperationInfo(opcode));
        }

        short[] registerCopy = new short[vRegister.length];
        System.arraycopy(vRegister, 0, registerCopy, 0, vRegister.length);
        MachineState state = new MachineState(programCounter, registerCopy);
        for (DebuggerListener l : ll.getListeners(DebuggerListener.class)) {
            l.executionStarted(state, operations);
        }
    }

    private void fireExecuteStateChanged() {
        short[] registerCopy = new short[vRegister.length];
        System.arraycopy(vRegister, 0, registerCopy, 0, vRegister.length);
        MachineState state = new MachineState(programCounter, registerCopy);
        for (DebuggerListener l : ll.getListeners(DebuggerListener.class)) {
            l.machineStateChanged(state);
        }
    }

    private void fireRenderNeeded() {
        boolean[] graphicsCopy = new boolean[graphics.length];
        System.arraycopy(graphics, 0, graphicsCopy, 0, graphics.length);
        for (RenderListener l : ll.getListeners(RenderListener.class)) {
            l.render(graphicsCopy);
        }
    }
}