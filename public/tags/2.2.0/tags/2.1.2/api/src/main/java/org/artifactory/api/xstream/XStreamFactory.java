/*
 * This file is part of Artifactory.
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

package org.artifactory.api.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import javanet.staxutils.StaxUtilsXMLOutputFactory;
import org.artifactory.api.md.xstream.PropertiesConverter;

import javax.xml.stream.XMLOutputFactory;

/**
 * @author Yoav Landman
 */
public abstract class XStreamFactory {
    /**
     * Creates an XStream object with a standard stax driver
     *
     * @param annotatedClassesToProcess Classes to process
     * @return XStream - XStream object
     */
    public static XStream create(Class... annotatedClassesToProcess) {
        return createXStream(new StaxDriver() {
            private XMLOutputFactory outputFactory;

            @Override
            public XMLOutputFactory getOutputFactory() {
                if (outputFactory == null) {
                    //Decorate the original output factory - make it pretty print
                    outputFactory = new StaxUtilsXMLOutputFactory(super.getOutputFactory());
                    outputFactory.setProperty(StaxUtilsXMLOutputFactory.INDENTING, true);
                }
                return outputFactory;
            }
        }, annotatedClassesToProcess);
    }

    /**
     * Creates an XStream object
     *
     * @param driver                    Driver to base XStream on
     * @param annotatedClassesToProcess Classes to process
     * @return XStream - XStream object
     */
    private static XStream createXStream(StaxDriver driver, Class... annotatedClassesToProcess) {
        XStream xstream = new XStream(driver);
        xstream.registerConverter(new PropertiesConverter());
        for (Class annotatedClass : annotatedClassesToProcess) {
            xstream.processAnnotations(annotatedClass);
        }
        return xstream;
    }
}
