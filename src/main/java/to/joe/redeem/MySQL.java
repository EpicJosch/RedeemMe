package to.joe.redeem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQL {

    private Connection conn;
    private RedeemMe plugin;

    private void initTable(String table) throws SQLException {
        final ResultSet tableExists = conn.getMetaData().getTables(null, null, table, null);
        if (!tableExists.first()) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(plugin.getResource(table + ".sql")));
            final StringBuilder builder = new StringBuilder();
            String next;
            try {
                while ((next = reader.readLine()) != null) {
                    builder.append(next);
                }
                getFreshPreparedStatementHotFromTheOven(builder.toString()).execute();
            } catch (final IOException e) {
                throw new SQLException("Could not load default table creation text", e);
            }
        }
    }

    public MySQL(RedeemMe plugin, String url, String username, String password) throws SQLException {
        conn = DriverManager.getConnection(url, username, password);
        this.plugin = plugin;
        initTable("couponcodes");
        initTable("packages");
        initTable("packageitems");
        initTable("packagecommands");
    }

    public PreparedStatement getFreshPreparedStatementHotFromTheOven(String query) throws SQLException {
        return conn.prepareStatement(query);
    }

    public PreparedStatement getFreshPreparedStatementWithGeneratedKeys(String query) throws SQLException {
        return conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
    }
}