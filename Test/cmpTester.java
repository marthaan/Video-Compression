package Test;

import java.io.*;
import java.util.*;

public class cmpTester {

    private static final int BLOCK_SIZE = 8; // Size of the macroblock (8x8)

    public static void main(String[] args) {
        // String inputFile = "/Users/marthaannwilliams/Desktop/2frames.cmp";  // Input .cmp file
        String inputFile = "/Users/marthaannwilliams/Desktop/shortvideo.cmp";   // 1 frame 
        String outputFile = "Test/cmpTester.out"; // Output text file to save readable data

        try (DataInputStream dis = new DataInputStream(new FileInputStream(inputFile)); BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // Read n1, n2
            int n1 = dis.readInt();
            int n2 = dis.readInt();
            writer.write("n1, n2: " + n1 + ", " + n2 + "\n\n");

            int blockCount = 0; 

            while (dis.available() > 0) {
                blockCount += 1;
                writer.write("BLOCK NUMBER: " + blockCount + "\n");
                
                // Read block type (int)
                int blockType = dis.readInt();
                writer.write("Block Type: " + blockType + "\n");

                // Read quantized blocks
                for (int block = 0; block < 1; block++) { // assuming each macroblock is one quantized block
                    // Each quantized block has three color channels (R, G, B)
                    for (int channel = 0; channel < 3; channel++) {
                        writer.write("Channel " + (channel == 0 ? "R" : (channel == 1 ? "G" : "B")) + ":\n");

                        // Read the 8x8 block of coefficients for the current channel
                        for (int row = 0; row < BLOCK_SIZE; row++) {
                            for (int col = 0; col < BLOCK_SIZE; col++) {
                                if (dis.available() != 0) {
                                    int coefficient = dis.readInt(); // Read coefficient for current channel
                                    writer.write(coefficient + "\t");
                                }
                                
                            }
                            writer.write("\n");
                        }
                    }
                }
                writer.write("\n\n");  // Separate blocks by new lines for readability
            }

            System.out.println("Successfully converted .cmp file to text format!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

