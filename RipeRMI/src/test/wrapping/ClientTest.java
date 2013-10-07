/*
 * ClientTest.java
 * 
 * Copyright (c) 2009, Ralf Biedert All rights reserved.
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
package test.wrapping;

import java.awt.Dimension;
import java.io.IOException;

import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.net.Client;
import test.wrapping.plugins.server.Callback;
import test.wrapping.plugins.server.Input;
import test.wrapping.plugins.server.Service;
import test.wrapping.plugins.server.SubService;

public class ClientTest {
    public static void main(final String[] args) throws IOException, LipeRMIException {
        
        final CallHandler callHandler = new CallHandler();
        final Client client = new Client("127.0.0.1", 55661, callHandler);
        final Callback c = new Callback() {
            public Dimension doSomething(final Input inpit) {
                String string = inpit.getString();
                return new Dimension(22, 22);
            }
        };

        final Service service = (Service) client.getGlobal(Service.class);

        System.out.println("A");
        service.addCallback(c);
        System.out.println(service.r());
        System.out.println("B");
        SubService subService = service.getSubService();
        System.out.println("C");

        System.out.println(subService.yo());
        service.test(34);
        System.out.println("D");
    }
}
