package org.artifactory.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author  Ben Walding
 * @version $Id: IOUtility.java,v 1.1 2004/09/23 08:36:15 bwalding Exp $
 */
public class IoUtil {
    protected static final int BUFFER_SIZE = 16384;

    /**
     * Transfers all remaining data in the input stream to the output stream
     * 
     * Neither stream will be closed at completion.
     * @return
     **/
    public static long transferStream(InputStream is, OutputStream os) throws IOException
    {
        long bytes = 0;
        final byte[] buffer = new byte[BUFFER_SIZE];
        while (true)
        {
            int bytesRead = is.read(buffer, 0, buffer.length);
            if (bytesRead == -1)
            {
                break;
            }
            bytes += bytesRead;
            os.write(buffer, 0, bytesRead);
        }
        return bytes;
    }

    /**
     * Closes an InputStream without throwing an IOException.
     * The IOException is swallowed.
     * @param in the input stream to close. If null, nothing is closed.
     */
    public static void close(InputStream in)
    {
        try
        {
            if (in != null)
            {
                in.close();
            }
        }
        catch (IOException ioex)
        {
            //Do nothing
        }
    }

    /**
     * Closes an OutputStream without throwing an IOException.
     * The IOException is swallowed.
     * @param out the OutputStream to close. If null, nothing is closed.
     */
    public static void close(OutputStream out)
    {
        try
        {
            if (out != null)
            {
                out.close();
            }
        }
        catch (IOException ioex)
        {
            //Do nothing
        }
    }

}
