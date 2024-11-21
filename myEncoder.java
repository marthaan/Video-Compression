import java.io.File;

public class myEncoder {
    private static File inputFile;
    
    private static int n1; // foreground quantization step
    private static int n2; // background quantization step
    
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

        // .rgb folder --> video files = 960 x 540 (1/2 HD)
            // 30 fps
            // varied lengths --> read frame by frame until none left 
        // wavs folder --> mp4 audio files
            // 44.1 KHz


        // open file
    }
}
