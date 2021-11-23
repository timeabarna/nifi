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
package org.apache.nifi.web;

import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.CollectorRegistry;
import org.apache.nifi.prometheus.util.BulletinMetricsRegistry;
import org.apache.nifi.prometheus.util.ConnectionAnalyticsMetricsRegistry;
import org.apache.nifi.prometheus.util.JvmMetricsRegistry;
import org.apache.nifi.prometheus.util.NiFiMetricsRegistry;
import org.apache.nifi.web.api.FlowResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestFlowResource {
    private static final String PRODUCER = "json";
    private static final String FIRST_FIELD_NAME = "beans";
    private static final String SAMPLE_NAME_JVM = "_jvm_";
    private static final String SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP = "RootProcessGroup";
    private static final String SAMPLE_LABEL_VALUES_PROCESS_GROUP = "ProcessGroup";
    private static final String COMPONENT_TYPE_LABEL = "component_type";
    private static final int COMPONENT_TYPE_VALUE_INDEX = 1;

    private static FlowResource resource;

    @BeforeAll
    public static void setup() throws Exception {
        resource = getFlowResource();
    }

    @Test
    public void testFlowMetricsOutputWithBothSampleNameAndSampleLabelValuesAreNull() throws Exception {
        final Response response = resource.getFlowMetrics(PRODUCER, null, null, FIRST_FIELD_NAME);

        final Map<String, List<Sample>> metrics = (Map<String, List<Sample>>) response.getEntity();
        assertThat(metrics.keySet(), hasSize(1));
        assertThat(metrics, hasKey(FIRST_FIELD_NAME));

        final List<Sample> registries = metrics.get(FIRST_FIELD_NAME);
        assertThat(registries, hasSize(9));

        final Map<String, Long> result = getResult(registries);
        assertThat(3L, equalTo(result.get(SAMPLE_NAME_JVM)));
        assertThat(4L, equalTo(result.get(SAMPLE_LABEL_VALUES_PROCESS_GROUP)));
        assertThat(2L, equalTo(result.get(SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP)));
    }

    @Test
    public void testFlowMetricsOutputWithBothSampleNameIsNullAndSampleLabelValuesAreNotNull() throws Exception {
        final Response response = resource.getFlowMetrics(PRODUCER, null, "label", FIRST_FIELD_NAME);

        final Map<String, List<Sample>> metrics = (Map<String, List<Sample>>) response.getEntity();
        assertThat(metrics.keySet(), hasSize(1));
        assertThat(metrics, hasKey(FIRST_FIELD_NAME));

        final List<Sample> registries = metrics.get(FIRST_FIELD_NAME);
        assertThat(registries, hasSize(9));

        final Map<String, Long> result = getResult(registries);
        assertThat(3L, equalTo(result.get(SAMPLE_NAME_JVM)));
        assertThat(4L, equalTo(result.get(SAMPLE_LABEL_VALUES_PROCESS_GROUP)));
        assertThat(2L, equalTo(result.get(SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP)));
    }

    @Test
    public void testFlowMetricsOutputWithBothSampleNameAndSampleLabelValuesHaveValuesWithNoResult() throws Exception {
        final Response response = resource.getFlowMetrics(PRODUCER, "name", "label", FIRST_FIELD_NAME);

        final Map<String, List<Sample>> metrics = (Map<String, List<Sample>>) response.getEntity();
        assertThat(metrics.entrySet(), hasSize(0));
    }

    @Test
    public void testFlowMetricsOutputWithBothSampleNameAndSampleLabelValuesAreEmptyString() throws Exception {
        final Response response = resource.getFlowMetrics(PRODUCER, "", "", FIRST_FIELD_NAME);

        final Map<String, List<Sample>> metrics = (Map<String, List<Sample>>) response.getEntity();
        assertThat(metrics.keySet(), hasSize(1));
        assertThat(metrics, hasKey(FIRST_FIELD_NAME));

        final List<Sample> registries = metrics.get(FIRST_FIELD_NAME);
        assertThat(registries, hasSize(9));

        final Map<String, Long> result = getResult(registries);
        assertThat(3L, equalTo(result.get(SAMPLE_NAME_JVM)));
        assertThat(4L, equalTo(result.get(SAMPLE_LABEL_VALUES_PROCESS_GROUP)));
        assertThat(2L, equalTo(result.get(SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP)));
    }

    @Test
    public void testFlowMetricsOutputWithBothSampleNameEmptyStringAndSampleLabelValuesNotNull() throws Exception {
        final Response response = resource.getFlowMetrics(PRODUCER, "", "label", FIRST_FIELD_NAME);

        final Map<String, List<Sample>> metrics = (Map<String, List<Sample>>) response.getEntity();
        assertThat(metrics.keySet(), hasSize(1));
        assertThat(metrics, hasKey(FIRST_FIELD_NAME));

        final List<Sample> registries = metrics.get(FIRST_FIELD_NAME);
        assertThat(registries, hasSize(9));

        final Map<String, Long> result = getResult(registries);
        assertThat(3L, equalTo(result.get(SAMPLE_NAME_JVM)));
        assertThat(4L, equalTo(result.get(SAMPLE_LABEL_VALUES_PROCESS_GROUP)));
        assertThat(2L, equalTo(result.get(SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP)));
    }

    @Test
    public void testFlowMetricsOutputWithBothSampleNameNotNullAndSampleLabelValuesEmptyString() throws Exception {
        final Response response = resource.getFlowMetrics(PRODUCER, "name", "", FIRST_FIELD_NAME);

        final Map<String, List<Sample>> metrics = (Map<String, List<Sample>>) response.getEntity();
        assertThat(metrics.keySet(), hasSize(1));
        assertThat(metrics, hasKey(FIRST_FIELD_NAME));

        final List<Sample> registries = metrics.get(FIRST_FIELD_NAME);
        assertThat(registries, hasSize(9));

        final Map<String, Long> result = getResult(registries);
        assertThat(3L, equalTo(result.get(SAMPLE_NAME_JVM)));
        assertThat(4L, equalTo(result.get(SAMPLE_LABEL_VALUES_PROCESS_GROUP)));
        assertThat(2L, equalTo(result.get(SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP)));
    }

    @Test
    public void testFlowMetricsOutputWithBothSampleNameJvmAndSampleLabelValuesRootProcessGroupWithPartialResult() throws Exception {
        final Response response = resource.getFlowMetrics(PRODUCER, SAMPLE_NAME_JVM, SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP, FIRST_FIELD_NAME);

        final Map<String, List<Sample>> metrics = (Map<String, List<Sample>>) response.getEntity();
        assertThat(metrics.keySet(), hasSize(1));
        assertThat(metrics, hasKey(FIRST_FIELD_NAME));

        final List<Sample> registries = metrics.get(FIRST_FIELD_NAME);
        assertThat(registries, hasSize(5));

        final Map<String, Long> result = getResult(registries);
        assertThat(3L, equalTo(result.get(SAMPLE_NAME_JVM)));
        assertThat(2L, equalTo(result.get(SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP)));
    }

    private Map<String, Long> getResult(List<Sample> registries) {
        return registries.stream()
                .collect(Collectors.groupingBy(
                        sample -> getResultKey(sample),
                        Collectors.counting()));
    }

    private String getResultKey(final Sample sample) {
        return sample.labelNames.contains(COMPONENT_TYPE_LABEL) ? sample.labelValues.get(COMPONENT_TYPE_VALUE_INDEX) : SAMPLE_NAME_JVM;
    }

    private static FlowResource getFlowResource() {
        final NiFiServiceFacade serviceFacade = mock(NiFiServiceFacade.class);

        when(serviceFacade.generateFlowMetrics()).thenReturn(getCollectorRegistryList());

        final FlowResource resource = new FlowResource();
        resource.setServiceFacade(serviceFacade);
        return resource;
    }

    private static List<CollectorRegistry> getCollectorRegistryList() {
        List<CollectorRegistry> registryList = new ArrayList<>();

        registryList.add(getNifiMetricsRegistry());
        registryList.add(getConnectionMetricsRegistry());
        registryList.add(getJvmMetricsRegistry());
        registryList.add(getBulletinMetricsRegistry());

        return registryList;

    }

    private static CollectorRegistry getNifiMetricsRegistry() {
        NiFiMetricsRegistry registry = new NiFiMetricsRegistry();

        registry.setDataPoint(136, "TOTAL_BYTES_READ",
                "RootPGId", SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP, "rootPGName", "", "");
        registry.setDataPoint(136, "TOTAL_BYTES_READ",
                "PGId", SAMPLE_LABEL_VALUES_PROCESS_GROUP, "PGName", "RootPGId", "");

        return registry.getRegistry();
    }

    private static CollectorRegistry getConnectionMetricsRegistry() {
        ConnectionAnalyticsMetricsRegistry connectionMetricsRegistry = new ConnectionAnalyticsMetricsRegistry();

        connectionMetricsRegistry.setDataPoint(1.0,
                "TIME_TO_BYTES_BACKPRESSURE_PREDICTION",
                "PGId", SAMPLE_LABEL_VALUES_PROCESS_GROUP, "success", "connComponentId",
                "RootPGId", "sourceId", "sourceName", "destinationId", "destinationName");
        connectionMetricsRegistry.setDataPoint(1.0,
                "TIME_TO_BYTES_BACKPRESSURE_PREDICTION",
                "RootPGId", SAMPLE_LABEL_VALUES_ROOT_PROCESS_GROUP, "rootPGName", "", "", "", "", "", "");

        return connectionMetricsRegistry.getRegistry();
    }

    private static CollectorRegistry getJvmMetricsRegistry() {
        JvmMetricsRegistry jvmMetricsRegistry = new JvmMetricsRegistry();

        jvmMetricsRegistry.setDataPoint(4.0, "JVM_HEAP_USED", "instanceId");
        jvmMetricsRegistry.setDataPoint(6.0, "JVM_HEAP_USAGE", "instanceId");
        jvmMetricsRegistry.setDataPoint(10.0, "JVM_HEAP_NON_USAGE", "instanceId");

        return jvmMetricsRegistry.getRegistry();
    }

    private static CollectorRegistry getBulletinMetricsRegistry() {
        BulletinMetricsRegistry bulletinMetricsRegistry = new BulletinMetricsRegistry();

        bulletinMetricsRegistry.setDataPoint(1, "BULLETIN", "B1Id", SAMPLE_LABEL_VALUES_PROCESS_GROUP, "PGId", "RootPGId",
                "nodeAddress", "category", "sourceName", "sourceId", "level");
        bulletinMetricsRegistry.setDataPoint(1, "BULLETIN", "B2Id", SAMPLE_LABEL_VALUES_PROCESS_GROUP, "PGId", "RootPGId",
                "nodeAddress", "category", "sourceName", "sourceId", "level");

        return bulletinMetricsRegistry.getRegistry();
    }
}
