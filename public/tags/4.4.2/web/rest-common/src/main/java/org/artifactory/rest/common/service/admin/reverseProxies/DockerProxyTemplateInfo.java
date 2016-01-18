package org.artifactory.rest.common.service.admin.reverseProxies;

/**
 * @author Shay Yaakov
 */
public class DockerProxyTemplateInfo {

    private String template;

    public DockerProxyTemplateInfo(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
}
