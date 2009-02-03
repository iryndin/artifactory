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
package org.artifactory.utils;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class ExceptionUtils {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ExceptionUtils.class);

    /**
     * Unwrap a RuntimeException
     *
     * @param throwable     the throwable to unwrap
     * @param typesToUnwrap the types to keep unwrapping
     * @return The wrpped cause
     */
    public static Throwable unwrapThrowable(Throwable throwable,
            Class<? extends Throwable>... typesToUnwrap) {
        if (!isTypeOf(throwable, typesToUnwrap)) {
            return throwable;
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            cause = unwrapThrowable(cause, typesToUnwrap);
        } else {
            cause = throwable;
        }
        return cause;
    }

    private static boolean isTypeOf(Object source, Class... targetTypes) {
        Class sourceType = source.getClass();
        for (Class targetType : targetTypes) {
            if (targetType.isAssignableFrom(sourceType)) {
                return true;
            }
        }
        return false;
    }
}
