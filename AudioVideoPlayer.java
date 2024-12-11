import javafx.application.Application;  
import javafx.scene.Scene;              
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage; 
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import java.util.List;


// javaFX video player
// play video at frame rate w/ synchronized sound
    // must be able to play, pause, and step thru video frames
public class AudioVideoPlayer extends Application {
    // private List<WritableImage> frames;
    // private String audioPath;
    private String mediaUrl = "/Users/marthaannwilliams/Desktop/USC/CSCI_576/video/videos_540p/WalkingStaticBackground.mp4";

    // public AudioVideoPlayer() {}

    // default constructor required for javaFX
    // so this method is used to still initialize an AV player object 
    public void setInputData(List<WritableImage> frames, String audioPath) {
        // this.frames = frames;
        // this.audioPath = audioPath;
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception{
        // Create a simple media player for video and audio
        String videoPath = "file:///" + mediaUrl; // Update this path
        Media media = new Media(videoPath);
        MediaPlayer mediaPlayer = new MediaPlayer(media);

        // Create a MediaView to display the video
        MediaView mediaView = new MediaView(mediaPlayer);

        // Set the MediaView to frame size
        mediaView.setFitWidth(960);
        mediaView.setFitHeight(540);
        mediaView.setPreserveRatio(true);   // maintain aspect ratio

        // Create a basic layout for the window
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 960, 540);

        // primaryStage.setOpacity(0.0);
        primaryStage.setTitle("AudioVideoPlayer");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Play the media
        mediaPlayer.play();

        // Ensure the video plays fully by listening to the media player’s events
        mediaPlayer.setOnReady(() -> {
            System.out.println("Video ready to play!");
            mediaPlayer.play(); // Play after it's fully loaded
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            System.out.println("Video has finished playing.");
        });

    }

    // needed? or no
    public void run() {
        launch();
    }

    // may not need
    // will probably need a public run() method or something instead 
    public static void main(String[] args) {
        launch(args); // Starts the JavaFX application
    }  
}
