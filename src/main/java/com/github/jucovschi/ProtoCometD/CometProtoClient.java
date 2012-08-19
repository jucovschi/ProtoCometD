package com.github.jucovschi.ProtoCometD;

import java.util.Map;
import java.util.Random;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.common.AbstractClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.AbstractMessage;

/**
 * This class is a wrapper around CometD client object to allow 
 * sending/receiving Protobuffer messages. It is also used by the
 * ProtoService object for the server side but should not be used
 * directly.
 * 
 * Additionally allows response callbacks i.e. pushing a message and
 * being notified when the other client responded to that message.
 * 
 * @author cjucovschi
 *
 */
public class CometProtoClient {
	AbstractClientSession client;
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final Random rand = new Random();

	/*
	 * cached store for matching responses to callbacks
	 */
	private final Cache invokeCache;

	/**
	 * Constructs the CometProtoClient wrapper arround the client object
	 * @param client - the client/server session obeject
	 */
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

	/**
	 * Class responsible for  to messages received at a certain channel
	 * @author cjucovschi
	 *
	 */
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
			CommunicationContext context = CommunicationContext.getInstance(message);
			CommunicationCallback _invoker = invoker;
			if (context.isResponse()) {
				if (invokeCache.isKeyInCache(context.getMsgId())) {
					Object _rInvoker = invokeCache.get(context.getMsgId()).getObjectValue();
					if (_rInvoker instanceof CommunicationCallback) {
						_invoker = (CommunicationCallback) _rInvoker;
					}
				} else {
					logger.debug("Response message could not be matched to the caller. Timed out? ");
					return;
				}
			}
			if (_invoker.isAllowedMessage(msg) && _invoker.enrichContext(channel.getId(), message, context)) {
				_invoker.invoke(channel, msg, context);
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
		if (commContext.hasCallback()) {
			toSend.put("rmsgid", commContext.getMsgId());
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
