/**
 * User: Dror Bereznitsky
 * Date: 10/03/2007
 * Time: 18:31:18
 */
package org.jfrog.maven.viewer.ui.event;

import edu.uci.ics.jung.graph.Graph;

import org.springframework.context.ApplicationEvent;

public class NewGraphEvent extends ApplicationEvent {
    private final Graph graph;

    public NewGraphEvent(Object o, Graph graph) {
        super(o);
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }
}
