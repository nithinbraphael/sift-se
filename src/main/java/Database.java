import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database implements AutoCloseable {
    private final Connection connection;
    public Database() throws IOException, SQLException {
        Path dataDirectory = Path.of("data");
        Files.createDirectories(dataDirectory);
        String url = "jdbc:sqlite:data/sift.db";
        connection = DriverManager.getConnection(url);

        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            createTables();

        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    private void createTables() throws SQLException {
        String pagesTable = """
            CREATE TABLE IF NOT EXISTS pages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                url TEXT NOT NULL UNIQUE,
                title TEXT,
                content TEXT,
                status_code INTEGER,
                content_type TEXT,
                crawled_at TEXT NOT NULL
            )
            """;

        String linksTable = """
            CREATE TABLE IF NOT EXISTS links (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_page_id INTEGER NOT NULL,
                target_url TEXT NOT NULL,
                UNIQUE (source_page_id, target_url),
                FOREIGN KEY (source_page_id)
                    REFERENCES pages(id)
                    ON DELETE CASCADE
            )
            """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(pagesTable);
            statement.execute(linksTable);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        connection.close();
    }
}
