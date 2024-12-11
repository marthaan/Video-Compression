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

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AudioVideoPlayer extends Application {

    private List<WritableImage> frames;  // Store frames to display


    @Override
    public void start(Stage primaryStage) {
        loadAndDisplayFrames();  // Start loading and displaying frames
    }

    // This method is responsible for fetching frames and displaying them
    private void loadAndDisplayFrames() {
        // Example: Passing dummy paths to the decoder (update with real paths)
        File encoderFile = new File("/Users/marthaannwilliams/Desktop/2frames.cmp");
        String audioPath = "/Users/marthaannwilliams/Desktop/USC/CSCI_576/day1/1.wav";
        
        MyDecoder decoder = new MyDecoder(encoderFile, audioPath);

        frames = decoder.testFrames();  // Fetch frames from MyDecoder --> can also use this to pass in file and audio
        // frames = decoder.getFrames();    // gets actual frames 

        // If no frames are found, handle gracefully
        if (frames == null || frames.isEmpty()) {
            System.out.println("No frames available to display.");
            return;
        }

        // Proceed to display the frames
        displayFrames();
    }

    private void displayFrames() {
        // Set up the ImageView to display the images
        ImageView imageView = new ImageView();
        StackPane root = new StackPane();
        root.getChildren().add(imageView);

        // Set up the Scene and Stage
        Scene scene = new Scene(root, 960, 540);
        Stage primaryStage = new Stage();
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
        launch(args);  // Start the JavaFX application
    }
}