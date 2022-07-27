package org.apache.nifi.flow.resource;

import com.jayway.jsonpath.InvalidJsonException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalResourceJsonParserTest extends AbstractHttpsExternalResourceProviderTest {
    private static ExternalResourceParserConfiguration parserConfiguration;

    @BeforeAll
    static void setUp() {
        parserConfiguration = new ExternalResourceParserConfiguration("$[?(@.type)]",
                "$..name",
                "$..['last modified']",
                "$.[?(@.type=~ /.*d-directory/)]",
                "yyy-MM-dd HH:mm");
    }

    @Test
    void parseEmptyResponse() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final String emptyJson = "[]";

        final ExternalResourceJsonParser parser = new ExternalResourceJsonParser(parserConfiguration);
        final Collection<ExternalResourceParserResult> expected = Collections.emptyList();

        final Collection<ExternalResourceParserResult> result = parser.parseResponse(emptyJson, "https://test");

        assertEquals(expected, result);
    }

    @Test
    void parseResponseWithParentItemOnly() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final String parentOnlyJson = "[{\"name\": \"Parent Directory\"}]";

        final ExternalResourceJsonParser parser = new ExternalResourceJsonParser(parserConfiguration);
        final Collection<ExternalResourceParserResult> expected = Collections.emptyList();

        final Collection<ExternalResourceParserResult> result = parser.parseResponse(parentOnlyJson, "https://test");

        assertEquals(expected, result);
    }

    @Test
    void parseResponseWithFileItemsOnly() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final String fileOnlyJson = "[" +
                "{\"name\": \"Parent Directory\"}," +
                "{\"name\": \"file1\"," +
                "\"last modified\" : \"2021-05-17 21:09\"," +
                "\"size\" : \"size\"," +
                "\"type\" : \"blob\"}" +
                "]";

        final ExternalResourceJsonParser parser = new ExternalResourceJsonParser(parserConfiguration);

        final Collection<ExternalResourceParserResult> expected = new ArrayList<>();
        final ExternalResourceParserResult file1 = new ExternalResourceParserResult("file1", 1621285740000L, "https://test", false);
        expected.add(file1);

        final Collection<ExternalResourceParserResult> result = parser.parseResponse(fileOnlyJson, "https://test");

        assertEquals(expected, result);
    }

    @Test
    void parseResponseWithDirectoryItemsOnly() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final String directoryOnlyJson = "[" +
                "{\"name\": \"Parent Directory\"}," +
                "{\"name\": \"dir1/\"," +
                "\"last modified\" : \"-\"," +
                "\"size\" : \"size\"," +
                "\"type\" : \"g.com.d-directory\"}" +
                "]";

        final ExternalResourceJsonParser parser = new ExternalResourceJsonParser(parserConfiguration);

        final Collection<ExternalResourceParserResult> expected = new ArrayList<>();
        final ExternalResourceParserResult dir1 = new ExternalResourceParserResult("dir1/", 0, "https://test", true);
        expected.add(dir1);

        final Collection<ExternalResourceParserResult> result = parser.parseResponse(directoryOnlyJson, "https://test");

        assertEquals(expected, result);
    }

    @Test
    void parseResponseWithMixedItems() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final String mixedJson = "[" +
                "{\"name\": \"Parent Directory\"}," +
                "{\"name\": \"dir1/\"," +
                "\"last modified\" : \"-\"," +
                "\"size\" : \"size\"," +
                "\"type\" : \"g.com.d-directory\"}," +
                "{\"name\": \"file1\"," +
                "\"last modified\" : \"2021-05-17 21:09\"," +
                "\"size\" : \"size\"," +
                "\"type\" : \"blob\"}" +
                "]";

        final ExternalResourceJsonParser parser = new ExternalResourceJsonParser(parserConfiguration);

        final Collection<ExternalResourceParserResult> expected = new ArrayList<>();
        final ExternalResourceParserResult dir1 = new ExternalResourceParserResult("dir1/", 0, "https://test", true);
        final ExternalResourceParserResult file1 = new ExternalResourceParserResult("file1", 1621285740000L, "https://test", false);
        expected.add(dir1);
        expected.add(file1);

        final Collection<ExternalResourceParserResult> result = parser.parseResponse(mixedJson, "https://test");

        assertEquals(expected, result);
    }

    @Test
    void parseResponseWithInvalidJson() {
        final String invalidJson = "{[]}";

        final ExternalResourceJsonParser parser = new ExternalResourceJsonParser(parserConfiguration);

        assertThrows(InvalidJsonException.class, () -> parser.parseResponse(invalidJson, "https://test"));
    }

    @Test
    void parseResponseWithIncorrectDate() {
        final String jsonWithIncorrectDate = "[" +
                "{\"name\": \"Parent Directory\"}," +
                "{\"name\": \"file1\"," +
                "\"last modified\" : \"-\"," +
                "\"size\" : \"size\"," +
                "\"type\" : \"blob\"}" +
                "]";

        final ExternalResourceJsonParser parser = new ExternalResourceJsonParser(parserConfiguration);

        assertThrows(DateTimeParseException.class, () -> parser.parseResponse(jsonWithIncorrectDate, "https://test"));
    }

    @Test
    void parseResponseWithIncorrectDateFormat() {
        final String jsonWithIncorrectDateFormat = "[" +
                "{\"name\": \"Parent Directory\"}," +
                "{\"name\": \"file1\"," +
                "\"last modified\" : \"2021.05.17 21:09\"," +
                "\"size\" : \"size\"," +
                "\"type\" : \"blob\"}" +
                "]";

        final ExternalResourceJsonParser parser = new ExternalResourceJsonParser(parserConfiguration);

        assertThrows(DateTimeParseException.class, () -> parser.parseResponse(jsonWithIncorrectDateFormat, "https://test"));
    }
}