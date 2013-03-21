/**
 * 
 */
package com.github.jucovschi.ProtoCometD;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.cometd.bayeux.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.GeneratedMessage;

/**
 * @author cjucovschi
 * 
 */
public class ProtoUtils {
	private static Logger myLogger = LoggerFactory.getLogger(ProtoUtils.class);

	public static AbstractMessage createProto(Message message) {
		Map<String, Object> input = message.getDataAsMap();
		String type = (String)input.get("type");
		String s = (String)input.get("s");
		if (type == null || s == null)
			return null;
		return ProtoUtils.stringToProto(type, s);
	}

	public static Map<String, Object> prepareProto(AbstractMessage message) {
		return prepareProto(message, null);
	}
	
	public static Map<String, Object> prepareProto(AbstractMessage message, CommunicationContext context) {
		Map<String, Object> output = new HashMap<String, Object>();
		if (message!=null) {
			output.put("type", message.getClass().getName());
			output.put("s", Base64.encodeBase64String(message.toByteArray()));
		} else{
			output.put("empty", true);
		}
		if (context != null && context.hasMsgId) {
			output.put("msgrid", context.getMsgId());
		}
		return output;
	}

	public static AbstractMessage stringToProto(String type, String message) {
		String classString = type;
		try {
			Class<?> t;
			t = Class.forName(classString);
			if (t == null) {
				myLogger.debug("Class "+classString+" not found");
				return null;
			}
			if (!t.getSuperclass().equals(GeneratedMessage.class)) {
				myLogger.debug("Class "+classString+" is not a protobuffered class");
				return null; 
			}
			String restMessage = message;
			byte [] binaryData = Base64.decodeBase64(restMessage);

			Object result = t.getMethod("parseFrom", byte[].class).invoke(null, binaryData);
			if (result == null)
				return null;

			return (AbstractMessage) result;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
