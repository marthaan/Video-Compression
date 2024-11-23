import java.io.File;
import java.io.FileInputStream;
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
    
    private int[] prevFrame;
    private List<int[][][]> prevMacroblocks;

    private int[] currFrame;
    private List<int[][][]> currMacroblocks;

    /**
     * Constructor
     * @param inputFile
     * @param n1
     * @param n2
     */
    public MyEncoder(File inputFile, int n1, int n2) {
        this.inputFile = inputFile; 
        this.n1 = n1;
        this.n2 = n2;

        prevFrame = new int[FRAME_SIZE];
        prevMacroblocks = new ArrayList<>();

        currFrame = new int[FRAME_SIZE];
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

            for (int i = 0; readFrame(fis); i++) {
                formatFrame();
                // if not I-frame --> if P-frame
                if (i != 0) {
                    // PART 1: VIDEO SEGMENTATION
                    // not sure if this is the best way to store all of this (maybe 1D is better) - but for now at least lines up all the info fine
                    // goal: macroblocks[i] has motion vector at motionVectors[i] and has layer type at layers[i]
                    currMacroblocks = macroblock();             
                    List<int[]> motionVectors = computeMotionVectors(currMacroblocks);    
                    List<Integer> layers = getLayers();

                    // PART 2: COMPRESSION
                    List<int[][][]> blocks = block(currMacroblocks);
                    List<int[][][]> dctBlocks = dct(blocks);
                    List<int[][][]> quantizedBlocks = quantize(dctBlocks);  // quantize(n1, n2);
                    // write to compressed file
                    // store in prevFrame:
                    prevFrame = currFrame;
                    prevMacroblocks = currMacroblocks;
                }
                // if I-frame
                else {
                    currMacroblocks = macroblock(); // still macroblock so compression steps can be the same
                    List<int[][][]> blocks = block(currMacroblocks);
                    List<int[][][]> dctBlocks = dct(blocks);
                    List<int[][][]> quantizedBlocks = quantize(dctBlocks);  // quantize with higher resolution (lower quantization step)
                    // write to compressed file
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
            // used temp variable so that final currFrame value is not assigned -1
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

        // iterate over frame macroblock-by-macroblock
        for (int x = 0; x < WIDTH; x += MACROBLOCK_SIZE) {
            for (int y = 0; y < HEIGHT; y += MACROBLOCK_SIZE) {
                // create the current 16x16 macroblock to hold RGB channel data
                int[][][] macroblock = new int[MACROBLOCK_SIZE][MACROBLOCK_SIZE][3];
                
                // iterate over each pixel within the current macroblock
                for (int i = 0; i < MACROBLOCK_SIZE; i++) {
                    for (int j = 0; j < MACROBLOCK_SIZE; j++) {
                        // calculate where the current pixel's RGB values begin in currFrame
                        int index = ((x + i) * WIDTH + (y + j)) * 3;    

                        // assign the current pixel's RGB values to the current macroblock
                        macroblock[i][j][0] = currFrame[index];         // red channel
                        macroblock[i][j][1] = currFrame[index + 1];     // green channel
                        macroblock[i][j][2] = currFrame[index + 2];     // blue channel
                    }
                }

                macroblocks.add(macroblock);
            }
        }

        return macroblocks;
    }
    
    // finds displacement between motion vector of prevFrame and currFrame (AKA, for each macroblock)
    // technique to use = MAD
    // will need previous macro blocks and current macro blocks
    private List<int[]> computeMotionVectors(List<int[][][]> macroblocks) {
        List<int[]> motionVectors = new ArrayList<>(); // list of (dx, dy) vectors
        
        int[] vector = new int[2]; // int[0] = dx, int[1] = dy

        return motionVectors;
    }

    // background macroblock --> motion vector = 0 (if camera is still), constant (if camera is moving)
    // foreground macroblock --> motion vector = ?
    // could do a boolean list or integer list using 1 = foreground, 2 = background for a macroblock
    private List<Integer> getLayers() {
        List<Integer> layers = new ArrayList<>();

        return layers;
    }


    // PART 2: COMPRESSION

    // I-frames: divide entire frame into 8x8 blocks 
    // P-frames: divide each macroblock into 8x8 blocks for each frame
    private List<int[][][]> block(List<int[][][]> macroblocks) {
        List<int[][][]> blocks = new ArrayList<>();

        return blocks;
    }

    private List<int[][][]> dct(List<int[][][]> blocks) {
        List<int[][][]> dctBlocks = new ArrayList<>();

        return dctBlocks;
    }

    private List<int[][][]> quantize(List<int[][][]> dctBlocks) {
        List<int[][][]> quantizedBlocks = new ArrayList<>();

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
