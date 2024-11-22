import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class myEncoder {
    private static File inputFile;
    
    private static int n1; // foreground quantization step
    private static int n2; // background quantization step
    private static int width = 960;
    private static int height = 540;
    private static int channelSize = width * height;
    private static int frameSize = channelSize * 3;
    private static int[] prevFrame = new int[frameSize];
    private static int[] currFrame = new int[frameSize];
    
    // PART1: videoSegmentation

    // getIFrame
    // macroblock --> divide frames into 16 x 16 blocks
    // computerMotionVector --> technique = MAD
    // getLayers


    // PART2: compression
    // block --> divide each macroblock into 8x8 blocks for each frame
    // DCT
    // quantize(n1, n2)
    // scan

    public static void main(String[]args) {
        inputFile = new File(args[0]);
        n1 = Integer.parseInt(args[1]);
        n2 = Integer.parseInt(args[2]);

        readFile(inputFile);

        // .rgb folder --> video files = 960 x 540 (1/2 HD)
            // 30 fps
            // varied lengths --> read frame by frame until none left 
        // wavs folder --> mp4 audio files
            // 44.1 KHz


        // open file
    }

    public static void readFile(File inputFile) {
        try {
            FileInputStream fis = new FileInputStream(inputFile);

            for (int i = 0; readFrame(fis); i++) {
                formatFrame();
                // if not I-frame
                if (i != 0) {
                    // PART1: videoSegmentation
                    // macroblock --> divide frames into 16 x 16 blocks
                    // computerMotionVector --> technique = MAD
                    // getLayers

                    // PART2: compression
                    // block --> divide each macroblock into 8x8 blocks for each frame
                    // DCT
                    // quantize(n1, n2)
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
                System.out.printf("R: %d, G: %d, B: %d\n", currFrame[i], currFrame[i+1], currFrame[i+2]);
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean readFrame(FileInputStream fis) throws IOException {
        for (int i = 0; i < frameSize; i++) {
            // used temp variable so that final currFrame value is not assigned -1
            int temp = 0;
            if ((temp = fis.read()) == -1) {
                return false;
            }
            else {
                currFrame[i] = temp;
            }
        }
        return true;
    }

    public static void formatFrame() {
        int[] tempArray = new int[frameSize];

        // change currFrame from RRR.GGG.BBB to RGB.RGB.RGB
        for (int i = 0; i < channelSize; i++) {
            tempArray[i*3] = currFrame[i];
            tempArray[i*3+1] = currFrame[channelSize + i];
            tempArray[i*3+2] = currFrame[channelSize * 2 + i];
        }

        currFrame = tempArray;
    }
}
