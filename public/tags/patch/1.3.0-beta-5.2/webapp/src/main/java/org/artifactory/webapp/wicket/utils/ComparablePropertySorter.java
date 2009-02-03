package org.artifactory.webapp.wicket.utils;

import org.apache.log4j.Logger;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sort a collection according to a comparable property of the collection's items. Currently
 * supports only direct (non-inherited) getters.
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ComparablePropertySorter<T> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ComparablePropertySorter.class);

    private Class<T> clazz;
    private Map<String, Method> getters;

    public ComparablePropertySorter(Class<T> clazz) {
        this.clazz = clazz;
        this.getters = new HashMap<String, Method>();
    }

    public void sort(List<T> list, SortParam sp) {
        sort(list, sp, null);
    }

    public void sort(List<T> list, SortParam sp, final Comparable firstAlwaysValue) {
        String property = sp.getProperty();
        //Lazily find the property getter
        final Method getter = findGetter(property);
        if (getter == null) {
            throw new IllegalArgumentException("Could not find the property '" + property +
                    "' in the list of comparable properties for class '" + clazz.getName() + "'.");
        }
        boolean asc = sp.isAscending();
        if (asc) {
            Collections.sort(list, new Comparator<T>() {
                @SuppressWarnings({"unchecked"})
                public int compare(T o1, T o2) {
                    try {
                        Comparable comparable = (Comparable) getter.invoke(o1);
                        if (firstAlwaysValue != null && comparable.equals(firstAlwaysValue)) {
                            return Integer.MIN_VALUE;
                        } else {
                            return comparable.compareTo(getter.invoke(o2));
                        }
                    } catch (Exception e) {
                        throw new WicketRuntimeException(e);
                    }
                }
            });
        } else {
            Collections.sort(list, new Comparator<T>() {
                @SuppressWarnings({"unchecked"})
                public int compare(T o1, T o2) {
                    try {
                        Comparable comparable = (Comparable) getter.invoke(o1);
                        if (firstAlwaysValue != null && comparable.equals(firstAlwaysValue)) {
                            return Integer.MIN_VALUE;
                        } else {
                            return ((Comparable) getter.invoke(o2)).compareTo(comparable);
                        }
                    } catch (Exception e) {
                        throw new WicketRuntimeException(e);
                    }
                }
            });
        }
    }

    @SuppressWarnings({"EmptyCatchBlock"})
    private Method findGetter(String fieldName) {
        Method getter = getters.get(fieldName);
        if (getter != null) {
            return getter;
        }
        String name = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            getter = clazz.getMethod("get" + name);
        }
        catch (Exception e) {
        }
        if (getter == null) {
            try {
                getter = clazz.getMethod("is" + name);
            }
            catch (Exception e) {
                LOGGER.debug("Cannot find getter " + clazz + "." + fieldName, e);
            }
        }
        if (getter != null) {
            Class<?> retrunType = getter.getReturnType();
            if (Comparable.class.isAssignableFrom(retrunType)) {
                getters.put(fieldName, getter);
                return getter;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
