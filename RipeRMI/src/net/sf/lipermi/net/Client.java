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

package net.sf.lipermi.net;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.CallProxy;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.IConnectionHandlerListener;
import net.sf.lipermi.handler.filter.DefaultFilter;
import net.sf.lipermi.handler.filter.IProtocolFilter;

/**
 * The LipeRMI client.
 * Connects to a LipeRMI Server in a address:port
 * and create local dynamic proxys to call remote
 * methods through a simple interface.
 * 
 * @author lipe
 * @date   05/10/2006
 * 
 * @see    net.sf.lipermi.handler.CallHandler
 * @see    net.sf.lipermi.net.Server
 */
public class Client {

    private final ConnectionHandler connectionHandler;

    private final IConnectionHandlerListener connectionHandlerListener = new IConnectionHandlerListener() {
        public void connectionClosed() {
            for (final IClientListener listener : Client.this.listeners) {
                listener.disconnected();
            }
        }
    };

    final List<IClientListener> listeners = new LinkedList<IClientListener>();

    private final Socket socket;

    /**
     * @param address
     * @param port
     * @param callHandler
     * @throws IOException
     */
    public Client(final String address, final int port, final CallHandler callHandler)
                                                                                      throws IOException {
        this(address, port, callHandler, new DefaultFilter());
    }

    /**
     * @param address
     * @param port
     * @param callHandler
     * @param filter
     * @throws IOException
     */
    public Client(final String address, final int port, final CallHandler callHandler,
                  final IProtocolFilter filter) throws IOException {

        this.socket = new Socket(address, port);
        this.socket.setTcpNoDelay(true);
        this.connectionHandler = ConnectionHandler.createConnectionHandler(this.socket, callHandler, filter, this.connectionHandlerListener);
    }

    /**
     * @param listener
     */
    public void addClientListener(final IClientListener listener) {
        this.listeners.add(listener);
    }

    /**
     * @throws IOException
     */
    public void close() throws IOException {
        this.socket.close();
    }

    /**
     * @param clazz
     * @return .
     */
    @SuppressWarnings("unchecked")
    public <T> T getGlobal(final Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new CallProxy(this.connectionHandler));
    }

    /**
     * @param listener
     */
    public void removeClientListener(final IClientListener listener) {
        this.listeners.remove(listener);
    }
}
