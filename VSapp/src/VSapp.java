import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.nio.ByteBuffer;

public class VSapp extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final String[] RTSP_LINKS = {
            "rtsp://your_rtsp_link_1",
            "rtsp://your_rtsp_link_2",
            "rtsp://your_rtsp_link_3",
            "rtsp://your_rtsp_link_4"
    };

    private static final int NUM_WINDOWS = 4;
    private static final int WINDOW_WIDTH = 640;
    private static final int WINDOW_HEIGHT = 480;

    @Override
    public void start(Stage primaryStage) {
        GridPane gridPane = new GridPane();
        Scene scene = new Scene(gridPane, WINDOW_WIDTH * 2, WINDOW_HEIGHT * 2);

        ImageView[] imageViews = new ImageView[NUM_WINDOWS];
        for (int i = 0; i < NUM_WINDOWS; i++) {
            imageViews[i] = new ImageView();
            imageViews[i].setFitWidth(WINDOW_WIDTH);
            imageViews[i].setFitHeight(WINDOW_HEIGHT);
            gridPane.add(imageViews[i], i % 2, i / 2);
        }

        Thread[] threads = new Thread[NUM_WINDOWS];
        for (int i = 0; i < NUM_WINDOWS; i++) {
            final int index = i;
            threads[i] = new Thread(() -> playRTSPStream(index, imageViews[index]));
            threads[i].start();
        }

        primaryStage.setTitle("RTSP Video Viewer");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            for (Thread thread : threads) {
                thread.interrupt();
            }
        });
    }

    private void playRTSPStream(int index, ImageView imageView) {
        VideoCapture capture = new VideoCapture(RTSP_LINKS[index]);

        if (!capture.isOpened()) {
            System.out.println("Error opening video stream.");
            return;
        }

        Mat frame = new Mat();
        while (!Thread.interrupted()) {
            if (capture.read(frame)) {
                Image image = matToImage(frame);
                if (image != null) {
                    imageView.setImage(image);
                }
            }
        }

        capture.release();
    }

    private Image matToImage(Mat frame) {
        int width = frame.width();
        int height = frame.height();
        int channels = frame.channels();
        byte[] buffer = new byte[width * height * channels];
        frame.get(0, 0, buffer);

        WritableImage writableImage = new WritableImage(width, height);
        PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteRgbInstance();
        writableImage.getPixelWriter().setPixels(0, 0, width, height, pixelFormat, buffer, 0, width * channels);

        return writableImage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}