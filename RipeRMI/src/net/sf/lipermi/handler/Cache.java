/*
 * Cache.java
 * 
 * Copyright (c) 2011, Ralf Biedert All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of the author nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.sf.lipermi.handler;

import static net.jcores.jre.CoreKeeper.$;

import java.lang.reflect.Method;

import net.jcores.jre.interfaces.functions.F1;
import net.jcores.jre.utils.map.MapUtil;

public class Cache {
    /** Keeps information for one class */
    class ClassCache {
        final MapUtil<Long, String> methodNameCache;
        
        final MapUtil<Long, Method> methodCache;

        public ClassCache() {
            this.methodNameCache = $.concurrentMap();
            this.methodCache = $.concurrentMap();
        }
    }
    
    /** Keeps cache information for classes */
    private final MapUtil<String, ClassCache> classCache;

    // TODO: Does not work out easily, as many objects don't see the cache
    /** Caches names */
    private final MapUtil<Long, String> classNameCache;

    
    public Cache() {
        this.classCache = $.concurrentMap();
        this.classCache.generator(new F1<String, Cache.ClassCache>() {
            public ClassCache f(String arg0) {
                return new ClassCache();
            }
        });

        this.classNameCache = $.concurrentMap();
    }
    
    
    /**
     * @param id
     * @return .
     */
    @SuppressWarnings("boxing")
    public boolean hasClassID(long id) {
        return this.classNameCache.get(id) != null;
    }
    
    /**
     * @param className
     * @param classID
     * @return .
     */
    @SuppressWarnings("boxing")
    public String getClassID(String className, long classID) {
        if(className != null) {
            this.classNameCache.put(classID, className);
            return className;
        }
        
        return this.classNameCache.get(classID);
    }
 

    /**
     * @param clazz
     * @param methodName
     * @param methodID
     * @return .
     */
    @SuppressWarnings("boxing")
    public boolean hasMethodID(String clazz, String methodName, long methodID) {
        return this.classCache.get(clazz).methodNameCache.get(methodID) != null;
    }

    
    /**
     * @param clazz
     * @param methodName
     * @param methodID
     * @return .
     */
    @SuppressWarnings("boxing")
    public String getMethodID(String clazz, String methodName, long methodID) {
        if(methodName != null) {
            this.classCache.get(clazz).methodNameCache.put(methodID, methodName);
            return methodName;
        }
        
        return this.classCache.get(clazz).methodNameCache.get(methodID);
    }
    
    
    /**
     * @param clazz
     * @param method 
     * @param methodID
     * @return .
     */
    @SuppressWarnings("boxing")
    public Method getMethod(String clazz, Method method, long methodID) {
        if(method != null) {
            this.classCache.get(clazz).methodCache.put(methodID, method);
            return method;
        }
        
        return this.classCache.get(clazz).methodCache.get(methodID);
    }

}
