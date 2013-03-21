package com.github.jucovschi.ProtoCometD;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.cometd.bayeux.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.AbstractMessage;

public class CommunicationCallback {
	Object obj;
	Method invoker;
	List<Class<? extends AbstractMessage>> allowedMsgTypes;
	List<IContextEnricher> contextEnricher;
	ProtoService service;

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

	public List<Object> getInvokeParams(Object channel, AbstractMessage msg, CommunicationContext context) {
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
		return params;
	}	
};
