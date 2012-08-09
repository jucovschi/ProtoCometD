package com.github.jucovschi.ProtoCometD;

import java.util.Map;

import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.common.AbstractClientSession;
import org.cometd.server.ServerMessageImpl;

public class MockBayeuxClient extends AbstractClientSession {

	public void handshake() {
	}
	
	public void handshake(Map<String, Object> template) {
	}

	public String getId() {
		return "g43qw4";
	}

	public boolean isConnected() {
		return true;
	}

	public boolean isHandshook() {
		return true;
	}

	public void disconnect() {
	}

	@Override
	protected ChannelId newChannelId(String channelId) {
		return new ChannelId(channelId);
	}

	@Override
	protected AbstractSessionChannel newChannel(ChannelId channelId) {
		return new MockAbstractSessionChannel(channelId);
	}

	@Override
	protected void sendBatch() {
	}

	class MockAbstractSessionChannel extends AbstractSessionChannel{
		public MockAbstractSessionChannel(ChannelId channelId) {
			super(channelId);
		}

		public ClientSession getSession() {
			// TODO Auto-generated method stub
			return null;
		}

		public void publish(Object data) {
			ServerMessageImpl t = new ServerMessageImpl();
			t.setData(data);
			String channelid = getChannelId().toString();
			t.setChannel(channelid);
			receive(t);
		}

		public void publish(Object data, String messageId) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void sendSubscribe() {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void sendUnSubscribe() {
			// TODO Auto-generated method stub
			
		}
		
		
	}


}
