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
package org.apache.jackrabbit.core.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test node types with binary default values.
 */
public class NodeTypeTest extends AbstractJCRTest {

    /**
     * Test a node type with a binary default value
     * @throws RepositoryException
     * @throws ParseException
     */
    public void testNodeTypesWithBinaryDefaultValue()
            throws RepositoryException, IOException, ParseException {

        doTestNodeTypesWithBinaryDefaultValue(0);
        doTestNodeTypesWithBinaryDefaultValue(10);
        doTestNodeTypesWithBinaryDefaultValue(10000);
    }

    public void doTestNodeTypesWithBinaryDefaultValue(int len)
            throws RepositoryException, IOException, ParseException {
        char[] chars = new char[len];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = 'a';
        }
        String def = new String(chars);

        String type = "test:foo" + len;
        String cnd =
            "<test='http://www.apache.org/jackrabbit/test'>\n"
            + "[" + type + "]\n - value(binary) = '" + def + "' m a";
        JackrabbitNodeTypeManager manager = (JackrabbitNodeTypeManager)
            superuser.getWorkspace().getNodeTypeManager();
        if (!manager.hasNodeType(type)) {
            Reader cndReader = new InputStreamReader(new ByteArrayInputStream(cnd.getBytes("UTF-8")));
            CndImporter.registerNodeTypes(cndReader, superuser);
        }

        Node root = superuser.getRootNode();
        Node node = root.addNode("testfoo" + len, type);
        Value value = node.getProperty("value").getValue();
        assertEquals(PropertyType.BINARY, value.getType());
        assertEquals(def, value.getString());
    }

}
