package com.solace.demo.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;

public class PublishEventHandler implements JCSMPStreamingPublishEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(PublishEventHandler.class);

    public void responseReceived(String messageID) {
        logger.info("Producer received response for msg: " + messageID);
    }

    public void handleError(String messageID, JCSMPException e, long timestamp) {
        logger.info("Producer received error for msg: %s@%s - %s%n", messageID, timestamp, e);
    }
}