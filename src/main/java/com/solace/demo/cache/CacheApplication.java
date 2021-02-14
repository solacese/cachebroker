package com.solace.demo.cache;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.CacheLiveDataAction;
import com.solacesystems.jcsmp.CacheRequestResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.CacheSession;
import com.solacesystems.jcsmp.CacheSessionProperties;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.SpringJCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CacheApplication implements CommandLineRunner {
	private static Logger logger = LoggerFactory.getLogger(CacheApplication.class);
	@Autowired
	private SpringJCSMPFactory solaceFactory;
	@Autowired
	private CacheProperties cacheProperties;

	private JCSMPSession session;
	protected CacheSession cacheSession = null;
	private XMLMessageProducer prod = null;
	private final AtomicLong atomRequestId = new AtomicLong();
	private Cache<Long, CacheRequest> id2request;
	private CountDownLatch latch = new CountDownLatch(1);

	public static void main(String[] args) {
		SpringApplication.run(CacheApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		id2request = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
		
		session = solaceFactory.createSession();
		final XMLMessageConsumer cons = session.getMessageConsumer(new CacheMessageListener(this, cacheProperties));
		prod = session.getMessageProducer(new PublishEventHandler());

		final CacheSessionProperties cacheProps = new CacheSessionProperties(cacheProperties.getName(),
				cacheProperties.getMaxMsgsPerTopic(), cacheProperties.getMaxMsgAge(), cacheProperties.getTimeout());
		cacheSession = session.createCacheSession(cacheProps);

		logger.info("Listen on the cache request topic: {}", cacheProperties.getRequestTopic());
		final Topic topic = JCSMPFactory.onlyInstance().createTopic(cacheProperties.getRequestTopic());
		// listen on the cache request topic
		session.addSubscription(topic);
		logger.info("Connected. Awaiting requests...");
		cons.start();

		Helper.pressEnter("Press Ctrl+C to exit ...");
		latch.await();
	}

	protected void onCacheRequest(final BytesXMLMessage msg) {
		final String json = ((TextMessage)msg).getText();
		CacheRequest cr = CacheRequest.fromJson(json);
		final long requestId = atomRequestId.incrementAndGet();
		
		id2request.put(requestId, cr);
		try {
			final Topic cacheTopic = JCSMPFactory.onlyInstance().createTopic(cr.getTopic());
			final CacheRequestResult result = cacheSession.sendCacheRequest(requestId, cacheTopic, false,
					CacheLiveDataAction.FLOW_THRU);
			logger.debug("Cache Request NO:{} is {}, [{}] from [{}]", requestId, result, cr.getTopic(), cr.getReplyTo());
		} catch (final Exception e) {
			logger.warn("Unable to send cache request: {}", e.getMessage());
		}

	}

	protected void onCachedMessage(final BytesXMLMessage msg) {
		try {
			final long requestId = msg.getCacheRequestId();
			final CacheRequest cr = id2request.getIfPresent(requestId);
			if (null == cr) {
				// non existed cache request id, just discard it
				logger.warn("Get un-matched cached message, ID="+requestId);
				return;
			}

			final String text = ((TextMessage)msg).getText();
			TextMessage message = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
			message.setText(text);
			prod.send(message, JCSMPFactory.onlyInstance().createTopic(cr.getReplyTo()));
		} catch (final Exception e) {
			logger.warn("onCachedMessage: {}", e);
		}
	}
}
