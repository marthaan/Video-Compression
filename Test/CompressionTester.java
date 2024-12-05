package Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CompressionTester {
    private int n1 = 10;        // foreground quantization step
    private int n2 = 20;        // background quantization step
    
    private static final int MACROBLOCK_SIZE = 4;
    private static final int BLOCK_SIZE = 2;
    private static final int CHANNEL_SIZE = 3;
    
    // create a 16x16x3 macroblock with random values
    private int[][][] makeRandomMacroblock() {
        int[][][] macroblock = new int[MACROBLOCK_SIZE][MACROBLOCK_SIZE][3];
        Random random = new Random();

        for (int x = 0; x < MACROBLOCK_SIZE; x++) {
            for (int y = 0; y < MACROBLOCK_SIZE; y++) {
                for (int z = 0; z < CHANNEL_SIZE; z++) {
                    macroblock[x][y][z] = random.nextInt(256); // Random value in [0, 255]
                }
            }
        }

        return macroblock;
    }

    // print single block
    private void printBlock(int[][][] block, String header) {
        System.out.println(header);
        
        for (int row = 0; row < block.length; row++) {
            for (int col = 0; col < block[0].length; col++) {
                System.out.print("[");
                
                for (int channel = 0; channel < CHANNEL_SIZE; channel++) {
                    System.out.print(block[row][col][channel] + (channel < 2 ? ", " : ""));
                }
                
                System.out.print("] ");
            }
            
            System.out.println();
        }
    }

    // print list of blocks
    private void printBlockList(List<int[][][]> blocks, String header) {
        System.out.println(header);

        for (int b = 0; b < blocks.size(); b++) {
            int[][][] block = blocks.get(b);
            
            printBlock(block,"Block " + b + ":");
        }
    }

    // divide macroblock into four 8x8 blocks
    private List<int[][][]> block(int[][][] macroblock) {
        List<int[][][]> blocks = new ArrayList<>();     // want top left, top right, bottom left, bottom right

        // iterate over the whole macroblock, block-by-block
        for (int row = 0; row < MACROBLOCK_SIZE; row += BLOCK_SIZE) {
            for (int col = 0; col < MACROBLOCK_SIZE; col += BLOCK_SIZE) {
                int[][][] block = new int[BLOCK_SIZE][BLOCK_SIZE][CHANNEL_SIZE];

                // iterate within each smaller block
                for (int r = 0; r < BLOCK_SIZE; r++) {
                    for (int c = 0; c < BLOCK_SIZE; c++) {
                        block[r][c][0] = macroblock[row + r][col + c][0];
                        block[r][c][1] = macroblock[row + r][col + c][1];
                        block[r][c][2] = macroblock[row + r][col + c][2];
                    }
                }

                blocks.add(block);
            }
        }

        return blocks;
    }

    // run dct on all 4 blocks
    private List<int[][][]> dct(List<int[][][]> blocks) {
        List<int[][][]> dctBlocks = new ArrayList<>();
        
        for (int[][][] block : blocks) {
            // frequency coefficients for the current block
            int[][][] dctBlock = new int[BLOCK_SIZE][BLOCK_SIZE][3];

            // iterate over rows and cols of the block
            for (int u = 0; u < BLOCK_SIZE; u++) {
                for (int v = 0; v < BLOCK_SIZE; v++) {
                    double dctRed = calculateDct(u, v, block, 0);
                    double dctGreen = calculateDct(u, v, block, 1);
                    double dctBlue = calculateDct(u, v, block, 2);

                    dctBlock[u][v][0] = (int) Math.round(dctRed);
                    dctBlock[u][v][1] = (int) Math.round(dctGreen);
                    dctBlock[u][v][2] = (int) Math.round(dctBlue);
                }
            }

            dctBlocks.add(dctBlock);
        }

        return dctBlocks;
    }

    // calculate dct for a specific channel 
    private double calculateDct(int u, int v, int[][][] block, int channel) {
        double dct = 0.0;
        
        double sum = 0.0;

        double[] scalars = getScalars(u, v);
        double scalar = (1.0 / 4.0) * scalars[0] * scalars[1];

        for (int x = 0; x < BLOCK_SIZE; x++) {
            for (int y = 0; y < BLOCK_SIZE; y++) {
                sum += block[x][y][channel] * Math.cos(((2 * x + 1) * u * Math.PI) / (2 * BLOCK_SIZE)) * 
                    Math.cos(((2 * y + 1) * v * Math.PI) / (2 * BLOCK_SIZE));
            }
        }

        dct = scalar * sum;

        return dct;
    }

    // calculate scalars for dct calculation
    private double[] getScalars(int u, int v) {
        double[] scalars = new double[2];

        scalars[0] = 1.0;
        scalars[1] = 1.0;

        if (u == 0) {
            scalars[0] *= (1.0 / Math.sqrt(2));
        }
        if (v == 0) {
            scalars[1] *= (1.0 / Math.sqrt(2));
        }

        return scalars;
    }

    // quantize all 4 dct blocks
    private List<int[][][]> quantize(List<int[][][]> dctBlocks, int layer) {
        List<int[][][]> quantizedBlocks = new ArrayList<>();

        int step = 0;
        
        // if foreground block
        if (layer == 0) {   
            step = n1;
            
        }
        // if background block
        else {  
            step = n2;
        }

        // iterate over each pixel & quantize each of its channel values
        for (int[][][] dctBlock : dctBlocks) {
            int[][][] quantizedBlock = new int[BLOCK_SIZE][BLOCK_SIZE][3];
            
            for (int x = 0; x < BLOCK_SIZE; x++) {
                for (int y = 0; y < BLOCK_SIZE; y++) {
                    quantizedBlock[x][y][0] = (int) Math.round(dctBlock[x][y][0] / step);   // red / step
                    quantizedBlock[x][y][1] = (int) Math.round(dctBlock[x][y][1] / step);   // green / step
                    quantizedBlock[x][y][2] = (int) Math.round(dctBlock[x][y][2] / step);   // blue / step
                }
            }

            quantizedBlocks.add(quantizedBlock);
        }

        return quantizedBlocks;
    }

    public static void main(String[] args) {
        CompressionTester tester = new CompressionTester();
        
        System.out.println("---STEP 1: GENERATE MACROBLOCK---");
        int[][][] macroblock = tester.makeRandomMacroblock();
        tester.printBlock(macroblock, "Random macroblock:");

        System.out.println("\n---STEP 2: BLOCK MACROBLOCK---");
        List<int[][][]> blocks = tester.block(macroblock);
        tester.printBlockList(blocks, "List of original blocks");

        System.out.println("\n---STEP 3: APPLY DCT---");
        List<int[][][]> dctBlocks = tester.dct(blocks);
        tester.printBlockList(dctBlocks, "List of DCT blocks");

        System.out.println("\n---STEP 4: QUANTIZE BLOCKS (foreground)---");
        List<int[][][]> quantizedBlocksForeground = tester.quantize(dctBlocks, 0);
        tester.printBlockList(quantizedBlocksForeground, "List of quantized foreground blocks (n1 = " + tester.n1 + ")");

        System.out.println("\n---STEP 5: QUANTIZE BLOCKS (background)---");
        List<int[][][]> quantizedBlocksBackground = tester.quantize(dctBlocks, 1);
        tester.printBlockList(quantizedBlocksBackground, "List of quantized background blocks (n2 = " + tester.n2 + ")");
    }
}
