/*
 * LipeRMI - a light weight Internet approach for remote method invocation
 * Copyright (C) 2006  Felipe Santos Andrade
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * For more information, see http://lipermi.sourceforge.net/license.php
 * You can also contact author through lipeandrade@users.sourceforge.net
 */

package net.sf.lipermi.handler;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.lipermi.call.IRemoteMessage;
import net.sf.lipermi.call.RemoteBeacon;
import net.sf.lipermi.call.RemoteCall;
import net.sf.lipermi.call.RemoteInstance;
import net.sf.lipermi.call.RemoteReturn;
import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.filter.IProtocolFilter;

/**
 * A ConnectionHandler is object which can call remote
 * methods, receive remote calls and dispatch its returns.
 *
 * @author lipe
 * @date   05/10/2006
 *
 * @see	   net.sf.lipermi.handler.CallHandler
 * @see	   net.sf.lipermi.call.RemoteInstance
 * @see	   net.sf.lipermi.call.RemoteCall
 * @see	   net.sf.lipermi.call.RemoteReturn
 * @see	   net.sf.lipermi.net.Client
 * @see	   net.sf.lipermi.net.Server
 * @see	   net.sf.lipermi.handler.filter.DefaultFilter
 */
public class ConnectionHandler implements Runnable {

    private static AtomicLong callId = new AtomicLong(0L);

    /**
     * @param socket
     * @param callHandler
     * @param filter
     * @return .
     */
    @SuppressWarnings("boxing")
    public static ConnectionHandler createConnectionHandler(
                                                            final Socket socket,
                                                            final CallHandler callHandler,
                                                            final IProtocolFilter filter) {
        final ConnectionHandler connectionHandler = new ConnectionHandler(socket, callHandler, filter);
        final String threadName = String.format("ConnectionHandler (%s:%d)", socket.getInetAddress().getHostAddress(), socket.getPort()); //$NON-NLS-1$
        final Thread connectionHandlerThread = new Thread(connectionHandler, threadName);
        
        connectionHandlerThread.setDaemon(true);
        connectionHandlerThread.start();

        return connectionHandler;
    }

    /**
     * @param socket
     * @param callHandler
     * @param filter
     * @param listener
     * @return .
     */
    public static ConnectionHandler createConnectionHandler(
                                                            final Socket socket,
                                                            final CallHandler callHandler,
                                                            final IProtocolFilter filter,
                                                            final IConnectionHandlerListener listener) {
        
        final ConnectionHandler connectionHandler = createConnectionHandler(socket, callHandler, filter);
        
        connectionHandler.addConnectionHandlerListener(listener);
        
        return connectionHandler;
    }


    /** Handles incoming calls */
    private final CallHandler callHandler;

    /** Filter, not really used */
    private final IProtocolFilter filter;

    /** Listen for connection status messages */
    private final List<IConnectionHandlerListener> listeners = new LinkedList<IConnectionHandlerListener>();

    /** Used to write messages (over the network) */
    private ObjectOutputStream output;

    /** Send lock */
    private final Lock sendChannelLock = new ReentrantLock();

    /** ? */
    private final Map<RemoteInstance, Object> remoteInstanceProxys = new HashMap<RemoteInstance, Object>();

    /** Contains queues to all waiting threads */
    private final List<BlockingQueue<RemoteReturn>> remoteReturnsQueues = new LinkedList<BlockingQueue<RemoteReturn>>();

    /** Queues lock */
    private final Lock returnQueuesLock = new ReentrantLock();

    /** Sockets we talk with */
    private final Socket socket;

    /** ThreadPool the all the delegator threads */
    private final Executor threadPool;
    
    /** Our cache */
    private Cache cache = new Cache();


