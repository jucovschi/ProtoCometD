package com.github.jucovschi.ProtoCometD;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.jucovschi.ProtoCometD.Mockproto.InvalidMsg;
import com.github.jucovschi.ProtoCometD.Mockproto.TestMsg;

public class SessionDataManagerTest {

	
	@Test
	public void testFetchingExisting() {
		SessionDataManager man = SessionDataManager.getInstance();
		String token = man.newActionToken(TestMsg.newBuilder().setMsg("original message").build());
		TestMsg q = man.getDataOrInit(token, TestMsg.newBuilder().setMsg("Some default").build());
		assertEquals(q.getMsg(), "original message");
	}

	@Test
	public void testFetchingInvalidExisting() {
		SessionDataManager man = SessionDataManager.getInstance();
		String token = man.newActionToken(InvalidMsg.newBuilder().setMsg("invalid original message").build());
		TestMsg q = man.getDataOrInit(token, TestMsg.newBuilder().setMsg("Some default").build());
		assertEquals(q.getMsg(), "original message");
	}

	@Test
	public void testFetchingNonExisting() {
		SessionDataManager man = SessionDataManager.getInstance();
		TestMsg q = man.getDataOrInit("nonexisting", TestMsg.newBuilder().setMsg("Some default").build());
		assertEquals(q.getMsg(), "Some default");
	}

	@Test
	public void testFetchingWithoutINnit() {
		SessionDataManager man = SessionDataManager.getInstance();
		String token = man.newActionToken(TestMsg.newBuilder().setMsg("original message").build());
		Object q = man.getData(token);
		assertEquals(((TestMsg)q).getMsg(), "original message");
	}

}
