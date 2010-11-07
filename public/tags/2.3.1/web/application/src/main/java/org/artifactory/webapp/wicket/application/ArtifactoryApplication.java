/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.wicket.application;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Resource;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.authorization.strategies.CompoundAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadWebRequest;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.request.target.coding.IRequestTargetUrlCodingStrategy;
import org.apache.wicket.settings.IApplicationSettings;
import org.apache.wicket.settings.IMarkupSettings;
import org.apache.wicket.settings.IRequestCycleSettings;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.settings.ISecuritySettings;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.artifactory.addon.BootstrapListener;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.common.wicket.application.AddWicketPathListener;
import org.artifactory.common.wicket.application.NoLocaleResourceStreamLocator;
import org.artifactory.common.wicket.component.panel.sidemenu.SiteMapAware;
import org.artifactory.common.wicket.contributor.ResourcePackage;
import org.artifactory.common.wicket.model.sitemap.MenuNode;
import org.artifactory.common.wicket.model.sitemap.SiteMap;
import org.artifactory.common.wicket.model.sitemap.SiteMapBuilder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.version.CompoundVersionDetails;
import org.artifactory.webapp.spring.ArtifactorySpringComponentInjector;
import org.artifactory.webapp.wicket.application.sitemap.ArtifactorySiteMapBuilder;
import org.artifactory.webapp.wicket.page.base.BasePage;
import org.artifactory.webapp.wicket.page.browse.listing.ArtifactListPage;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.SimpleRepoBrowserPage;
import org.artifactory.webapp.wicket.page.error.AccessDeniedPage;
import org.artifactory.webapp.wicket.page.error.InternalErrorPage;
import org.artifactory.webapp.wicket.page.error.PageExpiredErrorPage;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.search.artifact.ArtifactSearchPage;
import org.artifactory.webapp.wicket.page.security.login.LoginPage;
import org.artifactory.webapp.wicket.page.security.login.LogoutPage;
import org.artifactory.webapp.wicket.page.security.login.forgot.ForgotPasswordPage;
import org.artifactory.webapp.wicket.page.security.login.reset.ResetPasswordPage;
import org.artifactory.webapp.wicket.page.security.profile.ProfilePage;
import org.artifactory.webapp.wicket.resource.LogoResource;
import org.artifactory.webapp.wicket.service.authentication.LogoutService;
import org.slf4j.Logger;
import org.wicketstuff.annotation.scan.AnnotatedMountList;
import org.wicketstuff.annotation.scan.AnnotatedMountScanner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

/**
 * @author Yoav Landman
 */
