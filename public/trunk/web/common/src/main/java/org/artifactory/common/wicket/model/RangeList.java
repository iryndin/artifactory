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

package org.artifactory.common.wicket.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Yoav Aharoni
 */
public class RangeList implements List<Integer> {
    private int min;
    private int max;

    public RangeList(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int size() {
        return max - min + 1;
    }

    public boolean isEmpty() {
        return max < min;
    }

    public boolean contains(Object o) {
        if (o instanceof Integer) {
            Integer integer = (Integer) o;
            return integer >= min && integer <= max;
        }

        return false;
    }

    public Iterator<Integer> iterator() {
        return null;
    }

    public Object[] toArray() {
        return new Object[0];
    }

    public <T> T[] toArray(T[] a) {
        return null;
    }

    public boolean add(Integer integer) {
        return false;
    }

    public boolean remove(Object o) {
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        return false;
    }

    public boolean addAll(Collection<? extends Integer> c) {
        return false;
    }

    public boolean addAll(int index, Collection<? extends Integer> c) {
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        return false;
    }

    public boolean retainAll(Collection<?> c) {
        return false;
    }

    public void clear() {
    }

    public Integer get(int index) {
        return null;
    }

    public Integer set(int index, Integer element) {
        return null;
    }

    public void add(int index, Integer element) {
    }

    public Integer remove(int index) {
        return null;
    }

    public int indexOf(Object o) {
        return 0;
    }

    public int lastIndexOf(Object o) {
        return 0;
    }

    public ListIterator<Integer> listIterator() {
        return null;
    }

    public ListIterator<Integer> listIterator(int index) {
        return null;
    }

    public List<Integer> subList(int fromIndex, int toIndex) {
        return null;
    }
}
