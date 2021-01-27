package chip8;

import jdk.jshell.execution.Util;

import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.IntStream;

import static chip8.Utilities.*;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class CPU {

    // -------------------- Private Statics --------------------

    // system font set
    private static final byte[] FONT_SET = new byte[] {
            (byte) 0x00F0, (byte) 0x0090, (byte) 0x0090, (byte) 0x0090, (byte) 0x00F0, // 0
            (byte) 0x0020, (byte) 0x0060, (byte) 0x0020, (byte) 0x0020, (byte) 0x0070, // 1
            (byte) 0x00F0, (byte) 0x0010, (byte) 0x00F0, (byte) 0x0080, (byte) 0x00F0, // 2
            (byte) 0x00F0, (byte) 0x0010, (byte) 0x00F0, (byte) 0x0010, (byte) 0x00F0, // 3
            (byte) 0x0090, (byte) 0x0090, (byte) 0x00F0, (byte) 0x0010, (byte) 0x0010, // 4
            (byte) 0x00F0, (byte) 0x0080, (byte) 0x00F0, (byte) 0x0010, (byte) 0x00F0, // 5
            (byte) 0x00F0, (byte) 0x0080, (byte) 0x00F0, (byte) 0x0090, (byte) 0x00F0, // 6
            (byte) 0x00F0, (byte) 0x0010, (byte) 0x0020, (byte) 0x0040, (byte) 0x0040, // 7
            (byte) 0x00F0, (byte) 0x0090, (byte) 0x00F0, (byte) 0x0090, (byte) 0x00F0, // 8
            (byte) 0x00F0, (byte) 0x0090, (byte) 0x00F0, (byte) 0x0010, (byte) 0x00F0, // 9
            (byte) 0x00F0, (byte) 0x0090, (byte) 0x00F0, (byte) 0x0090, (byte) 0x0090, // A
            (byte) 0x00E0, (byte) 0x0090, (byte) 0x00E0, (byte) 0x0090, (byte) 0x00E0, // B
            (byte) 0x00F0, (byte) 0x0080, (byte) 0x0080, (byte) 0x0080, (byte) 0x00F0, // C
            (byte) 0x00E0, (byte) 0x0090, (byte) 0x0090, (byte) 0x0090, (byte) 0x00E0, // D
            (byte) 0x00F0, (byte) 0x0080, (byte) 0x00F0, (byte) 0x0080, (byte) 0x00F0, // E
            (byte) 0x00F0, (byte) 0x0080, (byte) 0x00F0, (byte) 0x0080, (byte) 0x0080  // F
    };

    // -------------------- Private Methods --------------------

    // registers and memory
    private byte[] memory = new byte[4096];
    private byte[] vRegister = new byte[16];
    private short indexRegister = 0;
    private short programCounter = 512;

    // stack variables
    private short[] stack = new short[16];
    private short stackPointer = 0;

    // graphics "memory"
    private boolean[][] graphics = new boolean[32][64];

    // timers
    private short delayTimer = -1;
    private short soundTimer = -1;

    // rng
    private final Random rng = new Random();

    // general stuff
    private final EventListenerList ll = new EventListenerList();
    private final ClockSimulator delayClock;
    private final Keyboard keyboard;

    // -------------------- Constructors --------------------

    CPU(Keyboard keyboard, ClockSimulator delayClock) {
        this.delayClock = Objects.requireNonNull(delayClock);
        this.keyboard = Objects.requireNonNull(keyboard);
    }

    // -------------------- Default Methods --------------------

    final void addDebuggerListener(DebuggerListener l) {
        Objects.requireNonNull(l);
        ll.add(DebuggerListener.class, l);
    }

    final void addRenderListener(RenderListener l) {
        Objects.requireNonNull(l);
        ll.add(RenderListener.class, l);
    }

    final void initAndLoadRom(String romLocation) throws IOException {
        File romFile = new File(romLocation);
        if (!romFile.exists()) {
            throw new FileNotFoundException("File '%s' not found!".formatted(romLocation));
        }

        this.programCounter = 512;
        this.indexRegister = 0;
        this.stackPointer = 0;
        this.stack = new short[16];
        this.memory = new byte[4096];
        this.vRegister = new byte[16];
        this.graphics = new boolean[32][64];
        this.delayTimer = 0;
        this.soundTimer = 0;
        this.delayTimer = 0;
        this.soundTimer = 0;

        // load the system font set
        System.arraycopy(FONT_SET, 0, this.memory, 0, FONT_SET.length);

        // load the file contents into memory
        byte[] fileBytes = Files.readAllBytes(romFile.toPath());
        assert romFile.length() < memory.length - programCounter; // make sure we don't overrun memory
        System.arraycopy(fileBytes, 0, memory, programCounter, fileBytes.length);
        fireInit();
    }

    final void emulateCycle() {
        fireExecuteStateChanged();

        boolean render = false;

        byte highByte = memory[programCounter];
        byte lowByte = memory[programCounter + 1];
        programCounter += 2;

        short currentOpcode = (short) ((highByte << 8) | lowByte);
        System.out.println(toHex(currentOpcode));
        short nnn = (short) (currentOpcode & 0x0FFF);
        byte n = (byte) (currentOpcode & 0x000F);
        byte x = (byte) (highByte & 0x0F);
        byte y = (byte) ((lowByte & 0xF0) >> 4);

        byte highNibble = (byte) ((highByte & 0xF0) >> 4);
        switch (highNibble) {
            case 0x0:
                render = do0X(currentOpcode);
                break;
            case 0x1:
                // 1NNN - Jumps to memory address NNN
                programCounter = nnn; // probably should check that this is in bounds
                break;
            case 0x2:
                // 2NNN - Jumps to subroutine at NNN
                stack[stackPointer++] = programCounter;
                programCounter = nnn; // probably should check that this is in bounds
                break;
            case 0x3:
                // 3XNN - Skips the next instruction if VX equals NN
                if (isEqual(vRegister[x], lowByte)) {
                    programCounter += 2;
                }
                break;
            case 0x4:
                // 4XNN - Skips the next instruction if VX doesn't equal NN
                if (!isEqual(vRegister[x], lowByte)) {
                    programCounter += 2;
                }
                break;
            case 0x5:
                // 5XY0 - Skips the next instruction if VX equals VY.
                if (isEqual(vRegister[x], vRegister[y])) {
                    programCounter += 2;
                }
                break;
            case 0x6:
                // 6XNN - Sets VX to NN
                vRegister[x] = lowByte;
                break;
            case 0x7:
                // 7XNN - Adds NN to VX
                vRegister[x] += lowByte;
                break;
            case 0x8:
                do8XY(n, x, y);
                break;
            case 0x9:
                // 9XY0 - Skips the next instruction if VX doesn't equal VY
                if (!isEqual(vRegister[x], vRegister[y])) {
                    programCounter += 2;
                }
                break;
            case 0xA:
                /*
                 *  ANNN - LD I, addr
                 *  Set I = nnn.
                 *
                 *  The value of register I is set to nnn.
                 */
                indexRegister = nnn;
                break;
            case 0xB:
                // BNNN - Jumps to the address NNN plus V0
                programCounter = (short) (nnn + (short) vRegister[0x0]);
                break;
            case 0xC:
                // CXNN - Sets VX to a random number and NN
                short randomShort = (short) rng.nextInt(255);
                vRegister[x] = (byte) ((randomShort & 0x00FF) & lowByte);
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

        delayClock.withClockRegulation(() -> {
            delayTimer = (short) Math.max(0, delayTimer - 1);
            if (soundTimer > 0) {
                System.out.println("Beep!");
            }
            soundTimer = (short) Math.max(0, soundTimer - 1);
        });

        if (render) {
            fireRenderNeeded();
        }
    }

    // -------------------- Private Methods --------------------

    private boolean do0XD(short n, short x, short y) {
        // DXYN - Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels.
        // The interpreter reads n bytes from memory, starting at the address stored in I. These bytes are then displayed as sprites
        // on screen at coordinates (Vx, Vy). Sprites are XORed onto the existing screen. If this causes any pixels to
        // be erased, VF is set to 1, otherwise it is set to 0. If the sprite is positioned so part of it is outside the
        // coordinates of the display, it wraps around to the opposite side of the screen. See instruction 8xy3 for more
        // information on XOR, and section 2.4, Display, for more information on the Chip-8 screen and sprites.

        boolean collision = false;
        int xCoord = vRegister[x];
        int yCoord = vRegister[y];
        for (int i = 0; i < n; i++) {
            byte spriteLine =  memory[indexRegister + i];
            boolean[] bitLine = new boolean[8];
            bitLine[0] = ((byte) ((spriteLine & 0b1000_0000) >> 7)) == 1; // bit 8 (MSB)
            bitLine[1] = ((byte) ((spriteLine & 0b0100_0000) >> 6)) == 1; // bit 6
            bitLine[2] = ((byte) ((spriteLine & 0b0010_0000) >> 5)) == 1; // bit 6
            bitLine[3] = ((byte) ((spriteLine & 0b0001_0000) >> 4)) == 1; // bit 5
            bitLine[4] = ((byte) ((spriteLine & 0b0000_1000) >> 3)) == 1; // bit 4
            bitLine[5] = ((byte) ((spriteLine & 0b0000_0100) >> 2)) == 1; // bit 3
            bitLine[6] = ((byte) ((spriteLine & 0b0000_0010) >> 1)) == 1; // bit 2
            bitLine[7] = ((byte) (spriteLine  & 0b0000_0001))       == 1; // bit 1 (LSB)

            for (int j = 0; j < bitLine.length; j++) {
                boolean current = graphics[yCoord + i][xCoord + j];
                boolean newValue = bitLine[j];
                boolean flipped = current || newValue;
                if (current != flipped) {
                    collision = true;
                }
                graphics[yCoord + i][xCoord + j] = newValue;
            }
        }

        if (collision) {
            vRegister[0xF] = 1;
        } else {
            vRegister[0xF] = 0;
        }

        return true;
    }

    private boolean do0X(short currentOpcode) {
        if (isEqual(currentOpcode, 0x00E0)) {
            // 00E0 - Clear the screen
            graphics = new boolean[32][64];
            return true;
        } else if (isEqual(currentOpcode, 0x00EE)) {
            // 00EE - Returns from a subroutine
            programCounter = stack[--stackPointer];
        } else {
            // 0NNN - Calls RCA 1802 program at address NNN. Ignored by modern interpreters.
            System.out.println("0NNN called: Ignoring");
        }
        return false;
    }

    @SuppressWarnings("EnhancedSwitchMigration")
    private void do8XY(short n, short x, short y) {
        switch (n) {
            case 0x0:
                // 8XY0 - Sets VX to the value of VY
                vRegister[x] = vRegister[y];
                break;
            case 0x1:
                // 8XY1 - Sets VX to VX or VY
                vRegister[x] = (byte) (vRegister[x] | vRegister[y]);
                break;
            case 0x2:
                // 8XY2 - Sets VX to VX and VY
                vRegister[x] = (byte) (vRegister[x] & vRegister[y]);
                break;
            case 0x3:
                // 8XY3 - Sets VX to VX xor VY
                vRegister[x] = (byte) (vRegister[x] ^ vRegister[y]);
                break;
            case 0x4:
                // 8XY4 - Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't
                int result = ((int) vRegister[x]) + ((int) vRegister[y]);
                if (result > 255) {
                    vRegister[0xF] = 1;
                } else {
                    vRegister[0xF] = 0;
                }
                vRegister[x] = (byte)(result & 0x00FF);
                break;
            case 0x5:
                /*
                 * 8xy5 - SUB Vx, Vy
                 * Set Vx = Vx - Vy, set VF = NOT borrow.
                 *
                 * If Vx > Vy, then VF is set to 1, otherwise 0. Then Vy is subtracted from Vx, and the results stored in Vx.
                 */
                if (vRegister[x] > vRegister[y]) {
                    vRegister[0xF] = 1;
                } else {
                    vRegister[0xF] = 0;
                }
                vRegister[x] = (byte)(((short) vRegister[x]) - ((short) vRegister[y]));
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
                vRegister[x] = (byte) ((short) vRegister[y] - ((short) vRegister[x]));
                break;
            case 0xE:
                // 8XYE - Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift
                vRegister[0xF] = (byte) ((vRegister[x] >> 4) & 0b0001);
                vRegister[x] = (byte) (vRegister[x] << 1);
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

    @SuppressWarnings("EnhancedSwitchMigration")
    private void doFX(short lowByte, short x) {
        switch (lowByte) {
            case 0x07:
                // FX07 - Sets VX to the value of the delay timer
                vRegister[x] = (byte) delayTimer;
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
                indexRegister += ((short) (vRegister[x]));
                break;
            case 0x29:
                // FX29 - Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are
                // represented by a 4x5 font
                indexRegister = (short) (((short) vRegister[x]) * 5);
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

        byte[] registerCopy = new byte[vRegister.length];
        System.arraycopy(vRegister, 0, registerCopy, 0, vRegister.length);
        MachineState state = new MachineState(programCounter, registerCopy);
        for (DebuggerListener l : ll.getListeners(DebuggerListener.class)) {
            l.executionStarted(state, operations);
        }
    }

    private void fireExecuteStateChanged() {
        byte[] registerCopy = new byte[vRegister.length];
        System.arraycopy(vRegister, 0, registerCopy, 0, vRegister.length);
        MachineState state = new MachineState(programCounter, registerCopy);
        for (DebuggerListener l : ll.getListeners(DebuggerListener.class)) {
            l.machineStateChanged(state);
        }
    }

    private void fireRenderNeeded() {
        boolean[][] graphicsCopy = new boolean[32][64];
        arrayCopy(graphics, graphicsCopy);
        for (RenderListener l : ll.getListeners(RenderListener.class)) {
            l.render(graphicsCopy);
        }
    }

    public static void arrayCopy(boolean[][] src, boolean[][] dest) {
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, src[i].length);
        }
    }
}