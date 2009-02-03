/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.io;

import org.apache.commons.io.input.ProxyInputStream;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class NonClosingInputStream extends ProxyInputStream {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(NonClosingInputStream.class);


    public NonClosingInputStream(InputStream proxy) {
        super(proxy);
    }

    /**
     * Does nothing!
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        //Do not close the stream, since we need to continue processing it -
        //it will be close by the caller
    }

    /**
     * Forces the stream to be closed
     * @throws IOException
     */
    public void forceClose() throws IOException {
        super.close();
    }
}
