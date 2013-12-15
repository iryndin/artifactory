package org.artifactory.converters;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.common.property.ArtifactoryConverter;
import org.artifactory.version.CompoundVersionDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.artifactory.converters.helpers.ConvertersManagerTestHelper.*;
import static org.artifactory.version.ArtifactoryVersion.*;

/**
 * Author: gidis
 */
@Test

public class ConvertersManagerTest {

    /**
     * Convert artifactory which home is version 3.0.1  DBProperties is 3.0.1 and cluster version is 3.0.1
     *
     * @throws IOException
     */
    public void convertAll301() throws IOException {
        ArtifactoryContext context = createEnvironment(v301, v301, v301);
        // Invoke the ConvertersManagerImpl methods (events) by the real time order
        context.getConverterManager().homeReady();
        context.getConverterManager().dbAccessReady();
        ArtifactoryConverterSimulator artifactoryConverter = new ArtifactoryConverterSimulator();
        context.getConverterManager().serviceConvert(artifactoryConverter);
        context.getConverterManager().conversionEnded();
        // Make sure that the artifactory.properties.file has been updated
        assertArtifactoryPropertiesHasBeenUpdate(context);
        // make sure that the mimetypes file and version are valid
        assertValidMimeTypes();
        //Make sure some conversion has been executed
        assertConversionHasBeenExecuted(context);
        assertDBPropertiesHasBeenUpdated(context);
        // Make sure that no conversion has been running  service conversion
        Assert.assertTrue(artifactoryConverter.isConverted());
    }

    /**
     * Convert artifactory which home is version 3.0.4  DBProperties is 3.0.4 and cluster version is 3.0.4
     *
     * @throws IOException
     */
    public void convertAll304() throws IOException {
        ArtifactoryContext context = createEnvironment(v304, v304, v304);
        // Invoke the ConvertersManagerImpl methods (events) by the real time order
        context.getConverterManager().homeReady();
        context.getConverterManager().dbAccessReady();
        ArtifactoryConverterSimulator artifactoryConverter = new ArtifactoryConverterSimulator();
        context.getConverterManager().serviceConvert(artifactoryConverter);
        context.getConverterManager().conversionEnded();
        // Make sure that the artifactory.properties.file has been updated
        assertArtifactoryPropertiesHasBeenUpdate(context);
        // make sure that the mimetypes file and version are valid
        assertValidMimeTypes();
        //Make sure some conversion has been executed
        assertConversionHasBeenExecuted(context);
        assertDBPropertiesHasBeenUpdated(context);
        // Make sure that no conversion has been running  service conversion
        Assert.assertTrue(artifactoryConverter.isConverted());
    }

    /**
     * Convert artifactory which home is version 3.1.0  DBProperties is 3.1.0 and cluster version is 3.1.0
     *
     * @throws IOException
     */
    public void convertAllCurrent() throws IOException {
        ArtifactoryContext context = createEnvironment(v310, v310, v310);
        // Invoke the ConvertersManagerImpl methods (events) by the real time order
        context.getConverterManager().homeReady();
        context.getConverterManager().dbAccessReady();
        ArtifactoryConverterSimulator artifactoryConverter = new ArtifactoryConverterSimulator();
        context.getConverterManager().serviceConvert(artifactoryConverter);
        context.getConverterManager().conversionEnded();
        // Make sure that the artifactory.properties.file has been updated
        assertArtifactoryPropertiesHasBeenUpdate(context);
        // make sure that the mimetypes file and version are valid
        assertValidMimeTypes();
        //Make sure no conversion has been executed
        assertConverterHasNotBeenExecuted(context);
        // Make sure that no conversion has been running  service conversion
        Assert.assertFalse(artifactoryConverter.isConverted());
    }

    /**
     * Convert artifactory which home is version null  DBProperties is null and cluster version is null
     *
     * @throws IOException
     */
    public void convertNoVersions() throws IOException {
        ArtifactoryContext context = createEnvironment(null, null, null);
        // Invoke the ConvertersManagerImpl methods (events) by the real time order
        context.getConverterManager().homeReady();
        context.getConverterManager().dbAccessReady();
        ArtifactoryConverterSimulator artifactoryConverter = new ArtifactoryConverterSimulator();
        context.getConverterManager().serviceConvert(artifactoryConverter);
        context.getConverterManager().conversionEnded();
        // Make sure that the artifactory.properties.file has been updated
        assertArtifactoryPropertiesHasBeenUpdate(context);
        // make sure that the mimetypes file and version are valid
        assertValidMimeTypes();
        //Make sure no conversion has been executed
        assertConversionHasNotBeenExecuted(context);
        // Make sure that the no DbProperties update has been done (no service conversion has been executed)
        assertDBPropertiesHasNotBeenUpdated(context);
        // Make sure that no conversion has been running  service conversion
        Assert.assertFalse(artifactoryConverter.isConverted());
    }

