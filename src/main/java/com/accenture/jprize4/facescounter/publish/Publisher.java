package com.accenture.jprize4.facescounter.publish;
import com.accenture.jprize4.facescounter.domain.MonitorInfo;
import java.io.IOException;
import java.io.Closeable;
import java.util.Properties;

/**
 *
 * @author Mariano
 */
public interface Publisher extends Closeable {
    

    void connect(Properties properties) throws IOException;
    
    void publish(MonitorInfo event) throws IOException;
    
}
