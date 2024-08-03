package org.hibernate.omm.dialect;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLArrayJdbcType;
import org.hibernate.dialect.PostgreSQLArrayJdbcTypeConstructor;
import org.hibernate.dialect.PostgreSQLCastingInetJdbcType;
import org.hibernate.dialect.PostgreSQLCastingIntervalSecondJdbcType;
import org.hibernate.dialect.PostgreSQLCastingJsonJdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.dialect.PostgreSQLOrdinalEnumJdbcType;
import org.hibernate.dialect.PostgreSQLStructCastingJdbcType;
import org.hibernate.dialect.PostgreSQLUUIDJdbcType;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.PostgreSQLAggregateSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.omm.ast.MongoSqlAstTranslatorFactory;
import org.hibernate.omm.jdbc.adapter.ArrayAdapter;
import org.hibernate.omm.type.ObjectIdJavaType;
import org.hibernate.omm.type.ObjectIdJdbcType;
import org.hibernate.omm.util.StringUtil;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @author Nathan Xu
 * @since 1.0.0
 */
public class MongoDialect extends Dialect {

    public static final int MINIMUM_MONGODB_MAJOR_VERSION_SUPPORTED = 3;

    private final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make(MINIMUM_MONGODB_MAJOR_VERSION_SUPPORTED);

    public MongoDialect() {
        this(MINIMUM_VERSION);
    }

    public MongoDialect(DatabaseVersion version) {
        super(version);
    }

    public MongoDialect(DialectResolutionInfo dialectResolutionInfo) {
        super(dialectResolutionInfo);
    }

    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return new MongoSqlAstTranslatorFactory();
    }

    @Override
    public void appendLiteral(SqlAppender appender, String literal) {
        appender.appendSql(StringUtil.writeStringHelper(literal));
    }

    @Override
    public boolean supportsNullPrecedence() {
        return false;
    }

    @Override
    public boolean supportsStandardArrays() {
        return true;
    }

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contribute(typeContributions, serviceRegistry); // need to call the super method to enable Array support
        TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
        typeConfiguration.getJavaTypeRegistry().addDescriptor(ObjectIdJavaType.INSTANCE);
        typeConfiguration.getJdbcTypeRegistry().addDescriptor(ObjectIdJdbcType.INSTANCE);
        contributePostgreSQLTypes(typeContributions, serviceRegistry);
    }

    /**
     * Allow for extension points to override this only
     */
    protected void contributePostgreSQLTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
                .getJdbcTypeRegistry();
        // For discussion of BLOB support in Postgres, as of 8.4, see:
        //     http://jdbc.postgresql.org/documentation/84/binary-data.html
        // For how this affects Hibernate, see:
        //     http://in.relation.to/15492.lace

        // Force BLOB binding.  Otherwise, byte[] fields annotated
        // with @Lob will attempt to use
        // BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING.  Since the
        // dialect uses oid for Blobs, byte arrays cannot be used.
        jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.BLOB_BINDING );
        jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.CLOB_BINDING );
        // Don't use this type due to https://github.com/pgjdbc/pgjdbc/issues/2862
        //jdbcTypeRegistry.addDescriptor( TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE );
        jdbcTypeRegistry.addDescriptor( XmlJdbcType.INSTANCE );

        jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingInetJdbcType.INSTANCE );
        jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingIntervalSecondJdbcType.INSTANCE );
        jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLStructCastingJdbcType.INSTANCE );
        jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingJsonJdbcType.JSONB_INSTANCE );

        // PostgreSQL requires a custom binder for binding untyped nulls as VARBINARY
        typeContributions.contributeJdbcType( ObjectNullAsBinaryTypeJdbcType.INSTANCE );

        // Until we remove StandardBasicTypes, we have to keep this
        typeContributions.contributeType(
                new JavaObjectType(
                        ObjectNullAsBinaryTypeJdbcType.INSTANCE,
                        typeContributions.getTypeConfiguration()
                                .getJavaTypeRegistry()
                                .getDescriptor( Object.class )
                )
        );

        jdbcTypeRegistry.addDescriptor( PostgreSQLEnumJdbcType.INSTANCE );
        jdbcTypeRegistry.addDescriptor( PostgreSQLOrdinalEnumJdbcType.INSTANCE );
        jdbcTypeRegistry.addDescriptor( PostgreSQLUUIDJdbcType.INSTANCE );

        jdbcTypeRegistry.addTypeConstructor( new PostgreSQLArrayJdbcTypeConstructor() {
            @Override
            public JdbcType resolveType(
                    TypeConfiguration typeConfiguration,
                    Dialect dialect,
                    JdbcType elementType,
                    ColumnTypeInformation columnTypeInformation) {
                return new PostgreSQLArrayJdbcType(elementType) {
                    @Override
                    public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
                        //noinspection unchecked
                        final ValueBinder<Object> elementBinder = getElementJdbcType().getBinder(((BasicPluralJavaType<Object>) javaTypeDescriptor).getElementJavaType());
                        return new BasicBinder<>(javaTypeDescriptor, this) {

                            @Override
                            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                                st.setArray(index, (ArrayAdapter) () -> value);
                            }

                            @Override
                            protected void doBind(final CallableStatement st, final X value, final String name, final WrapperOptions options) throws SQLException {

                            }
                        };
                    }
                };
            }
        } );
    }

    @Override
    public AggregateSupport getAggregateSupport() {
        return PostgreSQLAggregateSupport.valueOf( this );
    }
}
