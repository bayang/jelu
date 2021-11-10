package io.github.bayang.jelu.dialect;

import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.LockOptions;

/**
 * Provides SQLite-specific {@code limit} and {@code offset} clauses for constructing SQL statements
 */
public class SqliteDialect extends AbstractDialect {
    public static final SqliteDialect INSTANCE = new SqliteDialect();

    private static final LimitClause LIMIT_CLAUSE = new LimitClause() {
        @Override
        public String getLimit(long limit) {
            return "LIMIT " + limit;
        }

        @Override
        public String getOffset(long offset) {
            return "OFFSET " + offset;
        }

        @Override
        public String getLimitOffset(long limit, long offset) {
            return this.getLimit(limit) + " " + getOffset(offset);
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    private static final LockClause LOCK_CLAUSE = new LockClause() {
        @Override
        public String getLock(LockOptions lockOptions) {
            return "";
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    protected SqliteDialect() {}

    @Override
    public LimitClause limit() {
        return LIMIT_CLAUSE;
    }

    @Override
    public LockClause lock() {
        return LOCK_CLAUSE;
    }
}
