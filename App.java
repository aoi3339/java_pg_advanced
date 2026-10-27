import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

class Term {
    private final int id;
    private final String word;
    private final String meaning;
    private boolean memorized;

    Term(int id, String word, String meaning) {
        this.id = id;
        this.word = word;
        this.meaning = meaning;
        this.memorized = false;
    }

    int getId() { return id; }
    String getWord() { return word; }
    String getMeaning() { return meaning; }
    boolean isMemorized() { return memorized; }
    void setMemorized(boolean memorized) { this.memorized = memorized; }
}

public class App {
    static final String DB_URL = "jdbc:sqlite:terms.db";

    static void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS terms ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "word TEXT NOT NULL, "
                + "meaning TEXT NOT NULL, "
                + "memorized INTEGER NOT NULL DEFAULT 0)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    static void insertTerm(String word, String meaning) throws IOException {
        String sql = "INSERT INTO terms (word, meaning, memorized) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, word);
            statement.setString(2, meaning);
            statement.setInt(3, 0);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static void updateMemorized(int id, boolean memorized) throws IOException {
        String sql = "UPDATE terms SET memorized = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, memorized ? 1 : 0);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static void deleteTerm(int id) throws IOException {
        String sql = "DELETE FROM terms WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static List<Term> selectTerms() throws IOException {
        List<Term> terms = new ArrayList<>();
        String sql = "SELECT id, word, meaning, memorized FROM terms ORDER BY id";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                Term term = new Term(results.getInt("id"), results.getString("word"),
                        results.getString("meaning"));
                term.setMemorized(results.getInt("memorized") == 1);
                terms.add(term);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
        return terms;
    }

    static String termsToJson(List<Term> terms) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < terms.size(); i++) {
            Term term = terms.get(i);
            if (i > 0) json.append(",");
            json.append("{\"word\":\"").append(escapeJson(term.getWord()));
            json.append("\",\"meaning\":\"").append(escapeJson(term.getMeaning()));
            json.append("\",\"memorized\":").append(term.isMemorized()).append("}");
        }
        return json.append("]").toString();
    }

    static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        createTable();

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String word = "";
                String meaning = "";
                for (String value : body.split("&")) {
                    if (value.startsWith("word=")) {
                        word = URLDecoder.decode(value.substring(5), StandardCharsets.UTF_8);
                    } else if (value.startsWith("meaning=")) {
                        meaning = URLDecoder.decode(value.substring(8), StandardCharsets.UTF_8);
                    }
                }
                if (!word.isEmpty() && !meaning.isEmpty()) insertTerm(word, meaning);
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            }

            if (path.equals("/memorized") && method.equals("GET")) {
                String[] values = exchange.getRequestURI().getQuery().split("&");
                int id = Integer.parseInt(values[0].substring(3));
                boolean memorized = Boolean.parseBoolean(values[1].substring(10));
                updateMemorized(id, memorized);
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            }

            if (path.equals("/delete") && method.equals("GET")) {
                int id = Integer.parseInt(exchange.getRequestURI().getQuery().substring(3));
                deleteTerm(id);
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            }

            if (path.equals("/")) {
                String html = "<html><head><meta charset='UTF-8'></head><body>";
                html += "<h1>セキュマネ用語リスト</h1>";
                html += "<form method='post' action='/add'>";
                html += "<label>用語</label><input name='word'>";
                html += "<label>意味</label><input name='meaning'>";
                html += "<button>用語を追加</button></form><ul>";
                for (Term term : selectTerms()) {
                    html += "<li><strong>" + term.getWord() + "とは？</strong>";
                    html += "<details><summary>答えを見る</summary><p>";
                    html += term.getMeaning();
                    html += "</p></details>";
                    html += term.isMemorized() ? " ✔ " : " ";
                    html += "<a href='/memorized?id=" + term.getId() + "&memorized="
                            + !term.isMemorized() + "'>" + (term.isMemorized() ? "復習する" : "覚えた") + "</a> ";
                    html += "<a href='/delete?id=" + term.getId() + "'>削除</a></li>";
                }
                html += "</ul></body></html>";
                byte[] response = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
                return;
            }
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        server.createContext("/api/terms", exchange -> {
            byte[] response = termsToJson(selectTerms()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        server.start();
        System.out.println("サーバー起動: http://localhost:8080（止めるときは Ctrl+C）");
    }
}
