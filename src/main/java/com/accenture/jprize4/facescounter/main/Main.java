 package com.accenture.jprize4.facescounter.main;

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
    
    public static final String PATH_IMAGES = System.getProperty("user.home") + File.separator + "Pictures";
    private static final MonitorInfo MONITOR_INFO = new MonitorInfo();
    
    private static Publisher publisher;    
    private static CascadeClassifier cascadeClassifier;
    private static Boolean flagOutputImages;
    private static String deviceId;
    public static File classifierFile;
    
    public static void main(String[] args) {
        if (args.length < 1) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Properties file path expected as argument");
        } else {
            final Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(args[0]));
                System.out.println("Using the following properties:");
                properties.list(System.out);
                deviceId = properties.getProperty("publisher.device.id");
                publisher = PublisherFactory.getInstance().getPublisher(properties.getProperty("publisher.impl"));
                flagOutputImages = Boolean.valueOf(properties.getProperty("save.frames"));
                MONITOR_INFO.setId(deviceId);
                classifierFile = new File(properties.getProperty("classifier.file"));
                Logger.getLogger(Main.class.getName()).log(Level.INFO, "Loading opencv native libraries...");
                System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error loading properties file", ex);
            }
            if (flagOutputImages) {
                final File fileImagePath = new File(PATH_IMAGES);
                if (!fileImagePath.exists()) {
                    try {
                        fileImagePath.createNewFile();
                    } catch (IOException ex) {
                        flagOutputImages = false;
                        ex.printStackTrace();
                        Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Unable to create dir: {0}", PATH_IMAGES);
                    }
                }
            }
            if (flagOutputImages) {
                System.out.println("Saving images to " + PATH_IMAGES);
            }
            if (classifierFile.exists()) {
                cascadeClassifier = new CascadeClassifier(classifierFile.getPath());
                try {
                    publisher.connect(properties);
                    Runtime.getRuntime().addShutdownHook(
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    publisher.close();
                                } catch (IOException ex) {

                                }
                            }
                        }
                    );
                    detectFromCam();
                } catch (Exception ioe) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ioe.getMessage(), ioe);
                } finally {
                    try {
                        publisher.close();
                    } catch (IOException ex) {

                    }
                }
            } else {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Classfier XML File Not Found !!!");
            }
        }
    }
    
    private static void detectFromCam() {
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "Detecting from cam...");
        final long lStartTime = System.currentTimeMillis();
        final VideoCapture videoCapture = new VideoCapture(0);
        final Mat frame = new Mat();
        final Mat grayFrame = new Mat();
        int absoluteFaceSize = 0;
        int lastCounter = -1;
        if (videoCapture.isOpened()) {
            Logger.getLogger(Main.class.getName()).log(Level.INFO, "Camera OK!!!");
            int i = 0;
            while (videoCapture.read(frame)) {
                Logger.getLogger(Main.class.getName()).log(Level.FINE, "Image read!");
                // convert the frame in grayscale and equalize the histogram to improve the results
                Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.equalizeHist(grayFrame, grayFrame);
                // set the minimum size of the face to be detected (this required is need in the actual detection function). 
                // Let’s set the minimum size as the 20% of the frame height
                if (absoluteFaceSize <= 0) {
                    absoluteFaceSize = Math.round(grayFrame.rows() * 0.2f);
                }
                final MatOfRect faceDetections = new MatOfRect();
                cascadeClassifier.detectMultiScale(
                        grayFrame, 
                        faceDetections, 
                        1.1, 
                        2, 
                        0 | Objdetect.CASCADE_SCALE_IMAGE, 
                        new Size(absoluteFaceSize, absoluteFaceSize), 
                        new Size()
                );
                final Rect[] rectangles = faceDetections.toArray();
                if (rectangles.length > 0 && rectangles.length != lastCounter) {                    
                    if (publisher != null) {
                        MONITOR_INFO.setCounter(rectangles.length);
                        lastCounter = rectangles.length;
                        try {
                            publisher.publish(MONITOR_INFO);
                            Logger.getLogger(Main.class.getName()).log(Level.FINE, "Message sent. {} faces found", rectangles.length);
                        } catch (IOException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "publish fail: {0}",ex.getMessage());
                        }
                    }
                }
                if (flagOutputImages) {                        
                    saveFrame(i, frame, rectangles);
                    i ++;
                }
            }
            
        } else {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "No camera");
        }
        System.out.println("Detection from cam. Time(ms): " + (System.currentTimeMillis() - lStartTime));
    }

    private static void saveFrame(int i, final Mat frame, Rect[] rectangles) {
        for (Rect rect : rectangles) {
            Imgproc.rectangle(
                    frame,
                    new Point(rect.x, rect.y), 
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0)
            );
        }
        final String filename = PATH_IMAGES + File.separator + "output" + i + ".png";
        Logger.getLogger(Main.class.getName()).log(Level.FINE, "Writting to {0}", filename);
        Imgcodecs.imwrite(filename, frame);
    }
}
