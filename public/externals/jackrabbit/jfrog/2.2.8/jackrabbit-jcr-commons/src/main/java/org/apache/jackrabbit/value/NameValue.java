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
package org.apache.jackrabbit.value;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.util.Calendar;
import java.math.BigDecimal;

/**
 * A <code>NameValue</code> provides an implementation
 * of the <code>Value</code> interface representing a <code>NAME</code> value
 * (a string that is namespace-qualified).
 */
public class NameValue extends BaseValue {

    public static final int TYPE = PropertyType.NAME;

    private final String name;

    /**
     * Returns a new <code>NameValue</code> initialized to the value
     * represented by the specified <code>String</code>.
     * <p/>
     * The specified <code>String</code> must be a valid JCR name.
     *
     * @param s the string to be parsed.
     * @return a newly constructed <code>NameValue</code> representing the
     *         the specified value.
     * @throws javax.jcr.ValueFormatException If the <code>String</code> is not a valid
     *                              name.
     */
    public static NameValue valueOf(String s) throws ValueFormatException {
        if (s != null) {
            return new NameValue(s);
        } else {
            throw new ValueFormatException("not a valid name format: " + s);
        }
    }


    /**
     * Protected constructor creating a <code>NameValue</code> object
     * without validating the name.
     *
     * @param name the name this <code>NameValue</code> should represent
     * @see #valueOf
     */
    protected NameValue(String name) {
        super(TYPE);
        this.name = name;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p/>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>NameValue</code> object that
     * represents the same value as this object.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NameValue) {
            NameValue other = (NameValue) obj;
            if (name == other.name) {
                return true;
            } else if (name != null && other.name != null) {
                return name.equals(other.name);
            }
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    //------------------------------------------------------------< BaseValue >
    /**
     * {@inheritDoc}
     */
    protected String getInternalString() throws ValueFormatException {
        if (name != null) {
            return name;
        } else {
            throw new ValueFormatException("empty value");
        }
    }

    //----------------------------------------------------------------< Value >
    /**
     * {@inheritDoc}
     */
    public Calendar getDate()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to date failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public long getLong()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to long failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to boolean failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to double failed: inconvertible types");
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getDecimal()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
        throw new ValueFormatException("conversion to Decimal failed: inconvertible types");
    }
}
