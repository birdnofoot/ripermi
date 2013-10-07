/*
 * ServerImpl.java
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
package test.wrapping.plugins.server.impl;

import java.awt.Point;
import java.awt.Rectangle;

import test.wrapping.plugins.server.Callback;
import test.wrapping.plugins.server.Input;
import test.wrapping.plugins.server.Service;
import test.wrapping.plugins.server.SubService;

/**
 * @author rb
 *
 */
public class ServerImpl implements Service {

    public Rectangle addCallback(final Callback c) {

        if (c == null) {

        return new Rectangle(); }

        final Input i = new Input() {

            public String getString() {
                StringBuilder sb = new StringBuilder();
                for (int ii = 0; ii < 1000; ii++) {
                    sb.append(ii);
                }
                return sb.toString();
            }

            public Point getArea() {
                return new Point(22, 33);
            }
        };

        for (int j = 0; j < 10; j++) {

            c.doSomething(i);

        }
        return new Rectangle(1, 2, 3, 4);
    }

    public int test(final int y) {

        return y * 2 + 1;
    }

    public SubService getSubService() {
        return new SubService() {

            public int yo() {

                return 1234;
            }
        };
    }

    public Rectangle r() {
        return new Rectangle(3, 4, 5, 6);
    }

}
