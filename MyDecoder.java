import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;


public class MyDecoder {
    File encoderFile;       // input file
    String audioPath;       // path to Media object --> may change to set up Media object here
    int n1;                 // foreground quantization step
    int n2;                 // background quantization step
    
    private static final int WIDTH = 960;                       // width of each frame
    private static final int HEIGHT = 540;                      // height of each frame
    private static final int NUM_CHANNELS = 3;                  // r + g + b = 3
    private static final int CHANNEL_SIZE = WIDTH * HEIGHT;                 // 518,400 bytes per channel (per frame)
    private static final int FRAME_SIZE = CHANNEL_SIZE * NUM_CHANNELS;      // 1,555,200 total bytes per frame
    
    private static final int MACROBLOCKS_PER_FRAME = 2040;                  // 60 per row x 33.75 per col ~= 2040
    private static final int BLOCKS_PER_MACROBLOCK = 4;

    private static final int MACROBLOCK_SIZE = 16;
    private static final int BLOCK_SIZE = 8;

    boolean endOfFile;      // tracks if there is still a next byte to be read --> if == -1, then EOF (regardless of context)

    // List<WritableImage> frames;  // or WritableImage = currFrame;

    // constructor
    public MyDecoder(File encoderFile, String audioPath) {
        this.encoderFile = encoderFile;
        this.audioPath = audioPath;

        this.n1 = 0;
        this.n2 = 0; 

        endOfFile = false;
    }


    // ----- PRE-PROCESS COMPRESSED FILE DATA -----

