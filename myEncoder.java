import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyEncoder {
    private File inputFile; // input file for each instance 
    private int n1;  // foreground quantization step
    private int n2;  // background quantization step

    private static final int WIDTH = 960;   // width of each frame
    private static final int HEIGHT = 540;  // height of each frame
    private static final int CHANNEL_SIZE = WIDTH * HEIGHT;  // 518,400 bytes per channel (per frame)
    private static final int FRAME_SIZE = CHANNEL_SIZE * 3;   // 1,555,200 total bytes per frame

    private static final int MACROBLOCK_SIZE = 16;
    private static final int BLOCK_SIZE = 8;
    
    private int[] prevFrame;
    private int[] currFrame;

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
        currFrame = new int[FRAME_SIZE];
    }
    

    // PART 0: PRE-PROCESS FILE DATA
    
    /**
     * Reads the entire .rgb file frame-by-frame
     * Processes each frame based on frame type (I-frame vs. P-frame)
     */
    public void readFile() {
        try {
            FileInputStream fis = new FileInputStream(inputFile);

            for (int i = 0; readFrame(fis); i++) {
                formatFrame();
                // if not I-frame
                if (i != 0) {
                    // PART 1: VIDEO SEGMENTATION
                    List<int[][][]> macroblocks = macroblock();
                    // computerMotionVector();
                    // getLayer();

                    // PART 2: COMPRESSION
                    // block();
                    // DCT();
                    // quantize(n1, n2);
                    // write to compressed file
                    // store in prevFrame
                }
                // if I-frame
                else {
                    // divide I-frame into 8x8 blocks
                    // DCT
                    // quantize with higher resolution (lower quantization step)
                    // write to compressed file
                    // store in prevFrame
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

    // PART 1: VIDEO SEGMENTATION

    // divide current frame into 16 x 16 blocks
    private List<int[][][]> macroblock() {
        List<int[][][]> macroblocks = new ArrayList<>();

        return macroblocks;
    }
    
    // technique to use = MAD
    private void computeMotionVector() {

    }

    private void getLayer() {

    }


    // PART 2: COMPRESSION

    // divide each macroblock into 8x8 blocks for each frame
    private void block() {

    }

    private void dct() {

    }

    private void quantize() {

    }

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
