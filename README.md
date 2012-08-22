ProtoCometD
===========

This Project provides a set of wrappers over [CometD](http://cometd.org/) communication classes allowing sending/receiving [Protobuffer](https://developers.google.com/protocol-buffers/)  messages over the network. 

Here is a list of features:
 * Extension of the AbstractService class (called ProtoService) allowing restricting messages sent over channel(s) to be ProtoBuffer objects. Allows restricting ProtoBuffer messages to be of certain type.
 * Wrapper arround BayeuxClient object (called ProtoCometClient) allowing 
   - sending ProtoBuffer messages over Bayeux channels
   - define callbacks when message gets a response
 * Caching session data in ProtoBuffer objects

Examples
========

Creating CometD channels exchanging ProtoBuffer objects

```java
// Extend from ProtoService (not from AbstractService)
public class MyService extends ProtoService {

  public MyService(CometD cometd) {
	super(cometd.getBayeux(), "sissi.alex");
	addService("/service/register", // channel
		CommunicationCallback.new_builder().
			allowMessages(MyProtoMsg.class). // allow only messages of type MyProtoMsg
		build("register", this));  // function to be called 
  }

  public void register(ServerSession remote, MyProtoMsg msg) {
  }
}
```

Sending ProtoBuffer messages from BayeuxClients
```java
  bayeux_client = new MockBayeuxClient();
  client = new ProtoCometClient(bayeux_client); // wrapper arround the BayeuxClient 

  // publish a ProtoBuffer object - don't care about responses
  client.publish(channel, myProtoMsg);

  // publish a ProtoBuffer object - call function register when response comes back
  client.publish(channel, myProtoMsg, CommunicationCallback.new_builder().
			allowMessages(MyProtoMsg.class). // allow responses of type MyProtoMsg
			build("register", this));  // function to be called );

  // subscribing to Protobuffer enabled CometD channel is done as follows
  client.addService("/some/channel", CommunicationCallback.new_builder().build("TestRespondService", this));
)

```


