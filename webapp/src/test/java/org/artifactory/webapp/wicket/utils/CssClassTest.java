package org.artifactory.webapp.wicket.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the ItemCssClass.
 *
 * @author Yossi Shaul
 */
@Test
public class CssClassTest {

    public void pomCss() {
        String path = "/a/path/to/somewhere/my.pom";
        CssClass cssClass = CssClass.getFileCssClass(path);
        Assert.assertEquals(cssClass.cssClass(), "pom", "Expected pom css class");
    }

    public void jarCss() {
        String path = "my.jar";
        CssClass cssClass = CssClass.getFileCssClass(path);
        Assert.assertEquals(cssClass.cssClass(), "jar", "Expected jar css class");
    }

    public void rarCss() {
        String path = "my.jar";
        CssClass cssClass = CssClass.getFileCssClass(path);
        Assert.assertEquals(cssClass.cssClass(), "jar", "Expected jar css class");
    }

    public void zipAsDocCss() {
        String path = "a/path/to/my.zip";
        CssClass cssClass = CssClass.getFileCssClass(path);
        Assert.assertEquals(cssClass.cssClass(), "doc", "Expected jar css class");
    }

    public void parentCss() {
        String path = "..";
        CssClass cssClass = CssClass.getFileCssClass(path);
        Assert.assertEquals(cssClass.cssClass(), "parent", "Expected parent css class");
    }

    public void genericCss() {
        String path = "/none/of/the/standard.par";
        CssClass cssClass = CssClass.getFileCssClass(path);
        Assert.assertEquals(cssClass.cssClass(), "doc", "Expected doc css class");
    }

    public void genericCss2() {
        String path = "/none/of/the/standard.pom.tot";
        CssClass cssClass = CssClass.getFileCssClass(path);
        Assert.assertEquals(cssClass.cssClass(), "doc", "Expected doc css class");
    }

}