    /**
     * * Convert artifactory which home is version 3.1.0  DBProperties is 3.0.1 and cluster version is 3.1.0
     *
     * @throws IOException
     */
    public void convertCurrentHomeOldDbCurrentCluster() throws IOException {
        ArtifactoryContext context = createEnvironment(v310, v301, v310);
        // Invoke the ConvertersManagerImpl methods (events) by the real time order
        context.getConverterManager().homeReady();
        context.getConverterManager().dbAccessReady();
        ArtifactoryConverterSimulator service = new ArtifactoryConverterSimulator();
        context.getConverterManager().serviceConvert(service);
        context.getConverterManager().conversionEnded();
        // Make sure that the artifactory.properties.file has been updated
        assertArtifactoryPropertiesHasBeenUpdate(context);
        // make sure that the mimetypes file and version are valid
        assertValidMimeTypes();
        //Make sure some conversion has been executed
        assertConversionHasBeenExecuted(context);
        // Make sure that the DbProperties has been updated (service conversion has been executed)
        assertDBPropertiesHasBeenUpdated(context);
        // Make sure that the service conversion has been running
        Assert.assertTrue(service.isConverted());
    }

    /**
     * Convert artifactory which home is version 3.0.1  DBProperties is 3.1.0 and cluster version is 3.1.0
     *
     * @throws IOException
     */
    public void convertOldHomeCurrentDbCurrentCluster() throws IOException {
        ArtifactoryContext context = createEnvironment(v301, v310, v310);
        // Invoke the ConvertersManagerImpl methods (events) by the real time order
        context.getConverterManager().homeReady();
        context.getConverterManager().dbAccessReady();
        ArtifactoryConverterSimulator artifactoryConverter = new ArtifactoryConverterSimulator();
        context.getConverterManager().serviceConvert(artifactoryConverter);
        context.getConverterManager().conversionEnded();
        // Make sure that the artifactory.properties.file has been updated
        assertArtifactoryPropertiesHasBeenUpdate(context);
        // make sure that the mimetypes file and version are valid
        assertValidMimeTypes();
        //Make sure some conversion has been executed
        assertConversionHasBeenExecuted(context);
        // Make sure that the no DbProperties update has been done (no service conversion has been executed)
        assertDBPropertiesHasNotBeenUpdated(context);
        // Make sure that no conversion has been running  service conversion
        Assert.assertFalse(artifactoryConverter.isConverted());
    }

    /**
     * Convert artifactory which home is version 3.1.0  DBProperties is 3.1.0 and cluster version is 3.0.1
     *
     * @throws IOException
     */
    public void convertCurrentHomeCurrentDbOldCluster() throws IOException {
        ArtifactoryContext context = createEnvironment(v310, v310, v301);
        // Invoke the ConvertersManagerImpl methods (events) by the real time order
        context.getConverterManager().homeReady();
        context.getConverterManager().dbAccessReady();
        ArtifactoryConverterSimulator artifactoryConverter = new ArtifactoryConverterSimulator();
        context.getConverterManager().serviceConvert(artifactoryConverter);
        context.getConverterManager().conversionEnded();
        // Make sure that the artifactory.properties.file has been updated
        assertArtifactoryPropertiesHasBeenUpdate(context);
        // make sure that the mimetypes file and version are valid
        assertValidMimeTypes();
        //Make sure some conversion has been executed
        assertConversionHasBeenExecuted(context);
        // Make sure that the no DbProperties update has been done (no service conversion has been executed)
        assertDBPropertiesHasNotBeenUpdated(context);
        // Make sure that no conversion has been running  service conversion
        Assert.assertFalse(artifactoryConverter.isConverted());
    }

    private class ArtifactoryConverterSimulator implements ArtifactoryConverter {

        private boolean converted;

        @Override
        public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
            converted = true;
        }

        private boolean isConverted() {
            return converted;
        }
    }


}






