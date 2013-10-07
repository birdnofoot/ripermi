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

/**
 * Class that holds method return information.
 * 
 * @date   05/10/2006 
 * @author Ralf Biedert
 */
public class RemoteBeacon implements IRemoteMessage {

    private static final long serialVersionUID = -2364273867409L;

    /** */
    private byte[] payload = new byte[32];

    /** */
    private int id = 0;

    /**
     * Constructs a remote beacon with a given ID.
     * 
     * @param i The ID to create.
     */
    public RemoteBeacon(int i) {
        this.payload.hashCode();
        this.id = i;
    }

    /**
     * Returns the ID.
     * 
     * @return the id
     */
    public int getId() {
        return this.id;
    }

}
