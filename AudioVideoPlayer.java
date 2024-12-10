import java.util.List;

import javafx.application.Application;  // in base jar
import javafx.scene.Scene;              // in base jar
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;              // in base jar


// javaFX video player
// play video at frame rate w/ synchronized sound
    // must be able to play, pause, and step thru video frames
public class AudioVideoPlayer extends Application {
    List<WritableImage> frames;
    String audioPath;

    public AudioVideoPlayer(List<WritableImage> frames, String audioPath) {
        this.frames = frames;
        this.audioPath = audioPath;
    }
    
    @Override
    public void start(Stage primaryStage) {
        // test code, will change
        // Example: Create a simple media player for video and audio
        String mediaUrl = "file:///path/to/your/video_or_audio.mp4"; // Update this path
        Media media = new Media(mediaUrl);
        MediaPlayer mediaPlayer = new MediaPlayer(media);

        // Create a basic layout for the window
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("AudioVideoPlayer");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Play the media
        mediaPlayer.play();
    }

    // may not need
    // will probably need a public run() method or something instead 
    public static void main(String[] args) {
        launch(args); // Starts the JavaFX application
    }  
}
