package com.github.jucovschi.ProtoCometD;

import junit.framework.Assert;

import org.junit.Test;

import com.google.protobuf.AbstractMessage;

public class ProtoUtilsTest {

	AbstractMessage msg;
	
	@Test
	public void test() {
		msg = Mockproto.TestMsg.newBuilder().setMsg("test").build();
		AbstractMessage msg2 = ProtoUtils.deserialize(ProtoUtils.serialize(msg));
		Assert.assertEquals(msg, msg2);
	}
}
