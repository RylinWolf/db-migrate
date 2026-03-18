package com.wolfhouse.dbmig.core.datasource.sourcedata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * InfluxDB 数据对象
 *
 * @author Rylin Wolf
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class InfluxData extends BaseSourceData {
    private final LinkedHashMap<String, Object> data;

    private InfluxData() {
        this.data = new LinkedHashMap<>();
    }

    public static InfluxData of() {
        return new InfluxData();
    }

    public static InfluxData of(Map<String, Object> map) {
        return new InfluxData().fromMap(map);
    }

    @Override
    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(data);
    }

    @Override
    public InfluxData fromMap(Map<String, Object> map) {
        data.putAll(map);
        return this;
    }

    @Override
    public boolean has(String key) {
        return data.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Collection<String> keys() {
        return data.keySet();
    }

    @Override
    public Collection<Object> values() {
        return data.values();
    }

    @Override
    public BaseSourceData remove(String key) {
        data.remove(key);
        return this;
    }

    @Override
    public BaseSourceData removeIf(Predicate<Map.Entry<String, Object>> filter) {
        data.entrySet().removeIf(filter);
        return this;
    }
}
