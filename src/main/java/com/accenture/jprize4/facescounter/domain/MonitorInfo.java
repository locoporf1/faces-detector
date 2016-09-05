package com.accenture.jprize4.facescounter.domain;

import java.io.Serializable;

/**
 *
 * @author Mariano
 */
public class MonitorInfo implements Serializable {
    private String id;
    private Integer counter;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public Integer getCounter() {
        return this.counter;
    }
    
    public void setCounter(Integer counter) {
        this.counter = counter;
    }
    
    public String serialize() {
        return String.format("{\"id\":\"%s\",\"counter\":%d}", id, counter);
    }
}
