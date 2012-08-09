package info.kwarc.net.cometd;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage.Mutable;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.AbstractService;
import org.cometd.server.BayeuxServerImpl;
import org.eclipse.jetty.util.thread.ThreadPool;
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
    protected final Logger _logger = LoggerFactory.getLogger(getClass());

	public ProtoService(BayeuxServer bayeux, String name) {
		super(bayeux, name);
		_bayeux = (BayeuxServerImpl) bayeux;
		_name = name;
	}

	protected void addService(String channelName, CommunicationCallback callback) {
		_logger.debug("Mapping {}#{} to {}", new Object[]{_name, callback.getInvoker().getName(), channelName});
		_bayeux.createIfAbsent(channelName);
		ServerChannel channel = _bayeux.getChannel(channelName);
		String methodName = callback.getInvoker().getName();
		TypedInvoker invoker = new TypedInvoker(channelName, callback);
		invokers.put(methodName, invoker);
		channel.addListener(invoker);	
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
    
    protected void doInvoke(Method method, ServerSession fromClient, String channel, AbstractMessage msg, String messageid, Object src)
    {
        if (method != null)
        {
            try
            {

                boolean accessible = method.isAccessible();
                Object reply = null;
                try
                {
                    method.setAccessible(true);
                    if (src == null)
                    	reply = method.invoke(this, fromClient, channel, msg);
                    else
                    	reply = method.invoke(this, fromClient, channel, msg, src);
                    if (reply != null && !(reply instanceof AbstractMessage) ) {
                        exception(method.toString(), fromClient, getLocalSession(), null, new Exception("Return value is not a protobuffer object"));
                        reply = ProtoUtils.prepareProto((AbstractMessage)reply);
                    }
                }
                finally
                {
                    method.setAccessible(accessible);
                }

                if (reply != null)
                    send(fromClient, channel, reply, messageid);
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

    private void invoke(final Method method, final ServerSession fromClient, final String channelid, final AbstractMessage msg, final String messageid, final Object src)
    {
        ThreadPool threadPool = getThreadPool();
        if (threadPool == null)
            doInvoke(method, fromClient, channelid, msg, messageid, src);
        else
        {
            threadPool.dispatch(new Runnable()
            {
                public void run()
                {
                    doInvoke(method, fromClient, channelid, msg, messageid, src);
                }
            });
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

        public boolean onMessage(ServerSession from, ServerChannel channel, Mutable message)
        {
            if (isSeeOwnPublishes() || from != getServerSession()) {
            	
            	AbstractMessage msg = ProtoUtils.createProto(message);
            	// check if it parses into a protobuffer
            	if (msg == null) {
                	_logger.debug("Unparsable message from {}", new Object[] {message.getChannel()});
            		return true;
            	}
            	if (callback.isAllowedMessage(msg) && callback.isAllowedUser(message)) {
            		callback.invoke(channel, msg, message);
    			}
            }
            return true;
        }
    }
}
