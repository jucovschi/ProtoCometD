package com.github.jucovschi.ProtoCometD;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cometd.bayeux.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunicationContext {
	boolean hasMsgId, hasrMsgId;
	long msgId;
	String channel;
	boolean abort;
    protected final static Logger _logger = LoggerFactory.getLogger(CommunicationCallback.class);
    List<Object> params;
    
    public List<Object> getParams() {
		return params;
	}
    
	public CommunicationContext() {
		abort = false;
		params = new ArrayList<Object>();
	}
	
	void addParam(Object obj) {
		params.add(obj);
	}
	
	public boolean getAbort() {
		return abort;
	}
	
	public void abortProcessing(String message) {
		_logger.debug("Aborting message processing: "+message);
		abort = true;
	}
	
	public boolean isResponse() {
		return hasrMsgId;
	}

	public boolean hasCallback() {
		return hasMsgId;
	}

	public String getChannel() {
		return channel;
	}

	public long getMsgId() {
		return msgId;
	}

	static CommunicationContext getInstance(Message msg) {
		CommunicationContext result = new CommunicationContext();
		Map<String, Object> obj = msg.getDataAsMap();
		if (obj.containsKey("rmsgid")) {
			result.hasrMsgId = true;
			result.msgId = Long.parseLong(obj.get("rmsgid").toString());
		}
		if (obj.containsKey("msgid")) {
			result.hasMsgId = true;
			result.msgId = Long.parseLong(obj.get("msgid").toString());
		}
		result.channel = msg.getChannel();
		return result;
	}
}
