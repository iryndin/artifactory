/*
* Artifactory is a binaries repository manager.
* Copyright (C) 2012 JFrog Ltd.
*
* Artifactory is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Artifactory is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.artifactory.support.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation used to mark suppliers of
 * {@link org.artifactory.support.core.collectors.ContentCollector}
 *
 * @author Michael Pasternak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CollectService {
    /**
     * @return Invocation priority
     */
    public Priority priority() default Priority.NORMAL;

    /**
     * Invocation priority
     */
    public enum Priority {
        LOW(1), NORMAL(2), MEDIUM(3), HIGH(4);
        private final Integer value;

        private Priority(int value) {
            this.value = Integer.valueOf(value);
        }

        /**
         * Compares its two priorities for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second
         *
         * @param p1 {@link Priority}
         * @param p2 {@link Priority}
         *
         * @return -1/0/1
         */
        public int compare(Priority p1, Priority p2) {
            return p1.value.compareTo(p2.value);
        }
    }
}
