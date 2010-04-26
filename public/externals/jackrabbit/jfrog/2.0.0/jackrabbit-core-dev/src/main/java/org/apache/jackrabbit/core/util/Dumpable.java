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
package org.apache.jackrabbit.core.util;

import java.io.PrintStream;

/**
 * Utility interface for internal use.
 * <p/>
 * A <code>Dumpable</code> object supports dumping its state in a human readable
 * format for diagnostic/debug purposes.
 */
public interface Dumpable {
    /**
     * Dumps the state of this instance in a human readable format for
     * diagnostic purposes.
     *
     * @param ps stream to dump state to
     */
    void dump(PrintStream ps);
}