    private ConnectionHandler(final Socket socket, final CallHandler callHandler,
                              final IProtocolFilter filter) {
        this.callHandler = callHandler;
        this.socket = socket;
        this.filter = filter;
        
        /** Create a daemonic thread factory */
        this.threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

        // Create the output stream
        try {
            this.output = new ObjectOutputStream(this.socket.getOutputStream());
            this.output.flush();

            sendMessage(new RemoteBeacon(0));
            sendMessage(new RemoteBeacon(1));
            sendMessage(new RemoteBeacon(2));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param listener
     */
    public void addConnectionHandlerListener(final IConnectionHandlerListener listener) {
        this.listeners.add(listener);
    }

    /**
     * @return .
     */
    public Socket getSocket() {
        return this.socket;
    }

    /**
     * @param listener
     */
    public void removeConnectionHandlerListener(final IConnectionHandlerListener listener) {
        this.listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            // Check if the socket is already connected.
            if (!this.socket.isConnected()) {
                Thread.sleep(500);
            }

            // First, try to obtain the input stream for this connection
            final InputStream inputStream = this.socket.getInputStream();

            // Now try to create a OIS. NOTE: THIS WILL BLOCK UNTIL THE OIS STREAM HEADER HAS BEEN READ! OOS HAS
            // TO WRITE BEFORE THAT.
            final ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(inputStream));

            // Socket might be disconnected in the meantime, check that.

            Object objFromStream = null;

            while (this.socket.isConnected()) {
                // Try to read a object from the stream, but only wait half a second
                try {
                    objFromStream = input.readUnshared();
                } catch (StreamCorruptedException e) {
                    Thread.sleep(250);
                    continue;
                }

                // Convert object to a remote message using the filter
                final IRemoteMessage remoteMessage = this.filter.readObject(objFromStream);

                // Check if the incoming message is a remote call we are supposed to handle
                if (remoteMessage instanceof RemoteCall) {
                    final RemoteCall remoteCall = (RemoteCall) remoteMessage;
                    remoteCall.handler = this;

                    // In case there are arguments attached to the call ...
                    if (remoteCall.getArgs() != null) {

                        // We check if any of these arguments is a remote instance, in that case we're using a proxy,
                        // otherwise the argument should have been serialized and is used directly.
                        for (int n = 0; n < remoteCall.getArgs().length; n++) {
                            final Object arg = remoteCall.getArgs()[n];
                            if (arg instanceof RemoteInstance) {
                                final RemoteInstance remoteInstance = (RemoteInstance) arg;
                                remoteCall.getArgs()[n] = getProxyFromRemoteInstance(remoteInstance);
                            }
                        }
                    }

                    // Okay, in order not to block we create a delegator thread.
                    // This thread executes the requested function
                    this.threadPool.execute(new DelegatorThread(this, remoteCall));
                    continue;
                }

                // Handle returns
                if (remoteMessage instanceof RemoteReturn) {

                    // In case we have a new return value add this to our remote returns, the appropriate
                    // call will remove and use it...
                    final RemoteReturn remoteReturn = (RemoteReturn) remoteMessage;


                    this.returnQueuesLock.lock();
                    try {
                        for (BlockingQueue<RemoteReturn> queue : this.remoteReturnsQueues) {
                            final boolean offer = queue.offer(remoteReturn);
                            if (!offer)
                                System.err.println("Error storing remote return. One of your remote answers might never arrive.");
                        }
                    } finally {
                        this.returnQueuesLock.unlock();
                    }

                    continue;
                }

                // Handle beacons
                if (remoteMessage instanceof RemoteBeacon) {
                    continue;
                }

                // We should never, ever see this !
                throw new LipeRMIException("Unknown IRemoteMessage type"); //$NON-NLS-1$
            }

        } catch (final Exception e) {
            // Decision depends on type of the exceptions
            if (!(e instanceof EOFException)) e.printStackTrace();

            // Try to close the socket
            try {
                this.socket.close();
            } catch (final IOException e1) {
                //
            }

            // Make all return-awaiters wake up
            this.returnQueuesLock.lock();
            try {
                for (BlockingQueue<RemoteReturn> queue : this.remoteReturnsQueues) {
                    // FIXME: A really nasty hack to provoke an exception in the listening threads ...
                    queue.offer((RemoteReturn) new Object());
                }
            } catch (ClassCastException ee) {
                // silently fail
            } finally {
                this.returnQueuesLock.unlock();
            }

            // Tell all listeners
            for (final IConnectionHandlerListener listener : this.listeners) {
                listener.connectionClosed();
            }
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }


    private class DelegatorThread extends Thread {
        private final ConnectionHandler connectionHandler;
        private final RemoteCall remoteCall;

        public DelegatorThread(ConnectionHandler connectionHandler, RemoteCall remoteCall) {
            this.connectionHandler = connectionHandler;
            this.remoteCall = remoteCall;
            this.setDaemon(true);

            this.setName("Delegator");
        }

        @Override
        public void run() {
            RemoteReturn remoteReturn;
            try {
                // Try to delegate the local call to the call handler
                remoteReturn = this.connectionHandler.getCallHandler().delegateCall(this.remoteCall);

                // And return the result
                this.connectionHandler.sendMessage(remoteReturn);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }





    /**
     * For a given remote instnace, try to get or construct the proper proxy.
     *
     * @param remoteInstance
     * @return
     */
    private Object getProxyFromRemoteInstance(final RemoteInstance remoteInstance) {
        Object proxy = this.remoteInstanceProxys.get(remoteInstance);
        if (proxy == null) {
            try {
                proxy = CallProxy.buildProxy(remoteInstance, this);
            } catch (final ClassNotFoundException e) {
                e.printStackTrace();
            }
            this.remoteInstanceProxys.put(remoteInstance, proxy);
        }
        return proxy;
    }

    /**
     * Try to locate a remote proxy instance for the given proxy object.
     *
     * @param proxy
     * @return
     */
    private RemoteInstance getRemoteInstanceFromProxy(final Object proxy) {
        for (final RemoteInstance remoteInstance : this.remoteInstanceProxys.keySet()) {
            if (this.remoteInstanceProxys.get(remoteInstance) == proxy)
                return remoteInstance;
        }

        return null;
    }

    /**
     * Sends a message on the stream
     *
     * @param remoteMessage
     * @throws IOException
     */
    public void sendMessage(final IRemoteMessage remoteMessage) throws IOException {

        // Don't synchronize on the whole object (deadlock in complex scenarios), rather sync.
        // for a single call
        this.sendChannelLock.lock();
        if (this.output == null) {
            this.output = new ObjectOutputStream(this.socket.getOutputStream());
        }

        final Object objToWrite = this.filter.prepareWrite(remoteMessage);

        // Send and flush our message.
        this.output.reset();
        this.output.writeUnshared(objToWrite);
        this.output.flush();

        this.sendChannelLock.unlock();
    }

    /**
     * Invoke a method at the remote party
     *
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @SuppressWarnings("boxing")
    final Object remoteInvocation(final Object proxy, final Method method,
                                  final Object[] args) throws Throwable {

        // Obtain current call ID
        final Long id = callId.getAndIncrement();

        // Obtain remote instance if we have any ...
        RemoteInstance remoteInstance = getRemoteInstanceFromProxy(proxy);

        // ... or create a new one.
        if (remoteInstance == null) {
            remoteInstance = new RemoteInstance(0, proxy.getClass().getInterfaces()[0].getName());
        }

        // Will eventually contain the remote answer ...
        final BlockingQueue<RemoteReturn> ourReturnQueue = new LinkedBlockingQueue<RemoteReturn>();

        //  add our queue to the list of queues.
        this.returnQueuesLock.lock();
        this.remoteReturnsQueues.add(ourReturnQueue);
        this.returnQueuesLock.unlock();


        // If we have args, check if we should wrap them also
        if (args != null) {
            
            // 1. Auto export if parameter type permits
            final Class<?>[] parameterTypes = method.getParameterTypes();
            for (int n = 0; n < parameterTypes.length; n++) {
                Class<?> class1 = parameterTypes[n];

                // TODO: Why is it done like this, wouldn't an exception be thrown when 
                // an paramter is an interface and it was alrady 
                if (class1.isInterface() && args[n] != null) {
                    this.getCallHandler().exportObject(class1, args[n]);
                }
            }

            // 2. Get remote references
            for (int n = 0; n < args.length; n++) {
                final RemoteInstance remoteRef = this.getCallHandler().getRemoteReference(args[n]);
                if (remoteRef != null) {
                    args[n] = remoteRef;
                }
            }
        }

        // Extract a method id and assemble a call (why the fuck 15?)
        final String methodId = method.toString().substring(15);
        final String methodIDShort = CallHandler.generateCallName(methodId);
        final long callID = CallHandler.generateCAllID(methodIDShort);
        
        IRemoteMessage remoteCall = null;
        
        // Cache the method ID
        if(getCache().hasMethodID(remoteInstance.getClassName(), methodIDShort, callID)) {
            remoteCall = new RemoteCall(remoteInstance, null, callID, args, id);            
        } else {
            getCache().getMethodID(remoteInstance.getClassName(), methodIDShort, callID);
            remoteCall = new RemoteCall(remoteInstance, methodIDShort, callID, args, id);
        }
        

        // now really send the call over the network ...
        sendMessage(remoteCall);


        // And wait for a return until either we get one or until we are disconnected.
        RemoteReturn remoteReturn = null;
        boolean bReturned = false;


        do {
            RemoteReturn ret = null;

            // Take something
            try {
                ret = ourReturnQueue.take();
            } catch (ClassCastException e) {
                // The ClassCastHack exception is generated as we stored an object in there in case of an error.
                throw new SocketException("Conncetion to remote host closed.");
            }


            // Check if this answer is really the one we are looking for
            if (ret.getCallID() == id) {
                bReturned = true;
                remoteReturn = ret;
            }
        } while (this.socket.isConnected() && !bReturned);


        // Remove our queue from the list of all queues
        this.returnQueuesLock.lock();
        this.remoteReturnsQueues.remove(ourReturnQueue);
        this.returnQueuesLock.unlock();


        // In the connection was closed
        if ((!this.socket.isConnected() && !bReturned) || remoteReturn == null)
            throw new LipeRMIException("Connection aborted"); //$NON-NLS-1$

        // In case the other party threw an exception
        if (remoteReturn.isThrowing() && remoteReturn.getReturn() instanceof Throwable)
            throw ((Throwable) remoteReturn.getReturn());


        // In case the answer was a remote instance
        if (remoteReturn.getReturn() instanceof RemoteInstance) {

            final RemoteInstance remoteInstanceReturn = (RemoteInstance) remoteReturn.getReturn();

            // Check if we already have a proper proxy?
            Object proxyReturn = this.remoteInstanceProxys.get(remoteInstanceReturn);

            // In case there is none, build a proper proxy
            if (proxyReturn == null) {
                proxyReturn = CallProxy.buildProxy(remoteInstanceReturn, this);
                this.remoteInstanceProxys.put(remoteInstanceReturn, proxyReturn);
            }

            // and return it
            return proxyReturn;
        }

        // Just return the result (should be serializable at this point)
        return remoteReturn.getReturn();
    }

    /**
     * Returns the call handler associated with this connection
     * 
     * @return The call handler
     */
    public CallHandler getCallHandler() {
        return this.callHandler;
    }
    
    /**
     * Returns our cache. 
     * 
     * @return The cache.
     */
    public Cache getCache() {
        return this.cache;
    }
    
}
