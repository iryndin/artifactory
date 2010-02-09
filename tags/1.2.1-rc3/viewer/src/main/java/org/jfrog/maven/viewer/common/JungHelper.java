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
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.VertexFontFunction;
import edu.uci.ics.jung.visualization.FRLayout;
import edu.uci.ics.jung.visualization.ISOMLayout;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.SpringLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.contrib.CircleLayout;
import edu.uci.ics.jung.visualization.contrib.KKLayout;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

import org.jfrog.maven.viewer.ui.model.jung.JungGraph;
import org.springframework.richclient.application.ApplicationException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Set;

/**
 * User: Dror Bereznitsky
 * Date: 30/03/2007
 * Time: 19:13:56
 */
public class JungHelper {
    private final static Class[] layoutClasses;
    private final static double OFFSET = 25.0d;

    static {
        layoutClasses = new Class[]{
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
                return (String) e.getUserDatum(JungGraph.DEPENDENCY_SCOPE);
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
            return (Layout) constructor.newInstance(constructorArgs);
        } catch (Exception e) {
            throw new RuntimeException("Could not create layout", e);
        }
    }

    public static void writeImageFile(File file, String format, VisualizationViewer vv) {
        Color bg = vv.getBackground();
        Rectangle rect = calculateGraphRect(vv);
        Dimension size = rect.getSize();
        BufferedImage bi = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_BGR);
        Graphics2D graphics = bi.createGraphics();
        graphics.setColor(bg);
        graphics.fillRect(0, 0, size.width, size.height);
        Dimension visibleSize = vv.getSize();

        // Hide the visualization viewer, resize it to entire graph size and move the graph to
        // upper most left corner of the viewr.
        vv.setVisible(false);
        vv.setSize(size);
        vv.getViewTransformer().translate(OFFSET - rect.getX(), OFFSET - rect.getY());

        vv.paint(graphics);

        // Return the previous size and location and redisplay the graph
        vv.getViewTransformer().translate(rect.getX() - OFFSET, rect.getY() - OFFSET);
        vv.setSize(visibleSize);
        vv.setVisible(true);

        try {
            ImageIO.write(bi, format, file);
        } catch (IOException e) {
            throw new ApplicationException("Could not save graph to file " + file.getAbsolutePath(), e);
        }
    }

    /*
    * Calculate the entire graph rectangle (not only the visible part)
    * Calculation id done by:
    * (1) Finding the upper most left in lower most right vertices
    * (2) Adding the width of the right most vetrex label
    * (3) Adding a preset offset
    */
    public static Rectangle calculateGraphRect(VisualizationViewer vv) {
        double x;
        double y;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double labeloffset = OFFSET * 2.0d;
        Point2D location;

        Layout layout = vv.getGraphLayout();
        MutableTransformer layoutTransformer = vv.getLayoutTransformer();
        Graph graph = layout.getGraph();
        StringLabeller labeller = getStringLabeller(graph);
        Set<Vertex> vertices = graph.getVertices();
        Vertex mostRightVertex = vertices.iterator().next();

        // Find the upper most left in lower most right vertices
        for (Vertex v : vertices) {
            // Transform from graph layout coordinates to graphics2d coordinates
            location = layoutTransformer.transform(layout.getLocation(v));
            x = location.getX();
            y = location.getY();
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
                mostRightVertex = v;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }

        // Calculate the width of the right most vetrex label
        String label = labeller.getLabel(mostRightVertex);
        if (vv.getRenderer() instanceof PluggableRenderer) {
            VertexFontFunction vertexFontFunction =
                    ((PluggableRenderer) vv.getRenderer()).getVertexFontFunction();
            Font font = vertexFontFunction.getFont(mostRightVertex);
            Rectangle2D labelBounds = font.getStringBounds(label, ((Graphics2D) vv.getGraphics()).getFontRenderContext());
            labeloffset += labelBounds.getWidth();
        } else {
            Font font = vv.getFont();
            Rectangle2D labelBounds = font.getStringBounds(label, ((Graphics2D) vv.getGraphics()).getFontRenderContext());
            labeloffset += labelBounds.getWidth();
        }

        final Dimension actual = new Dimension((int) (maxX - minX) + (int) labeloffset, (int) (maxY - minY) + (int) OFFSET * 2);
        return new Rectangle(new Point((int) minX, (int) minY), actual);
    }

    public static Class[] getLayoutClasses() {
        return layoutClasses;
    }
}
