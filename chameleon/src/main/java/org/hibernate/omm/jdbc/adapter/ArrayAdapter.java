package org.hibernate.omm.jdbc.adapter;

import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author Nathan Xu
 * @see org.hibernate.omm.jdbc.MongoResultSet#getArray(String)
 * @see org.hibernate.omm.jdbc.MongoConnection#createArrayOf(String, Object[])
 * @since 1.0.0
 */
public interface ArrayAdapter extends Array {

    @Override
    default int getBaseType() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default String getBaseTypeName() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Object getArray(Map<String, Class<?>> map) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Object getArray(long index, int count) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default Object getArray(long index, int count, Map<String, Class<?>> map) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default ResultSet getResultSet() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default ResultSet getResultSet(Map<String, Class<?>> map) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default ResultSet getResultSet(long index, int count) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    default void free() throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }
}
