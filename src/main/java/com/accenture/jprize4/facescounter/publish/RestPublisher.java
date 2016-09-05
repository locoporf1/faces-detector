package com.accenture.jprize4.facescounter.publish;

import com.accenture.jprize4.facescounter.domain.MonitorInfo;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

/**
 *
 * @author Mariano
 */
public class RestPublisher implements Publisher {
    
    public static final String URL_WSRESTFUL = "http://localhost:8080/jprize4/rest"; //"http://jprize4-locoporf1.rhcloud.com/rest";
    
    private HttpURLConnection conn;
    private URL url;

    @Override
    public void publish(MonitorInfo message) throws IOException {
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        try (final OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream())) {        
            osw.write(message.serialize());
            osw.flush();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {                
                throw new IOException("HttpCode: " + conn.getResponseCode());
            }
        }
    }

    @Override
    public void connect(Properties properties) throws IOException {
        url = new URL(URL_WSRESTFUL + "?deviceId=" + properties.getProperty("publisher.device.id"));
    }
    
    @Override
    public void close() throws IOException {
        if (conn != null) {
            conn.disconnect();
        }
    }
    
}
