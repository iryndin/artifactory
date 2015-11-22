package org.artifactory.addon.support;

import org.artifactory.addon.Addon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Michael Pasternak
 */
public interface SupportAddon extends Addon {

    boolean isSupportAddonEnabled();

    /**
     * Generates support bundle/s
     *
     * @param bundleConfiguration config to be used
     *
     * @return name/s of generated bundles
     */
    List<String> generate(Object bundleConfiguration);

    /**
     * List earlier created support bundle/s
     *
     * @return name/s of generated bundles
     */
    List<String> list();

    /**
     * Downloads support bundles
     *
     * @param bundleName
     * @return {@link InputStream} to support bundle
     *
     * @throws FileNotFoundException
     */
    public InputStream download(String bundleName) throws FileNotFoundException;

    /**
     * Deletes support bundles
     *
     * @param bundleName name of bundle to delete
     * @param async whether delete should be performed asynchronously
     *
     * @return result
     *
     * @throws FileNotFoundException
     */
    public boolean delete(String bundleName, boolean async) throws FileNotFoundException;
}
