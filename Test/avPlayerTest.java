package Test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class avPlayerTest extends Application {

    // Local video path
    private String mediaUrl = "/Users/marthaannwilliams/Desktop/USC/CSCI_576/video/videos_540p/WalkingStaticBackground.mp4";

    @Override
    public void start(Stage primaryStage) {
        // Format the media URL to use 'file://' prefix
        String videoPath = "file:///" + mediaUrl;

        // Create Media and MediaPlayer
        Media media = new Media(videoPath);
        MediaPlayer mediaPlayer = new MediaPlayer(media);

        // Create a MediaView to display the video
        MediaView mediaView = new MediaView(mediaPlayer);
        mediaView.setFitWidth(960); // Set width
        mediaView.setFitHeight(540); // Set height
        mediaView.setPreserveRatio(true); // Keep the aspect ratio

        // StackPane to hold the MediaView
        StackPane root = new StackPane();
        root.getChildren().add(mediaView);

        // Create a Scene and set it on the Stage
        Scene scene = new Scene(root, 960, 540);
        primaryStage.setTitle("AudioVideoPlayer");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Play the media when it's ready
        mediaPlayer.setOnReady(() -> {
            mediaPlayer.play();
        });

        // End of media event
        mediaPlayer.setOnEndOfMedia(() -> {
            System.out.println("Video finished playing.");
        });
    }

    public static void main(String[] args) {
        launch(args); // Starts the JavaFX application
    }
}
