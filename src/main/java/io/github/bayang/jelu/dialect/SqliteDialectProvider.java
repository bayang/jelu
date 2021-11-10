package io.github.bayang.jelu.dialect;

import org.springframework.data.jdbc.repository.config.DialectResolver;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.JdbcOperations;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/**
 * Provides a {@link SqliteDialect} from {@link JdbcOperations} using {@link DialectResolver} for Spring Data JDBC.
 * Dialect resolution uses Spring's {@code spring.factories} to determine available extensions
 */
public class SqliteDialectProvider implements DialectResolver.JdbcDialectProvider {
    @Override
    public Optional<Dialect> getDialect(JdbcOperations operations) {
        return Optional.ofNullable(operations.execute(this::createDialectIfConnectedToSqliteElseNull));
    }

    private Dialect createDialectIfConnectedToSqliteElseNull(Connection connection) throws SQLException {
        return is(connection).connectedToSqlite() ? SqliteDialect.INSTANCE : null;
    }

    private static ConnectionWrapper is(Connection connection) {
        return new ConnectionWrapper(connection);
    }

    private static class ConnectionWrapper {
        private static final String PRODUCT_NAME_SQLITE = "sqlite";

        private final Connection connection;

        private ConnectionWrapper(Connection connection) {
            this.connection = connection;
        }

        private boolean connectedToSqlite() throws SQLException {
            return getProductName().contains(PRODUCT_NAME_SQLITE);
        }

        private String getProductName() throws SQLException {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName().toLowerCase(Locale.ENGLISH);
        }
    }
}
