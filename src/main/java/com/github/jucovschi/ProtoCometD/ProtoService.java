package com.github.jucovschi.ProtoCometD;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage.Mutable;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.AbstractService;
import org.cometd.server.BayeuxServerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.AbstractMessage;

/**
 * Extension of CometD services allowing services to filter the type of
 * messages they receive and the sender of the message. 
 * @author cjucovschi
 *
 */
public class ProtoService extends AbstractService {
	private final BayeuxServerImpl _bayeux;
	private final String _name;
	private final Map<String, TypedInvoker> invokers = new ConcurrentHashMap<String, TypedInvoker>();
	protected final Logger _logger = LoggerFactory.getLogger(ProtoService.class);

	public ProtoService(BayeuxServer bayeux, String name) {
		super(bayeux, name);
		_bayeux = (BayeuxServerImpl) bayeux;
		_name = name;
	}

	protected void addService(String channelName, CommunicationCallback callback) {
		if (callback == null) {
			_logger.warn("Callback for channel '{}' is invalid", new Object[]{channelName});
			return;
		}
		_logger.info("Mapping {}#{} to {}", new Object[]{_name, callback.getInvoker().getName(), channelName});
		_bayeux.createIfAbsent(channelName);
		ServerChannel channel = _bayeux.getChannel(channelName);
		String methodName = callback.getInvoker().getName();
		TypedInvoker invoker = new TypedInvoker(channelName, callback);
		invokers.put(methodName, invoker);
		channel.addListener(invoker);	
	}

	protected void send(ServerSession toClient, String onChannel, AbstractMessage data) {
		_logger.debug("---> "+toClient.getId()+":"+onChannel+":"+data);
		super.send(toClient, onChannel, ProtoUtils.prepareProto(data), null);
	}
	
	protected void send(ServerSession toClient, String onChannel, AbstractMessage data, CommunicationContext context) {
		_logger.debug("--->"+toClient.getId()+"::"+onChannel+"::"+data.getClass().getName()+"::"+data);
		super.send(toClient, onChannel, ProtoUtils.prepareProto(data, context), null);
	}

	/**
	 * <p>Unmaps the method with the given name that has been mapped to the given channel.</p>
	 *
	 * @param channelName The channel name
	 * @param methodName  The name of the method to unmap
	 * @see #addService(String, String)
	 * @see #removeService(String)
	 */
	protected void removeService(String channelName, String methodName)
	{
		ServerChannel channel = _bayeux.getChannel(channelName);
		if (channel != null)
		{
			TypedInvoker invoker = invokers.remove(methodName);
			channel.removeListener(invoker);
		}
	}

	/**
	 * <p>Unmaps all the methods that have been mapped to the given channel.</p>
	 *
	 * @param channelName The channel name
	 * @see #addService(String, String)
	 * @see #removeService(String, String)
	 */
	protected void removeService(String channelName)
	{
		ServerChannel channel = _bayeux.getChannel(channelName);
		if (channel != null)
		{
			for (TypedInvoker invoker : invokers.values())
			{
				if (invoker.channelName.equals(channelName))
					channel.removeListener(invoker);
			}
		}
	}

	private class TypedInvoker implements ServerChannel.MessageListener
	{
		private final CommunicationCallback callback;
		private final String channelName;

		public TypedInvoker(String channelName, CommunicationCallback callback)
		{
			this.callback = callback;
			this.channelName = channelName;
		}

		protected void doInvoke(ServerSession fromClient, String channel, AbstractMessage msg, CommunicationContext context)
		{
			List<Object> params = callback.getInvokeParams(fromClient, msg, context);
			Method method = callback.getInvoker();

			if (method != null)
			{
				try
				{
					boolean accessible = method.isAccessible();
					Object reply = null;
					try
					{
						method.setAccessible(true);
						reply = method.invoke(callback.getObj(), params.toArray());
					}
					finally
					{
						method.setAccessible(accessible);
					}

					if (reply == null)
						return;

					if (!(reply instanceof AbstractMessage) ) {
						exception(method.toString(), fromClient, getLocalSession(), null, new Exception("Return value is not a protobuffer object"));
						return;
					} 

					send(fromClient, channel, (AbstractMessage) reply, context);
				}
				catch (Exception e)
				{
					exception(method.toString(), fromClient, getLocalSession(), null, e);
				}
				catch (Error e)
				{
					exception(method.toString(), fromClient, getLocalSession(), null, e);
				}
			}
		}


		public boolean onMessage(ServerSession from, ServerChannel channel, Mutable message)
		{
			if (isSeeOwnPublishes() || from != getServerSession()) {

				AbstractMessage msg = ProtoUtils.createProto(message);
				// check if it parses into a protobuffer
				if (msg == null) {
					_logger.warn("Unparsable message from {}", new Object[] {message.getChannel()});
					return true;
				}
				_logger.debug("<---"+from.getId()+"::"+channel.getId()+"::"+msg.getClass().getName()+"::"+msg);
				CommunicationContext context = CommunicationContext.getInstance(message);
				if (callback.isAllowedMessage(msg) && callback.enrichContext(from.getId(), message, context)) {
					doInvoke(from, channel.getId(), msg, context);
				}
			}
			return true;
		}
	}
}
