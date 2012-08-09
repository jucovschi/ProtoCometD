package info.kwarc.net.cometd;

import org.cometd.bayeux.Message;

public interface ISenderValidator {
	boolean isValid(Message msg);
}
