import java.io.File;
import java.io.FileInputStream;
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
    private static final int MACROBLOCKS_PER_FRAME = 2040;      // 60 per row x 33.75 per col ~= 2040

    private static final int MACROBLOCK_SIZE = 16;
    private static final int BLOCK_SIZE = 8;

    // List<List<int[][][]>> currMacroblocks;   // list of macroblocks for curr frame, with each macroblock = list of its blocks

    // constructor
    public MyDecoder(File encoderFile, String audioPath) {
        this.encoderFile = encoderFile;
        this.audioPath = audioPath;

        this.n1 = 0;
        this.n2 = 0; 
    }


    // ----- PRE-PROCESS COMPRESSED INPUT DATA -----

    // parse compressed input file
    private void parseFile() {
        try {
            FileInputStream fis = new FileInputStream(encoderFile);

            // get n1, n2
            n1 = fis.read();
            n2 = fis.read();

            // we compressed one frame at a time, by compressing one macroblock at a time

            // for each frame:
            // get list of blocks per macroblock 
                // 4 blocks = 1 macroblock
                // get data of all 8x8 blocks for each frame --> get layer type, get coefficients
             
            // get list of macroblocks
                // macroblocks per frame = 2040

            
            // process one frame at a time --> loop until all frames processed
            for (int i = 0; readBlocks(fis); i++) {

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 
    private boolean readBlocks(FileInputStream fis) throws IOException {
        int blockType; 

        // probably want to do the encoder scan method first
        // says we can format the output file however we want, so we could do: 
        // n1 n2 on its own line 
        // 1 block per line (block_type R1...R64 G1...G64 B1...B64)
        // AKA splitting all this up with line breaks would be easiest 

        return true; 
    }


    // ----- PART3: decompression -----

    // decompress driver method
    // decompress each macroblock = list of its blocks
    private void decompress(List<List<int[][][]>> macroblocks) {   
        // for each macroblock: 
        // dequantize
        // idct
        // etc.

        for (int i = 0; i < macroblocks.size(); i++) {
            List<int[][][]> dequantizedBlocks = dequantize(macroblocks.get(i), 0);  // placeholder blockType
            List<int[][][]> idctBlocks = idct(dequantizedBlocks);
            scanMacroblock(idctBlocks);
        }
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


    // display --> calls AudioVideoPlayer.java 
    // will need to display input video and output video 
        // want to be able to see OG video vs. compressed-decompressed video 
    private void display(File rgbFile) {

    }


    public static void main(String[] args) {
        File encoderFile = new File(args[0]);   // input = MyEncoder .cmp output file 
        String audioPath = args[1];         // input = MP3 audio file --> store just path for now

        MyDecoder decoder = new MyDecoder(encoderFile, audioPath);

        decoder.parseFile();
    }
}
