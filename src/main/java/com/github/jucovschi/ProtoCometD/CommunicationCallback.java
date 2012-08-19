package com.github.jucovschi.ProtoCometD;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.ServerChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.AbstractMessage;

public class CommunicationCallback {
	Object obj;
	Method invoker;
	List<Class<? extends AbstractMessage>> allowedMsgTypes;
	List<IContextEnricher> contextEnricher;
	
    protected final static Logger _logger = LoggerFactory.getLogger(CommunicationCallback.class);
	
	public List<IContextEnricher> getContextEnrichers() {
		return contextEnricher;
	}
	
	public List<Class<? extends AbstractMessage>> getAllowedMsgTypes() {
		return allowedMsgTypes;
	}
	
	public Method getInvoker() {
		return invoker;
	}
	
	public Object getObj() {
		return obj;
	}
	
	
	public CommunicationCallback(Object obj, Method m, List<Class<? extends AbstractMessage>> allowedMsgTypes, List<IContextEnricher> contextEnrichers) {
		this.obj = obj;
		this.invoker = m;
		this.allowedMsgTypes = allowedMsgTypes;
		this.contextEnricher = contextEnrichers;
	}
	
	
	public static CommunicationCallbackBuilder new_builder() {
		return new CommunicationCallbackBuilder();
	}

	
	public boolean isAllowedMessage(AbstractMessage msg) {
		if (allowedMsgTypes.size() == 0)
			return true;
		for (Class<? extends AbstractMessage> cls : allowedMsgTypes) {
			if (cls.isAssignableFrom(msg.getClass()))
				return true;
		}
		return false;
	}

	public boolean enrichContext(String channelid, Message msg, CommunicationContext context) {
		if (contextEnricher.size() == 0)
			return true;
		for (IContextEnricher validator : contextEnricher) {
			validator.enrich(channelid, msg, context);
			if (context.getAbort())
				return false;
		}
		return true;
	}

	private Object invoke(Object channel, AbstractMessage msg, CommunicationContext context) {
		try {
			int noparams = invoker.getParameterTypes().length;
			if (noparams == 2)
				return invoker.invoke(obj, channel, msg);
			else {
				return invoker.invoke(obj, channel, msg, context);
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public Object invoke(ClientSessionChannel channel, AbstractMessage msg, CommunicationContext context) {
		return invoke((Object)channel, msg, context);
	}
	
	public Object invoke(ServerChannel channel, AbstractMessage msg, CommunicationContext context) {
		return invoke((Object)channel, msg, context);
	}
	
};
