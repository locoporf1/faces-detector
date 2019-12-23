 package com.accenture.jprize4.facescounter.main;

import com.accenture.jprize4.facescounter.livepreview.LivePreviewFrame;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import java.io.File;
import java.io.IOException;
import org.opencv.videoio.VideoCapture;
import com.accenture.jprize4.facescounter.publish.Publisher;
import com.accenture.jprize4.facescounter.domain.MonitorInfo;
import com.accenture.jprize4.facescounter.publish.PublisherFactory;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opencv.core.Size;
import org.opencv.objdetect.Objdetect;

public class Main {

    private static final String PATH_IMAGES = System.getProperty("user.home") + File.separator + "Pictures";
    private static final MonitorInfo MONITOR_INFO = new MonitorInfo();
    
    private static Publisher publisher;    
    private static CascadeClassifier cascadeClassifier;
    private static Boolean saveOutputImages;
    private static File classifierFile;

    public static String lastFilename;
    public static File lastFile;

    public static volatile boolean running;

    public static volatile boolean publisherReady;

    private static LivePreviewFrame application;

    public static void main(String[] args) {

        if (args.length < 1) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Properties file path expected as argument");
        } else {
            application = new LivePreviewFrame();
            application.setVisible(true);

            final Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(args[0]));

                publisher = PublisherFactory.getInstance().getPublisher(properties.getProperty("publisher.impl"));

                String deviceId = properties.getProperty("publisher.device.id");
                MONITOR_INFO.setId(deviceId);

                saveOutputImages = Boolean.valueOf(properties.getProperty("save.frames"));
                classifierFile = new File(properties.getProperty("classifier.file"));

                Logger.getLogger(Main.class.getName()).log(Level.INFO, "Loading OpenCV native interface...");
                System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                Logger.getLogger(Main.class.getName()).log(Level.INFO, "OpenCV native interface ready to work :-)");
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error loading properties file", ex);
            }

            if (saveOutputImages) {
                Logger.getLogger(Main.class.getName()).log(Level.INFO, "Saving analyzed images to target directory: {0}", PATH_IMAGES);
                final File fileImagePath = new File(PATH_IMAGES);
                if (!fileImagePath.exists()) {
                    try {
                        fileImagePath.createNewFile();
                    } catch (IOException ioe) {
                        saveOutputImages = false;
                        Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Unable to create directory: {0}", ioe.getMessage());
                        Logger.getLogger(Main.class.getName()).log(Level.WARNING, "->stack trace", ioe);
                        Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Analyzed images will not be saved");
                    }
                }
            }
            if (classifierFile.exists()) {
                cascadeClassifier = new CascadeClassifier(classifierFile.getPath());
            } else {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Classifier file not found :-(");
                return;
            }

            new Thread(() -> openPublisher(properties)).start();

            detectFromCamera();
        }
    }

    private static void detectFromCamera() {
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "Initiating connection with camera...");
        final VideoCapture videoCapture = new VideoCapture(0);
        if (videoCapture.isOpened()) {
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Camera OK :-)");
        } else {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "No camera :-(");
            return;
        }

        running = true;
        new Thread(() -> {
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Detection session started");
            final Mat frame = new Mat();
            final Mat grayFrame = new Mat();
            int absoluteFeatureSize = 0;
            int i = 0;
            while (running && videoCapture.read(frame)) {
                Logger.getLogger(Main.class.getName()).log(Level.FINE, "Image read - analyzing it...");
                // convert the frame in grayscale and equalize the histogram to improve the results
                Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.equalizeHist(grayFrame, grayFrame);
                // set the minimum size of the feature to be detected
                // working threshold is 5% of the frame height
                if (absoluteFeatureSize <= 0) {
                    absoluteFeatureSize = Math.round(grayFrame.rows() * 0.05f);
                }
                final MatOfRect detectedFeatures = new MatOfRect();
                cascadeClassifier.detectMultiScale(
                        grayFrame,
                        detectedFeatures,
                        1.3, // try 1.05 - 1.3
                        3, // try 2 - 6
                        0 | Objdetect.CASCADE_SCALE_IMAGE,
                        new Size(absoluteFeatureSize, absoluteFeatureSize),
                        new Size()
                );
                final Rect[] rectangles = detectedFeatures.toArray();
                final int hits = rectangles.length;
                Logger.getLogger(Main.class.getName()).log(Level.FINE, "Image analyzed - {0} hits found", hits);
                if (hits > 0) {
                    publishDetectionData(hits);
                }
                decorateFrame(frame, rectangles);
                saveFrame(i++, frame);
                updateLivePreview();
            }
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Detection session finished");
            closePublisher();
        }).start();
    }

    private static void publishDetectionData(int hits) {
        if (publisher != null && publisherReady) {
            MONITOR_INFO.setCounter(hits);
            try {
                publisher.publish(MONITOR_INFO);
                Logger.getLogger(Main.class.getName()).log(Level.FINE, "Message sent: {0} hits found", hits);
            } catch (IOException ioe) {
                Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Publish fail: {0}", ioe.getMessage());
                Logger.getLogger(Main.class.getName()).log(Level.WARNING, "->stack trace", ioe);
            }
        }
    }

    private static void openPublisher(Properties properties) {
        try {
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Publisher expected at URL: {0}", properties.getProperty("publisher.broker.url"));
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Opening connection with publisher...");
            publisher.connect(properties);
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Connection with publisher established");
            publisherReady = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> closePublisher()));
        } catch (IOException ioe) {
            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Unable to connect with publisher: {0}", ioe.getMessage());
            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "->stack trace", ioe);
        }
    }

    private static void closePublisher() {
        try {
            if (publisher != null && publisherReady) {
                Logger.getLogger(Main.class.getName()).log(Level.INFO, "Closing connection with publisher...");
                publisher.close();
                Logger.getLogger(Main.class.getName()).log(Level.INFO, "Connection with publisher closed");
                publisher = null;
                publisherReady = false;
            }
        } catch (IOException ioe) {
            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Unable to close publisher: {0}", ioe.getMessage());
            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "->stack trace", ioe);
        }
    }

    private static void decorateFrame(Mat frame, Rect[] rectangles) {
        for (Rect rect : rectangles) {
            Imgproc.rectangle(
                    frame,
                    new Point(rect.x, rect.y), 
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0)
            );
        }
    }

    private static void saveFrame(int i, Mat frame) {
        lastFilename = PATH_IMAGES + File.separator + "last.jpg";
        lastFile = new File(lastFilename);
        Imgcodecs.imwrite(lastFilename, frame);

        if (saveOutputImages) {
            final String frameFilename = PATH_IMAGES + File.separator + "output" + i + ".png";
            Imgcodecs.imwrite(frameFilename, frame);
            Logger.getLogger(Main.class.getName()).log(Level.FINE, "Analyzed image successfully written to file: {0}", frameFilename);
        }
    }

    private static void updateLivePreview() {
        if (lastFilename != null) {
            try {
                application.paintImage(application.loadImage(new FileInputStream(lastFilename)));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
