package com.github.jucovschi.ProtoCometD;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.AbstractMessage;

public class CommunicationCallbackBuilder {
	List<Class<? extends AbstractMessage>> allowedMsgTypes;
	List<IContextEnricher> allowedUsers;
    protected final static Logger _logger = LoggerFactory.getLogger(CommunicationCallback.class);
	
	CommunicationCallbackBuilder() {
		allowedMsgTypes = new ArrayList<Class<? extends AbstractMessage>>();
		allowedUsers = new ArrayList<IContextEnricher>();
	}
	
	public CommunicationCallbackBuilder allowMessages(Class<? extends AbstractMessage> msg) {
		allowedMsgTypes.add(msg);
		return this;
	}
	
	public CommunicationCallbackBuilder allowUsers(IContextEnricher ...users) {
		for (IContextEnricher userTypeType : users) {
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
}
