package org.artifactory.test.mock;

import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.spring.InternalArtifactoryContext;
import org.easymock.EasyMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yoavl
 */
public class MockUtils {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger log = LoggerFactory.getLogger(MockUtils.class);

    public static InternalArtifactoryContext getThreadBoundedMockContext() {
        InternalArtifactoryContext context = EasyMock.createMock(InternalArtifactoryContext.class);
        EasyMock.expect(context.isReady()).andReturn(true).anyTimes();
        EasyMock.expect(context.getClassLoader()).andReturn(
                Thread.currentThread().getContextClassLoader()).anyTimes();
        ArtifactoryContextThreadBinder.bind(context);
        return context;
    }
}
