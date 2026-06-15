package com.seohamin.fshs.v2.global.config;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import java.sql.SQLException;

public class FshsSQLiteDialect extends SQLiteDialect {

    private static final int SQLITE_CONSTRAINT = 19;

    private static final IdentityColumnSupport IDENTITY_COLUMN_SUPPORT = new IdentityColumnSupportImpl() {
        @Override
        public boolean supportsIdentityColumns() {
            return true;
        }

        @Override
        public boolean hasDataTypeInIdentityColumn() {
            return true;
        }

        @Override
        public String getIdentityColumnString(int type) {
            return "";
        }

        @Override
        public String getIdentitySelectString(String table, String column, int type) {
            return "select last_insert_rowid()";
        }
    };

    private final UniqueDelegate uniqueDelegate = new UniqueDelegate() {
        @Override
        public String getColumnDefinitionUniquenessFragment(final Column column, final SqlStringGenerationContext context) {
            return " unique";
        }

        @Override
        public String getTableCreationUniqueConstraintsFragment(final Table table, final SqlStringGenerationContext context) {
            final StringBuilder fragment = new StringBuilder();

            for (final UniqueKey uniqueKey : table.getUniqueKeys().values()) {
                fragment.append(", constraint ")
                        .append(quote(uniqueKey.getName()))
                        .append(" unique (");

                boolean first = true;
                for (final Column column : uniqueKey.getColumns()) {
                    if (first) {
                        first = false;
                    } else {
                        fragment.append(", ");
                    }
                    fragment.append(column.getQuotedName(FshsSQLiteDialect.this));
                }

                fragment.append(")");
            }

            return fragment.toString();
        }

        @Override
        public String getAlterTableToAddUniqueKeyCommand(
                final UniqueKey uniqueKey,
                final Metadata metadata,
                final SqlStringGenerationContext context
        ) {
            return "";
        }

        @Override
        public String getAlterTableToDropUniqueKeyCommand(
                final UniqueKey uniqueKey,
                final Metadata metadata,
                final SqlStringGenerationContext context
        ) {
            return "";
        }
    };

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return IDENTITY_COLUMN_SUPPORT;
    }

    @Override
    public UniqueDelegate getUniqueDelegate() {
        return uniqueDelegate;
    }

    @Override
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        final SQLExceptionConversionDelegate parentDelegate = super.buildSQLExceptionConversionDelegate();

        return (sqlException, message, sql) -> {
            if (isConstraintViolation(sqlException)) {
                return new ConstraintViolationException(message, sqlException, sql, null);
            }

            return parentDelegate.convert(sqlException, message, sql);
        };
    }

    private boolean isConstraintViolation(final SQLException sqlException) {
        return (JdbcExceptionHelper.extractErrorCode(sqlException) & 0xFF) == SQLITE_CONSTRAINT;
    }
}
