import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;


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
    
    private static final int MACROBLOCKS_PER_ROW = 60;          
    private static final int MACROBLOCKS_PER_COL = 34;          // 33.75 ~= 34
    private static final int MACROBLOCKS_PER_FRAME = MACROBLOCKS_PER_ROW * MACROBLOCKS_PER_COL;                  
    private static final int BLOCKS_PER_MACROBLOCK = 4;

    private static final int MACROBLOCK_SIZE = 16;
    private static final int BLOCK_SIZE = 8;

    boolean endOfFile;      // tracks if there is still a next byte to be read --> if == -1, then EOF (regardless of context)

    List<WritableImage> frames;  // or WritableImage = currFrame; but I think we have to do them all before display

    // constructor
    public MyDecoder(File encoderFile, String audioPath) {
        this.encoderFile = encoderFile;
        this.audioPath = audioPath;

        this.n1 = 0;
        this.n2 = 0; 

        endOfFile = false;

        frames = new ArrayList<>();
    }


    // ----- PRE-PROCESS COMPRESSED FILE DATA -----

    // parse compressed input file frame-by-frame
    private void parseFile() {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(encoderFile))) {     // change this for encoder also
            // get n1, n2
            n1 = readAndCheckInt(dis);
            n2 = readAndCheckInt(dis);

            System.out.println("PARSEFILE():");
            System.out.println("n1, n2 parsed: " + n1 + ", " + n2 + "\n");

            // process file one frame at a time, until EOF
            for (int f = 0; !endOfFile; f++) {
                boolean frameProcessed = processFrame(dis);     // false = endOfFrame vs endOfFile

                if (!frameProcessed && !endOfFile) {  // need to fix to deal with parsing errors vs. just EOF 
                    System.out.println("ERROR: parseFile() --> frame not processed");
                    endOfFile = true;   // redundant but shouldn't cause errors
                    break;
                }
                if (endOfFile) {
                    System.out.println("---END OF FILE----");
                }

                System.out.println("FRAME PROCESSED: " + f);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // .readInt(dis) but checks it & updates endOfFile if needed
    private int readAndCheckInt(DataInputStream dis) throws IOException {
        if (dis.available() == 0) {     // no more bytes available
            endOfFile = true; 
            System.out.println("END OF FILE\n");
            return -1;
        }
        
        int nextInt = dis.readInt();

        return nextInt;
    }

    // processes current frame macroblock-by-macroblock
    // decompresses each macroblock, which also writes it to the output file
    // checks if EOF along the way 
    private boolean processFrame(DataInputStream dis) throws IOException {
        List<List<int[][][]>> decompressedFrame = new ArrayList<>();    // list of all decomp. macroblocks for the curr frame

        boolean frameProcessed = false;

        // loop over one frame at a time = 2040 macroblocks at a time
        for (int m = 0; m < MACROBLOCKS_PER_FRAME && !endOfFile; m++) {
            // get & decompress curr macroblock
            int currBlockType = readAndCheckInt(dis);
            List<int[][][]> currMacroblock = parseMacroblock(currBlockType, dis);
            List<int[][][]> decompressedMacroblock = decompress(currMacroblock, currBlockType);
            // System.out.println("DECOMP CURR MACROBLOCK SIZE: " + decompressedMacroblock.size());

            // add decompressed macroblock to frame list
            decompressedFrame.add(decompressedMacroblock);

            // if all expected macroblocks of frame processed
            if (m == MACROBLOCKS_PER_FRAME - 1) {
                System.out.println("LAST MACROBLOCK OF FRAME PROCESSED --> FRAME SUCCESSFULLY PARSED");
                frameProcessed = true;
            }
        }

        System.out.println("DECOMP FRAME SIZE: " + decompressedFrame.size() + "\n");

        if (!endOfFile) {
            WritableImage frameImage = formatFrame(decompressedFrame);
            frames.add(frameImage);
        }

        return frameProcessed; 
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

            
            if (b > 0) { 
                int currBlockType = readAndCheckInt(dis);

                if (currBlockType != blockType) { 
                    System.out.println("ERROR: parseMacroblock() --> mismatched block types");
                    endOfFile = true;   // will kill the compression since there's an error (if that's what we want)
                    break; 
                }
                // else, no need to update the blockType since its the same 
                // nextInt should now be the first R value 
            }
            
            for (int channel = 0; channel < NUM_CHANNELS && !endOfFile; channel++) {
                for (int row = 0; row < BLOCK_SIZE && !endOfFile; row++) {
                    for (int col = 0; col < BLOCK_SIZE && !endOfFile; col++) {
                        // get current channel value at (row, col) --> can be -1 since quant vals 
                        block[row][col][channel] = dis.readInt();         // R0...R7, then R8...R15, until R56...R63, 
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

    // redo
    private WritableImage formatFrame(List<List<int[][][]>> decompressedFrame) {
        System.out.println("FORMATFRAME(): ");
        System.out.println("decompressedFrame size = # of macroblocks in frame =  " + decompressedFrame.size());
        if (decompressedFrame.size() != 2040) { System.out.println("ERROR --> invalid # of macroblocks"); }
        
        WritableImage frame = new WritableImage(WIDTH, HEIGHT); // initialize image for the current frame
        PixelWriter writer = frame.getPixelWriter();            // writes RGB pixel data to frame image

        for (int mb = 0; mb < decompressedFrame.size(); mb++) {
            List<int[][][]> currMacroblock = decompressedFrame.get(mb);

            int mbRow = mb / MACROBLOCKS_PER_ROW;
            int mbCol = mb % MACROBLOCKS_PER_ROW;
            int mbX = mbCol * MACROBLOCK_SIZE;
            int mbY = mbRow * MACROBLOCK_SIZE;

            for (int b = 0; b < BLOCKS_PER_MACROBLOCK; b++) {
                int[][][] currBlock = currMacroblock.get(b);

                int bRow = b / (BLOCKS_PER_MACROBLOCK / 2);
                int bCol = b % (BLOCKS_PER_MACROBLOCK / 2);
                int bX = mbX + bCol * BLOCK_SIZE;
                int bY = mbY + bRow * BLOCK_SIZE;

                for (int r = 0; r < BLOCK_SIZE; r++) {
                    for (int c = 0; c < BLOCK_SIZE; c++) {
                        int red = currBlock[r][c][0];
                        int green = currBlock[r][c][1];
                        int blue = currBlock[r][c][2];

                        int pixelX = Math.max(0, Math.min(WIDTH - 1, bX + c));  // clamp = [0, 959]
                        int pixelY = Math.max(0, Math.min(HEIGHT - 1, bY + r)); // clamp = [0, 539]

                        int rgb = packArgb(red, green, blue);
                        writer.setArgb(pixelX, pixelY, rgb);     
                    }
                }
            }
        }

        return frame;
    }

    /** 
    // convert curr frame (decompressed macroblocks) to a WritableImage
    private WritableImage formatFrame(List<List<int[][][]>> decompressedFrame) {
        WritableImage frame = new WritableImage(WIDTH, HEIGHT); // initialize image for the current frame

        PixelWriter writer = frame.getPixelWriter();    // writes RGB pixel data to frame image

        // write each macroblock's RGB data to the frame image (for the current frame)
        // macroblock = list of 4 blocks
        
        // loop over the frame, one macroblock at a time 
        for (int row = 0; row < MACROBLOCKS_PER_COL; row++) {
            for (int col = 0; col < MACROBLOCKS_PER_ROW; col++) {
                // if last col == MACROBLOCKS_PER_COL - 1, handle differently (partial macroblocks)

                int mbIndex = (row * MACROBLOCKS_PER_ROW + col * MACROBLOCKS_PER_COL); 
                List<int[][][]> currMacroblock = decompressedFrame.get(mbIndex);

                // loop over the macroblock, one block at a time
                for (int b = 0; b < BLOCKS_PER_MACROBLOCK; b++) {
                    int[][][] currBlock = currMacroblock.get(b);

                    for (int r = 0; r < BLOCK_SIZE; r++) {
                        for (int c = 0; c < BLOCK_SIZE; c++) {
                            int red = currBlock[r][c][0];
                            int green = currBlock[r][c][1];
                            int blue = currBlock[r][c][2];

                            int rgb = packArgb(red, green, blue);
                            writer.setArgb(r, c, rgb);      // adjust r and c to reflect frame x, y
                        }
                    }
                }
            }
        }
        
        return frame;
    }
        */

    /** packArgb
    * Packs given RGB channels into one ARGB value
    * @param red [0, 255] red channel value
    * @param green [0, 255] green channel value
    * @param blue [0, 255] blue channel value
    * @return rgb packed ARGB value
    */
   private int packArgb(int red, int green, int blue) {
        // ensure channel values are between 0 and 255
        int alpha = 255;    // assume fully opaque
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        int rgb = (red << 16) | (green << 8) | blue;

        return rgb;
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
        AudioVideoPlayer player = new AudioVideoPlayer();
       // player.setInputData(frames, audioPath);
        // player.run();
    }


    public static void main(String[] args) {
        File encoderFile = new File(args[0]);   // input = MyEncoder .cmp output file 
        String audioPath = args[1];             // input = MP3 audio file --> store just path for now

        System.out.println("\nINPUT: ");
        System.out.println("encoderFile: " + encoderFile.getName());
        System.out.println("audioPath: " + audioPath + "\n");
        
        MyDecoder decoder = new MyDecoder(encoderFile, audioPath);

        decoder.parseFile();

        // decoder.display();
    }
}
