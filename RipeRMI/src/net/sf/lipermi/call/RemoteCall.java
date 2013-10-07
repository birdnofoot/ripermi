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

package net.sf.lipermi.call;

import net.sf.lipermi.handler.ConnectionHandler;

/**
 * Class that holds method call informations.
 * 
 * @date 05/10/2006
 * @author lipe
 */
public class RemoteCall implements IRemoteMessage {
    private static final long serialVersionUID = -4057457700512552099L;

    /** Method's arguments */
    Object[] args;

    /** The id is a number unique in client and server to identify the call */
    long callId;

    /** method's name */
    String methodId;

    /** A hash for the method ID in case it was already transmitted */
    long methodIdHash;

    /** Instance will receive the call */
    RemoteInstance remoteInstance;
    
    /** The connection handler that received the call */
    transient public ConnectionHandler handler;

    /**
     * @param remoteInstance
     * @param methodId
     * @param methodIdHash
     * @param args
     * @param callId
     */
    public RemoteCall(final RemoteInstance remoteInstance, final String methodId,
                      final long methodIdHash, final Object[] args, final long callId) {
        this.remoteInstance = remoteInstance;
        this.methodId = methodId;
        this.methodIdHash = methodIdHash;
        this.args = args;
        this.callId = callId;
    }

    /**
     * @return .
     */
    public Object[] getArgs() {
        return this.args;
    }

    /**
     * @return .
     */
    public long getCallID() {
        return this.callId;
    }

    /**
     * @return .
     */
    public String getMethodID() {
        return this.methodId;
    }

    /**
     * @return .
     */
    public long getMethodIDHash() {
        return this.methodIdHash;
    }

    /**
     * @return .
     */
    public RemoteInstance getRemoteInstance() {
        return this.remoteInstance;
    }

}
