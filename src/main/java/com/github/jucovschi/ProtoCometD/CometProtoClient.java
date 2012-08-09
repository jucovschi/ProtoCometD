package com.github.jucovschi.ProtoCometD;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheBuilder;
import javax.cache.CacheConfiguration.Duration;
import javax.cache.CacheConfiguration.ExpiryType;
import javax.cache.CacheManager;
import javax.cache.Caching;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.common.AbstractClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.AbstractMessage;

public class CometProtoClient {
	AbstractClientSession client;
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final Random rand = new Random();

	protected final Cache<Long, CommunicationCallback> invokeCache;

	public CometProtoClient(AbstractClientSession client) {
		this.client = client;
		CacheManager cacheManager = Caching.getCacheManager();
		Cache<Long, CommunicationCallback> _invokeCache;

		_invokeCache = cacheManager.getCache("testCache");
		if (_invokeCache == null) {
			CacheBuilder<Long, CommunicationCallback> builder = cacheManager.createCacheBuilder("testCache");
			builder.setExpiry(ExpiryType.ACCESSED, new Duration(TimeUnit.HOURS, 1));

			_invokeCache = builder.build();
		}
		invokeCache = _invokeCache;
	}

	class CometProtoService implements ClientSessionChannel.MessageListener {
		CommunicationCallback invoker;

		public CometProtoService(CommunicationCallback invoker) {
			this.invoker = invoker;
		}

		public void onMessage(ClientSessionChannel channel, Message message) {
			AbstractMessage msg = ProtoUtils.createProto(message);
			if (msg == null) {
				logger.debug("Message "+message.getJSON()+" could not be parsed into a known protobuffer");
				return;
			}
			if (invoker.isAllowedMessage(msg) && invoker.isAllowedUser(message)) {
				invoker.invoke(channel, msg, message);
			}
		}
	}

	public void publish(String channel, AbstractMessage msg) {
		client.getChannel(channel).publish(ProtoUtils.prepareProto(msg));
	}

	public void publish(String channel, AbstractMessage msg, CommunicationCallback invoker) {
		Map<String, Object> toSend = ProtoUtils.prepareProto(msg);
		long msgid = rand.nextLong();
		toSend.put("msgid", msgid);
		invokeCache.put(msgid, invoker);
		client.getChannel(channel).publish(toSend);
	}

	public void respond(AbstractMessage msg, Message originalMessage) {
		Map<String, Object> toSend = ProtoUtils.prepareProto(msg);
		if (originalMessage.containsKey("msgid")) {
			toSend.put("msgid", originalMessage.get("msgid"));
		}
		client.getChannel(originalMessage.getChannel()).publish(toSend);
	}

	public void addService(String channel, CommunicationCallback info) {
		if (info==null) {
			return;
		}
		client.getChannel(channel).addListener(new CometProtoService(info));

	}
}
