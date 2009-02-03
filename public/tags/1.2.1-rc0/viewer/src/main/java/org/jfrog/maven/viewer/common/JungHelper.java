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
package org.jfrog.maven.viewer.common;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.contrib.KKLayout;
import edu.uci.ics.jung.visualization.contrib.CircleLayout;

import javax.imageio.ImageIO;
import java.lang.reflect.Constructor;
import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.springframework.richclient.application.ApplicationException;

/**
 * User: Dror Bereznitsky
 * Date: 30/03/2007
 * Time: 19:13:56
 */
public class JungHelper {
    private final static Class[] layoutClasses;

    static {
        layoutClasses = new Class[] {
                KKLayout.class,
                FRLayout.class,
                CircleLayout.class,
                SpringLayout.class,
                ISOMLayout.class
        };
    }

    public static EdgeStringer getEdgeStringer() {
        return new EdgeStringer() {
            public String getLabel(ArchetypeEdge e) {
                return (String) e.getUserDatum("label");
            }
        };
    }

    public static StringLabeller getStringLabeller(Graph graph) {
        return StringLabeller.getLabeller(graph);
    }

    public static Layout creatLayout(Class clazz, Graph graph) {
        Object[] constructorArgs = {graph};
        try {
            Constructor constructor = clazz
                    .getConstructor(Graph.class);
            Object o = constructor.newInstance(constructorArgs);
            Layout l = (Layout) o;
            return l;
        } catch (Exception e) {
            throw new RuntimeException("Could not create layout", e);
        }
    }

    public static void writeImageFile(File file, String format, VisualizationViewer vv) {
        // TODO find a more accurate way to calculate the width
        int width = (int)vv.getWidth();
        int height = (int) vv.getHeight();
        Color bg = vv.getBackground();

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics2D graphics = bi.createGraphics();
        graphics.setColor(bg);
        graphics.fillRect(0, 0, width, height);
        vv.paint(graphics);

        try {
            ImageIO.write(bi, format, file);
        } catch (IOException e) {
            throw new ApplicationException("Could not save graph to file " + file.getAbsolutePath(), e);
        }
    }

    public static Class[] getLayoutClasses() {
        return layoutClasses;
    }
}
