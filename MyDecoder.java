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

    // decodedMacroblocks --> can then scan those into .rgb

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
            n1 = dis.readInt();
            n2 = dis.readInt();

            if (n1 == -1 || n2 == -1) { endOfFile = true; }

            // process file one frame at a time
            // loop until endOfFile == true == EOF
            for (int f = 0; !endOfFile; f++) {
                boolean frameProcessed = processFrame(dis);

                if (!frameProcessed) { endOfFile = true; }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // processes current frame, checking if EOF along the way 
    private boolean processFrame(DataInputStream dis) throws IOException {
        // loop over one frame at a time = 2040 macroblocks at a time
        for (int m = 0; m < MACROBLOCKS_PER_FRAME && !endOfFile; m++) {
            
            List<int[][][]> currMacroblock = parseMacroblock(dis);
            decompress(currMacroblock);
        }

        return endOfFile; 
    }


    private List<int[][][]> parseMacroblock(DataInputStream dis) throws IOException {
        List<int[][][]> macroblock = new ArrayList<>();     // list of 4 blocks
        
        // loop over one macroblock at a time = 4 blocks at a time 
        for (int b = 0; b < BLOCKS_PER_MACROBLOCK && !endOfFile; b++) {
            int[][][] block = new int[BLOCK_SIZE][BLOCK_SIZE][NUM_CHANNELS];
            
            for (int channel = 0; channel < NUM_CHANNELS; channel++) {
                for (int row = 0; row < BLOCK_SIZE; row++) {
                    for (int col = 0; col < BLOCK_SIZE; col++) {
                        int rgb = dis.readInt();

                        if (rgb == -1) { 
                            endOfFile = true; 
                            break;
                        }      
                        
                        block[row][col][channel] = rgb;         // R0...R7, then R8...R15, until R56...R63
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
    private void decompress(List<int[][][]> macroblock) {   
        // for each macroblock: 
        List<int[][][]> dequantizedBlocks = dequantize(macroblock, 0);  // placeholder blockType
        List<int[][][]> idctBlocks = idct(dequantizedBlocks);
        scanMacroblock(idctBlocks);
    }

    // dequantize --> multiply each coeff. by 2^step
    // quantize needs to be changed from dividing by the step to dividing by 2^step
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


    // may need to unblock / format for display first?
    // unblock into a frame to display each frame at a time?
    // output to a .rgb file to send to AudioVideoPlayer
    private void scanMacroblock(List<int[][][]> idctBlocks) {

    }


    // display --> calls AudioVideoPlayer.java (assuming its easier to make AVplayer a separate file)
    // will need to display input video and output video 
        // want to be able to see OG video vs. compressed-decompressed video 
    private void display(File rgbFile) {

    }


    public static void main(String[] args) {
        File encoderFile = new File(args[0]);   // input = MyEncoder .cmp output file 
        String audioPath = args[1];             // input = MP3 audio file --> store just path for now

        MyDecoder decoder = new MyDecoder(encoderFile, audioPath);

        decoder.parseFile();
    }
}
