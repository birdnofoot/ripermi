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

import java.io.Serializable;

/**
 * Class that holds informations about a remote instance,
 * making the instance unique in all remote JVM.
 * All remote instances have a generated random UUID,
 * except the global ones (registered with {@link net.sf.lipermi.handler.CallHandler#registerGlobal CallHandler}).
 * 
 * @date   05/10/2006 
 * @author lipe
 */

public class RemoteInstance implements Serializable {

    private static final long serialVersionUID = -4597780264243542810L;

    /** */
    String className;

    /** */
    long instanceId;

    /**
     * @param instanceId
     * @param className
     */
    public RemoteInstance(final long instanceId, final String className) {
        this.instanceId = instanceId;
        this.className = className;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof RemoteInstance) {
            final RemoteInstance ri = (RemoteInstance) obj;
            final boolean _instanceId = getInstanceID() == ri.getInstanceID();
            final boolean _className = getClassName().equals(ri.getClassName());
            return _className && _instanceId;
        }
        return false;
    }

    /**
     * @return .
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * @return .
     */
    public long getInstanceID() {
        return this.instanceId;
    }

    @Override
    public int hashCode() {
        return this.className.hashCode();
    }
}
