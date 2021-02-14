package com.solace.demo.cache;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.XMLMessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class CacheMessageListener implements XMLMessageListener {
	protected static final Logger logger = LoggerFactory.getLogger(CacheMessageListener.class);
	private CacheProperties cacheProperties;

	private CacheApplication broker = null;

	public CacheMessageListener(CacheApplication _broker, CacheProperties _cacheProperties) {
		super();
		broker = _broker;
		cacheProperties = _cacheProperties;
	}

	@Override
	public void onReceive(BytesXMLMessage msg) {
		String recvTopic = msg.getDestination().getName();
		logger.debug("Receive message on " + recvTopic);
		// check if this is a cache request
		if (recvTopic.equals(cacheProperties.getRequestTopic())) {
			broker.onCacheRequest(msg);
		} else if (msg.isCacheMessage()) {
			broker.onCachedMessage(msg);
		} else {
			logger.warn("Received unexpected message: " + msg.getClass().getSimpleName());
		}
	}

	@Override
	public void onException(JCSMPException e) {
		System.out.printf("Consumer received exception: %s%n", e);
	}
}
