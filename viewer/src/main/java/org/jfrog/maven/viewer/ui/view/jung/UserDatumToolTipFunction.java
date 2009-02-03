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
package org.jfrog.maven.viewer.ui.view.jung;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.DefaultToolTipFunction;
import org.jfrog.maven.viewer.common.ArtifactIdentifier;

/**
 * User: Dror Bereznitsky
 * Date: 31/12/2006
 * Time: 00:56:26
 */
public class UserDatumToolTipFunction extends DefaultToolTipFunction {

    @Override
    public String getToolTipText(Vertex v) {
        ArtifactIdentifier id = (ArtifactIdentifier) v.getUserDatum("identifier");
        return id.toString();
    }

    @Override
    public String getToolTipText(Edge edge) {
        return (String) edge.getUserDatum("tooltip");
    }
}
