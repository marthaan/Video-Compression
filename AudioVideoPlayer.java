import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AudioVideoPlayer extends Application {
    private List<WritableImage> frames;  // Store frames to display
    private MediaPlayer mediaPlayer;
    private Timeline timeline;

    @Override
    public void start(Stage primaryStage) {
        loadAndDisplayFrames();  // Start loading and displaying frames
    }

    // runs decoder to get decompressed frames 
    // initiates display process
    private void loadAndDisplayFrames() {
        // pass paths to the decoder (update with args)
        File encoderFile = new File("3.cmp");      // should only hold 90 frames = 3 secs for now 
        String audioPath = "/Users/marthaannwilliams/Desktop/day3/3.wav";
        
        MyDecoder decoder = new MyDecoder(encoderFile, audioPath);
        frames = decoder.testFrames();  // fetch frames from MyDecoder --> could also use this to pass in file and audio
        // frames = decoder.getFrames();    // gets actual frames 

        // If no frames are found
        if (frames == null || frames.isEmpty()) {
            System.out.println("No frames available to display.");
            return;
        }

        // Load the audio file
        Media audioMedia = new Media(new File(audioPath).toURI().toString());
        mediaPlayer = new MediaPlayer(audioMedia);

        // Proceed to display the frames
        displayFrames();
    }

    private void displayFrames() {
        // set up the ImageView to display the images
        ImageView imageView = new ImageView();
        StackPane root = new StackPane();
        root.getChildren().add(imageView);

        // create pause & play buttons
        Button playButton = new Button("Play");
        Button pauseButton = new Button("Pause");

        // add buttons to layout
        StackPane buttonLayout = new StackPane();
        buttonLayout.getChildren().addAll(playButton, pauseButton);
        buttonLayout.setTranslateY(200);    // position buttons vertically

        root.getChildren().add(buttonLayout);

        // set up the Scene & Stage
        Scene scene = new Scene(root, 960, 540);
        Stage primaryStage = new Stage();   // global or not?
        primaryStage.setTitle("Audio-Video Player");
        primaryStage.setScene(scene);
        primaryStage.show();

        // initialize Timeline to display the frames in order
        timeline = new Timeline();
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

        // Synchronize audio & video playback 
        timeline.setCycleCount(1);      // play video sequence once
        // timeline.setOnFinished(event -> mediaPlayer.stop());    // Stop audio when video ends
        
        // Start audio and video 
        mediaPlayer.play();
        timeline.play();
    }

    public static void main(String[] args) {
        launch(args);  // Start the JavaFX application
    }
}