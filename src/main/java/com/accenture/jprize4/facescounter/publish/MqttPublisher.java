package com.accenture.jprize4.facescounter.publish;

import com.accenture.jprize4.facescounter.domain.MonitorInfo;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 *
 * @author Mariano
 */
public class MqttPublisher implements Publisher {

    
    public String topic;
    
    private MqttClient mqttClient;

    @Override
    public void connect(Properties properties) throws IOException {
        topic = properties.getProperty("publisher.topic");
        try {
            mqttClient = new MqttClient(
                    properties.getProperty("publisher.broker.url"), 
                    properties.getProperty("publisher.device.id"), 
                    new MemoryPersistence()
            );
            final MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);
        } catch (MqttException ex) {
            Logger.getLogger(MqttPublisher.class.getName()).log(Level.SEVERE, "Unable to connect to MQTT Broker", ex);
            throw new IOException(ex);
        }
    }

    @Override
    public void publish(MonitorInfo event) throws IOException {
        if (mqttClient != null && event != null) {
            final MqttMessage message = new MqttMessage(event.serialize().getBytes());
            message.setQos(2);
            try {
                mqttClient.publish(topic, message);
            } catch (MqttException ex) {
                Logger.getLogger(MqttPublisher.class.getName()).log(Level.SEVERE, "Unable to publish message", ex);
                throw new IOException(ex);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
            } catch (MqttException ex) {
                Logger.getLogger(MqttPublisher.class.getName()).log(Level.SEVERE, null, ex);
                throw new IOException(ex);
            }
        }
    }
    
    
    
}
