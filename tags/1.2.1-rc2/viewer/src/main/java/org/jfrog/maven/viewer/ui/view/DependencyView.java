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
package org.jfrog.maven.viewer.ui.view;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.filters.EdgePredicateFilter;
import edu.uci.ics.jung.graph.filters.Filter;
import edu.uci.ics.jung.graph.filters.SerialFilter;
import edu.uci.ics.jung.graph.filters.VertexPredicateFilter;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import org.jfrog.maven.viewer.common.Config;
import org.jfrog.maven.viewer.common.JungHelper;
import org.jfrog.maven.viewer.ui.event.FilterEvent;
import org.jfrog.maven.viewer.ui.event.NewGraphEvent;
import org.jfrog.maven.viewer.ui.event.SaveAsEvent;
import org.jfrog.maven.viewer.ui.model.jung.filter.NoIncomingEdgesVertexPredicate;
import org.jfrog.maven.viewer.ui.model.jung.filter.NoVertexEdgePredicate;
import org.jfrog.maven.viewer.ui.view.jung.UserDatumEdgePaintFunction;
import org.jfrog.maven.viewer.ui.view.jung.UserDatumToolTipFunction;
import org.jfrog.maven.viewer.ui.view.jung.UserDatumVertexPaintFunction;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.richclient.application.support.AbstractView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Dror Bereznitsky
 * Date: 19/02/2007
 * Time: 23:07:14
 */
public class DependencyView extends AbstractView implements ApplicationListener {

    private VisualizationViewer vv;
    // The graph without any filters applied
    private Graph originalGraph;
    // The displayed graph
    private Graph currentGraph;
    // A list of currenlt applied filters
    private List<Filter> filters;

    public DependencyView() throws HeadlessException {
        filters = new ArrayList<Filter>();
    }

    protected JComponent createControl() {
        return new JPanel(new BorderLayout());
    }

    //TODO delegate event handling to methods
    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof NewGraphEvent) {
            setCurrentGraph(((NewGraphEvent) e).getGraph());
            setOriginalGraph(getCurrentGraph());
            applyFilters();
            renderNewGraph();
        } else if (e instanceof FilterEvent) {
            final FilterEvent event = (FilterEvent) e;
            final Filter filter = event.getFilter();
            switch (event.getActionType()) {
                case ADD:
                    addFilter(filter);
                    break;
                case REMOVE:
                    removeFilter(filter);
                    break;
                case REPLACE:
                    replaceFilter(filter);
                    break;
            }
            applyFilters();
            reRenderGraph();
        } else if (e instanceof SaveAsEvent) {
            SaveAsEvent event = (SaveAsEvent) e;
            JungHelper.writeImageFile(event.getFile(), event.getType(), vv);
        }
    }

    private void addFilter(Filter filter) {
        filters.add(filter);
    }

    private void removeFilter(Filter filter) {
        if (filters.contains(filter)) {
            filters.remove(filter);
        }
    }

    private void replaceFilter(Filter filter) {
        removeFilter(filter);
        addFilter(filter);
    }

    private void applyFilters() {
        if (filters.size() > 0) {
            List filtersList = new ArrayList(filters);
            SerialFilter sFilter = new SerialFilter(filtersList);
            Graph tmpGraph = sFilter.filter(getOriginalGraph()).assemble();

            /*
            * Post processing - iteratively remove all edges with only one vertex
            * and all vertices that has no incoming edges.
             */
            filtersList.clear();
            filtersList.add(new EdgePredicateFilter(new NoVertexEdgePredicate()));
            filtersList.add(new VertexPredicateFilter(new NoIncomingEdgesVertexPredicate()));
            sFilter = new SerialFilter(filtersList);
            int depth = Config.getTransitiveDepth();
            for (int i = 0; i < depth; i++) {
                tmpGraph = sFilter.filter(tmpGraph).assemble();
            }

            setCurrentGraph(tmpGraph);
        } else {
            setCurrentGraph(getOriginalGraph());
        }
    }

    private void renderNewGraph() {
        final ModalGraphMouse gm = new DefaultModalGraphMouse();
        vv = new VisualizationViewer(new FRLayout(getCurrentGraph()), getRenderer());

        vv.setBackground(Color.white);
        vv.setGraphMouse(gm);
        vv.setPickSupport(new ShapePickSupport());
        // add my listener for ToolTips
        vv.setToolTipFunction(new UserDatumToolTipFunction());
        getControl().removeAll();
        getControl().add(getGraphZoomScrollPane());
        getControl().revalidate();
    }

    private void reRenderGraph() {
        Layout l = JungHelper.creatLayout(vv.getGraphLayout().getClass(), getCurrentGraph());
        applyNewLayout(l);
    }

    private PluggableRenderer getRenderer() {
        EdgeStringer stringer = JungHelper.getEdgeStringer();
        // Use the labeller of the original graph to avoid duplicating all labels
        StringLabeller labeller = JungHelper.getStringLabeller(getOriginalGraph());

        PluggableRenderer renderer = new PluggableRenderer();
        renderer.setVertexStringer(labeller);
        renderer.setEdgeShapeFunction(new EdgeShape.QuadCurve());
        renderer.setVertexPaintFunction(new UserDatumVertexPaintFunction());
        renderer.setEdgePaintFunction(new UserDatumEdgePaintFunction());
        renderer.setEdgeStringer(stringer);

        return renderer;
    }

    private GraphZoomScrollPane getGraphZoomScrollPane() {
        final ScalingControl scaler = new CrossoverScalingControl();
        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);

        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1 / 1.1f, vv.getCenter());
            }
        });

        final JComboBox jcb = new JComboBox(JungHelper.getLayoutClasses());
        // use a renderer to shorten the layout name presentation
        jcb.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String valueString = value.toString();
                valueString = valueString.substring(valueString.lastIndexOf('.') + 1);
                return super.getListCellRendererComponent(list, valueString, index, isSelected,
                        cellHasFocus);
            }
        });

        jcb.addActionListener(new LayoutChooser());
        jcb.setSelectedItem(FRLayout.class);

        JPanel controls = new JPanel();
        controls.add(plus);
        controls.add(minus);
        controls.add(jcb);
        controls.add(((DefaultModalGraphMouse) vv.getGraphMouse()).getModeComboBox());
        panel.add(controls, BorderLayout.SOUTH);

        return panel;
    }

    private void applyNewLayout(Layout l) {
        vv.stop();
        vv.setGraphLayout(l, false);
        vv.restart();
    }

    private void setCurrentGraph(Graph currentGraph) {
        this.currentGraph = currentGraph;
    }

    private Graph getCurrentGraph() {
        return currentGraph;
    }

    private Graph getOriginalGraph() {
        return originalGraph;
    }

    private void setOriginalGraph(Graph originalGraph) {
        this.originalGraph = originalGraph;
    }

    private final class LayoutChooser implements ActionListener {
        public void actionPerformed(ActionEvent arg0) {
            JComboBox comboBox = (JComboBox) arg0.getSource();
            Class lay = (Class) comboBox.getSelectedItem();
            Layout l = JungHelper.creatLayout(lay, vv.getGraphLayout().getGraph());
            applyNewLayout(l);
        }
    }
}
