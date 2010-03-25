package org.artifactory.common.wicket.component.label.highlighter;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.artifactory.api.mime.ContentType;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Yoav Aharoni
 */
public enum Syntax {
    ActionScript3("actionscript3", "shBrushAS3.js"),
    Bash("shell", "shBrushBash.js"),
    ColdFusion("coldfusion", "shBrushColdFusion.js"),
    CSharp("csharp", "shBrushCSharp.js"),
    CPP("cpp", "shBrushCpp.js"),
    CSS("css", "shBrushCss.js"),
    Delphi("delphi", "shBrushDelphi.js"),
    Pascal("pascal", "shBrushDelphi.js"),
    Diff("diff", "shBrushDiff.js"), // or patch
    Erlang("erlang", "shBrushErlang.js"),
    Groovy("groovy", "shBrushGroovy.js"),
    JavaScript("javascript", "shBrushJScript.js"),
    Java("java", "patchedBrushJava.js"),
    JavaFX("javafx", "shBrushJavaFX.js"),
    Perl("perl", "shBrushPerl.js"),
    PHP("php", "shBrushPhp.js"),
    PlainText("plain", "shBrushPlain.js"),
    PowerShell("powershell", "shBrushPowerShell.js"),
    Python("python", "shBrushPython.js"),
    Ruby("ruby", "shBrushRuby.js"),
    Scala("scala", "shBrushScala.js"),
    SQL("sql", "shBrushSql.js"),
    VisualBasic("vb", "shBrushVb.js"),
    XML("xml", "shBrushXml.js");

    private static final Map<String, Syntax> mimeTypes = new HashMap<String, Syntax>();

    private String brush;
    private ResourceReference jsReference;

    private Syntax(String brush, String jsFile) {
        this.brush = brush;
        final String cssPath = format("resources/scripts/%s", jsFile);
        this.jsReference = new JavascriptResourceReference(Syntax.class, cssPath);
    }

    String getBrush() {
        return brush;
    }

    ResourceReference getJsReference() {
        return jsReference;
    }

    static {
        // XML
        mimeTypes.put("text/xml", XML);
        mimeTypes.put("text/html", XML);
        mimeTypes.put("text/xsl", XML);
        mimeTypes.put("text/xslt", XML);
        mimeTypes.put("application/xml", XML);
        mimeTypes.put("application/xhtml+xml", XML);
        mimeTypes.put("application/x-maven-pom+xml", XML);
        mimeTypes.put("application/xml-schema", XML);
        mimeTypes.put("application/xml-external-parsed-entity", XML);
        mimeTypes.put("application/x-java-jnlp-file", XML);
        mimeTypes.put("application/x-ivy+xml", XML);

        // java
        mimeTypes.put("text/x-java-source", Java);

        // groovy
        mimeTypes.put("text/x-groovy-source", Groovy);

        // cpp
        mimeTypes.put("text/x-c", CPP);
        mimeTypes.put("text/x-c-source", CPP);

        mimeTypes.put(ContentType.css.getMimeType(), CSS);

        // add all content types that represent xml files
        for (ContentType contentType : ContentType.values()) {
            if (contentType.isXml()) {
                mimeTypes.put(contentType.getMimeEntry().getMimeType(), XML);
            }
        }
    }

    public static Syntax fromMimeType(String mimeType) {
        return mimeTypes.get(mimeType);
    }

    public static Syntax fromContentType(ContentType contentType) {
        return fromMimeType(contentType.getMimeType());
    }
}
