/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.schedule.aop;

import com.google.common.collect.ImmutableList;
import org.aopalliance.intercept.MethodInvocation;
import org.artifactory.api.repo.Async;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.springframework.security.util.SimpleMethodInvocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Used mainly for stacking async invocations on the same async thread (shared asyncs).
 * <p/>
 * E.g. indexing a remotely downloaded jar and updating its download count.
 *
 * @author Yoav Landman
 */
public class CompoundInvocation extends SimpleMethodInvocation {

    List<MethodInvocation> invocations = new ArrayList<MethodInvocation>();

    public void add(MethodInvocation invocation) {
        invocations.add(invocation);
    }

    @Override
    public Object proceed() throws Throwable {
        List<MethodInvocation> tmpInvocations = ImmutableList.copyOf(invocations);
        invocations.clear();
        for (MethodInvocation invocation : tmpInvocations) {
            //Wrap in tx if needed
            Async annotation = invocation.getMethod().getAnnotation(Async.class);
            if (annotation == null) {
                throw new IllegalArgumentException(
                        "An async invocation (" + invocation.getMethod() +
                                ") should be used with an @Async annotated invocation.");
            }
            if (annotation.transactional()) {
                //Wrap in a transaction
                new LockingAdvice().invoke(invocation, true);
            } else {
                invocation.proceed();
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return invocations.isEmpty();
    }

    public void clear() {
        invocations.clear();
    }
}