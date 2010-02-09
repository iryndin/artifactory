package org.artifactory.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Tests the ExceptionUtils.
 *
 * @author Yossi Shaul
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class ExceptionUtilsTest {
    @Test
    public void testUnwrapThrowablesOfTypes() {
        IOException ioException = new IOException();
        IllegalArgumentException e = new IllegalArgumentException((new RuntimeException(ioException)));
        Throwable cause = ExceptionUtils.unwrapThrowablesOfTypes(e, IOException.class);
        Assert.assertSame(cause, cause, "Nothing should be wrapped");
        Throwable ioCause = ExceptionUtils.unwrapThrowablesOfTypes(e, RuntimeException.class);
        Assert.assertSame(ioCause, ioException, "Should have unwrapped any runtime exceptions");
    }

    @Test
    public void testGetCauseOfTypes() {
        IOException ioException = new IOException();
        IllegalArgumentException e = new IllegalArgumentException((new RuntimeException(ioException)));
        Throwable ioCause = ExceptionUtils.getCauseOfTypes(e, IOException.class);
        Assert.assertSame(ioCause, ioException, "Should return the same wrapped io exception");
        Throwable notFound = ExceptionUtils.getCauseOfTypes(e, IllegalStateException.class);
        Assert.assertNull(notFound, "Should not have found this type of exception");
    }
}
