/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.flow.resource;

import org.apache.nifi.xml.processing.parsers.StandardDocumentProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

public class ExternalResourceXmlParser implements ExternalResourceParser {
    private final XPathExpression xPathIdentifierForFileList;
    private final XPathExpression xPathIdentifierForLocation;
    private final XPathExpression xPathIdentifierForLastModified;
    private final XPathExpression xPathIdentifierForDirectory;
    private final DateTimeFormatter formatter;

    public ExternalResourceXmlParser(final ExternalResourceParserConfiguration configuration) throws XPathExpressionException {
        final XPath xPath = XPathFactory.newInstance().newXPath();
        xPathIdentifierForFileList = xPath.compile(configuration.getFileListIdentifier());
        xPathIdentifierForLocation = xPath.compile(configuration.getLocationIdentifier());
        xPathIdentifierForLastModified = xPath.compile(configuration.getLastModificationIdentifier());
        xPathIdentifierForDirectory = xPath.compile(configuration.getDirectoryIdentifier());
        formatter = DateTimeFormatter.ofPattern(configuration.getDateTimeFormat());
    }

    public Collection<ExternalResourceParserResult> parseResponse(final String response, final String url) throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        final Collection<ExternalResourceParserResult> result = new ArrayList<>();
        final NodeList nodeList = gatherResourceNodeList(response);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (isDirectory(node)) {
                final ExternalResourceParserResult parserResult = createParserResult(parseLocation(node), 0, url, true);
                result.add(parserResult);
            } else {
                final ExternalResourceParserResult parserResult = createParserResult(parseLocation(node), parseLastModified(node), url, false);
                result.add(parserResult);
            }
        }
        return result;
    }

    private NodeList gatherResourceNodeList(final String response) throws XPathExpressionException {
        final Document doc = new StandardDocumentProvider().parse(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
        return (NodeList) xPathIdentifierForFileList.evaluate(doc, XPathConstants.NODESET);
    }

    private boolean isDirectory(final Node node) throws XPathExpressionException {
        return !xPathIdentifierForDirectory.evaluate(node).isEmpty();
    }

    private ExternalResourceParserResult createParserResult(final String location, final long lastModified, final String path, final boolean isDirectory) {
        return new ExternalResourceParserResult(location, lastModified, path, isDirectory);
    }

    private String parseLocation(final Node node) throws XPathExpressionException {
            return xPathIdentifierForLocation.evaluate(node);
    }

    private long parseLastModified(final Object node) throws XPathExpressionException {
        final String time = xPathIdentifierForLastModified.evaluate(node);
        return LocalDateTime.parse(time, formatter).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
    }
}
