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
package org.artifactory.util;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class ExceptionUtils {

    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null) {
            cause = getRootCause(cause);
        } else {
            cause = throwable;
        }
        return cause;
    }

    /**
     * Unwrap an exception
     *
     * @param throwable     the throwable to unwrap
     * @param typesToUnwrap the types to keep unwrapping
     * @return The wrapped cause
     */
    public static Throwable unwrapThrowablesOfTypes(Throwable throwable,
            Class<? extends Throwable>... typesToUnwrap) {
        if (!isTypeOf(throwable, typesToUnwrap)) {
            return throwable;
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            cause = unwrapThrowablesOfTypes(cause, typesToUnwrap);
        } else {
            cause = throwable;
        }
        return cause;
    }

    /**
     * Unwrap an exception
     *
     * @param throwable  the throwable to examine
     * @param causeTypes the desired cause types to find
     * @return The wrapped cause or null if not found
     */
    public static Throwable getCauseOfTypes(Throwable throwable, Class<? extends Throwable>... causeTypes) {
        Throwable cause = throwable.getCause();
        if (cause != null) {
            if (isTypeOf(cause, causeTypes)) {
                return cause;
            } else {
                return getCauseOfTypes(cause, causeTypes);
            }
        } else {
            return null;
        }
    }

    private static boolean isTypeOf(Throwable source, Class<? extends Throwable>... targetTypes) {
        Class<? extends Throwable> sourceType = source.getClass();
        for (Class<? extends Throwable> targetType : targetTypes) {
            if (targetType.isAssignableFrom(sourceType)) {
                return true;
            }
        }
        return false;
    }
}
