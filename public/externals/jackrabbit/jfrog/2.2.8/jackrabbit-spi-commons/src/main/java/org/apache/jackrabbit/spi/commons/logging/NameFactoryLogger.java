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
package org.apache.jackrabbit.spi.commons.logging;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;

/**
 * Log wrapper for a {@link NameFactory}.
 */
public class NameFactoryLogger extends AbstractLogger implements NameFactory {
    private final NameFactory nameFactory;

    /**
     * Create a new instance for the given <code>nameFactory</code> which uses
     * <code>writer</code> for persisting log messages.
     * @param nameFactory
     * @param writer
     */
    public NameFactoryLogger(NameFactory nameFactory, LogWriter writer) {
        super(writer);
        this.nameFactory = nameFactory;
    }

    /**
     * @return  the wrapped NameFactory
     */
    public NameFactory getNameFactory() {
        return nameFactory;
    }

    public Name create(final String namespaceURI, final String localName) {
        return (Name) execute(new SafeCallable() {
            public Object call() {
                return nameFactory.create(namespaceURI, localName);
            }}, "create(String, String)", new Object[]{namespaceURI, localName});
    }

    public Name create(final String nameString) {
        return (Name) execute(new SafeCallable() {
            public Object call() {
                return nameFactory.create(nameString);
            }}, "create(String)", new Object[]{nameString});
    }

}
