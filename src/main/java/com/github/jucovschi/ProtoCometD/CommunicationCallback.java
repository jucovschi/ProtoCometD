package com.github.jucovschi.ProtoCometD;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.ServerSession;
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
	
	
	public static CommunicationCallbackBuilder newBuilder() {
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
			context.addParam(validator.enrich(channelid, msg, context));
			if (context.getAbort())
				return false;
		}
		return true;
	}

	private Object invoke(Object channel, AbstractMessage msg, CommunicationContext context) {
		try {
			int noparams = invoker.getParameterTypes().length - context.getParams().size();
			
			if (noparams <= 1 || noparams >= 4) {
				return null;
			}
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(channel);
			params.add(msg);
			if (noparams == 3)
				params.add(context);
			params.addAll(context.getParams());
			invoker.invoke(obj, params.toArray());
		} catch (IllegalArgumentException e) {
			System.out.println("error invoking msg "+msg);
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.out.println("error invoking msg "+msg);
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.out.println("error invoking msg "+msg);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public Object invoke(ClientSessionChannel channel, AbstractMessage msg, CommunicationContext context) {
		return invoke((Object)channel, msg, context);
	}
	
	public Object invoke(ServerSession channel, AbstractMessage msg, CommunicationContext context) {
		return invoke((Object)channel, msg, context);
	}
	
};
