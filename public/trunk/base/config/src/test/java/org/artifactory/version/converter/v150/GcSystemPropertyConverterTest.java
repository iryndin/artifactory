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

package org.artifactory.version.converter.v150;

import org.artifactory.common.ConstantValues;
import org.artifactory.convert.XmlConverterTest;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Noam Y. Tenne
 */
public class GcSystemPropertyConverterTest extends XmlConverterTest {

    @Test
    public void testDefaultGcInterval() throws Exception {
        testConversion(ConstantValues.gcIntervalSecs.getDefValue(), "0 0 /4 * * ?");
    }

    @Test
    public void testGcIntervalShorterThanHour() throws Exception {
        testConversion("150", "0 0 /1 * * ?");
    }

    @Test
    public void testGcIntervalLessThanADay() throws Exception {
        testConversion(Long.toString(TimeUnit.HOURS.toSeconds(4)), "0 0 /4 * * ?");
    }

    @Test
    public void testGcIntervalMoreThanADay() throws Exception {
        testConversion(Long.toString(TimeUnit.HOURS.toSeconds(52)), "0 0 4 /2 * ?");
    }

    private void testConversion(String intervalSecs, String expectedCron) throws Exception {
        getBound().setProperty(ConstantValues.gcIntervalSecs, intervalSecs);
        Document document = convertXml("/config/test/config.1.4.9.no.gc.xml", new GcSystemPropertyConverter());
        Element rootElement = document.getRootElement();
        Namespace namespace = rootElement.getNamespace();
        Element gcConfigElement = rootElement.getChild("gcConfig", namespace);

        assertNotNull(gcConfigElement, "Expected to find a GC configuration element.");

        String cronExp = gcConfigElement.getChildText("cronExp", namespace);
        assertEquals(cronExp, expectedCron, "Unexpected default GC cron exp");
    }
}
