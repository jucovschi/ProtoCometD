package com.github.jucovschi.ProtoCometD;

import org.cometd.bayeux.Message;

public interface IContextEnricher {
	Object enrich(String channelid, Message msg, CommunicationContext context);
}
