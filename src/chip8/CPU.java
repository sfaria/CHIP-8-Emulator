package chip8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.stream.IntStream;

/**
 *
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
    private final byte[] fontset = new byte[] {
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

        currentOpcode = (short) (memory[programCounter] << 8 | memory[programCounter + 1]);
        short x = (short) ((currentOpcode & 0x0F00) >> 8);
        short y = (short) ((currentOpcode & 0x00F0) >> 4);

        programCounter += 2;

        if ((currentOpcode & 0xF000) == 0x0000) {
            // 0NNN - Calls RCA 1802 program at address NNN.
            throw new UnsupportedOperationException("Un-supported opcode (0x0nnn): ${currentOpcode}");
        } else if (currentOpcode == 0x00E0) {
            // 00E0 - Clear the screen
            graphics = new boolean[64 * 32];
            render = true;
        } else if (currentOpcode == 0x00EE) {
            // 00EE - Returns from a subroutine
            programCounter = stack[--stackPointer];
        } else if ((currentOpcode & 0xF000) == 0x1000) {
            // 1NNN - Jumps to memory address NNN
            programCounter = currentOpcode & 0x0FFF;
        } else if ((currentOpcode & 0xF000) == 0x2000) {
            // 2NNN - Jumps to subroutine at NNN
            stack[stackPointer++] = programCounter;
            programCounter = currentOpcode & 0x0FFF;
        } else if ((currentOpcode & 0xF000) == 0x3000) {
            // 3XNN - Skips the next instruction if VX equals NN
            if (vRegister[x] == (currentOpcode & 0x00FF))  {
                programCounter += 2;
            }
        } else if ((currentOpcode & 0xF000) == 0x4000) {
            // 4XNN - Skips the next instruction if VX doesn't equal NN
            if (vRegister[x] != (currentOpcode & 0x00FF))  {
                programCounter += 2;
            }
        } else if ((currentOpcode & 0xF000) == 0x5000) {
            // 5XY0 - Skips the next instruction if VX equals VY.
            if (vRegister[x] == vRegister[y]) {
                programCounter += 2;
            }
        } else if ((currentOpcode & 0xF000) == 0x6000) {
            // 6XNN - Sets VX to NN
            vRegister[x] = currentOpcode & 0x00FF;
        } else if ((currentOpcode & 0xF000) == 0x7000) {
            // 7XNN - Adds NN to VX
            vRegister[x] += (currentOpcode & 0x00FF);
        } else if ((currentOpcode & 0xF00F) == 0x8000) {
            // 8XY0 - Sets VX to the value of VY
            vRegister[x] = vRegister[y];
        } else if ((currentOpcode & 0xF00F) == 0x8001) {
            // 8XY1 - Sets VX to VX or VY
            vRegister[x] |= vRegister[y];
        } else if ((currentOpcode & 0xF00F) == 0x8002) {
            // 8XY2 - Sets VX to VX and VY
            vRegister[x] &= vRegister[y];
        } else if ((currentOpcode & 0xF00F) == 0x8003) {
            // 8XY3 - Sets VX to VX xor VY
            vRegister[x] ^= vRegister[y];
        } else if ((currentOpcode & 0xF00F) == 0x8004) {
            // 8XY4 - Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't
            if(vRegister[y] > (0x00FF - vRegister[x])) {
                vRegister[0xF] = 1; //carry
            } else {
                vRegister[0xF] = 0;
            }
            vRegister[x] += vRegister[y];
        } else if ((currentOpcode & 0xF00F) == 0x8005) {
            // 8XY5 - VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't
            int borrow = (vRegister[x] - vRegister[y]) & (1 << 16);
            if (borrow) {
                vRegister[0xF] = 0;
            } else {
                vRegister[0xF] = 1;
            }
        } else if ((currentOpcode & 0xF00F) == 0x8006) {
            // 8XY6 - Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift
            vRegister[0xF] = vRegister[x] & (-vRegister[x]);
            vRegister[x] = vRegister[x] >> 1;
        } else if ((currentOpcode & 0xF00F) == 0x8007) {
            // 8XY7 - Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't
            vRegister[x] = vRegister[y] - vRegister[x];
            int borrow = vRegister[x] & (1<<16);
            if (borrow) {
                vRegister[0xF] = 0;
            } else {
                vRegister[0xF] = 1;
            }
        } else if ((currentOpcode & 0xF00F) == 0x800E) {
            // 8XYE - Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift
            if ((vRegister[x] & 0xF000) == vRegister[0xF]) {
                vRegister[x] = vRegister[x] << 1;
            }
        } else if ((currentOpcode & 0xF000) == 0x9000) {
            // 9XY0 - Skips the next instruction if VX doesn't equal VY
            if (x != y) {
                programCounter += 2;
            }
        } else if ((currentOpcode & 0xF000) == 0xA000) {
            // ANNN - Sets I to the address NNN
            indexRegister = currentOpcode & 0x0FF;
        } else if ((currentOpcode & 0xF000) == 0xB000) {
            // BNNN - Jumps to the address NNN plus V0
            programCounter = (currentOpcode & 0x0FFF) + vRegister[0x0];
        } else if ((currentOpcode & 0xF000) == 0xC000) {
            // CXNN - Sets VX to a random number and NN
            byte[] randomByte = new byte[1];
            rng.nextBytes(randomByte);
            vRegister[x] = randomByte[0] & (currentOpcode & 0x00FF);
        } else if ((currentOpcode & 0xF000) == 0xD000) {
            // DXYN - Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels.
            // Each row of 8 pixels is read as bit-coded (with the most significant bit of each byte displayed on the left)
            // starting from memory location I; I value doesn't change after the execution of this instruction. As described
            // above, VF is set to 1 if any screen pixels are flipped from set to unset when the sprite is drawn, and to 0
            // if that doesn't happen
            int height = (currentOpcode & 0x000F) >> 8;
            vRegister[0xF] = 0;
            for (int row = 0; row < height; row++) {
                byte pixel = memory[indexRegister + row];
                for (int col = 0; col < 8; col++) {
                    if((pixel & (0x80 >> row)) != 0) {
                        int index = x + col + ((y + row) * 64);
                        if(graphics[index]) {
                            vRegister[0xF] = 1;
                        }
                        graphics[index] = !graphics[index];
                    }
                }
            }
            render = true;
        } else if ((currentOpcode & 0xF00F) == 0xE00E) {
            // EX9E - Skips the next instruction if the key stored in VX is pressed
            if (keyboard.getKeyPressState(vRegister[x])) {
                programCounter += 2;
            }
        } else if ((currentOpcode & 0xF00F) == 0xE001) {
            // EXA1 - Skips the next instruction if the key stored in VX isn't pressed
            if (keyboard.getKeyPressState(vRegister[x])) {
                programCounter += 2;
            }
        } else if ((currentOpcode & 0xF00F) == 0xF007) {
            // FX07 - Sets VX to the value of the delay timer
            vRegister[x] = delayTimer;
        } else if ((currentOpcode & 0xF00F) == 0xF00A) {
            // FX0A - A key press is awaited, and then stored in VX
            vRegister[x] = keyboard.waitForKeyPress();
            System.out.println("Key stored in vRegister[x]: ${vRegister[x]}");
        } else if ((currentOpcode & 0x0F00) == 0xF015) {
            // FX15 - Sets the delay timer to VX
            delayTimer = vRegister[x];
        } else if ((currentOpcode & 0x0F00) == 0xF018) {
            // FX18 - Sets the sound timer to VX
            soundTimer = vRegister[x];
        } else if ((currentOpcode & 0x0F00) == 0xF01E) {
            // FX1E - Adds VX to I
            indexRegister += vRegister[x];
        } else if ((currentOpcode & 0x0F00) == 0xF029) {
            // FX29 - Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are
            // represented by a 4x5 font
            indexRegister = vRegister[x] * 5;
        } else if ((currentOpcode & 0x0F00) == 0xF033) {
            // FX33 - Stores the Binary-coded decimal representation of VX, with the most significant of three digits at
            // the address in I, the middle digit at I plus 1, and the least significant digit at I plus 2. (In other
            // words, take the decimal representation of VX, place the hundreds digit in memory at location in I, the tens
            // digit at location I+1, and the ones digit at location I+2.)
            memory[indexRegister] = vRegister[x] / 100;
            memory[indexRegister + 1] = (vRegister[x] / 10) % 10;
            memory[indexRegister + 2] = (vRegister[x] % 100) % 10;
        } else if ((currentOpcode & 0x0F00) == 0xF055) {
            // FX55 - Stores V0 to VX in memory starting at address I
            for (int offset = 0; offset < x; offset++) {
                memory[indexRegister + offset] = vRegister[offset];
            }
        } else if ((currentOpcode & 0x0F00) == 0xF065) {
            // FX65 - Fills V0 to VX with values from memory starting at address I
            for (int offset = 0; offset < x; offset++) {
                vRegister[offset] = memory[indexRegister + offset];
            }
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