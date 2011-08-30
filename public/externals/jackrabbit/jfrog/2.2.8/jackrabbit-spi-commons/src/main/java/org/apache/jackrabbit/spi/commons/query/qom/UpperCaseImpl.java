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
package org.apache.jackrabbit.spi.commons.query.qom;

import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.UpperCase;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * <code>UpperCaseImpl</code>...
 */
public class UpperCaseImpl extends DynamicOperandImpl implements UpperCase {

    /**
     * The operand whose value is converted to a upper-case string.
     */
    private final DynamicOperandImpl operand;

    UpperCaseImpl(NamePathResolver resolver, DynamicOperandImpl operand) {
        super(resolver, operand.getSelectorQName());
        this.operand = operand;
    }

    /**
     * Gets the operand whose value is converted to a upper-case string.
     *
     * @return the operand; non-null
     */
    public DynamicOperand getOperand() {
        return operand;
    }

    //------------------------< AbstractQOMNode >-------------------------------

    /**
     * Accepts a <code>visitor</code> and calls the appropriate visit method
     * depending on the type of this QOM node.
     *
     * @param visitor the visitor.
     */
    public Object accept(QOMTreeVisitor visitor, Object data) throws Exception {
        return visitor.visit(this, data);
    }

    //------------------------< Object >----------------------------------------

    public String toString() {
        return "UPPER(" + operand + ")";
    }

}
