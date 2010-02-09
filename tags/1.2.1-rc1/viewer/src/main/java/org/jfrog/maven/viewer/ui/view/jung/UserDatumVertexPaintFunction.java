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

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;

import java.awt.*;

/**
 * User: Dror Bereznitsky
 * Date: 27/12/2006
 * Time: 00:06:57
 */
public class UserDatumVertexPaintFunction implements VertexPaintFunction {

    public Paint getFillPaint(Vertex vertex) {
        Integer depth = (Integer)vertex.getUserDatum("depth");
        if (depth == 0) {
            return Color.YELLOW;
        } else if (vertex.getUserDatum("dummy") != null) {
            return Color.GRAY;
        } else {
            int g = ((depth - 1) * 25);
            if (g > 255) g = 255;
            return new Color(255, g, 0);
        }
    }

    public Paint getDrawPaint(Vertex vertex) {
        if ((Integer)vertex.getUserDatum("depth") == 0) {
            return Color.ORANGE;
        } else if (vertex.getUserDatum("dummy") != null) {
            return Color.DARK_GRAY;
        } else {
            return Color.BLACK;
    	}
    }
}
