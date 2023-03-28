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
package org.apache.nifi.kafka.processors.consumer.convert;

import org.apache.nifi.kafka.service.api.record.ByteRecord;
import org.apache.nifi.processor.ProcessSession;

import java.util.Iterator;

/**
 * Implementations of {@link KafkaMessageConverter} employ specialized strategies for converting Kafka messages into
 * NiFi FlowFiles.
 */
public interface KafkaMessageConverter {

    void toFlowFiles(final ProcessSession session, final Iterator<ByteRecord> consumerRecords);

    String TRANSIT_URI_FORMAT = "kafka://%s/%s";
}
