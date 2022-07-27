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

public class ExternalResourceParserConfiguration {
    private final String fileListIdentifier ;
    private final String locationIdentifier;
    private final String lastModificationIdentifier;
    private final String directoryIdentifier;
    private final String dateTimeFormat;

    public ExternalResourceParserConfiguration(final String fileListIdentifier, final String locationIdentifier, final String lastModificationIdentifier,
                                               final String directoryIdentifier, final String dateTimeFormat) {
        this.fileListIdentifier = fileListIdentifier;
        this.locationIdentifier = locationIdentifier;
        this.lastModificationIdentifier = lastModificationIdentifier;
        this.directoryIdentifier = directoryIdentifier;
        this.dateTimeFormat = dateTimeFormat;
    }

    public String getFileListIdentifier() {
        return fileListIdentifier;
    }

    public String getLocationIdentifier() {
        return locationIdentifier;
    }

    public String getLastModificationIdentifier() {
        return lastModificationIdentifier;
    }

    public String getDirectoryIdentifier() {
        return directoryIdentifier;
    }

    public String getDateTimeFormat() {
        return dateTimeFormat;
    }
}
