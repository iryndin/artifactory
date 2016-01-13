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

package org.artifactory.schedule;

import org.springframework.util.ClassUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Predicate;

/**
 * Date: 9/5/11
 * Time: 5:03 PM
 *
 * @author Fred Simon
 */
public class TaskTypePredicate implements Predicate<Task> {
    private final Class<? extends TaskCallback>[] types;
    private Predicate<Task> delegate;

    public TaskTypePredicate(Class<? extends TaskCallback>... types) {
        this.types = types;
    }

    @SuppressWarnings({"unchecked"})
    public TaskTypePredicate(TaskTypePredicate from, Class<? extends TaskCallback>... types) {
        HashSet<Class<? extends TaskCallback>> classes = new HashSet<>();
        Collections.addAll(classes, from.types);
        Collections.addAll(classes, types);
        this.types = classes.toArray(new Class[classes.size()]);
    }

    public void addSubPredicate(Predicate<Task> subPredicate) {
        if (this.delegate == null) {
            this.delegate = subPredicate;
        } else {
            this.delegate = delegate.or(subPredicate);
        }
    }

    @Override
    public boolean test(@Nullable Task input) {
        if (delegate != null && delegate.test(input)) {
            return true;
        }
        for (Class<? extends TaskCallback> type : types) {
            if (ClassUtils.isAssignable(type, input.getType())) {
                return true;
            }
        }
        return false;
    }
}
