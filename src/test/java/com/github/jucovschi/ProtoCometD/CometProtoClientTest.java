package com.github.jucovschi.ProtoCometD;

import static org.junit.Assert.assertEquals;

import org.cometd.bayeux.client.ClientSessionChannel;
import org.junit.Before;
import org.junit.Test;

import com.github.jucovschi.ProtoCometD.CometProtoClient;
import com.github.jucovschi.ProtoCometD.CommunicationCallback;
import com.google.protobuf.AbstractMessage;

public class CometProtoClientTest {

	CometProtoClient client;
	MockBayeuxClient bayeux_client;
	AbstractMessage [] messages;
	Boolean [] received;
	
	public void reset() {
		for (int i=0; i<received.length; i++)
			received[i] = false;
	}
	
	public void checkReceived(AbstractMessage msg) {
		for (int i=0; i<messages.length; i++)
			if (messages[i].equals(msg))
				received[i] = true;
	}
	
	public void send(String channel) {
		for (int i=0; i<messages.length; i++)
			client.publish(channel, messages[i]);
	}
	
	@Before
	public void setup() {
		bayeux_client = new MockBayeuxClient();
		client = new CometProtoClient(bayeux_client);
		messages = new AbstractMessage[] {
				Mockproto.TestMsg.newBuilder().setMsg("ok message").build(),
				Mockproto.InvalidMsg.newBuilder().setMsg("bad message").build()
		};
		received = new Boolean[messages.length];
	}
	
	public void TestService(ClientSessionChannel channel, Mockproto.TestMsg msg) {
		checkReceived(msg);
	}

	public void Test2Service(ClientSessionChannel channel, AbstractMessage msg) {
		checkReceived(msg);
	}
	
	@Test
	public void checkInvalidMessages() throws InterruptedException {
		client.addService("/service/test", CommunicationCallback.new_builder().allowMessages(Mockproto.TestMsg.class).build("TestService", this));
		reset();
		send("/service/test");
		Thread.sleep(100);
		assertEquals(received[0], true);
		assertEquals(received[1], false);
	}

	@Test
	public void checkMessageSending() throws InterruptedException {
		client.addService("/service/test2", CommunicationCallback.new_builder().build("Test2Service", this));
		send("/service/test2");
		Thread.sleep(100);
		assertEquals(received[0], true);
		assertEquals(received[1], true);
	}
}
