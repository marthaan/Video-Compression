import javafx.application.Application;  // base class for JavaFX apps
import javafx.stage.Stage;      // main window of the app 
import javafx.scene.Scene;      // content & appearance of the stage (window)
import javafx.scene.media.Media;        // represents a media file (video, audio)
import javafx.scene.media.MediaPlayer;  // controls playback of media objects
import javafx.scene.image.ImageView;    // displays images on the UI
import javafx.scene.image.WritableImage;    // frame; similar to buffered image --> can be modified dynamically
import javafx.scene.control.Button;    // UI --> pause, play, next frame, previous frame
import javafx.scene.layout.HBox;        // horizontal frame for the buttons --> organizes any UI components in a row
import javafx.scene.layout.StackPane;   // stack-based layout for stacking/overlaying multiple UI components
import javafx.animation.KeyFrame;       // ensures correct frame displayed at correct time (animation control)
import javafx.animation.Timeline;   // creates frame-by-frame animation based on a timeline 
import javafx.geometry.*;               // provides alignments (BOTTOM_CENTER, etc.)
import javafx.util.Duration;    // represents a span of time (frame timing, 30 fps)

import java.io.File;
import java.util.List;

public class AudioVideoPlayer extends Application {

    private List<WritableImage> frames;  // stores decoded frames to display
    private MediaPlayer mediaPlayer;    // controls playback of the audio file
    private Timeline timeline;          // syncs & displays video frames based on time (30 fps)
    private int currentFrameIndex = 0;  // keeps track of the currently displayed frame

    @Override
    public void start(Stage primaryStage) {
        loadAndDisplayFrames();  // starts decoding & playback of video/audio
    }

    // decodes video & prepares for display
    private void loadAndDisplayFrames() {
        File encoderFile = new File("3.cmp");      // encoded video file to be decoded, currently holds 10 frames
        String audioPath = "/Users/marthaannwilliams/Desktop/day3/3.wav";   // path to audio file

        MyDecoder decoder = new MyDecoder(encoderFile, audioPath);  // create decoder instance
        frames = decoder.getFrames();    // returns actual decoded frames --> 10 frames per 35 sec
        // frames = decoder.testFrames();      // returns test list of solid color writable images

        // check if frames were decoded successfully
        if (frames == null || frames.isEmpty()) {
            System.out.println("No frames available to display.");
            return;
        }

        // load audio file into media player for playback 
        Media audioMedia = new Media(new File(audioPath).toURI().toString());
        mediaPlayer = new MediaPlayer(audioMedia);

        // call method to display the frames
        displayFrames();
    }

    private void displayFrames() {
        // set up ImageView to display each frame as an image
        ImageView imageView = new ImageView();
        imageView.setImage(frames.get(currentFrameIndex)); // Display the first frame

        StackPane root = new StackPane();   // overlays image and buttons
        root.getChildren().add(imageView);  // add ImageView to layout

        // create control buttons
        Button playButton = new Button("Play");
        Button pauseButton = new Button("Pause");
        Button nextFrameButton = new Button("Next Frame");
        Button prevFrameButton = new Button("Previous Frame");

        // arrange buttons horizontally using HBox
        HBox buttonLayout = new HBox(10);  // 10-pixel spacing between buttons
        buttonLayout.getChildren().addAll(playButton, pauseButton, nextFrameButton, prevFrameButton);

        // align the HBox to the bottom of the StackPane and center horizontally
        buttonLayout.setAlignment(Pos.BOTTOM_CENTER);

        // add the HBox to the root
        root.getChildren().add(buttonLayout);

        // set up the Scene and Stage for UI display
        Scene scene = new Scene(root, 960, 540);    // 960 x 540 window created
        Stage primaryStage = new Stage();
        primaryStage.setTitle("Audio-Video Player");
        primaryStage.setScene(scene);
        primaryStage.show();    // show the window

        // create a Timeline to display the frames sequentially
        timeline = new Timeline();
        double fps = 30.0; // Set frame rate (frames per second)
        for (int i = 0; i < frames.size(); i++) {
            final int frameIndex = i;       // index of frame currently being displayed
            KeyFrame keyFrame = new KeyFrame(
                    Duration.seconds(i / fps),  // delay between frames (based on FPS)
                    event -> imageView.setImage(frames.get(frameIndex))  // set the current frame image
            );
            timeline.getKeyFrames().add(keyFrame);
        }

        // synchronize audio & video playback
        timeline.setCycleCount(Timeline.INDEFINITE); // loop video indefinitely

        // event handler for Play button
        playButton.setOnAction(event -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY) {
                mediaPlayer.play();  // resume or start audio
                timeline.play();  // resume video frames
            }
        });

        // event handler for Pause button
        pauseButton.setOnAction(event -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();  // pause audio
                timeline.pause();  // pause video frames
            }
        });

        // event handler for Next Frame button
        nextFrameButton.setOnAction(event -> {
            if (currentFrameIndex < frames.size() - 1) {
                currentFrameIndex++;
                imageView.setImage(frames.get(currentFrameIndex));
                timeline.pause();  // Pause timeline to prevent automatic playback
                mediaPlayer.pause();
            }
        });

        // event handler for Previous Frame button
        prevFrameButton.setOnAction(event -> {
            if (currentFrameIndex > 0) {
                currentFrameIndex--;
                imageView.setImage(frames.get(currentFrameIndex));
                timeline.pause();  // Pause timeline to prevent automatic playback
                mediaPlayer.pause();
            }
        });

        // start both audio and video playback initially
        mediaPlayer.play();
        timeline.play();
    }

    // main method to launch the JavaFX app
    public static void main(String[] args) {
        launch(args);  // calls start() method to begin execution 
    }
}
