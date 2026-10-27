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
    private final String examPoint;
    private boolean memorized;

    Term(int id, String word, String meaning, String examPoint) {
        this.id = id;
        this.word = word;
        this.meaning = meaning;
        this.examPoint = examPoint;
        this.memorized = false;
    }

    int getId() {
        return id;
    }

    String getWord() {
        return word;
    }

    String getMeaning() {
        return meaning;
    }

    String getExamPoint() {
        return examPoint;
    }

    boolean isMemorized() {
        return memorized;
    }

    void setMemorized(boolean memorized) {
        this.memorized = memorized;
    }
}

public class App {
    static final String DB_URL = "jdbc:sqlite:terms.db";

    static void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS terms ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "word TEXT NOT NULL, "
                + "meaning TEXT NOT NULL, "
                + "exam_point TEXT NOT NULL DEFAULT '', "
                + "memorized INTEGER NOT NULL DEFAULT 0)";

        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    static void insertTerm(String word, String meaning, String examPoint) throws IOException {
        String sql = "INSERT INTO terms (word, meaning, exam_point, memorized) VALUES (?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, word);
            statement.setString(2, meaning);
            statement.setString(3, examPoint);
            statement.setInt(4, 0);
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
        String sql = "SELECT id, word, meaning, exam_point, memorized FROM terms ORDER BY id";

        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                Term term = new Term(
                        results.getInt("id"),
                        results.getString("word"),
                        results.getString("meaning"),
                        results.getString("exam_point")
                );
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
            if (i > 0) {
                json.append(",");
            }
            json.append("{\"word\":\"");
            json.append(escapeJson(term.getWord()));
            json.append("\",\"meaning\":\"");
            json.append(escapeJson(term.getMeaning()));
            json.append("\",\"examPoint\":\"");
            json.append(escapeJson(term.getExamPoint()));
            json.append("\",\"memorized\":");
            json.append(term.isMemorized());
            json.append("}");
        }
        json.append("]");
        return json.toString();
    }

    static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    static String termToHtml(Term term) {
        String html = "<article>";
        html += "<h2>" + escapeHtml(term.getWord()) + "とは？</h2>";
        html += "<details><summary>答えを見る</summary><p>";
        html += escapeHtml(term.getMeaning());
        html += "</p><h3>試験のポイント</h3><p>";
        html += escapeHtml(term.getExamPoint());
        html += "</p></details>";

        if (term.isMemorized()) {
            html += "<p>状態：覚えた</p>";
            html += "<a href='/memorized?id=" + term.getId()
                    + "&memorized=false'>復習するに戻す</a>";
        } else {
            html += "<p>状態：復習する</p>";
            html += "<a href='/memorized?id=" + term.getId()
                    + "&memorized=true'>覚えた</a>";
        }
        html += "　<a href='/delete?id=" + term.getId() + "'>削除</a>";
        html += "</article>";
        return html;
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        createTable();

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                String word = "";
                String meaning = "";
                String examPoint = "";
                String[] values = body.split("&");
                for (String value : values) {
                    if (value.startsWith("word=")) {
                        word = URLDecoder.decode(value.substring(5), StandardCharsets.UTF_8);
                    } else if (value.startsWith("meaning=")) {
                        meaning = URLDecoder.decode(value.substring(8), StandardCharsets.UTF_8);
                    } else if (value.startsWith("examPoint=")) {
                        examPoint = URLDecoder.decode(value.substring(10), StandardCharsets.UTF_8);
                    }
                }

                if (!word.isEmpty() && !meaning.isEmpty() && !examPoint.isEmpty()) {
                    insertTerm(word, meaning, examPoint);
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            }

            if (path.equals("/memorized") && method.equals("GET")) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null) {
                    String[] values = query.split("&");
                    int id = Integer.parseInt(values[0].substring(3));
                    boolean memorized = Boolean.parseBoolean(values[1].substring(11));
                    updateMemorized(id, memorized);
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            }

            if (path.equals("/delete") && method.equals("GET")) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    int id = Integer.parseInt(query.substring(3));
                    deleteTerm(id);
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            }

            if (path.equals("/")) {
                List<Term> terms = selectTerms();
                String html = "<html><head><meta charset='UTF-8'>";
                html += "<style>";
                html += "body{max-width:700px;margin:40px auto;padding:0 20px;font-size:18px;background:#eef5f7;color:#183153;}";
                html += "h1{color:#075985;border-bottom:4px solid #14b8a6;padding-bottom:10px;}";
                html += "form{background:#ffffff;padding:20px;border-radius:10px;}";
                html += "label{font-weight:bold;color:#075985;}";
                html += "input,textarea{width:100%;margin:5px 0 15px;padding:8px;border:1px solid #94a3b8;border-radius:6px;}";
                html += "button{background:#0f766e;color:white;border:0;border-radius:6px;padding:10px 20px;font-size:16px;}";
                html += "article{background:#ffffff;border-left:6px solid #14b8a6;padding:15px;margin:15px 0;border-radius:8px;}";
                html += "summary{color:#0369a1;font-weight:bold;cursor:pointer;}";
                html += "a{color:#0f766e;font-weight:bold;}";
                html += ".learned-list{margin-top:30px;padding:15px;background:#dcebea;border-radius:8px;}";
                html += "</style></head><body>";
                html += "<h1>セキュマネ用語帳</h1>";
                html += "<form method='post' action='/add'>";
                html += "<label>用語</label><input name='word' required>";
                html += "<label>意味</label><textarea name='meaning' required></textarea>";
                html += "<label>試験のポイント</label><textarea name='examPoint' required></textarea>";
                html += "<button>登録</button></form>";

                html += "<h2>復習する用語</h2>";
                int reviewCount = 0;
                for (Term term : terms) {
                    if (!term.isMemorized()) {
                        html += termToHtml(term);
                        reviewCount++;
                    }
                }
                if (reviewCount == 0) {
                    html += "<p>復習する用語はありません。</p>";
                }

                int learnedCount = 0;
                for (Term term : terms) {
                    if (term.isMemorized()) {
                        learnedCount++;
                    }
                }

                html += "<details class='learned-list'><summary>覚えた用語（";
                html += learnedCount + "件）</summary>";
                for (Term term : terms) {
                    if (term.isMemorized()) {
                        html += termToHtml(term);
                    }
                }
                html += "</details>";

                html += "</body></html>";
                byte[] response = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
                return;
            }

            byte[] response = "ページが見つかりません".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        server.createContext("/api/terms", exchange -> {
            String json = termsToJson(selectTerms());
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        server.start();
        System.out.println("サーバー起動: http://localhost:8080（止めるときは Ctrl+C）");
    }
}
