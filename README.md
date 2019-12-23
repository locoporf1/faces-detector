# faces-detector

## setup instructions

1. Install OpenCV

https://linuxize.com/post/how-to-install-opencv-on-raspberry-pi/

NOTE: Ensure JAVA_HOME is set before (and points to Java 8) to build Java bindings


2. Install Jar file as Maven artifact

mvn install:install-file -Dfile=/to/path/opencv/build/bin/opencv-420.jar -DgroupId=org.opencv -DartifactId=opencv -Dversion=4.2.0 -Dpackaging=jar


3. Make the package

mvn package


4. Copy the dependencies

mvn dependency:copy-dependencies -DoutputDirectory=target


## run instructions

1. Version with one classifier (face recognition)

java -Djava.library.path=/usr/local/share/java/opencv4 -jar target/faces-detector-1.0-SNAPSHOT.jar faces-detector.properties


2. Version with two classifiers (face + eye recognition)

java -Djava.library.path=/usr/local/share/java/opencv4 -classpath target/faces-detector-1.0-SNAPSHOT.jar:target/opencv-4.2.0.jar com.accenture.jprize4.facescounter.main.Main2 faces-detector.properties

