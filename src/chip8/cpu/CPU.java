package chip8.cpu;

import chip8.hardware.ClockSimulator;
import chip8.hardware.Keyboard;
import chip8.hardware.PCSpeaker;
import chip8.hardware.RenderListener;
import chip8.ui.DebuggerListener;
import chip8.ui.MachineState;

import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static chip8.util.Utilities.arrayCopy;
import static chip8.util.Utilities.isEqual;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class CPU {

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

    // flags
    private boolean renderFlag;

    // rng
    private final Random rng = new Random();

    // general stuff
    private final EventListenerList ll = new EventListenerList();
    private final Keyboard keyboard;
    private final PCSpeaker speaker;
    private final ExecutorService ex = Executors.newSingleThreadExecutor();
    private final Breakpointer breakpointer = new Breakpointer(false);

    // -------------------- Constructors --------------------

    public CPU(Keyboard keyboard, PCSpeaker speaker) {
        this.keyboard = Objects.requireNonNull(keyboard);
        this.speaker = speaker;
    }

    // -------------------- Public Methods --------------------

    public final void addDebuggerListener(DebuggerListener l) {
        Objects.requireNonNull(l);
        ll.add(DebuggerListener.class, l);
    }

    public final void addRenderListener(RenderListener l) {
        Objects.requireNonNull(l);
        ll.add(RenderListener.class, l);
    }

    public final void setShouldWait(boolean shouldWait) {}

    public final void initAndLoadRom(File romFile) throws IOException {
        ex.submit(() -> {
            if (!romFile.exists()) {
                throw new RuntimeException("File '%s' not found!".formatted(romFile.toPath()));
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
            byte[] fileBytes = new byte[0];
            try {
                fileBytes = Files.readAllBytes(romFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assert romFile.length() < memory.length - programCounter; // make sure we don't overrun memory
            System.arraycopy(fileBytes, 0, memory, programCounter, fileBytes.length);

            fireInit();
        });

        ClockSimulator delayClock = new ClockSimulator(60);
        delayClock.withClockRegulation(() -> {
            ex.submit(() -> {
                delayTimer = (short) Math.max(0, delayTimer - 1);
                soundTimer = (short) Math.max(0, soundTimer - 1);
                if (soundTimer == 0) {
                    speaker.endBeep();
                } else {
                    speaker.startBeepIfNotStarted();
                }
            });
            return true;
        });
    }

    public final ExecutionResult emulateCycle() {
        Future<ExecutionResult> f = ex.submit(() -> {
            renderFlag = false;

            // we hit the end
            if (programCounter >= memory.length) {
                return ExecutionResult.END_PROGRAM;
            }

            OperationState state = new OperationState(programCounter, memory);
            fireExecuteStateChanged(state);

            if (isEqual(state.getCurrentOpcode(), 0x0000)) {
                // current operation is empty
                return ExecutionResult.END_PROGRAM;
            }

            programCounter += 2;
            byte lowByte = state.getLowByte();
            short currentOpcode = state.getCurrentOpcode();
            short nnn = state.getNNN();
            byte n = state.getN();
            byte x = state.getX();
            byte y = state.getY();

            switch (state.getHighNibble()) {
                case 0x0 -> do0X(currentOpcode);
                case 0x1 -> do1X(nnn);
                case 0x2 -> do2X(nnn);
                case 0x3 -> do3X(lowByte, x);
                case 0x4 -> do4X(lowByte, x);
                case 0x5 -> do5X(x, y);
                case 0x6 -> do6X(lowByte, x);
                case 0x7 -> do7X(lowByte, x);
                case 0x8 -> do8XY(n, x, y);
                case 0x9 -> do9X(x, y);
                case 0xA -> doAX(nnn);
                case 0xB -> doBX(nnn);
                case 0xC -> doCX(lowByte, x);
                case 0xD -> do0XD(n, x, y);
                case 0xE -> doEX(n, x);
                case 0xF -> doFX(lowByte, x);
                default -> throw new IllegalArgumentException();
            }

            fireExecuteStateChanged(state);

            if (renderFlag) {
                fireRenderNeeded();
            }

            return ExecutionResult.OK;
        });

        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            return ExecutionResult.FATAL;
        }
    }

    // -------------------- Private Methods --------------------

    private void do0XD(short n, short x, short y) {
        // DXYN - Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels.
        // The interpreter reads n bytes from memory, starting at the address stored in I. These bytes are then displayed as sprites
        // on screen at coordinates (Vx, Vy). Sprites are XORed onto the existing screen. If this causes any pixels to
        // be erased, VF is set to 1, otherwise it is set to 0. If the sprite is positioned so part of it is outside the
        // coordinates of the display, it wraps around to the opposite side of the screen. See instruction 8xy3 for more
        // information on XOR, and section 2.4, Display, for more information on the Chip-8 screen and sprites.

        int xCoord = vRegister[x] % 64;
        int yCoord = vRegister[y] % 32;
        vRegister[0xF] = 0;

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
                int actualY = yCoord + i;
                if (actualY >= graphics.length) {
                    continue;
                }
                int actualX = xCoord + j;
                if (actualX >= graphics[actualY].length) {
                    continue;
                }
                boolean current = graphics[actualY][actualX];
                boolean newValue = bitLine[j];
                if (current && newValue) {
                    graphics[actualY][actualX] = false;
                    vRegister[0xF] = 1; // collision
                } else if (!current) {
                    graphics[actualY][actualX] = newValue;
                }
            }
        }
        renderFlag = true;
    }

    private void do0X(short currentOpcode) {
        if (isEqual(currentOpcode, 0x00E0)) {
            // 00E0 - Clear the screen
            graphics = new boolean[32][64];
            renderFlag = true;
        } else if (isEqual(currentOpcode, 0x00EE)) {
            // 00EE - Returns from a subroutine
            programCounter = stack[--stackPointer];
        } else {
            // 0NNN - Calls RCA 1802 program at address NNN. Ignored by modern interpreters.
            System.out.println("0NNN called: Ignoring");
        }
    }

    private void doCX(byte lowByte, byte x) {
        // CXNN - Sets VX to a random number and NN
        short randomShort = (short) rng.nextInt(255);
        vRegister[x] = (byte) ((randomShort & 0x00FF) & lowByte);
    }

    private void doBX(short nnn) {
        // BNNN - Jumps to the address NNN plus V0
        programCounter = (short) (nnn + (short) vRegister[0x0]);
    }

    private void doAX(short nnn) {
        /*
         *  ANNN - LD I, addr
         *  Set I = nnn.
         *
         *  The value of register I is set to nnn.
         */
        indexRegister = nnn;
    }

    private void do7X(byte lowByte, byte x) {
        // 7XNN - Adds NN to VX
        vRegister[x] += lowByte;
    }

    private void do9X(byte x, byte y) {
        // 9XY0 - Skips the next instruction if VX doesn't equal VY
        if (!isEqual(vRegister[x], vRegister[y])) {
            programCounter += 2;
        }
    }

    private void do6X(byte lowByte, byte x) {
        // 6XNN - Sets VX to NN
        vRegister[x] = lowByte;
    }

    private void do5X(byte x, byte y) {
        // 5XY0 - Skips the next instruction if VX equals VY.
        if (isEqual(vRegister[x], vRegister[y])) {
            programCounter += 2;
        }
    }

    private void do4X(byte lowByte, byte x) {
        // 4XNN - Skips the next instruction if VX doesn't equal NN
        if (!isEqual(vRegister[x], lowByte)) {
            programCounter += 2;
        }
    }

    private void do3X(byte lowByte, byte x) {
        // 3XNN - Skips the next instruction if VX equals NN
        if (isEqual(vRegister[x], lowByte)) {
            programCounter += 2;
        }
    }

    private void do2X(short nnn) {
        // 2NNN - Jumps to subroutine at NNN
        stack[stackPointer++] = programCounter;
        programCounter = nnn; // probably should check that this is in bounds
    }

    private void do1X(short nnn) {
        // 1NNN - Jumps to memory address NNN
        programCounter = nnn; // probably should check that this is in bounds
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
                short result = (short) ((((short) vRegister[x]) & 0x00FF) + (((short) vRegister[y]) & 0x00FF));
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
                // 8XYE - Store the value of register VX shifted left one bit in register VX
                //Set register VF to the most significant bit prior to the shift
                vRegister[0xF] = (byte) ((vRegister[x] & 0x00FF) >> 7);
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
                vRegister[x] = keyboard.waitForKeyPress();
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
                short num = (short) ((short) vRegister[x] & 0x00FF);
                memory[indexRegister] = (byte) (num / 100);
                memory[indexRegister + 1] = (byte) ((num / 10) % 10);
                memory[indexRegister + 2] = (byte) ((num % 100) % 10);
                break;
            case 0x55:
                // FX55 - Stores V0 to VX in memory starting at address I
                for (int registerIndex = 0; registerIndex <= x; registerIndex++) {
                    int memoryIndex = indexRegister + registerIndex;
                    memory[memoryIndex] = vRegister[registerIndex];
                }
                break;
            case 0x65:
                // FX65 - Fills V0 to VX with values from memory starting at address I
                for (int registerIndex = 0; registerIndex <= x; registerIndex++) {
                    int memoryIndex = indexRegister + registerIndex;
                    vRegister[registerIndex] = memory[memoryIndex];
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void fireInit() {
        OperationState initialState = new OperationState(programCounter, memory);
        byte[] registerCopy = new byte[vRegister.length];
        System.arraycopy(vRegister, 0, registerCopy, 0, vRegister.length);
        MachineState state = new MachineState(initialState, programCounter, registerCopy);
        for (DebuggerListener l : ll.getListeners(DebuggerListener.class)) {
            l.machineStateChanged(state);
        }
    }

    private void fireExecuteStateChanged(OperationState operationState) {
        byte[] registerCopy = new byte[vRegister.length];
        System.arraycopy(vRegister, 0, registerCopy, 0, vRegister.length);
        MachineState state = new MachineState(operationState, programCounter, registerCopy);
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

}