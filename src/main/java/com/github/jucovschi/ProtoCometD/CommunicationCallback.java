package com.github.jucovschi.ProtoCometD;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.AbstractMessage;

class CommunicationCallback {
	Object obj;
	Method invoker;
	List<Class<? extends AbstractMessage>> allowedMsgTypes;
	List<ISenderValidator> allowedUsers;
    protected final static Logger _logger = LoggerFactory.getLogger(CommunicationCallback.class);
	
	public List<ISenderValidator> getAllowedUsers() {
		return allowedUsers;
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
	
	public static class CommunicationContext {
		boolean hasMsgId;
		long msgId;
		String channel;
		
		public CommunicationContext() {
		}
		
		public boolean isResponse() {
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
			if (msg.containsKey("msgid")) {
				result.hasMsgId = true;
				result.msgId = Long.parseLong(msg.get("msgid").toString());
			}
			result.channel = msg.getChannel();
			return result;
		}
	}
	
	public CommunicationCallback(Object obj, Method m, List<Class<? extends AbstractMessage>> allowedMsgTypes, List<ISenderValidator> allowedUsers) {
		this.obj = obj;
		this.invoker = m;
		this.allowedMsgTypes = allowedMsgTypes;
		this.allowedUsers = allowedUsers;
	}
	
	static class CommunicationCallbackBuilder {
		List<Class<? extends AbstractMessage>> allowedMsgTypes;
		List<ISenderValidator> allowedUsers;
		
		CommunicationCallbackBuilder() {
			allowedMsgTypes = new ArrayList<Class<? extends AbstractMessage>>();
			allowedUsers = new ArrayList<ISenderValidator>();
		}
		
		public CommunicationCallbackBuilder allowMessages(Class<? extends AbstractMessage> ...msg) {
			for (Class<? extends AbstractMessage> msgType : msg) {
				allowedMsgTypes.add(msgType);
			}
			return this;
		}
		
		public CommunicationCallbackBuilder allowUsers(ISenderValidator ...users) {
			for (ISenderValidator userTypeType : users) {
				allowedUsers.add(userTypeType);
			}
			return this;
		}
		
		public CommunicationCallback build(String method, Object obj) {
			for (Method mtd : obj.getClass().getMethods()) {
				if (mtd.getName().equals(method) && checkMethod(mtd)) {
					return new CommunicationCallback(obj, mtd, allowedMsgTypes, allowedUsers);
				}
			}
			_logger.debug("No method called '{}' could be used as communication callback. It should have signature (ServerSession, AbstractMessage, Message originalMessage)", new Object[]{method});
			return null;
		}
	}
	
	public static CommunicationCallbackBuilder new_builder() {
		return new CommunicationCallbackBuilder();
	}

	static boolean checkMethod(Method invoker) {
		Class<?>[] params = invoker.getParameterTypes();
		int np = params.length;
		if (np < 2 || np > 3)
			return false;
		
		if (!params[0].isAssignableFrom(ClientSessionChannel.class) && !params[0].isAssignableFrom(ServerSession.class)) {
			return false;
		}

		if (!AbstractMessage.class.isAssignableFrom(params[1])) {
			return false;
		}

		if (np>2 && !params[2].isAssignableFrom(CommunicationContext.class)) {
			return false;
		}
		return true;
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

	public boolean isAllowedUser(Message msg) {
		if (allowedUsers.size() == 0)
			return true;
		for (ISenderValidator validator : allowedUsers) {
			if (!validator.isValid(msg))
				return false;
		}
		return false;
	}

	private Object invoke(Object channel, AbstractMessage msg, Message message) {
		try {
			int noparams = invoker.getParameterTypes().length;
			if (noparams == 2)
				return invoker.invoke(obj, channel, msg);
			else {
				return invoker.invoke(obj, channel, msg, CommunicationContext.getInstance(message));
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
	
	public Object invoke(ClientSessionChannel channel, AbstractMessage msg, Message message) {
		return invoke((Object)channel, msg, message);
	}
	
	public Object invoke(ServerChannel channel, AbstractMessage msg, Message message) {
		return invoke((Object)channel, msg, message);
	}
	
};