public class ArtifactoryApplication extends AuthenticatedWebApplication implements SiteMapAware {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryApplication.class);

    @SpringBean
    private CentralConfigService centralConfig;

    @SpringBean
    private RepositoryService repositoryService;

    private SiteMap siteMap;
    private String sharedResourcesPath;

    private long logoModifyTime;

    private final EnumSet<ConstantValues> modes =
            Sets.newEnumSet(Collections.<ConstantValues>emptySet(), ConstantValues.class);

    public static ArtifactoryApplication get() {
        return (ArtifactoryApplication) Application.get();
    }

    public void mountResource(String path, ResourceReference resourceReference) {
        mountSharedResource(path, resourceReference.getSharedResourceKey());
    }

    /**
     * Mount a resource.
     *
     * @param path     The path of the resource.
     * @param resource The resource itself
     */
    public void mountResource(String path, final Resource resource) {
        mountResource(path, new ResourceReference(path) {
            @Override
            protected Resource newResource() {
                return resource;
            }
        });
    }

    public void mountPage(Class<? extends Page> pageClass) {
        String url = "/" + pageClass.getSimpleName().replaceFirst("Page", "").toLowerCase(Locale.ENGLISH) + ".html";
        mountPage(url, pageClass);
    }

    private void mountPage(String url, Class<? extends Page> pageClass) {
        unmount(url);   // un-mount first (in case of re-mounting)
        mountBookmarkablePage(url, pageClass);
    }

    @Override
    public RequestCycle newRequestCycle(Request request, Response response) {
        return new ArtifactoryRequestCycle(this, (WebRequest) request, response);
    }

    public void updateLogo() {
        this.logoModifyTime = new File(ContextHelper.get().getArtifactoryHome().getLogoDir(), "logo").lastModified();
    }

    @Override
    public Class<? extends BasePage> getHomePage() {
        return HomePage.class;
    }

    @Override
    public String getConfigurationType() {
        //Init the modes from the constants if needed
        if (modes.isEmpty()) {
            // use configuration from the servlet context since properties are not bound to the thread when this method is called
            ArtifactoryHome artifactoryHome = getArtifactoryContext().getArtifactoryHome();
            ArtifactorySystemProperties artifactorySystemProperties = artifactoryHome.getArtifactoryProperties();
            if (Boolean.parseBoolean(artifactorySystemProperties.getProperty(ConstantValues.dev))) {
                modes.add(ConstantValues.dev);
            }
            if (Boolean.parseBoolean(artifactorySystemProperties.getProperty(ConstantValues.test))) {
                modes.add(ConstantValues.test);
            }
            if (Boolean.parseBoolean(artifactorySystemProperties.getProperty(ConstantValues.qa))) {
                modes.add(ConstantValues.qa);
            }
        }
        if (modes.contains(ConstantValues.dev)) {
            return DEVELOPMENT;
        } else {
            return super.getConfigurationType();
        }
    }

    public CentralConfigService getCentralConfig() {
        return centralConfig;
    }

    public CompoundAuthorizationStrategy getAuthorizationStrategy() {
        IAuthorizationStrategy authorizationStrategy = getSecuritySettings().getAuthorizationStrategy();
        if (!(authorizationStrategy instanceof CompoundAuthorizationStrategy)) {
            throw new IllegalStateException(
                    "Unexpected authorization strategy: " + authorizationStrategy.getClass());
        }
        return (CompoundAuthorizationStrategy) authorizationStrategy;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public SiteMap getSiteMap() {
        return siteMap;
    }

    public String getSharedResourcesPath() {
        return sharedResourcesPath;
    }

    public boolean isLogoExists() {
        return logoModifyTime != 0;
    }

    public long getLogoModifyTime() {
        return logoModifyTime;
    }

    boolean isDevelopmentMode() {
        return DEVELOPMENT.equals(getConfigurationType());
    }

    @Override
    protected IRequestCycleProcessor newRequestCycleProcessor() {
        return new IgnoreAjaxUnfoundComponentRequestCycleProcessor();
    }

    @Override
    protected void onDestroy() {
        deleteUploadsFolder();
        super.onDestroy();
    }

    @Override
    protected Class<? extends AuthenticatedWebSession> getWebSessionClass() {
        return ArtifactoryWebSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return LoginPage.class;
    }

    @Override
    protected WebRequest newWebRequest(HttpServletRequest servletRequest) {
        return new UploadWebRequest(servletRequest);
    }

    @Override
    protected WebResponse newWebResponse(HttpServletResponse servletResponse) {
        return (getRequestCycleSettings().getBufferResponse() ? new EofAwareBufferedWebResponse(servletResponse) :
                new WebResponse(servletResponse));
    }

    @Override
    protected void init() {
        setupSpring();

        // bind context
        ArtifactoryContext originalContext = ContextHelper.get();
        boolean ctxAlreadyBound = originalContext != null;
        ArtifactoryContext context = getArtifactoryContext();
        if (!ctxAlreadyBound) {
            ArtifactoryContextThreadBinder.bind(context);
        } else {
            if (context != originalContext) {
                throw new IllegalStateException(
                        "Initialization of Wicket Application with a Spring context " + context +
                                " different from the Thread bound one " + originalContext);
            }
        }

        boolean artifactoryHomeAlreadyBound = ArtifactoryHome.isBound();
        if (!artifactoryHomeAlreadyBound) {
            ArtifactoryHome.bind(context.getArtifactoryHome());
        }

        super.init();

        try {
            doInit();
        } finally {
            // unbind context
            if (!ctxAlreadyBound) {
                ArtifactoryContextThreadBinder.unbind();
            }
            if (!artifactoryHomeAlreadyBound) {
                ArtifactoryHome.unbind();
            }
        }
    }

    /**
     * Rebuild the site map and re-mount the pages.
     */
    public void rebuildSiteMap() {
        buildSiteMap();
        mountPages();
    }

    private void setupSpring() {
        addComponentInstantiationListener(new ArtifactorySpringComponentInjector(this));
        inject(this);
    }

    private void mountLogo() {
        mountResource("/logo", new LogoResource(ContextHelper.get().getArtifactoryHome()));
    }

    private void notifyBootstrapListeners() {
        ArtifactoryContext artifactoryContext = getArtifactoryContext();
        Map<String, BootstrapListener> beanMap = artifactoryContext.beansForType(BootstrapListener.class);
        for (BootstrapListener listener : beanMap.values()) {
            listener.onApplicationInit();
        }
    }

    private ArtifactoryContext getArtifactoryContext() {
        return (ArtifactoryContext) getServletContext()
                .getAttribute(ArtifactoryContext.APPLICATION_CONTEXT_KEY);
    }

    /**
     * Delete the upload folder (in case we were not shut down cleanly)
     */
    private void deleteUploadsFolder() {
        ArtifactoryHome artifactoryHome = getArtifactoryContext().getArtifactoryHome();
        File tmpUploadsDir = artifactoryHome.getTmpUploadsDir();
        if (tmpUploadsDir.exists()) {
            try {
                FileUtils.cleanDirectory(tmpUploadsDir);
            } catch (IOException ignore) {
                log.warn("Failed to delete the upload directory.");
            }
        }
    }

    private void setupListeners() {
        // wrap the unauthorizedComponentInstantiation listener so that we can discard repoPath
        // attributes from the request when needing to login
        ISecuritySettings securitySettings = getSecuritySettings();
        IUnauthorizedComponentInstantiationListener orig =
                securitySettings.getUnauthorizedComponentInstantiationListener();

        securitySettings.setUnauthorizedComponentInstantiationListener(
                new RepoBrowsingAwareUnauthorizedComponentInstantiationListener(orig));
    }

    private void mountPages() {
        AnnotatedMountList annotatedMountList = new AnnotatedMountScanner().scanPackage(
                "org.artifactory.webapp.wicket.page.build.page");
        // first make sure to un-mount all paths (required if we rebuild mounting)
        for (IRequestTargetUrlCodingStrategy strategy : annotatedMountList) {
            unmount(strategy.getMountPath());
        }
        annotatedMountList.mount(ArtifactoryApplication.get());

        mountPage(SimpleRepoBrowserPage.PATH, SimpleRepoBrowserPage.class);
        mountPage(ArtifactListPage.PATH, ArtifactListPage.class);

        // mount services
        mountPage("/service/logout", LogoutService.class);

        // mount general pages
        mountPage(InternalErrorPage.class);
        mountPage(AccessDeniedPage.class);
        mountPage(PageExpiredErrorPage.class);

        mountPage(LoginPage.class);
        mountPage(LogoutPage.class);
        mountPage(ProfilePage.class);
        mountPage(ResetPasswordPage.class);
        mountPage(ForgotPasswordPage.class);

        mountPage("/search/artifact", ArtifactSearchPage.class);

        for (MenuNode pageNode : siteMap.getPages()) {
            if (pageNode.getMountUrl() != null) {
                mountPage(pageNode.getMountUrl(), pageNode.getPageClass());
            } else {
                mountPage(pageNode.getPageClass());
            }
        }
    }

    /**
     * Mount all resources to version-sensitive path, Keep in cache for 10 years.
     */
    private void mountResources() {
        final CompoundVersionDetails details = ContextHelper.get().getArtifactoryHome().getRunningVersionDetails();
        sharedResourcesPath = details.getVersion() + "/resources/";
        mount(new VersionedSharedResourceUrlCodingStrategy(sharedResourcesPath, Duration.days(365)));
    }

    private void buildSiteMap() {
        SiteMapBuilder builder = newSiteMapBuilder();
        builder.buildSiteMap();
        builder.cachePageNodes();
        siteMap = builder.getSiteMap();
    }

    private static void inject(Object injectable) {
        InjectorHolder.getInjector().inject(injectable);
    }

    protected void doInit() {
        setup();

        buildSiteMap();
        mountPages();
        mountResources();
        mountLogo();

        deleteUploadsFolder();
        notifyBootstrapListeners();
        updateLogo();
    }

    protected void setup() {
        setupListeners();

        // look for pages at the root of the web-app
        IResourceSettings resourceSettings = getResourceSettings();
        resourceSettings.addResourceFolder("");

        // ResourcePackage resources are locale insensitive
        NoLocaleResourceStreamLocator locator = new NoLocaleResourceStreamLocator();
        locator.addNoLocaleClass(ResourcePackage.class);
        resourceSettings.setResourceStreamLocator(locator);

        // add the addons authorization strategy
        AddonsAuthorizationStrategy addonsAuthorizationStrategy = new AddonsAuthorizationStrategy();
        inject(addonsAuthorizationStrategy);
        getAuthorizationStrategy().add(addonsAuthorizationStrategy);

        // increase request timeout to support long running transactions
        IRequestCycleSettings requestCycleSettings = getRequestCycleSettings();
        requestCycleSettings.setTimeout(Duration.hours(5));

        // set error pages
        IApplicationSettings applicationSettings = getApplicationSettings();
        applicationSettings.setPageExpiredErrorPage(PageExpiredErrorPage.class);
        applicationSettings.setAccessDeniedPage(AccessDeniedPage.class);
        applicationSettings.setInternalErrorPage(InternalErrorPage.class);

        // markup settings
        IMarkupSettings markupSettings = getMarkupSettings();
        markupSettings.setDefaultMarkupEncoding("UTF-8");
        markupSettings.setCompressWhitespace(true);
        markupSettings.setStripComments(true);
        markupSettings.setStripWicketTags(true);
        markupSettings.setStripXmlDeclarationFromOutput(true);

        //QA settings
        if (modes.contains(ConstantValues.qa)) {
            addComponentInstantiationListener(new AddWicketPathListener());
        }
    }

    protected SiteMapBuilder newSiteMapBuilder() {
        SiteMapBuilder builder = new ArtifactorySiteMapBuilder();
        inject(builder);
        return builder;
    }
}