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
package org.apache.nifi.diagnostics.bootstrap.shell.result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class LineSplittingResult implements ShellCommandResult {
    private final Pattern regexForSplitting;
    private final List<String> labels;
    private final String commandName;

    public LineSplittingResult(final String regexStringForSplitting, final List<String> labels, final String commandName) {
        this.regexForSplitting = Pattern.compile(regexStringForSplitting);
        this.labels = labels;
        this.commandName = commandName;
    }

    @Override
    public Collection<String> createResult(final InputStream inputStream) {
        final List<String> result = new ArrayList<>();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] splitResults = regexForSplitting.split(line);
                    for (int i = 0; i < labels.size(); i++) {
                        result.add(String.format("%s : %s", labels.get(i), splitResults[i]));
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to process result for command: %s", commandName),e);
        }
    }
}
