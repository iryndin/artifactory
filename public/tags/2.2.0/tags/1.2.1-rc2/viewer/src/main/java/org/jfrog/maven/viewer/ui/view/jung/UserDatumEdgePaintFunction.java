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
import edu.uci.ics.jung.graph.decorators.EdgePaintFunction;
import org.jfrog.maven.viewer.common.Scope;
import org.jfrog.maven.viewer.ui.model.jung.JungGraph;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Dror Bereznitsky
 * Date: May 4, 2007
 * Time: 1:42:07 PM
 */
public class UserDatumEdgePaintFunction implements EdgePaintFunction {
    private final static Map<Scope, Color> colors = new HashMap<Scope, Color>();

    static {
        colors.put(Scope.COMPILE, Color.BLACK);
        colors.put(Scope.TEST, Color.BLUE);
        colors.put(Scope.PROVIDED, Color.DARK_GRAY);
        colors.put(Scope.RUNTIME, Color.GREEN);
        colors.put(Scope.SYSTEM, Color.GRAY);
    }

    public Paint getDrawPaint(Edge e) {
        final Scope scope = Scope.valueOf(((String) e.getUserDatum(JungGraph.DEPENDENCY_SCOPE)).toUpperCase());
        return colors.get(scope);
    }

    public Paint getFillPaint(Edge e) {
        return TRANSPARENT;
    }
}
