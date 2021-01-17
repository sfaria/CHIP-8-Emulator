package chip8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class CPU {

    // registers and memory
    private short currentOpcode = 0x0000;
    private byte[] memory = new byte[4096];
    private byte[] vRegister = new byte[16];
    private short indexRegister = 0x0000;
    private short programCounter = 0x2000;

    // graphics context
    private boolean[] graphics = new boolean[64 * 32];

    // timers
    private byte delayTimer = 0x00;
    private byte soundTimer = 0x00;

    // stack variables
    private short[] stack = new short[16];
    private short stackPointer = 0x0000;

    // rng
    private final Random rng = new Random();
    private Keyboard keyboard = null;

    // system font set
    private final byte[] fontset = new byte[]{
            (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0, // 0
            (byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70, // 1
            (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // 2
            (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 3
            (byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10, // 4
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 5
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 6
            (byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40, // 7
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 8
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 9
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, // A
            (byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, // B
            (byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, // C
            (byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, // D
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // E
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80  // F
    };

    final void init(Keyboard keyboard) {
        this.keyboard = keyboard;
        this.currentOpcode = 0x0000;
        this.programCounter = 0x200;
        this.indexRegister = 0x000;
        this.stackPointer = 0x000;
        this.stack = new short[16];
        this.memory = new byte[4096];
        this.vRegister = new byte[16];
        this.graphics = new boolean[64 * 32];
        this.delayTimer = 0x00;
        this.soundTimer = 0x00;
        this.delayTimer = 0;
        this.soundTimer = 0;

        // load the system font set
        IntStream.range(0, fontset.length)
                .forEach(index -> this.memory[index] = fontset[index]);
    }

    final void loadRom(String romLocation) throws IOException {
        File romFile = new File(romLocation);
        byte[] fileBytes = Files.readAllBytes(romFile.toPath());
        System.arraycopy(fileBytes, 0, memory, 512, fileBytes.length);
    }

    final boolean[] getGraphics() {
        boolean[] graphicsMemory = new boolean[graphics.length];
        System.arraycopy(graphics, 0, graphicsMemory, 0, graphicsMemory.length);
        return graphicsMemory;
    }

    final void emulateCycle(Runnable renderCallback) {
        boolean render = false;

        byte highByte = memory[programCounter];
        byte lowByte = memory[programCounter + 1];
        currentOpcode = (short) ((short) highByte << 8 | (short) lowByte);

        short nnn = (short) (currentOpcode & 0x0FFF);
        byte n = (byte) (currentOpcode & 0x000F);
        byte x = (byte) (highByte & 0x0F);
        byte y = (byte) ((lowByte & 0xF0) >> 2);
        byte nn = lowByte;

        programCounter += 2;

        byte topByte = (byte) ((currentOpcode & 0xF000) >> 12);
        if (currentOpcode == 0x00E0) {
            // 00E0 - Clear the screen
            graphics = new boolean[64 * 32];
            render = true;
        } else if (currentOpcode == 0x00EE) {
            // 00EE - Returns from a subroutine
            programCounter = stack[--stackPointer];
        } else if (topByte == 0x0) {
            // 0NNN - Calls RCA 1802 program at address NNN. Ignored by modern interpreters.
            System.out.println("0NNN called: Ignoring");
        } else if (topByte == 0x1) {
            // 1NNN - Jumps to memory address NNN
            programCounter = nnn;
        } else if (topByte == 0x2) {
            // 2NNN - Jumps to subroutine at NNN
            stack[stackPointer++] = programCounter;
            programCounter = nnn;
        } else if (topByte == 0x3) {
            // 3XNN - Skips the next instruction if VX equals NN
            if (vRegister[x] == nn) {
                programCounter += 2;
            }
        } else if (topByte == 0x4) {
            // 4XNN - Skips the next instruction if VX doesn't equal NN
            if (vRegister[x] != nn) {
                programCounter += 2;
            }
        } else if (topByte == 0x5) {
            // 5XY0 - Skips the next instruction if VX equals VY.
            if (vRegister[x] == vRegister[y]) {
                programCounter += 2;
            }
        } else if (topByte == 0x6) {
            // 6XNN - Sets VX to NN
            vRegister[x] = nn;
        } else if (topByte == 0x7) {
            // 7XNN - Adds NN to VX
            vRegister[x] += nn;
        } else if (topByte == 0x8 && n == 0x0) {
            // 8XY0 - Sets VX to the value of VY
            vRegister[x] = vRegister[y];
        } else if (topByte == 0x8 && n == 0x1) {
            // 8XY1 - Sets VX to VX or VY
            vRegister[x] |= vRegister[y];
        } else if (topByte == 0x8 && n == 0x2) {
            // 8XY2 - Sets VX to VX and VY
            vRegister[x] &= vRegister[y];
        } else if (topByte == 0x8 && n == 0x3) {
            // 8XY3 - Sets VX to VX xor VY
            vRegister[x] ^= vRegister[y];
        } else if (topByte == 0x8 && n == 0x4) {
            // 8XY4 - Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't
//            if (vRegister[y] > (0x00FF - vRegister[x])) {
//                vRegister[0xF] = 1; //carry
//            } else {
//                vRegister[0xF] = 0;
//            }
//            vRegister[x] += vRegister[y];
        } else if (topByte == 0x8 && n == 0x5) {
            // 8XY5 - VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't
//            int borrow = (vRegister[x] - vRegister[y]) & (1 << 16);
//            if (borrow) {
//                vRegister[0xF] = 0;
//            } else {
//                vRegister[0xF] = 1;
//            }
        } else if (topByte == 0x8 && n == 0x6) {
            // 8XY6 - Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift
            vRegister[0xF] = (byte) (vRegister[x] & 0b0001);
            vRegister[x] = (byte) (vRegister[x] >> 1);
        } else if (topByte == 0x8 && n == 0x7) {
            // 8XY7 - Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't
            vRegister[x] = (byte) (vRegister[y] - vRegister[x]);
//            int borrow = vRegister[x] & (1 << 16);
//            if (borrow) {
//                vRegister[0xF] = 0;
//            } else {
//                vRegister[0xF] = 1;
//            }
        } else if (topByte == 0x8 && n == 0xE) {
            // 8XYE - Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift
            vRegister[0xF] = (byte) ((vRegister[x] & 0b1000) >> 3);
            vRegister[x] = (byte) (vRegister[x] >> 1);
        } else if (topByte == 0x9 && n == 0x0) {
            // 9XY0 - Skips the next instruction if VX doesn't equal VY
            if (x != y) {
                programCounter += 2;
            }
        } else if (topByte == 0xA) {
            // ANNN - Sets I to the address NNN
            indexRegister = nnn;
        } else if (topByte == 0xB) {
            // BNNN - Jumps to the address NNN plus V0
            programCounter = (short) (nnn + vRegister[0x0]);
        } else if (topByte == 0xC) {
            // CXNN - Sets VX to a random number and NN
            byte[] randomByte = new byte[1];
            rng.nextBytes(randomByte);
            vRegister[x] = (byte) (randomByte[0] & nn);
        } else if (topByte == 0xD) {
            // DXYN - Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels.
            // Each row of 8 pixels is read as bit-coded (with the most significant bit of each byte displayed on the left)
            // starting from memory location I; I value doesn't change after the execution of this instruction. As described
            // above, VF is set to 1 if any screen pixels are flipped from set to unset when the sprite is drawn, and to 0
            // if that doesn't happen
            int height = n;
            vRegister[0xF] = 0;
            for (int row = 0; row < height; row++) {
                byte pixel = memory[indexRegister + row];
                for (int col = 0; col < 8; col++) {
                    if ((pixel & (0x80 >> row)) != 0) {
                        int index = x + col + ((y + row) * 64);
                        if (graphics[index]) {
                            vRegister[0xF] = 1;
                        }
                        graphics[index] = !graphics[index];
                    }
                }
            }
            render = true;
        } else if (topByte == 0xE && n == 0xE) {
            // EX9E - Skips the next instruction if the key stored in VX is pressed
            if (keyboard.getKeyPressState(vRegister[x])) {
                programCounter += 2;
            }
        } else if (topByte == 0xE && n == 0x1) {
            // EXA1 - Skips the next instruction if the key stored in VX isn't pressed
            if (keyboard.getKeyPressState(vRegister[x])) {
                programCounter += 2;
            }
        } else if (topByte == 0xF && n == 0x7) {
            // FX07 - Sets VX to the value of the delay timer
            vRegister[x] = delayTimer;
        } else if (topByte == 0xF && n == 0xA) {
            // FX0A - A key press is awaited, and then stored in VX
            vRegister[x] = (byte) keyboard.waitForKeyPress();
        } else if (topByte == 0xF && n == 0x5) {
            // FX15 - Sets the delay timer to VX
            delayTimer = vRegister[x];
        } else if (topByte == 0xF && n == 0x8) {
            // FX18 - Sets the sound timer to VX
            soundTimer = vRegister[x];
        } else if (topByte == 0xF && n == 0xE) {
            // FX1E - Adds VX to I
            indexRegister += vRegister[x];
        } else if (topByte == 0xF && lowByte == 0x29) {
            // FX29 - Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are
            // represented by a 4x5 font
            indexRegister = (short) (vRegister[x] * 5);
        } else if (topByte == 0xF && lowByte == 0x33) {
            // FX33 - Stores the Binary-coded decimal representation of VX, with the most significant of three digits at
            // the address in I, the middle digit at I plus 1, and the least significant digit at I plus 2. (In other
            // words, take the decimal representation of VX, place the hundreds digit in memory at location in I, the tens
            // digit at location I+1, and the ones digit at location I+2.)
            memory[indexRegister] = (byte) (vRegister[x] / 100);
            memory[indexRegister + 1] = (byte) ((vRegister[x] / 10) % 10);
            memory[indexRegister + 2] = (byte) ((vRegister[x] % 100) % 10);
        } else if (topByte == 0xF && lowByte == 0x55) {
            // FX55 - Stores V0 to VX in memory starting at address I
            System.arraycopy(vRegister, 0, memory, indexRegister, x);
        } else if (topByte == 0xF && lowByte == 0x65) {
            // FX65 - Fills V0 to VX with values from memory starting at address I
            System.arraycopy(memory, indexRegister, vRegister, 0, x);
        } else {
            throw new IllegalArgumentException("Unknown currentOpcode: ${currentOpcode}");
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
            renderCallback.run();
        }
    }

}