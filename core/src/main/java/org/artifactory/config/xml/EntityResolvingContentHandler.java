/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.config.xml;

import org.apache.log4j.Logger;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class EntityResolvingContentHandler extends DefaultHandler {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(EntityResolvingContentHandler.class);
    private ContentHandler handler;


    public EntityResolvingContentHandler(ContentHandler handler) {
        this.handler = handler;
    }

    public InputSource resolveEntity(String publicId, String systemId)
            throws IOException, SAXException {
        InputSource source = super.resolveEntity(publicId, systemId);
        if (source == null) {
            InputStream stream = this.getClass().getResourceAsStream("xml/characters.ent");
            if (stream != null) {
                source = new InputSource(stream);
            }
        }
        return source;
    }

    public void setDocumentLocator(Locator locator) {
        handler.setDocumentLocator(locator);
    }

    public void startDocument() throws SAXException {
        handler.startDocument();
    }

    public void endDocument() throws SAXException {
        handler.endDocument();
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        handler.startPrefixMapping(prefix, uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        handler.endPrefixMapping(prefix);
    }

    public void startElement(String uri, String localName, String qName, Attributes atts)
            throws SAXException {
        handler.startElement(uri, localName, qName, atts);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        handler.endElement(uri, localName, qName);
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        handler.characters(ch, start, length);
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        handler.ignorableWhitespace(ch, start, length);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        handler.processingInstruction(target, data);
    }

    public void skippedEntity(String name) throws SAXException {
        handler.skippedEntity(name);
    }

    public void fatalError(SAXParseException e) throws SAXException {
        //Temp hack to avoid broken plexus poms (ver 1.0.4 & 1.0.5)
        LOGGER.warn("Received the following error during xml parsing: '" + e.getMessage() + "'.");
        //No 'super.fatalError(e)'!;
    }
}