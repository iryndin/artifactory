package org.artifactory.utils;

/*
 * Copyright 2003-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * @author  Ben Walding
 * @version $Id: IOUtilityTest.java,v 1.1 2004/09/23 08:36:20 bwalding Exp $
 */
public class IoUtiTest extends TestCase
{

    public void testTransferStream() throws Exception
    {
        testTransfer( 0 );
        testTransfer( 1 );
        testTransfer( 500 );
        testTransfer( 1024 );
        testTransfer( 16385 );
        testTransfer( 1000000 ); //1 MILLLLIION BYTES!
    }

    public void testTransfer( int length ) throws Exception
    {
        byte[] bufIn = new byte[length];
        //Want repeatability, but different datasets depending on length
        Random r = new Random();
        r.setSeed( length );
        r.nextBytes( bufIn );

        ByteArrayInputStream bais = new ByteArrayInputStream( bufIn );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        IoUtil.transferStream( bais, baos );

        byte[] bufOut = baos.toByteArray();

        assertEquals( "input.length == output.length", bufIn.length, bufOut.length );

        for ( int i = 0; i < bufIn.length; i++ )
        {
            if ( bufIn[i] != bufOut[i] )
            {
                fail( "The input and output streams failed to match at position " + i + " (0 based)" );
            }
        }

    }

}