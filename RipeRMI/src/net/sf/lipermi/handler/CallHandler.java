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

import static net.jcores.jre.CoreKeeper.$;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.lipermi.call.RemoteCall;
import net.sf.lipermi.call.RemoteInstance;
import net.sf.lipermi.call.RemoteReturn;
import net.sf.lipermi.exception.LipeRMIException;

/**
 * A handler who know a RemoteInstance and its
 * local implementations. Used to delegate calls to
 * correct implementation objects.
 * 
 * Local implementation objects must register with
 * methods {@link net.sf.lipermi.handler.CallHandler#registerGlobal registerGlobal} and
 * {@link net.sf.lipermi.handler.CallHandler#exportObject exportObject} to work remotelly.
 * 
 * @author lipe
 * @date 05/10/2006
 * 
 * @see net.sf.lipermi.call.RemoteInstance
 */
public class CallHandler {
    /** All our exported objects */
    private final Map<RemoteInstance, Object> exportedObjects = new ConcurrentHashMap<RemoteInstance, Object>();

    /**
     * Generates a call name for the given class and method id.
     * 
     * @param methodID
     * 
     * @return A short call name
     */
    public static String generateCallName(String methodID) {
        final String b = methodID;

        // Assemble call request.
        final StringBuilder call = new StringBuilder();
        final String tokens[] = b.split("\\.");
        for (int i = 0; i < tokens.length; i++) {
            final String t = tokens[i];
            if (!t.contains("(")) continue;

            for (int j = i; j < tokens.length - 1; j++) {
                call.append(tokens[j]);
                call.append(".");
            }

            call.append(tokens[tokens.length - 1]);
        }

        return call.toString();
    }

    /**
     * Generates a call ID for the given call name
     * 
     * @param callName
     * @return .
     */
    public static long generateCAllID(String callName) {
        return callName.hashCode() + 2 ^ 32 * callName.substring(1).hashCode();
    }

    /**
     * Executes a call on a local object.
     * 
     * @param remoteCall
     * @return .
     * @throws LipeRMIException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public RemoteReturn delegateCall(final RemoteCall remoteCall)
                                                                 throws LipeRMIException,
                                                                 SecurityException,
                                                                 NoSuchMethodException,
                                                                 IllegalArgumentException,
                                                                 IllegalAccessException {

        // Try to get the object for which we have been called
        final Object implementator = this.exportedObjects.get(remoteCall.getRemoteInstance());
        if (implementator == null)
            throw new LipeRMIException(String.format("Class %s doesn't have implementation", remoteCall.getRemoteInstance().getClassName())); //$NON-NLS-1$

        final Cache cache = remoteCall.handler.getCache();
        final RemoteInstance instance = remoteCall.getRemoteInstance();

        // Try to get the method the peer was looking for
        final long methodID = remoteCall.getMethodIDHash();
        final String className = cache.getClassID(instance.getClassName(), instance.getInstanceID());
        final String request = cache.getMethodID(className, remoteCall.getMethodID(), methodID);

        // Try to get the cached method without doing reflection
        Method implementationMethod = cache.getMethod(className, null, methodID);

        // All the methods to consider
        final Method[] methods = implementator.getClass().getDeclaredMethods();
        final Method[] moreMethods = implementator.getClass().getMethods();
        
        if (implementationMethod == null) {
            // Locate the method for which we have been called (first check declared methods of this class)
            for (final Method method : $(methods).add(moreMethods)) {
                String mname = method.toString();

                Class<?> some = implementator.getClass();
                while (some != null) {
                    String name = some.getName();
                    mname = mname.replaceAll(name, "");
                    some = some.getSuperclass();
                }

                if (mname.endsWith(request)) {
                    implementationMethod = method;
                    break;
                }
            }

            // Make method accessible (only required once).
            if(implementationMethod != null)
                implementationMethod.setAccessible(true);

            
            // And store the method in the cache
            cache.getMethod(className, implementationMethod, methodID);
        }

        // Ups ... method does not exist ...
        if (implementationMethod == null) { 
            return new RemoteReturn(true, new NoSuchMethodException(request), remoteCall.getCallID()); 
        }

        // Prepare our return value
        RemoteReturn remoteReturn;

        try {
            // Unwrapped return value
            Object methodReturn = null;

            // Invoke the method>
            methodReturn = implementationMethod.invoke(implementator, remoteCall.getArgs());

            // Auto export return value, in case it is an interface
            if (implementationMethod.getReturnType().isInterface() && methodReturn != null) {
                exportObject(implementationMethod.getReturnType(), methodReturn);
            }

            // In case it was already exported, return the wrapped
            if (methodReturn != null && this.exportedObjects.containsValue(methodReturn)) {
                methodReturn = getRemoteReference(methodReturn);
            }

            // Setup method return value
            remoteReturn = new RemoteReturn(false, methodReturn, remoteCall.getCallID());
        } catch (final Exception e) {
            e.printStackTrace();
            remoteReturn = new RemoteReturn(true, e, remoteCall.getCallID());
        } 

        return remoteReturn;
    }

    /**
     * @param cInterface
     * @param exportedObject
     * @throws LipeRMIException
     */
    public void exportObject(final Class<?> cInterface, final Object exportedObject)
                                                                                    throws LipeRMIException {
        final UUID objUUID = java.util.UUID.randomUUID();
        exportObject(cInterface, exportedObject, objUUID.getMostSignificantBits());
    }

    /**
     * 
     * 
     * @param cInterface
     * @param objImplementation
     * @throws LipeRMIException
     */
    @SuppressWarnings("rawtypes")
    public void registerGlobal(final Class cInterface, final Object objImplementation)
                                                                                      throws LipeRMIException {
        exportObject(cInterface, objImplementation, 0);
    }

    /**
     * @param cInterface
     * @param objImplementation
     * @param instanceId
     * @throws LipeRMIException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void exportObject(final Class cInterface, final Object objImplementation,
                              final long instanceId) throws LipeRMIException {

        if (!cInterface.isAssignableFrom(objImplementation.getClass()))
            throw new LipeRMIException(String.format("Class %s is not assignable from %s", objImplementation.getClass().getName(), cInterface.getName())); //$NON-NLS-1$

        final RemoteInstance remoteInstance = new RemoteInstance(instanceId, cInterface.getName());
        this.exportedObjects.put(remoteInstance, objImplementation);
    }

    /**
     * Returns the remote reference for an object.
     * 
     * @param obj
     * @return
     */
    RemoteInstance getRemoteReference(final Object obj) {
        for (final RemoteInstance remoteInstance : this.exportedObjects.keySet()) {
            final Object exportedObj = this.exportedObjects.get(remoteInstance);
            if (exportedObj == obj) return remoteInstance;
        }
        return null;
    }
}
