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

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.nifi.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HttpsExternalResourceProvider implements ExternalResourceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpsExternalResourceProvider.class);

    private static final String PROXY_USER = "proxy.user";
    private static final String PROXY_PASSWORD = "proxy.password";
    private static final String PROXY_SERVER = "proxy.server";
    private static final String PROXY_SERVER_PORT = "proxy.server.port";
    private static final String USER_NAME = "user.name";
    private static final String PASSWORD = "password";
    private static final String CONNECT_TIMEOUT = "connect.timeout";
    private static final String READ_TIMEOUT = "read.timeout";
    private static final String BASE_URL = "base.url";
    private static final String FILTER = "filter";
    private static final String NAR_LOCATION = "nar.location";
    private static final String FILE_LIST_IDENTIFIER = "file.list.identifier";
    private static final String LOCATION_IDENTIFIER = "location.identifier";
    private static final String LAST_MODIFICATION_IDENTIFIER = "last.modification.identifier";
    private static final String DIRECTORY_IDENTIFIER = "directory.identifier";
    private static final String DATE_TIME_FORMAT = "date.time.format";

    private static final long DEFAULT_CONNECTION_TIMEOUT = 15;
    private static final long DEFAULT_READ_TIMEOUT = 60;

    private volatile OkHttpClient client;
    private volatile String baseUrl;
    private volatile String filter;
    private volatile ExternalResourceProviderInitializationContext context;
    private volatile boolean initialized = false;
    private volatile String narLocation;
    private volatile ExternalResourceParserConfiguration parserConfiguration;

    @Override
    public void initialize(ExternalResourceProviderInitializationContext context) {
        final Map<String, String> properties = context.getProperties();
        final String baseUrl = properties.get(BASE_URL);
        final String narLocation = properties.get(NAR_LOCATION);
        final String filter = properties.get(FILTER);
        final String fileListIdentifier = properties.get(FILE_LIST_IDENTIFIER);
        final String locationIdentifier = properties.get(LOCATION_IDENTIFIER);
        final String lastModificationIdentifier = properties.get(LAST_MODIFICATION_IDENTIFIER);
        final String directoryIdentifier = properties.get(DIRECTORY_IDENTIFIER);
        final String dateTimeFormat = properties.get(DATE_TIME_FORMAT);

        if (baseUrl == null) {
            throw new IllegalArgumentException("Base URL is required.");
        }
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl;
        } else {
            this.baseUrl = baseUrl + "/";
        }

        if (filter == null) {
            throw new IllegalArgumentException("Filter is required.");
        }
        this.filter = filter;

        if (narLocation == null) {
            this.narLocation = "";
        } else if (narLocation.endsWith("/")) {
            this.narLocation = narLocation;
        } else {
            this.narLocation = narLocation + "/";
        }

        if (ObjectUtils.anyNull(fileListIdentifier, locationIdentifier, lastModificationIdentifier,
                directoryIdentifier, dateTimeFormat)) {
            throw new IllegalArgumentException("Date time format and all identifiers are required.");
        }

        this.parserConfiguration = new ExternalResourceParserConfiguration(fileListIdentifier,
                locationIdentifier,
                lastModificationIdentifier,
                directoryIdentifier,
                dateTimeFormat);
        this.client = createHttpClient(properties);
        this.context = context;
        this.initialized = true;
    }

    @Override
    public Collection<ExternalResourceDescriptor> listResources() throws IOException {
        if (!initialized) {
            throw new IllegalStateException("Provider is not initialized.");
        }

        final ExternalResourceXmlParser parser;
        Collection<ExternalResourceParserResult> availableResources;
        Collection<String> visitedLocations = new HashSet<>();
        Collection<ExternalResourceParserResult> conflicts = new ArrayList<>();
        Collection<ExternalResourceDescriptor> results = new ArrayList<>();

        try {
            parser = new ExternalResourceXmlParser(parserConfiguration);
            availableResources = collectResources(parser, normalizeURL(baseUrl));
            while (!availableResources.isEmpty()) {
                final Collection<ExternalResourceParserResult> resourcesToCheck = new ArrayList<>();
                for (final ExternalResourceParserResult availableResource : availableResources) {
                    if (availableResource.isDirectory()) {
                        resourcesToCheck.addAll(listResources(availableResource));
                    } else if (visitedLocations.add(availableResource.getLocation())){
                        results.add(createDescriptor(availableResource));
                    } else {
                        conflicts.add(availableResource);
                        results.removeIf(visitedResource -> visitedResource.getLocation().endsWith(availableResource.getLocation()));
                    }
                }
                availableResources = resourcesToCheck;
            }

        } catch (Exception e) {
            throw new IOException("Provider cannot list resources", e);
        }

        if (!conflicts.isEmpty()) {
            LOGGER.error("NARs " + conflicts.stream().map(ExternalResourceParserResult::getLocation).collect(Collectors.joining(", ")) + "won't be included, as multiple NARs with the same name were found.");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The following NARs were found: " + results.stream().map(ExternalResourceDescriptor::getLocation).collect(Collectors.joining(", ")));
        }

        return results;
    }

        @Override
    public InputStream fetchExternalResource(ExternalResourceDescriptor descriptor) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("Provider is not initialized");
        }

        final Response httpResponse = sendRequest(this.baseUrl + descriptor.getLocation());

        return httpResponse.body().byteStream();
    }

    public Collection<ExternalResourceParserResult> listResources(ExternalResourceParserResult descriptor) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("Provider is not initialized.");
        }

        final Collection<ExternalResourceParserResult> result;

        try {
            final ExternalResourceXmlParser parser = new ExternalResourceXmlParser(parserConfiguration);
            result = collectResources(parser, createNarLocationUrl(descriptor));

        } catch (Exception e){
            throw new IOException("Provider cannot list resources", e);
        }

        return result;
    }

    private OkHttpClient createHttpClient(final Map<String, String> properties) {
        final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        final String proxyUser = properties.get(PROXY_USER);
        final String proxyPassword = properties.get(PROXY_PASSWORD);
        final String proxyServer = properties.get(PROXY_SERVER);
        final String proxyServerPort = properties.get(PROXY_SERVER_PORT);
        final String userName = properties.get(USER_NAME);
        final String password = properties.get(PASSWORD);

        if (proxyServer != null && proxyServerPort != null) {
            clientBuilder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyServer, Integer.parseInt(proxyServerPort))));
        }

        if (proxyUser != null && proxyPassword != null) {
            final Authenticator proxyAuthenticator = createAuthenticator(proxyUser, proxyPassword, "Proxy-Authorization");
            clientBuilder.proxyAuthenticator(proxyAuthenticator);
        }

        try {
            final long connectionTimeout = Math.round(FormatUtils.getPreciseTimeDuration(Objects.requireNonNull(properties.get(CONNECT_TIMEOUT)), TimeUnit.SECONDS));
            clientBuilder.connectTimeout(connectionTimeout, TimeUnit.SECONDS);
        } catch (NullPointerException e) {
            clientBuilder.connectTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        }

        try {
            final long readTimeout = Math.round(FormatUtils.getPreciseTimeDuration(Objects.requireNonNull(properties.get(READ_TIMEOUT)), TimeUnit.SECONDS));
            clientBuilder.readTimeout(readTimeout, TimeUnit.SECONDS);
        } catch (NullPointerException e) {
            clientBuilder.readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS);
        }

        if (userName != null && password != null) {
            clientBuilder.authenticator(createAuthenticator(userName, password, "Authorization"));
        } else {
            throw new IllegalArgumentException("User name and password are required.");
        }

        return clientBuilder.build();
    }

    private Authenticator createAuthenticator(final String user, final String password, final String header) {
        return (route, response) -> {
            final String credential = Credentials.basic(user, password);
            return response.request().newBuilder()
                    .header(header, credential)
                    .build();
        };
    }

    private Response sendRequest(final String url) throws IOException {
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(normalizeURL(url));
        requestBuilder.get();

        final Request httpRequest = requestBuilder.build();
        final Response httpResponse = client.newCall(httpRequest).execute();

        if (!httpResponse.isSuccessful()) {
            throw new IOException("Provider cannot list resources due to http error " + httpResponse.code());
        }
        return httpResponse;
    }

    private String normalizeURL(final String url) {
        return url.replaceAll("(?<!http:|https:)/+/", "/");
    }

    private String createNarLocationUrl(final ExternalResourceParserResult descriptor) {
        return normalizeURL(this.baseUrl + descriptor.getPath() + "/" + descriptor.getLocation() + "/" + this.narLocation);
    }

    private Collection<ExternalResourceParserResult> collectResources(final ExternalResourceXmlParser parser, final String url)
            throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        final String response = sendRequest(url).body().string();

        final Collection<ExternalResourceParserResult> descriptors = parser.parseResponse(response, url.replace(this.baseUrl, ""));

        return descriptors.stream()
                .filter(descriptor -> descriptor.getLocation().matches(this.filter))
                .collect(Collectors.toList());
    }

    private ImmutableExternalResourceDescriptor createDescriptor(final ExternalResourceParserResult parserResult) {
        return new ImmutableExternalResourceDescriptor(parserResult.getPath() + parserResult.getLocation(), parserResult.getLastModified());
    }
}
