package com.wolfhouse.dbmig.core.datasource.sourcedata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * MySQL 数据库数据对象
 *
 * @author Rylin Wolf
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MySqlData extends BaseSourceData {
    private final HashMap<String, Object> data;

    private MySqlData() {
        this.data = new HashMap<>();
    }

    public static MySqlData of() {
        return new MySqlData();
    }

    public static MySqlData of(Map<String, Object> map) {
        return new MySqlData().fromMap(map);
    }

    @Override
    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }

    @Override
    public MySqlData fromMap(Map<String, Object> map) {
        this.data.clear();
        this.data.putAll(map);
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
