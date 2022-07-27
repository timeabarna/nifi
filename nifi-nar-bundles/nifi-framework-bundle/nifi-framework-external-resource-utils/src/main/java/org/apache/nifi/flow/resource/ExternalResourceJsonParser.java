package org.apache.nifi.flow.resource;

import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

public class ExternalResourceJsonParser implements ExternalResourceParser {
    private final String jsonPathIdentifierForFileList;
    private final String jsonPathIdentifierForLocation;
    private final String jsonPathIdentifierForLastModified;
    private final String jsonPathIdentifierForDirectory;
    private final DateTimeFormatter formatter;

    public ExternalResourceJsonParser(final ExternalResourceParserConfiguration configuration) {
        jsonPathIdentifierForFileList = configuration.getFileListIdentifier();
        jsonPathIdentifierForLocation = configuration.getLocationIdentifier();
        jsonPathIdentifierForLastModified = configuration.getLastModificationIdentifier();
        jsonPathIdentifierForDirectory = configuration.getDirectoryIdentifier();
        formatter = DateTimeFormatter.ofPattern(configuration.getDateTimeFormat());
    }

    @Override
    public Collection<ExternalResourceParserResult> parseResponse(String response, String url) throws ParserConfigurationException, XPathExpressionException, IOException, SAXException {
        final Collection<ExternalResourceParserResult> result = new ArrayList<>();
        final JSONArray nodeList = JsonPath.parse(response).read(jsonPathIdentifierForFileList);
        for (Object node : nodeList) {
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

    private boolean isDirectory(final Object node) {
        final JSONArray directory = JsonPath.parse(node).read(jsonPathIdentifierForDirectory);
        return !directory.isEmpty();
    }

    private ExternalResourceParserResult createParserResult(final String location, final long lastModified, final String path, final boolean isDirectory) {
        return new ExternalResourceParserResult(location, lastModified, path, isDirectory);
    }

    private String parseLocation(final Object node) {
        final JSONArray location = JsonPath.parse(node).read(jsonPathIdentifierForLocation);
        return location.get(0).toString();
    }

    private long parseLastModified(final Object node) {
        final JSONArray time = JsonPath.parse(node).read(jsonPathIdentifierForLastModified);
        return LocalDateTime.parse(time.get(0).toString(), formatter).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
    }
}
