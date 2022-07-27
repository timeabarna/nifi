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

import java.util.Objects;

public class ExternalResourceParserResult {
    private final String location;
    private final long lastModified;
    private final String path;
    private final boolean directory;

    public ExternalResourceParserResult(String location, long lastModified, String path, boolean directory) {
        this.location = location;
        this.lastModified = lastModified;
        this.path = path;
        this.directory = directory;
    }

    public String getLocation() {
        return location;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return directory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExternalResourceParserResult that = (ExternalResourceParserResult) o;

        if (lastModified != that.lastModified) return false;
        if (directory != that.directory) return false;
        if (!Objects.equals(location, that.location)) return false;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        int result = location != null ? location.hashCode() : 0;
        result = 31 * result + (int) (lastModified ^ (lastModified >>> 32));
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (directory ? 1 : 0);
        return result;
    }
}
