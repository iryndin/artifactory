package org.artifactory.utils;

import junit.framework.TestCase;

/**
 * @author Ben Walding
 */
public class MimeTypesTest extends TestCase
{

    public void testGetMimeTypeByExtension()
    {
        assertEquals( "application/octet-stream", MimeTypes.getMimeTypeByExtension( ".jar" ).getMimeType() );
    }

    public void testGetMimeTypeByPath()
    {
        assertEquals( "application/octet-stream", MimeTypes.getMimeTypeByPath( "/a/b.jar" ).getMimeType() );
    }

}
