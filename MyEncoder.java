import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyEncoder {
    private File inputFile; // input file for each instance 
    private int n1;         // foreground quantization step
    private int n2;         // background quantization step

    private static final int WIDTH = 960;                       // width of each frame
    private static final int HEIGHT = 540;                      // height of each frame
    private static final int CHANNEL_SIZE = WIDTH * HEIGHT;     // 518,400 bytes per channel (per frame)
    private static final int FRAME_SIZE = CHANNEL_SIZE * 3;     // 1,555,200 total bytes per frame

    private static final int MACROBLOCK_SIZE = 16;
    private static final int BLOCK_SIZE = 8;
    private static final int SEARCH_PARAMETER_K = 5;
    
    private int[] prevFrame;
    private int[][][] prevFrame3DArray;
    // don't think we need prevMacroblocks
    private List<int[][][]> prevMacroblocks;

    private int[] currFrame;
    private int[][][] currFrame3DArray;
    private List<int[][][]> currMacroblocks;
    private List<Integer> layers;

    /**
     * Constructor
     * @param inputFile input .rgb file for each instance
     * @param n1 foreground quantization step
     * @param n2 background quantization step 
     */
    public MyEncoder(File inputFile, int n1, int n2) {
        this.inputFile = inputFile; 
        this.n1 = n1;
        this.n2 = n2;

        prevFrame = new int[FRAME_SIZE];
        prevMacroblocks = new ArrayList<>();

        currFrame = new int[FRAME_SIZE];
        // initialize everything?
        currMacroblocks = new ArrayList<>();
    }
    

    // ----- PART 0: PRE-PROCESS FILE DATA -----
    
    /**
     * Reads the entire .rgb file frame-by-frame
     * Processes each frame based on frame type (I-frame vs. P-frame)
     */
    public void readFile() {
        try {
            FileInputStream fis = new FileInputStream(inputFile);

            // got ahead of myself and wrote this output code... can uncomment this later
            // make sure to also uncomment fos.close();
            // create output file name by changing file.rgb to file.cmp
/*          String fileName = inputFile.getName();
            fileName.substring(0, fileName.length() - 3);
            File outputFile = new File(fileName + "cmp");
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(n1);
            fos.write(n2); */
            
            for (int i = 0; readFrame(fis); i++) {
                formatFrame();
                // if not I-frame --> if P-frame
                if (i != 0) {
                    // PART 1: VIDEO SEGMENTATION
                    // not sure if this is the best way to store all of this (maybe 1D is better) - but for now at least lines up all the info fine
                    // goal: macroblocks[i] has motion vector at motionVectors[i] and has layer type at layers[i]
                    currMacroblocks = macroblock();             
                    List<int[]> motionVectors = generateMotionVectorArray(currMacroblocks);
                    List<Integer> layers = getLayers();

                    // PART 2: COMPRESSION
                    // List<int[][][]> blocks = block(currMacroblocks);
                    // List<int[][][]> dctBlocks = dct(blocks);
                    // List<int[][][]> quantizedBlocks = quantize(dctBlocks);  // quantize(n1, n2);
                    // write to compressed file
                    scan();
                    // store in prevFrame:
                    prevFrame = currFrame;
                    prevMacroblocks = currMacroblocks;
                }
                // if I-frame
                else {
                    currMacroblocks = macroblock(); // still macroblock so compression steps can be the same
                    // compress(currMacroblocks);
                    // List<int[][][]> blocks = block(currMacroblocks);
                    // List<int[][][]> dctBlocks = dct(blocks);
                    // List<int[][][]> quantizedBlocks = quantize(dctBlocks);  // quantize with higher resolution (lower quantization step)
                    // write to compressed file
                    scan();
                    // store in prevFrame:
                    prevFrame = currFrame;
                    prevMacroblocks = currMacroblocks;
                }
                // DEBUG: displays progress
                System.out.println("Frame processed:" + i);
            }

            // DEBUG: prints last 90 RGB values of last frame
            for (int i = 0; i < 30; i += 3) {
                System.out.printf("R: %d, G: %d, B: %d\n", currFrame[i], currFrame[i + 1], currFrame[i + 2]);
            }

            fis.close();
            // fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the bytes of a single frame into currFrame object
     * @param fis input file stream for the .rgb file
     * @return true if the current frame is read successfully, false if EOF reached
     * @throws IOException if there is an error reading from the file
     */
    private boolean readFrame(FileInputStream fis) throws IOException {
        for (int i = 0; i < FRAME_SIZE; i++) {
            int temp = fis.read();

            if (temp == -1) {
                return false;
            }
            else {
                currFrame[i] = temp;
            }
        }

        return true;
    }

    /**
     * Rearranges the current frame's pixel data from RRR.GGG.BBB to RGB.RGB.RGB
     */
    private void formatFrame() {
        int[] tempArray = new int[FRAME_SIZE];

        for (int i = 0; i < CHANNEL_SIZE; i++) {
            tempArray[i * 3] = currFrame[i];
            tempArray[i * 3 + 1] = currFrame[CHANNEL_SIZE + i];
            tempArray[i * 3 + 2] = currFrame[CHANNEL_SIZE * 2 + i];
        }

        currFrame = tempArray;
    }

    
    // ----- PART 1: VIDEO SEGMENTATION -----

    /**
     * Divides the current frame into 16x16 macroblocks
     * @return a list of 16x16 macroblocks, each containing RGB channel data
     */
    private List<int[][][]> macroblock() {
        List<int[][][]> macroblocks = new ArrayList<>();    // int[x][y][r, g, or b val for (x, y)]

        currFrame3DArray = convertTo3DArray(currFrame);

        // iterate over frame macroblock-by-macroblock
        for (int x = 0; x < WIDTH; x += MACROBLOCK_SIZE) {
            for (int y = 0; y < HEIGHT; y += MACROBLOCK_SIZE) {
                // create the current 16x16 macroblock to hold RGB channel data
                int[][][] macroblock = new int[MACROBLOCK_SIZE][MACROBLOCK_SIZE][3];
                
                // iterate over each pixel within the current macroblock
                for (int i = 0; i < MACROBLOCK_SIZE; i++) {
                    for (int j = 0; j < MACROBLOCK_SIZE; j++) {
                        // to account for shortened macroblocks
                        if (y + j < HEIGHT) {
                            // assign the current pixel's RGB values to the current macroblock
                            macroblock[i][j][0] = currFrame3DArray[x + i][y + j][0];     // red channel
                            macroblock[i][j][1] = currFrame3DArray[x + i][y + j][1];     // green channel
                            macroblock[i][j][2] = currFrame3DArray[x + i][y + j][2];     // blue channel
                        }
                    }
                }

                macroblocks.add(macroblock);
            }
        }

        return macroblocks;
    }

    /**
     * Converts a 1D frame to a 3D array
     * @param frame a 1D frame array
     * @return 3D frame array
     */
    private int[][][] convertTo3DArray(int[] frame) {
        int[][][] newFrame = new int[WIDTH][HEIGHT][3];

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int channel = 0; channel < 3; channel++) {
                    newFrame[x][y][channel] = frame[x*3 + y*3*WIDTH + channel];
                }
            }
        }

        return newFrame;
    }
    
    // finds displacement between motion vector of prevFrame and currFrame (AKA, for each macroblock)
    // technique to use = MAD
    // will need current macroblocks and prevFrame
    private List<int[]> generateMotionVectorArray(List<int[][][]> macroblocks) {
        // goal: macroblocks[i] has motion vector at motionVectors[i]
        List<int[]> motionVectors = new ArrayList<>(); // list of (dx, dy) vectors

        // convert prevFrame to a 3D array -- needed for motion vector calculation
        prevFrame3DArray = convertTo3DArray(prevFrame);

        // for each macroblock
        // compute that macroblock's motion vector and add to motionVectors
        for (int i = 0; i < macroblocks.size(); i++) {
            motionVectors.add(computeMotionVector(i));
        }

        return motionVectors;
    }

    /**
     * Computes motion vector for macroblock i
     * @param i macroblock number
     * @return motion vector for macroblock i (vector[0] = dx, vector[1] = dy)
     */
    private int[] computeMotionVector(int i) {
        // find current macroblock's top left position
        // search in k neighborhood (checking boundaries), finding top left pixel of area with min difference
        // vector = top left pixel of area with min difference - top left pixel of macroblock

        int[] vector = new int[2]; // int[0] = dx, int[1] = dy

        int topLeftX = i * MACROBLOCK_SIZE % WIDTH;
        int topLeftY = i * MACROBLOCK_SIZE / WIDTH * MACROBLOCK_SIZE;

        int minDifference = Integer.MAX_VALUE;

        // search from k to the left to k to the right
        for (int x = topLeftX - SEARCH_PARAMETER_K; x < topLeftX + SEARCH_PARAMETER_K; x++) {
            // dealing with boundaries
            while (x < 0) {
                x++;
            }
            if (x >= WIDTH - MACROBLOCK_SIZE) {
                break;
            }
            // search from k up to k down
            for (int y = topLeftY - SEARCH_PARAMETER_K; y < topLeftY + SEARCH_PARAMETER_K; y++) {
                // dealing with boundaries
                while (y < 0) {
                    y++;
                }
                if (y >= HEIGHT - MACROBLOCK_SIZE) {
                    break;
                }
                // compare macroblocks
                // if this is the smallest difference we have found so far, save that motion vector in vector
                int difference = compareMacroblocks(currMacroblocks.get(i), x, y);
                
                if (difference < minDifference) {
                    vector[0] = x - topLeftX;
                    vector[1] = y - topLeftY;
                    minDifference = difference;
                }
            }
        }

        return vector;
    }

    /**
     * Compares all r values of currMacroblock with macroblock-sized area at (x,y) in prevFrame
     * @param currMacroblock 3D array of current macroblock pixels
     * @param x x-value of top left pixel of macroblock-sized area we're comparing with in prevFrame
     * @param y y-value of top left pixel of macroblock-sized area we're comparing with in prevFrame
     * @return integer representing the total difference between the two macroblocks
     */
    private int compareMacroblocks(int[][][] currMacroblock, int topLeftX, int topLeftY) {
        int difference = 0;

        // find the absolute value of the difference between each r pixel in currMacroblock and its
        // corresponding r pixel in prevFrame3DArray
        for (int x = 0; x < MACROBLOCK_SIZE; x++) {
            for (int y = 0; y < MACROBLOCK_SIZE; y++) {
                difference += Math.abs(currMacroblock[x][y][0] - prevFrame3DArray[topLeftX+x][topLeftY+y][0]);
            }
        }

        return difference;
    }

    // background macroblock --> motion vector = 0 (if camera is still), constant (if camera is moving)
    // foreground macroblock --> motion vector = ?
    // could do a boolean list or integer list using 1 = foreground, 2 = background for a macroblock
    private List<Integer> getLayers() {
        List<Integer> layers = new ArrayList<>();

        return layers;
    }


    // ----- PART 2: COMPRESSION -----
    private void compress(List<int[][][]> macroblocks) {
        for (int i = 0; i < macroblocks.size(); i++) {
            List<int[][][]> blocks = block(macroblocks.get(i));
            List<int[][][]> dctBlocks = dct(blocks);
            List<int[][][]> quantizedBlocks = quantize(dctBlocks, layers.get(i));  // (dctBlocks, layers[i]);
        }
    }


    // divide each macroblock into 8x8 blocks for each frame
    private List<int[][][]> block(int[][][] macroblock) {
        List<int[][][]> blocks = new ArrayList<>();

        // iterate over the whole macroblock, block-by-block
        for (int x = 0; x < MACROBLOCK_SIZE; x += BLOCK_SIZE) {
            for (int y = 0; y < MACROBLOCK_SIZE; y += BLOCK_SIZE) {
                int[][][] block = new int[BLOCK_SIZE][BLOCK_SIZE][3];

                // iterate within each 8x8 block
                for (int i = 0; i < BLOCK_SIZE; i++) {
                    for (int j = 0; j < BLOCK_SIZE; j++) {
                        block[i][j][0] = macroblock[x + i][y + j][0];
                        block[i][j][0] = macroblock[x + i][y + j][1];
                        block[i][j][0] = macroblock[x + i][y + j][2];
                    }
                }

                blocks.add(block);
            }
        }

        return blocks;
    }

    private List<int[][][]> dct(List<int[][][]> blocks) {
        List<int[][][]> dctBlocks = new ArrayList<>();

        return dctBlocks;
    }

    private List<int[][][]> quantize(List<int[][][]> dctBlocks, int layer) {
        List<int[][][]> quantizedBlocks = new ArrayList<>();

        if (layer == 0) {   // if foreground
            // use n1
        }
        else {  // if background
            // use n2
        }

        return quantizedBlocks;
    }

    // scan blocks into output compressed file
    private void scan() {

    }

    public static void main(String[]args) {
        File inputFile = new File(args[0]);
        int n1 = Integer.parseInt(args[1]);
        int n2 = Integer.parseInt(args[2]);

        MyEncoder encoder = new MyEncoder(inputFile, n1, n2);

        encoder.readFile();
    }
}

