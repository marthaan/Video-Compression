import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class MyEncoder {
    private File inputFile; // input file for each instance 
    private int n1;         // foreground quantization step
    private int n2;         // background quantization step

    private static final int WIDTH = 960;                                   // width of each frame
    private static final int HEIGHT = 540;                                  // height of each frame
    private static final int NUM_CHANNELS = 3;                              // r + g + b = 3
    private static final int CHANNEL_SIZE = WIDTH * HEIGHT;                 // 518,400 bytes per channel (per frame)
    private static final int FRAME_SIZE = CHANNEL_SIZE * NUM_CHANNELS;     // 1,555,200 total bytes per frame

    private static final int MACROBLOCK_SIZE = 16;
    private static final int BLOCK_SIZE = 8;
    private static final int SEARCH_PARAMETER_K = 5;
    // this variable denotes the amount of variance allowed in choosing which macroblocks are background macroblocks
    // it should stay between zero (meaning the motion vector must match the most common motion vector exactly)
    // to, at maximum, SEARCH_PARAMETER_K
    private static final int ALLOWED_VECTOR_ERROR = 1;
    
    private int[][][] prevFrame3DArray;
    
    private int[] currFrame;
    private int[][][] currFrame3DArray;
    private List<int[][][]> currMacroblocks;
    private List<int[]> motionVectors;
    private List<Integer> layers;

    private File outputFile;

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

        // initialize everything?

        currFrame = new int[FRAME_SIZE];
        currFrame3DArray = new int[WIDTH][HEIGHT][3];
        currMacroblocks = new ArrayList<>();
        motionVectors = new ArrayList<>();
        layers = new ArrayList<>();
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

                // if not I-frame --> if P-frame
                if (i != 0) {
                    processPFrame();
                }
                // if I-frame
                else {
                    processIFrame();
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
     * Handles video segmentation and compression for an I-frame
     */
    private void processIFrame() {
        // PART 1: VIDEO SEGMENTATION
        // goal: macroblocks[i] has motion vector at motionVectors[i] and has layer type at layers[i]
        currMacroblocks = macroblock(); // still macroblock so compression steps can be the same

        // all layers are quantized with foreground step
        for (int i = 0; i < currMacroblocks.size(); i++) {
            layers.add(0);
        }
        
        // PART 2: COMPRESSION
        compress(currMacroblocks);


        // store in prevFrame:
        prevFrame3DArray = currFrame3DArray;
    }

    /**
     * Handles video segmentation and compression for a P-frame
     */
    private void processPFrame() {
        // PART 1: VIDEO SEGMENTATION
        // goal: macroblocks[i] has motion vector at motionVectors[i] and has layer type at layers[i]
        currMacroblocks = macroblock();             
        motionVectors = generateMotionVectorArray(currMacroblocks);
        layers = getLayers();

        // DEBUG: print out layers array
        int numMacroblocksWide = (int)Math.ceil(WIDTH / (double)MACROBLOCK_SIZE);
        int numMacroblocksHigh = (int)Math.ceil(HEIGHT / (double)MACROBLOCK_SIZE);
        for (int y = 0; y < numMacroblocksHigh; y++) {
            for (int x = 0; x < numMacroblocksWide; x++) {
                int index = y * numMacroblocksWide + x;
                System.out.print(layers.get(index));
                // System.out.print("(" + motionVectors.get(index)[0] + " " + motionVectors.get(index)[1] + ")");
            }
            System.out.println();
        }

        // PART 2: COMPRESSION
        compress(currMacroblocks);

        // store in prevFrame:
        prevFrame3DArray = currFrame3DArray;
    }

    private void setupOutputFile() {
        // create output file name by changing file.rgb to file.cmp
        String fileName = inputFile.getName();
        fileName.substring(0, fileName.length() - 3);

        outputFile = new File(fileName + "cmp");
        // FileOutputStream fos = new FileOutputStream(outputFile);
        // fos.write(n1);
        // fos.write(n2); 

        // fos.close();
    }

    
    // ----- PART 1: VIDEO SEGMENTATION -----

    /**
     * Divides the current frame into 16x16 macroblocks
     * @return a list of 16x16 macroblocks, each containing RGB channel data
     */
    private List<int[][][]> macroblock() {
        List<int[][][]> macroblocks = new ArrayList<>();    // int[x][y][r, g, or b val for (x, y)]

        currFrame3DArray = convertTo3DArray(currFrame);

        try {
            MacroblockViewer.saveMacroblock(currFrame3DArray, "macroblock.png");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // iterate over frame macroblock-by-macroblock
        for (int y = 0; y < HEIGHT; y += MACROBLOCK_SIZE) {
            for (int x = 0; x < WIDTH; x += MACROBLOCK_SIZE) {
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

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                for (int channel = 0; channel < 3; channel++) {
                    newFrame[x][y][channel] = frame[(y * WIDTH + x) * 3 + channel];
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
        motionVectors = new ArrayList<>();

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

        // ALTERNATE
        // int topLeftX = i * MACROBLOCK_SIZE % WIDTH;
        // int topLeftY = i * MACROBLOCK_SIZE / WIDTH * MACROBLOCK_SIZE;

        int blocksPerRow = WIDTH / MACROBLOCK_SIZE;
        int topLeftX = (i % blocksPerRow) * MACROBLOCK_SIZE;
        int topLeftY = (i / blocksPerRow) * MACROBLOCK_SIZE;


        int minDifference = Integer.MAX_VALUE;
        
        // search k to the left, but not less than 0
        int xMin = Math.max(0, topLeftX - SEARCH_PARAMETER_K);
        // search k to the right, but not more than WIDTH - MACROBLOCK_SIZE
        int xMax = Math.min(WIDTH - MACROBLOCK_SIZE, topLeftX + SEARCH_PARAMETER_K);
        // search k up, but not less than 0
        int yMin = Math.max(0, topLeftY - SEARCH_PARAMETER_K);
        // search k down, but not more than HEIGHT - height of last macroblock
        int yMax = Math.min(HEIGHT / MACROBLOCK_SIZE * MACROBLOCK_SIZE, topLeftY + SEARCH_PARAMETER_K);

        int currMacroblockHeight = 16;
        // if we're dealing with a fractional macroblock
        if (topLeftY > HEIGHT - MACROBLOCK_SIZE) {
            currMacroblockHeight = HEIGHT - topLeftY;
        }

        // first, check if current macroblock hasn't moved or changed at all
        int diff = compareMacroblocks(currMacroblocks.get(i), topLeftX, topLeftY, currMacroblockHeight);
        if (diff == 0) {
            vector[0] = 0;
            vector[1] = 0;
            return vector;
        }


        for (int y = yMin; y <= yMax; y++) {
            for (int x = xMin; x <= xMax; x++) {
                // compare macroblocks
                // if this is the smallest difference we have found so far, save that motion vector in vector
                int difference = compareMacroblocks(currMacroblocks.get(i), x, y, currMacroblockHeight);
                
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
    private int compareMacroblocks(int[][][] currMacroblock, int topLeftX, int topLeftY, int currMacroblockHeight) {
        int difference = 0;

        // find the absolute value of the difference between each r pixel in currMacroblock and its
        // corresponding r pixel in prevFrame3DArray
        for (int y = 0; y < currMacroblockHeight; y++) {
            for (int x = 0; x < MACROBLOCK_SIZE; x++) {
                difference += Math.abs(currMacroblock[x][y][0] - prevFrame3DArray[topLeftX+x][topLeftY+y][0]);
            }
        }

        return difference;
    }

    // foreground = 0; macroblock --> motion vector = ?
    // background = 1; macroblock --> motion vector = 0 (if camera is still), constant (if camera is moving)
    private List<Integer> getLayers() {
        layers = new ArrayList<Integer>();

        int[] mostCommonVector = findMostCommonVector(motionVectors);

        // DEBUG
        System.out.println("mostCommonVector = (" + mostCommonVector[0] + " " + mostCommonVector[1] + ")");

        for (int i = 0; i < motionVectors.size(); i++) {
            // if the current motionVector is within the allowed error range from the mostCommonVector
            if (motionVectors.get(i)[0] <= mostCommonVector[0] + ALLOWED_VECTOR_ERROR &&
                motionVectors.get(i)[0] >= mostCommonVector[0] - ALLOWED_VECTOR_ERROR &&
                motionVectors.get(i)[1] <= mostCommonVector[1] + ALLOWED_VECTOR_ERROR &&
                motionVectors.get(i)[1] >= mostCommonVector[1] - ALLOWED_VECTOR_ERROR) {
                    // macroblock i is a background block (layers[i] = 1)
                    layers.add(1);
            }
            else {
                // macroblock i is a foreground block (layers[i] = 0)
                layers.add(0);
            }
        }

        // now check layers to make sure foreground elements are contiguous
       layers = checkContiguous(layers);

        return layers;
    }

    private List<Integer> checkContiguous(List<Integer> layers) {
        // ceil function accounts for fractional macroblocks, so 33.75 macroblocks becomes 34
        int numMacroblocksWide = (int)Math.ceil(WIDTH / (double)MACROBLOCK_SIZE);
        int numMacroblocksHigh = (int)Math.ceil(HEIGHT / (double)MACROBLOCK_SIZE);

        for (int y = 0; y < numMacroblocksHigh; y++) {
            for (int x = 0; x < numMacroblocksWide; x++) {
                int index = y * numMacroblocksWide + x;
                // if foreground block
                if (layers.get(index) == 0) {
                    boolean isForeground = false;
                    // if the macroblock above, below, left, and right are all background, then this macroblock is background
                    // so, if any macroblock around is foreground, this macroblock is foreground, else background
                    // if macroblock above is foreground, isForeground = true;
                    if ((y != 0) && (layers.get(index - numMacroblocksWide) == 0)) {
                        isForeground = true;
                    }
                    // if macroblock to the left is foreground
                    if ((x != 0) && (layers.get(index - 1) == 0)) {
                        isForeground = true;
                    }
                    // if macroblock to the right is foreground
                    if ((x != numMacroblocksWide - 1) && (layers.get(index + 1) == 0)) {
                        isForeground = true;
                    }
                    // if macroblock below is foreground
                    if ((y != numMacroblocksHigh - 1) && (layers.get(index + numMacroblocksWide) == 0)) {
                        isForeground = true;
                    }
                    if (isForeground == false) {
                        // set to background
                        layers.set(index, 1);
                    }
                }
            }
        }

        return layers;
    }
  
    private int[] findMostCommonVector(List<int[]> motionVectors) {
        // make a hashmap where (key = vector, value = frequency)
        HashMap<List<Integer>, Integer> vectorFrequencies = new HashMap<>();

        for (int[] vector : motionVectors) {
            // if vector is new, make a new entry (vector, 1)
            // if vector already exists in map, add 1 to its value
            List<Integer> key = Arrays.asList(vector[0], vector[1]);
            vectorFrequencies.put(key, vectorFrequencies.getOrDefault(key, 0)+1);
        }

        List<Integer> mostCommon = Arrays.asList(0, 0);
        int maxFrequency = 0;
        for (Map.Entry<List<Integer>,Integer> entry : vectorFrequencies.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                mostCommon = entry.getKey();
                maxFrequency = entry.getValue();
            }
        }

        int[] mostCommonAsVector = new int[2];
        mostCommonAsVector[0] = mostCommon.get(0);
        mostCommonAsVector[1] = mostCommon.get(1);

        return mostCommonAsVector;
    }


    // ----- PART 2: COMPRESSION -----
    
    /**
     * Carries out compression steps 
     * @param macroblocks
     */
    private void compress(List<int[][][]> macroblocks) {
        for (int i = 0; i < macroblocks.size(); i++) {
            List<int[][][]> blocks = block(macroblocks.get(i));
            List<int[][][]> dctBlocks = dct(blocks);
            List<int[][][]> quantizedBlocks = quantize(dctBlocks, layers.get(i));
            scanMacroblock(layers.get(i), quantizedBlocks);
        }
    }

    /**
     * Divides each macroblock into four 8x8 blocks (for each frame)
     * @param macroblock
     * @return
     */
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

    /** getScalars
    * Calculates scalars needed for DCT formulas
    * @param u current u index
    * @param v current v index
    * @return scalars
    */
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

    // scan blocks into output compressed file
    private void scanMacroblock(int layer, List<int[][][]> quantizedBlocks) {
        for (int[][][] quantizedBlock : quantizedBlocks) {
            // write each layer to output file
            // write each row to output file 
        }
    }


    public static void main(String[]args) {
        File inputFile = new File(args[0]);
        int n1 = Integer.parseInt(args[1]);
        int n2 = Integer.parseInt(args[2]);

        MyEncoder encoder = new MyEncoder(inputFile, n1, n2);

        encoder.readFile();

        // DEBUG: prints when output file is complete
        System.out.println("SUCCESS");
    }
}

