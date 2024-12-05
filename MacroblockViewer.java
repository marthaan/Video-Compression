import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MacroblockViewer {
    public static void saveMacroblock(int[][][] macroblock, String outputPath) throws Exception {
        int width = macroblock.length;       // Macroblock width
        int height = macroblock[0].length;  // Macroblock height
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int r = macroblock[x][y][0];
				int g = macroblock[x][y][1];
				int b = macroblock[x][y][2];
                //int b = macroblock[x][y][2];
                int rgb = (r << 16) | (g << 8) | b; // Combine RGB channels
                image.setRGB(x, y, rgb);
            }
        }

        File outputFile = new File(outputPath);
        ImageIO.write(image, "png", outputFile);
        System.out.println("Macroblock saved to: " + outputPath);
    }
}