package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Drops category_id column from skills, rules, external_skills, learning_articles.
 * Handles both Flyway V1 schema (named FKs) and Hibernate-created schema (auto-named FKs).
 */
public class V2__drop_category_columns extends BaseJavaMigration {

    private static final List<TableConfig> TABLES = List.of(
        new TableConfig("skills", "fk_skills_category", "category_id"),
        new TableConfig("rules", "fk_rules_category", "category_id"),
        new TableConfig("external_skills", "fk_external_skills_category", "category_id"),
        new TableConfig("learning_articles", "fk_learning_articles_category", "category_id")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        String catalog = conn.getCatalog();
        String schema = conn.getSchema();
        if (schema == null) schema = "";

        for (TableConfig cfg : TABLES) {
            if (!hasColumn(conn, catalog, schema, cfg.tableName, cfg.columnName)) {
                continue;
            }
            String fkName = findForeignKeyName(conn, catalog, schema, cfg.tableName, cfg.columnName);
            if (fkName != null) {
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE " + cfg.tableName + " DROP FOREIGN KEY " + fkName);
                }
            }
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + cfg.tableName + " DROP COLUMN " + cfg.columnName);
            }
        }
    }

    private boolean hasColumn(Connection conn, String catalog, String schema, String table, String column) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(catalog, schema, table, column)) {
            return rs.next();
        }
    }

    private String findForeignKeyName(Connection conn, String catalog, String schema, String table, String column) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getImportedKeys(catalog, schema, table)) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("FKCOLUMN_NAME"))) {
                    return rs.getString("FK_NAME");
                }
            }
        }
        return null;
    }

    private record TableConfig(String tableName, String defaultFkName, String columnName) {}
}
