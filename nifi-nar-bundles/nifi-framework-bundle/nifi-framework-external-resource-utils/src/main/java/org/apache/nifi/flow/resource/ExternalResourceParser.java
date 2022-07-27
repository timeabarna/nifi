package org.apache.nifi.flow.resource;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Collection;
import org.xml.sax.SAXException;

public interface ExternalResourceParser {
    Collection<ExternalResourceParserResult> parseResponse(final String response, final String url) throws ParserConfigurationException, XPathExpressionException, IOException, SAXException;
}
