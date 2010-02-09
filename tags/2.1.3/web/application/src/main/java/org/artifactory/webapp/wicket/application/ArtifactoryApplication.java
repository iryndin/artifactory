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

package org.artifactory.webapp.wicket.application;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.*;
import org.apache.wicket.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.authorization.strategies.CompoundAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadWebRequest;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.settings.*;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.artifactory.addon.BootstrapListener;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.common.wicket.application.NoLocaleResourceStreamLocator;
import org.artifactory.common.wicket.application.SetPathMarkupIdOnBeforeRenderListener;
import org.artifactory.common.wicket.component.panel.sidemenu.SiteMapAware;
import org.artifactory.common.wicket.contributor.ResourcePackage;
import org.artifactory.common.wicket.model.sitemap.MenuNode;
import org.artifactory.common.wicket.model.sitemap.SiteMap;
import org.artifactory.common.wicket.model.sitemap.SiteMapBuilder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.SpringConfigResourceLoader;
import org.artifactory.version.CompoundVersionDetails;
import org.artifactory.webapp.spring.ArtifactorySpringComponentInjector;
import org.artifactory.webapp.wicket.application.sitemap.ArtifactorySiteMapBuilder;
import org.artifactory.webapp.wicket.page.base.BasePage;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.SimpleRepoBrowserPage;
import org.artifactory.webapp.wicket.page.error.AccessDeniedPage;
import org.artifactory.webapp.wicket.page.error.InternalErrorPage;
import org.artifactory.webapp.wicket.page.error.PageExpiredErrorPage;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.security.login.LoginPage;
import org.artifactory.webapp.wicket.page.security.login.LogoutPage;
import org.artifactory.webapp.wicket.page.security.login.forgot.ForgotPasswordPage;
import org.artifactory.webapp.wicket.page.security.login.reset.ResetPasswordPage;
import org.artifactory.webapp.wicket.page.security.profile.ProfilePage;
import org.artifactory.webapp.wicket.resource.LogoResource;
import org.artifactory.webapp.wicket.service.authentication.LogoutService;
import org.slf4j.Logger;
import org.wicketstuff.annotation.scan.AnnotatedMountScanner;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryApplication extends AuthenticatedWebApplication implements SiteMapAware {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryApplication.class);
    private static final Locale LOCALE = Locale.US;

    @SpringBean
    private CentralConfigService centralConfig;

    @SpringBean
    private RepositoryService repositoryService;

    private SiteMap siteMap;

    private final EnumSet<ConstantValues> modes =
            Sets.newEnumSet(Collections.<ConstantValues>emptySet(), ConstantValues.class);
    private String sharedResourcesPath;

    public static ArtifactoryApplication get() {
        return (ArtifactoryApplication) Application.get();
    }

    public CentralConfigService getCentralConfig() {
        return centralConfig;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public SiteMap getSiteMap() {
        return siteMap;
    }

    @Override
    protected void init() {
        super.init();
        ArtifactoryHome artifactoryHome = getArtifactoryContext().getArtifactoryHome();
        ArtifactorySystemProperties.bind(artifactoryHome.getArtifactoryProperties());
        setup();
        inject(this);

        buildSiteMap();
        mountPages();
        new AnnotatedMountScanner().scanPackage("org.artifactory.webapp.wicket.page.build").
                mount(ArtifactoryApplication.get());
        mountResources(artifactoryHome.getRunningVersionDetails());
        mountLogo(artifactoryHome);
        deleteUploadsFolder();

        notifyBootstrapListeners();
        ArtifactorySystemProperties.unbind();
    }

    private void mountLogo(ArtifactoryHome artifactoryHome) {
        mountResource("/logo", new LogoResource(artifactoryHome));
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

    private void notifyBootstrapListeners() {
        ArtifactoryContext artifactoryContext = getArtifactoryContext();
        Map<String, BootstrapListener> beanMap = artifactoryContext.beansForType(BootstrapListener.class);
        for (BootstrapListener listener : beanMap.values()) {
            listener.onApplicationInit();
        }
    }

    private ArtifactoryContext getArtifactoryContext() {
        return (ArtifactoryContext) getServletContext()
                .getAttribute(SpringConfigResourceLoader.APPLICATION_CONTEXT_KEY);
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
            addComponentOnBeforeRenderListener(new SetPathMarkupIdOnBeforeRenderListener());
        }
    }

    public CompoundAuthorizationStrategy getAuthorizationStrategy() {
        IAuthorizationStrategy authorizationStrategy = getSecuritySettings().getAuthorizationStrategy();
        if (!(authorizationStrategy instanceof CompoundAuthorizationStrategy)) {
            throw new IllegalStateException(
                    "Unexpected autorization strategy: " + authorizationStrategy.getClass());
        }
        return (CompoundAuthorizationStrategy) authorizationStrategy;
    }

    boolean isDevelopmentMode() {
        return DEVELOPMENT.equals(getConfigurationType());
    }

    public void mountPage(Class<? extends Page> pageClass) {
        String url = "/" + pageClass.getSimpleName().replaceFirst("Page", "").toLowerCase(Locale.ENGLISH) + ".html";
        mountBookmarkablePage(url, pageClass);
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

    @Override
    protected IRequestCycleProcessor newRequestCycleProcessor() {
        return new IgnoreAjaxUnfoundComponentRequestCycleProcessor();
    }

    protected SiteMapBuilder newSiteMapBuilder() {
        SiteMapBuilder builder = new ArtifactorySiteMapBuilder();
        inject(builder);
        return builder;
    }

    @Override
    public Class<? extends BasePage> getHomePage() {
        return HomePage.class;
    }

    @Override
    public RequestCycle newRequestCycle(Request request, Response response) {
        return new ArtifactoryRequestCycle(this, (WebRequest) request, response);
    }

    public String getSharedResourcesPath() {
        return sharedResourcesPath;
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
        //Add the spring injector
        addComponentInstantiationListener(new ArtifactorySpringComponentInjector(this));
        // wrap the unauthorizedComponentInstantiation listener so that we can discard repoPath
        // attributes from the request when needing to login
        ISecuritySettings securitySettings = getSecuritySettings();
        IUnauthorizedComponentInstantiationListener orig =
                securitySettings.getUnauthorizedComponentInstantiationListener();

        securitySettings.setUnauthorizedComponentInstantiationListener(
                new RepoBrowsingAwareUnauthorizedComponentInstantiationListener(orig));
    }

    private void mountPages() {
        mountBookmarkablePage(SimpleRepoBrowserPage.PATH, SimpleRepoBrowserPage.class);

        // mount services
        mountBookmarkablePage("/service/logout", LogoutService.class);

        // mount general pages
        mountPage(InternalErrorPage.class);
        mountPage(AccessDeniedPage.class);
        mountPage(PageExpiredErrorPage.class);

        mountPage(LoginPage.class);
        mountPage(LogoutPage.class);
        mountPage(ProfilePage.class);
        mountPage(ResetPasswordPage.class);
        mountPage(ForgotPasswordPage.class);

        for (MenuNode pageNode : siteMap.getPages()) {
            if (pageNode.getMountUrl() != null) {
                mountBookmarkablePage(pageNode.getMountUrl(), pageNode.getPageClass());
            } else {
                mountPage(pageNode.getPageClass());
            }
        }
    }

    /**
     * Mount all resources to version-sensitive path, Keep in cache for 10 years.
     *
     * @param details
     */
    private void mountResources(CompoundVersionDetails details) {
        sharedResourcesPath = details.getVersion() + "/resources/";
        mount(new VersionedSharedResourceUrlCodingStrategy(sharedResourcesPath, Duration.days(365)));
    }

    private void buildSiteMap() {
        SiteMapBuilder builder = newSiteMapBuilder();
        builder.createSiteMap();
        builder.buildSiteMap();
        builder.cachePageNodes();
        siteMap = builder.getSiteMap();
    }

    private static void inject(Object injectable) {
        InjectorHolder.getInjector().inject(injectable);
    }
}