package org.hibernate.omm.jdbc;

import com.mongodb.lang.Nullable;
import org.hibernate.omm.jdbc.adapter.ArrayAdapter;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;

import java.util.Map;

public class MongoArray implements ArrayAdapter {

    private final Object elements;
    private final String baseTypeName;

    @Nullable
    private Integer baseType;

    public MongoArray(Object elements, String baseTypeName) {
        this.elements = elements;
        this.baseTypeName = baseTypeName;
    }

    public void setBaseType(Integer baseType) {
        this.baseType = baseType;
    }

    @Override
    public int getBaseType() throws SimulatedSQLException {
        if (baseType == null) {
            throw new SimulatedSQLException("baseType unknown");
        }
        return baseType;
    }

    @Override
    public Object getArray() {
        return elements;
    }

    @Override
    public String getBaseTypeName() {
        return baseTypeName;
    }

    @Override
    public Object getArray(Map<String, Class<?>> map) {
        return elements;
    }

    @Override
    public Object getArray(long index, int count) {
        return elements;
    }

    @Override
    public Object getArray(long index, int count, Map<String, Class<?>> map) {
        return elements;
    }
}
