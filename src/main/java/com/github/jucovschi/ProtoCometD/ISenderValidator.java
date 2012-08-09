package com.github.jucovschi.ProtoCometD;

import org.cometd.bayeux.Message;

public interface ISenderValidator {
	boolean isValid(Message msg);
}
