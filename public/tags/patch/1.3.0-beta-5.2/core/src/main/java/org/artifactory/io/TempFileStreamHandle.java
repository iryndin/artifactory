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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.common.ResourceStreamHandle;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class TempFileStreamHandle implements ResourceStreamHandle {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(NonClosingInputStream.class);

    private final File tmpFile;
    private final InputStream is;

    public TempFileStreamHandle(File tmpFile) throws FileNotFoundException {
        this.tmpFile = tmpFile;
        this.is = new BufferedInputStream(new FileInputStream(tmpFile));
    }

    public InputStream getInputStream() {
        return is;
    }

    public void close() {
        IOUtils.closeQuietly(is);
        boolean deleted = tmpFile.delete();
        if (!deleted) {
            LOGGER.warn("Failed to delete temporary file '" + tmpFile.getPath() + "'.");
        }
    }
}