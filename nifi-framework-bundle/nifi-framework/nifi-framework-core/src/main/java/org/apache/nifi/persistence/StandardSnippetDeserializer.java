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
package org.apache.nifi.persistence;

import java.io.InputStream;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.apache.nifi.controller.StandardSnippet;
import org.apache.nifi.controller.serialization.FlowSerializationException;
import org.apache.nifi.xml.processing.ProcessingException;
import org.apache.nifi.xml.processing.stream.StandardXMLStreamReaderProvider;
import org.apache.nifi.xml.processing.stream.XMLStreamReaderProvider;

public class StandardSnippetDeserializer {

    public static StandardSnippet deserialize(final InputStream inStream) {
        try {
            JAXBContext context = JAXBContext.newInstance(StandardSnippet.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            final XMLStreamReaderProvider provider = new StandardXMLStreamReaderProvider();
            XMLStreamReader xsr = provider.getStreamReader(new StreamSource(inStream));
            JAXBElement<StandardSnippet> snippetElement = unmarshaller.unmarshal(xsr, StandardSnippet.class);
            return snippetElement.getValue();
        } catch (final JAXBException | ProcessingException e) {
            throw new FlowSerializationException(e);
        }
    }
}
