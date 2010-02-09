package org.artifactory.webapp.wicket.common.component.links;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupException;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.common.Titled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;

/**
 * @author Yoav Aharoni
 */
public class BaseTitledLink extends AbstractLink implements Titled {
    private static final Logger LOG = LoggerFactory.getLogger(BaseTitledLink.class);
    private boolean wasOpenCloseTag;

    public BaseTitledLink(String id) {
        this(id, id);
    }

    public BaseTitledLink(String id, IModel titleModel) {
        super(id, titleModel);
    }

    public BaseTitledLink(String id, String title) {
        super(id);
        setModel(new Model(title));
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        if (tag.isOpenClose()) {
            wasOpenCloseTag = true;
            tag.setType(XmlTag.OPEN);
        }

        super.onComponentTag(tag);

        // senity
        if (!isTagName(tag, "a") && !isTagName(tag, "button")) {
            throw new RuntimeException(getClass().getName() + " can only be used with <a> or <button> tags.");
        }

        tag.put("class", getCssClass(tag));

        if (isEnabled()) {
            // is a <a> or other tag
            if (isTagName(tag, "a")) {
                tag.put("href", "#");
            }

            tag.put("onmousedown", "this.origCssClass=this.className; this.className+=' titled-down';");
            tag.put("onmouseout", "if (this.origCssClass) this.className=this.origCssClass;");
        } else {
            tag.put("onclick", "return false;");
        }
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        replaceBody(markupStream, openTag, "<span class='button-center'><span class='button-left'><span class='button-right'>" + getTitle() + "</span></span></span>");
    }

    public String getTitle() {
        try {
            if (getModelObjectAsString() == null) {
                LOG.error(getClass().getSimpleName()
                        + " title model is null, using id instead.");

                return "??" + getId() + "??";
            }

            return getModelObjectAsString();

        } catch (MissingResourceException e) {
            LOG.error(getClass().getSimpleName()
                    + " can't find text resource, using id instead { " + e.getMessage() + " }.");

            return "??" + getId() + "??";
        }
    }

    protected String getCssClass(ComponentTag tag) {
        String oldCssClass = StringUtils.defaultString(tag.getAttributes().getString("class"));
        if (!isEnabled()) {
            return oldCssClass + " button " + oldCssClass + "-disabled button-disabled";
        }
        return oldCssClass + " button";
    }

    private boolean isTagName(ComponentTag tag, String tagName) {
        return tagName.equalsIgnoreCase(tag.getName());
    }


    protected void replaceBody(MarkupStream markupStream, ComponentTag openTag, String html) {
        replaceComponentTagBody(markupStream, openTag, html);

        if (!wasOpenCloseTag) {
            markupStream.skipRawMarkup();
            if (!markupStream.get().closes(openTag)) {
                throw new MarkupException("close tag not found for tag: " + openTag.toString() +
                        ". Component: " + toString());
            }
        }
    }
}

