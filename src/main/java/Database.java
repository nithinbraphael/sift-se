import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;

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

    public void savePage(String url, String title, String content, int statusCode, String contentType) throws SQLException {
        String sql = """
            INSERT INTO pages
                (url, title, content, status_code, content_type, crawled_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(url) DO UPDATE SET
                title = excluded.title,
                content = excluded.content,
                status_code = excluded.status_code,
                content_type = excluded.content_type,
                crawled_at = excluded.crawled_at
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, url);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.setInt(4, statusCode);
            statement.setString(5, contentType);
            statement.setString(6, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        connection.close();
    }
}
