package com.github.jucovschi.ProtoCometD;

import java.util.Map;
import java.util.Random;

import javax.swing.text.html.parser.Entity;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.common.AbstractClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jucovschi.ProtoCometD.CommunicationCallback.CommunicationContext;
import com.google.protobuf.AbstractMessage;

public class CometProtoClient {
	AbstractClientSession client;
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final Random rand = new Random();

	private final Cache invokeCache;
	
	
	public CometProtoClient(AbstractClientSession client) {
		this.client = client;
		CacheManager cacheManager = CacheManager.getInstance();
		int oneDay = 24 * 60 * 60;
		Cache _cache = cacheManager.getCache("CometProtoRequests");
		if (_cache == null) {
			_cache = new Cache("CometProtoRequests", 200, false, false, oneDay, oneDay);		
			cacheManager.addCache(_cache);	
		}
		invokeCache = _cache;
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
		invokeCache.put(new Element(msgid, invoker));
		client.getChannel(channel).publish(toSend);
	}

	public void respond(AbstractMessage msg, CommunicationContext commContext) {
		Map<String, Object> toSend = ProtoUtils.prepareProto(msg);
		if (commContext.isResponse()) {
			toSend.put("msgid", commContext.getMsgId());
		}
		client.getChannel(commContext.getChannel()).publish(toSend);
	}

	public void addService(String channel, CommunicationCallback info) {
		if (info==null) {
			return;
		}
		client.getChannel(channel).addListener(new CometProtoService(info));
	}
}
