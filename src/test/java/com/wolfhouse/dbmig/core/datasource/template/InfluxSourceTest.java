package com.wolfhouse.dbmig.core.datasource.template;

import com.influxdb.v3.client.InfluxDBClient;
import com.wolfhouse.dbmig.core.datasource.sourcedata.InfluxData;
import com.wolfhouse.dbmig.properties.BaseDbProperty;
import com.wolfhouse.dbmig.properties.InfluxProperty;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class InfluxSourceTest {

    @Test
    void initDatasource_appliesConfiguredTimeAndTagFields() throws Exception {
        InfluxProperty property = new InfluxProperty();
        property.setHost("127.0.0.1");
        property.setPort("9090");
        property.setToken("secret-token");
        property.setDatabase("metrics");
        property.setTimeField("createdAt");
        property.setTags(new String[]{"sensor", "region"});

        InfluxDBClient influxDbClient = mock(InfluxDBClient.class);
        when(influxDbClient.getServerVersion()).thenReturn("test-version");

        try (MockedStatic<InfluxDBClient> influxDbClientMock = mockStatic(InfluxDBClient.class)) {
            influxDbClientMock.when(() -> InfluxDBClient.getInstance(
                    eq("http://127.0.0.1:9090"),
                    argThat(token -> java.util.Arrays.equals(token, "secret-token".toCharArray())),
                    eq("metrics")
            )).thenReturn(influxDbClient);

            InfluxSource source = new InfluxSource();
            source.initDatasource(property);

            assertEquals("createdAt", readField(source, "timeField"));
            assertEquals(Set.of("sensor", "region"), readField(source, "tagsField"));

            InfluxData processed = source.processIgnore(InfluxData.of(new HashMap<>(Map.of(
                    "createdAt", "2025-03-20T10:15:30Z",
                    "sensor", "sensor-01",
                    "region", "us-west",
                    "value", 42
            ))));

            assertFalse(processed.toMap().containsKey("createdAt"));
            assertEquals("sensor-01", processed.toMap().get("sensor"));
            assertEquals("us-west", processed.toMap().get("region"));

            Object extracted = invokeExtractData(source, processed.toMap());
            assertEquals(Map.of("sensor", "sensor-01", "region", "us-west"), invokeRecordAccessor(extracted, "tags"));
            assertEquals(Map.of("value", 42), invokeRecordAccessor(extracted, "fields"));
        }
    }

    @Test
    void initDatasource_rejectsUnsupportedPropertyType() {
        InfluxSource source = new InfluxSource();

        assertThrows(IllegalArgumentException.class, () -> source.initDatasource(new BaseDbProperty()));
    }

    private static Object readField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object invokeExtractData(InfluxSource source, Map<String, Object> data) throws Exception {
        Method method = InfluxSource.class.getDeclaredMethod("extractData", Map.class);
        method.setAccessible(true);
        return method.invoke(source, data);
    }

    private static Object invokeRecordAccessor(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
