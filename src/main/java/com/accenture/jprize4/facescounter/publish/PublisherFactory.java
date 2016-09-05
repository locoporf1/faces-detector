
package com.accenture.jprize4.facescounter.publish;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mariano
 */
public class PublisherFactory {
    
    private PublisherFactory() {
    }
    
    public static PublisherFactory getInstance() {
        return PublisherFactoryHolder.INSTANCE;
    }
    
    private static class PublisherFactoryHolder {

        private static final PublisherFactory INSTANCE = new PublisherFactory();
    }
    
    public Publisher getPublisher(String providerClass) {
        Publisher result = null;
        String qualifiedClassName;
        final String packageName = this.getClass().getPackage().getName();
        if (providerClass.startsWith(packageName)) {
            qualifiedClassName = providerClass;
        } else {
            qualifiedClassName = packageName + '.' + providerClass;
        }
        try {
            Class provider = Class.forName(qualifiedClassName);
            result = (Publisher) provider.newInstance();
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException ex) {
            Logger.getLogger(PublisherFactory.class.getName()).log(Level.SEVERE, "Unable to get publisher", ex);
        }
        return result;
    }
}
