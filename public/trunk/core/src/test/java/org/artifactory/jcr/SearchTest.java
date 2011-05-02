/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.jcr;

import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.io.InputStream;
import java.util.Calendar;

@Test
public class SearchTest extends RepositoryTestBase {

    public void dateSearch() throws Exception {
        InputStream xml = getClass().getClassLoader().getResourceAsStream("META-INF/jcr/artifactory_nodetypes.xml");
        QNodeTypeDefinition[] defs = NodeTypeReader.read(xml);
        xml.close();

        Session session = login();
        Workspace workspace = session.getWorkspace();
        JcrRepoInitHelper.registerTypes(workspace, defs);
        //workspace.get

        Node root = session.getRootNode();
        root.addNode("a", "nt:folder");
        Node b = root.addNode("b", "artifactory:folder");
        Node c = b.addNode("c", "artifactory:folder");
        Node d = c.addNode("d", "artifactory:folder");
        Calendar calendar = Calendar.getInstance();
        b.setProperty("artifactory:created", calendar);
        //c.setProperty("artifactory:created", calendar);
        d.setProperty("artifactory:created", calendar);
        session.save();

        QueryManager qm = workspace.getQueryManager();
        QueryResult result = qm.createQuery(
                "/jcr:root/(a, nt:folder)[@jcr:created > xs:dateTime('2008-11-05T00:00:00.000+00:00')]",
                Query.XPATH).execute();
        Assert.assertTrue(result.getNodes().hasNext());

        /*result = qm.createQuery(
                "/jcr:root/(a, nt:folder)[@jcr:created <= '" + System.currentTimeMillis() + "']",
                Query.XPATH).execute();
        Assert.assertTrue(result.getNodes().hasNext());*/

        result = qm.createQuery(
                "/jcr:root/b//element(*, artifactory:folder) [@artifactory:created > xs:dateTime('2010-04-28T01:26:17.129+03:00')]",
                Query.XPATH).execute();
        Assert.assertTrue(result.getNodes().hasNext());
        session.logout();

        /*
        /jcr:root/repositories//element(*, artifactory:file) [(@artifactory:created > xs:dateTime('2010-04-28T01:26:17.129+03:00') or @artifactory:lastModified > xs:dateTime('2010-04-28T01:26:17.129+03:00') ) and (@artifactory:created <= xs:dateTime('2010-04-28T01:28:17.130+03:00') or @artifactory:lastModified <= xs:dateTime('2010-04-28T01:28:17.130+03:00') )]
         */
    }
}