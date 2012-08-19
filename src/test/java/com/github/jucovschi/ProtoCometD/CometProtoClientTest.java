package com.github.jucovschi.ProtoCometD;

import static org.junit.Assert.assertEquals;

import java.nio.InvalidMarkException;

import org.cometd.bayeux.client.ClientSessionChannel;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.DynAnyPackage.Invalid;

import com.github.jucovschi.ProtoCometD.Mockproto.InvalidMsg;
import com.github.jucovschi.ProtoCometD.Mockproto.TestMsg;
import com.google.protobuf.AbstractMessage;

public class CometProtoClientTest {

	CometProtoClient client;
	MockBayeuxClient bayeux_client;
	AbstractMessage [] messages;
	Boolean [] received;
	Boolean [] responded;
	
	public void reset() {
		for (int i=0; i<received.length; i++) {
			received[i] = false;
			responded[i] = false;
		}
	}
	
	public void checkReceived(AbstractMessage msg) {
		for (int i=0; i<messages.length; i++)
			if (messages[i].equals(msg))
				received[i] = true;
	}

	String getMsg(AbstractMessage msg) {
		if (msg instanceof TestMsg) 
			return ((TestMsg) msg).getMsg();
		if (msg instanceof InvalidMsg) 
			return ((InvalidMsg) msg).getMsg();
		return null;
	}
	
	public void checkResponded(AbstractMessage msg) {
		String s = getMsg(msg);
		for (int i=0; i<messages.length; i++)
			if (s.equals("Ans: "+getMsg(messages[i])))
				responded[i] = true;
	}

	public void send(String channel) {
		for (int i=0; i<messages.length; i++)
			client.publish(channel, messages[i]);
	}

	public void send(String channel, CommunicationCallback callback) {
		for (int i=0; i<messages.length; i++)
			client.publish(channel, messages[i], callback);
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
		responded = new Boolean[messages.length];
	}
	
	public void TestService(ClientSessionChannel channel, Mockproto.TestMsg msg) {
		checkReceived(msg);
	}

	public void Test2Service(ClientSessionChannel channel, AbstractMessage msg) {
		checkReceived(msg);
	}
	
	public void TestRespondService(ClientSessionChannel channel, AbstractMessage msg, CommunicationContext context) {
		checkReceived(msg);
		client.respond(TestMsg.newBuilder().setMsg("Ans: "+getMsg(msg)).build(), context);
	}

	public void TestCallback(ClientSessionChannel channel, AbstractMessage msg, CommunicationContext context) {
		checkResponded(msg);
	}

	public void TestCallback2(ClientSessionChannel channel, AbstractMessage msg, CommunicationContext context) {
		checkResponded(msg);
	}

	
	@Test
	public void checkInvalidMessages() throws InterruptedException {
		client.addService("/service/test", CommunicationCallback.new_builder().allowMessages(Mockproto.TestMsg.class).build("TestService", this));
		reset();
		send("/service/test");
		Thread.sleep(100);
		assertEquals(true, received[0]);
		assertEquals(false, received[1]);
	}

	@Test
	public void checkMessageSending() throws InterruptedException {
		reset();
		client.addService("/service/test2", CommunicationCallback.new_builder().build("Test2Service", this));
		send("/service/test2");
		Thread.sleep(100);
		assertEquals(true, received[0]);
		assertEquals(true, received[1]);
	}
	
	@Test
	public void sendMessageWithRespond() throws InterruptedException {
		reset();
		client.addService("/service/test3", CommunicationCallback.new_builder().build("TestRespondService", this));
		send("/service/test3", CommunicationCallback.new_builder().build("TestCallback", this));
		Thread.sleep(100);
		assertEquals(true, received[0]);
		assertEquals(true, received[1]);	
		assertEquals(true, responded[0]);
		assertEquals(true, responded[1]);	
	}

	@Test
	public void sendMessageWithRestrictedRespond() throws InterruptedException {
		reset();
		client.addService("/service/test4", CommunicationCallback.new_builder().build("TestRespondService", this));
		send("/service/test4", CommunicationCallback.new_builder().allowMessages(InvalidMsg.class).build("TestCallback2", this));
		Thread.sleep(100);
		assertEquals(true, received[0]);
		assertEquals(true, received[1]);	
		assertEquals(false, responded[0]);
		assertEquals(false, responded[1]);	
	}
}