    // parse compressed input file frame-by-frame
    private void parseFile() {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(encoderFile))) {     // change this for encoder also
            // get n1, n2
            n1 = readAndCheckInt(dis);
            n2 = readAndCheckInt(dis);

            // process file one frame at a time, until EOF
            for (int f = 0; !endOfFile; f++) {
                boolean frameProcessed = processFrame(dis);

                if (!frameProcessed) { 
                    System.out.println("ERROR: parseFile() --> frame not processed");
                    endOfFile = true;
                    break;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // .readInt(dis) but checks it & updates endOfFile if needed
    private int readAndCheckInt(DataInputStream dis) throws IOException {
        int nextInt = dis.readInt();

        if (nextInt == -1) { endOfFile = true; }

        return nextInt;
    }

    // processes current frame macroblock-by-macroblock
    // decompresses each macroblock, which also writes it to the output file
    // checks if EOF along the way 
    private boolean processFrame(DataInputStream dis) throws IOException {
        List<List<int[][][]>> decompressedFrame = new ArrayList<>();    // list of all decomp. macroblocks for the curr frame

        // loop over one frame at a time = 2040 macroblocks at a time
        for (int m = 0; m < MACROBLOCKS_PER_FRAME && !endOfFile; m++) {
            // get & decompress curr macroblock
            int currBlockType = readAndCheckInt(dis);
            List<int[][][]> currMacroblock = parseMacroblock(currBlockType, dis);
            List<int[][][]> decompressedMacroblock = decompress(currMacroblock, currBlockType);

            // add decompressed macroblock to frame list
            decompressedFrame.add(decompressedMacroblock);
        }

        // WritableImage frameImage = formatFrame(decompressedFrame);
        // frames.add(frameImage);

        return endOfFile; 
    }

    // parses the current macroblock 
    // returns the current macroblock = list of its 4 blocks
    private List<int[][][]> parseMacroblock(int blockType, DataInputStream dis) throws IOException {
        List<int[][][]> macroblock = new ArrayList<>();
        
        // already have first block type
            // want to just check it each time --> checks that block type is the same for entire macroblock 
            // bc if not, then something wrong  

        // loop over one macroblock at a time = 4 blocks at a time 
        for (int b = 0; b < BLOCKS_PER_MACROBLOCK && !endOfFile; b++) {
            int[][][] block = new int[BLOCK_SIZE][BLOCK_SIZE][NUM_CHANNELS];
            
            if (b != 0) { 
                int currBlockType = readAndCheckInt(dis);

                if (currBlockType != blockType) { 
                    System.out.println("ERROR: parseMacroblock() --> mismatched block types");
                    endOfFile = true;   // will kill the compression since there's an error (if that's what we want)
                    break; 
                }
                // else, no need to update the blockType since its the same 
                // nextInt should now be the first R value 
            }
            
            for (int channel = 0; channel < NUM_CHANNELS; channel++) {
                for (int row = 0; row < BLOCK_SIZE; row++) {
                    for (int col = 0; col < BLOCK_SIZE; col++) {
                        int rgb = readAndCheckInt(dis);         // current channel value at (row, col)
                        
                        block[row][col][channel] = rgb;         // R0...R7, then R8...R15, until R56...R63, 
                    }
                }
            }

            macroblock.add(block);
        }

        return macroblock;
    }


    // ----- PART3: decompression -----

    // decompress driver method
    // decompress each macroblock = list of its blocks
    private List<int[][][]> decompress(List<int[][][]> macroblock, int blockType) {   
        List<int[][][]> dequantizedBlocks = dequantize(macroblock, blockType);
        List<int[][][]> idctBlocks = idct(dequantizedBlocks);

        return idctBlocks;
    }

    // dequantize --> multiply each coeff. by 2^step
    private List<int[][][]> dequantize(List<int[][][]> quantizedBlocks, int blockType) {
        List<int[][][]> dequantizedBlocks = new ArrayList<>();

        int step = 0; 

        // determine quantization step based on block type
        if (blockType == 0) {
            step = (int) Math.round(Math.pow(2, n1));   // use 2^n1 for foreground block
        }
        else {
            step = (int) Math.round(Math.pow(2, n2));   // use 2^n2 for background block
        }

        // iterate over each quantized block to dequantize it
        for (int[][][] quantizedBlock : quantizedBlocks) {
            int[][][] dequantizedBlock = new int[BLOCK_SIZE][BLOCK_SIZE][NUM_CHANNELS];

            // dequantize each channel of each pixel by multiplying by step
            for (int row = 0; row < BLOCK_SIZE; row++) {
                for (int col = 0; col < BLOCK_SIZE; col++) {
                    dequantizedBlock[row][col][0] = (int) Math.round(quantizedBlock[row][col][0] * step);
                    dequantizedBlock[row][col][1] = (int) Math.round(quantizedBlock[row][col][1] * step);
                    dequantizedBlock[row][col][2] = (int) Math.round(quantizedBlock[row][col][2] * step);
                }
            }

            // add current dequantized block to result list
            dequantizedBlocks.add(dequantizedBlock);
        }

        return dequantizedBlocks;
    }


    // ----- PART4: decoding -----

    // IDCT
    private List<int[][][]> idct(List<int[][][]> dequantizedBlocks) {
        List<int[][][]> idctBlocks = new ArrayList<>();

        for (int[][][] dequantizedBlock : dequantizedBlocks) {
            int[][][] idctBlock = new int[BLOCK_SIZE][BLOCK_SIZE][NUM_CHANNELS];

            // iterate over rows and columns of the block 
            for (int row = 0; row < BLOCK_SIZE; row++) {
                for (int col = 0; col < BLOCK_SIZE; col++) {
                    double idctRed = calculateIdct(row, col, dequantizedBlock, 0);
                    double idctGreen = calculateIdct(row, col, dequantizedBlock, 1);
                    double idctBlue = calculateIdct(row, col, dequantizedBlock, 2);

                    idctBlock[row][col][0] = (int) Math.round(idctRed);
                    idctBlock[row][col][1] = (int) Math.round(idctGreen);
                    idctBlock[row][col][2] = (int) Math.round(idctBlue);
                }
            }

            idctBlocks.add(idctBlock);
        }

        return idctBlocks;
    }

    // calculate IDCT
    private double calculateIdct(int row, int col, int[][][] dequantizedBlock, int channel) {
        double idct = 0.0;

        double sum = 0.0;

        double[] scalars = getScalars(row, col);
        double scalar = (1.0 / 4.0) * scalars[0] * scalars[1];

        for (int u = 0; u < BLOCK_SIZE; u++) {
            for (int v = 0; v < BLOCK_SIZE; v++) {
                sum += dequantizedBlock[u][v][channel] * 
                        Math.cos(((2 * row + 1) * u * Math.PI) / (2 * BLOCK_SIZE)) * 
                        Math.cos(((2 * col + 1) * v * Math.PI) / (2 * BLOCK_SIZE));
            }
        }

        idct = scalar * sum; 

        return idct; 
    }

    // get scalars for dct/idct calculations
    private double[] getScalars(int row, int col) {
        double[] scalars = new double[2];

        scalars[0] = 1.0;
        scalars[1] = 1.0;

        if (row == 0) {
            scalars[0] *= (1.0 / Math.sqrt(2));
        }
        if (col == 0) {
            scalars[1] *= (1.0 / Math.sqrt(2));
        }

        return scalars;
    }

    // convert curr frame (decompressed macroblocks) to a WritableImage
    private void formatFrame(List<List<int[][][]>> decompressedMacroblocks) {

    }


    // display --> calls AudioVideoPlayer.java (assuming its easier to make AV player a separate file)
        // can either display a .rgb video file
        // or could display frame-by-frame as each frame is processed
            // can't display macroblock-by-macroblock 
            // i think this is best since we need to be able to pause frames, step thru them, etc. ?
                // but actually not sure that this matters, think we still need to get all frames then display
                // but the frames can be WritableImage objects --> easiest for JavaFX
    // will need to display input video and output video 
        // want to be able to see OG video vs. compressed-decompressed video 
    private void display() {
        // AudioVideoPlayer player = new AudioVideoPlayer(frames, audioPath);
    }


    public static void main(String[] args) {
        File encoderFile = new File(args[0]);   // input = MyEncoder .cmp output file 
        String audioPath = args[1];             // input = MP3 audio file --> store just path for now

        MyDecoder decoder = new MyDecoder(encoderFile, audioPath);

        decoder.parseFile();

        // decoder.display();
    }
}
