import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
public class AudioVideoPlayer extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create a WritableImage (let's say 960x540 with solid red color)
        int width = 960;
        int height = 540;

        WritableImage writableImage = new WritableImage(width, height);

        // Set all pixels of the image to a solid color (e.g., red)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                writableImage.getPixelWriter().setColor(x, y, Color.RED);  // Red color
            }
        }

        // Set up ImageView to display the image
        ImageView imageView = new ImageView(writableImage);
        StackPane root = new StackPane();
        root.getChildren().add(imageView);

        // Set up the Scene and Stage
        Scene scene = new Scene(root, width, height);
        primaryStage.setTitle("Solid Color Image Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);  // Starts the JavaFX application
    }
}
 */

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

public class AudioVideoPlayer extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create a list of WritableImages with different solid colors
        List<WritableImage> frames = new ArrayList<>();
        int width = 960;
        int height = 540;
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.PURPLE};

        // Generate a WritableImage for each color and add it to the list
        for (Color color : colors) {
            WritableImage writableImage = new WritableImage(width, height);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    writableImage.getPixelWriter().setColor(x, y, color);  // Fill with the color
                }
            }
            frames.add(writableImage);  // Add image to the list
        }

        // Set up the ImageView to display the images
        ImageView imageView = new ImageView();
        StackPane root = new StackPane();
        root.getChildren().add(imageView);

        // Set up the Scene and Stage
        Scene scene = new Scene(root, width, height);
        primaryStage.setTitle("Solid Color Image Sequence");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Create a Timeline to display the frames sequentially
        Timeline timeline = new Timeline();
        for (int i = 0; i < frames.size(); i++) {
            final int frameIndex = i;
            KeyFrame keyFrame = new KeyFrame(
                    Duration.seconds(i),  // Delay between frames (1 second each here)
                    event -> imageView.setImage(frames.get(frameIndex))  // Set the current image
            );
            timeline.getKeyFrames().add(keyFrame);
        }

        // Play the timeline to show the images one by one
        timeline.setCycleCount(Timeline.INDEFINITE);  // Infinite loop of frames
        timeline.play();
    }

    public static void main(String[] args) {
        launch(args);  // Starts the JavaFX application
    }
}
