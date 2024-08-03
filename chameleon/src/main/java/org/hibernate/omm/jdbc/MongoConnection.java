package org.hibernate.omm.jdbc;

import com.mongodb.assertions.Assertions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.Nullable;
import org.bson.Document;
import org.hibernate.omm.jdbc.adapter.ConnectionAdapter;
import org.hibernate.omm.jdbc.adapter.DatabaseMetaDataAdapter;
import org.hibernate.omm.jdbc.exception.CommandRunFailSQLException;
import org.hibernate.omm.jdbc.exception.NotSupportedSQLException;
import org.hibernate.omm.jdbc.exception.SimulatedSQLException;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoConnection implements ConnectionAdapter {
    private static final String DB_VERSION_QUERY_FIELD_NAME = "buildinfo";

    private final ClientSession clientSession;
    private final MongoDatabase mongoDatabase;

    private boolean autoCommit;
    private boolean supportsTransaction;
    private boolean closed;

    @Nullable
    private SQLWarning sqlWarning;

    public MongoConnection(MongoDatabase mongoDatabase, ClientSession clientSession) {
        Assertions.notNull("mongoDatabase", mongoDatabase);
        this.clientSession = clientSession;
        this.mongoDatabase = mongoDatabase;
    }

    @Override
    public Statement createStatement() {
        return new MongoStatement(mongoDatabase, clientSession, this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) {
        return new MongoPreparedStatement(mongoDatabase, clientSession, this, sql);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SimulatedSQLException {
        Document result = mongoDatabase.runCommand(new Document(DB_VERSION_QUERY_FIELD_NAME, 1));
        if (result.getDouble("ok") != 1.0) {
            throw new CommandRunFailSQLException(result);
        }
        String version = result.getString("version");
        List<Integer> versionArray = result.getList("versionArray", Integer.class);

        if (versionArray.get(0) >= 4) {
            supportsTransaction = true;
        }
        return new DatabaseMetaDataAdapter() {

            @Override
            public String getDatabaseProductVersion() {
                return version;
            }

            @Override
            public int getDatabaseMajorVersion() {
                return versionArray.get(0);
            }

            @Override
            public int getDatabaseMinorVersion() {
                return versionArray.get(1);
            }

            @Override
            public Connection getConnection() {
                return MongoConnection.this;
            }

        };
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    public String nativeSQL(String sql) throws SimulatedSQLException {
        throw new NotSupportedSQLException();
    }

    @Override
    public boolean getAutoCommit() {
        return this.autoCommit;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) {
        if (!autoCommit && supportsTransaction) {
            this.clientSession.startTransaction();
        }
        this.autoCommit = autoCommit;
    }

    @Override
    public void commit() {
        if (!autoCommit && supportsTransaction) {
            clientSession.commitTransaction();
        }
    }

    @Override
    public void rollback() {
        if (!autoCommit && supportsTransaction) {
            clientSession.abortTransaction();
        }
    }

    @Override
    public void close() {
        this.clientSession.close();
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void clearWarnings() {
        sqlWarning = null;
    }

    @Override
    @Nullable
    @SuppressWarnings("nullness")
    public SQLWarning getWarnings() {
        return sqlWarning;
    }

    @Override
    public String getCatalog() {
        return "N/A";
    }

    @Override
    public String getSchema() {
        return "N/A";
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) {
        return new MongoArray(elements, typeName);
    }
}
