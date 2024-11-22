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
    private static int[] inputArray = new int[frameSize];
    
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

            int iterator = 0;
            while (readFrame(fis)) {
                formatFrame();
                // displays progress
                System.out.println(iterator);
                iterator++;
            }
            for (int i = 0; i < 30; i += 3) {
                System.out.printf("R: %d, G: %d, B: %d\n", inputArray[i], inputArray[i+1], inputArray[i+2]);
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean readFrame(FileInputStream fis) throws IOException {
        for (int i = 0; i < frameSize; i++) {
            int temp = 0;
            if ((temp = fis.read()) == -1) {
                return false;
            }
            else {
                inputArray[i] = temp;
            }
        }
        return true;
    }

    public static void formatFrame() {
        int[] tempArray = new int[frameSize];

        for (int i = 0; i < channelSize; i++) {
            tempArray[i*3] = inputArray[i];
            tempArray[i*3+1] = inputArray[channelSize + i];
            tempArray[i*3+2] = inputArray[channelSize * 2 + i];
        }

        inputArray = tempArray;
    }

    // public static void formatFrameArray() {
    //     ArrayList<Integer> tempArray = new ArrayList<Integer>(inputArray.size());
    //     int channelSize = width * height;
    //     int frameSize = channelSize * 3;
    //     int numFrames = inputArray.size() / frameSize;

    //     // for each frame
    //     for (int frame = 0; frame < numFrames; frame++) {
    //         int frameStartIndex = frame * frameSize;
    //         for (int i = 0; i < frameSize; i++) {
    //             // if this is an R value
    //             if (i < channelSize) {
    //                 tempArray.set(frameStartIndex + i * 3, inputArray.get(frameStartIndex + i));
    //             }
    //             // if this is a G value
    //             else if (i < channelSize * 2 / 3) {
    //                 tempArray.set(frameStartIndex + (i - channelSize) * 3 + 1, inputArray.get(frameStartIndex + i));
    //             }
    //             // if this is a B value
    //             else {
    //                 tempArray.set(frameStartIndex + (i - (channelSize * 2) * 3 + 2), inputArray.get(frameStartIndex + i));
    //             }
    //         }
    //     }
    //     inputArray = tempArray;
    // }
}
