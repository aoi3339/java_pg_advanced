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

    int getId() { return id; }
    String getWord() { return word; }
    String getMeaning() { return meaning; }
    String getExamPoint() { return examPoint; }
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

    static void updateTerm(int id, String word, String meaning, String examPoint) throws IOException {
        String sql = "UPDATE terms SET word = ?, meaning = ?, exam_point = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, word);
            statement.setString(2, meaning);
            statement.setString(3, examPoint);
            statement.setInt(4, id);
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
                Term term = new Term(results.getInt("id"), results.getString("word"),
                        results.getString("meaning"), results.getString("exam_point"));
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
            json.append("\",\"examPoint\":\"").append(escapeJson(term.getExamPoint()));
            json.append("\",\"memorized\":").append(term.isMemorized()).append("}");
        }
        return json.append("]").toString();
    }

    static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static String formValue(String body, String name) {
        String prefix = name + "=";
        for (String value : body.split("&")) {
            if (value.startsWith(prefix)) {
                return URLDecoder.decode(value.substring(prefix.length()), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    static Term findTerm(int id) throws IOException {
        for (Term term : selectTerms()) {
            if (term.getId() == id) return term;
        }
        return null;
    }

    static String termToHtml(Term term) {
        String html = "<li class='term-card'><div class='term-title'><strong>" + escapeHtml(term.getWord()) + "とは？</strong>";
        html += term.isMemorized() ? "<span class='badge'>覚えた</span>" : "<span class='badge learning'>学習中</span>";
        html += "</div><details class='answer'><summary>答えを見る</summary><div class='answer-body'><p>";
        html += escapeHtml(term.getMeaning());
        html += "</p><h3>試験のポイント</h3><p>";
        html += escapeHtml(term.getExamPoint());
        html += "</p></div></details><div class='actions'>";
        html += "<a class='action primary' href='/memorized?id=" + term.getId() + "&memorized=";
        html += !term.isMemorized() + "'>";
        html += term.isMemorized() ? "復習する" : "覚えた";
        html += "</a>";
        html += "<a class='action edit' href='/edit?id=" + term.getId() + "'>編集</a>";
        html += "<a class='action delete' href='/delete?id=" + term.getId() + "'>削除</a></div></li>";
        return html;
    }

    static String pageStyles() {
        return "<style>"
                + "*{box-sizing:border-box}"
                + "body{margin:0;background:#f4f7fb;color:#24324a;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;line-height:1.7}"
                + ".page{width:min(920px,calc(100% - 32px));margin:0 auto;padding:48px 0 72px}"
                + ".hero{margin-bottom:28px}"
                + ".hero h1{margin:0;color:#173b72;font-size:clamp(28px,5vw,42px)}"
                + ".hero p{margin:6px 0 0;color:#65738b}"
                + ".panel{padding:24px;margin-bottom:34px;background:#fff;border:1px solid #dfe7f2;border-radius:18px;box-shadow:0 10px 30px rgba(38,73,120,.08)}"
                + ".form-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}"
                + ".field{display:flex;flex-direction:column;gap:6px}.field.wide{grid-column:1/-1}"
                + "label{font-size:14px;font-weight:700;color:#42526b}"
                + "input{width:100%;padding:12px 14px;border:1px solid #cbd6e5;border-radius:10px;font:inherit;outline:none}"
                + "input:focus{border-color:#3978d4;box-shadow:0 0 0 3px rgba(57,120,212,.14)}"
                + "button{grid-column:1/-1;padding:13px;border:0;border-radius:10px;background:#2367c7;color:#fff;font:inherit;font-weight:700;cursor:pointer}"
                + "button:hover{background:#1957ad}"
                + ".section-title{margin:0 0 14px;font-size:22px;color:#173b72}"
                + ".term-list{display:grid;gap:16px;margin:0 0 34px;padding:0;list-style:none}"
                + ".term-card{padding:20px;background:#fff;border:1px solid #dfe7f2;border-radius:16px;box-shadow:0 5px 18px rgba(38,73,120,.06)}"
                + ".term-title{display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:18px}"
                + ".badge{flex:none;padding:3px 9px;border-radius:999px;background:#def6e8;color:#187443;font-size:12px;font-weight:700}"
                + ".badge.learning{background:#e6effd;color:#245da9}"
                + ".answer{margin:14px 0}.answer summary{cursor:pointer;color:#2367c7;font-weight:700}"
                + ".answer-body{margin-top:12px;padding:14px 16px;background:#f6f9fd;border-left:4px solid #79a7e7;border-radius:8px}"
                + ".answer-body p{margin:0}.answer-body h3{margin:14px 0 4px;font-size:14px;color:#52647e}"
                + ".actions{display:flex;gap:10px;margin-top:14px}"
                + ".action{padding:8px 13px;border-radius:9px;text-decoration:none;font-size:14px;font-weight:700}"
                + ".action.primary{background:#e7f0fd;color:#195bad}.action.edit{background:#edf2f7;color:#42526b}.action.delete{color:#a63d45;background:#fff0f1}"
                + ".memorized{padding:18px 20px;background:#eaf1fa;border-radius:16px}.memorized>summary{cursor:pointer;font-weight:700;color:#294e80}"
                + ".memorized .term-list{margin:18px 0 0}"
                + "@media(max-width:600px){.page{padding-top:28px}.panel{padding:18px}.form-grid{grid-template-columns:1fr}.term-title{align-items:flex-start}}"
                + "</style>";
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        createTable();

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String word = formValue(body, "word");
                String meaning = formValue(body, "meaning");
                String examPoint = formValue(body, "examPoint");
                if (!word.isEmpty() && !meaning.isEmpty() && !examPoint.isEmpty()) {
                    insertTerm(word, meaning, examPoint);
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            }

            if (path.equals("/update") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                int id = Integer.parseInt(formValue(body, "id"));
                String word = formValue(body, "word");
                String meaning = formValue(body, "meaning");
                String examPoint = formValue(body, "examPoint");
                if (!word.isEmpty() && !meaning.isEmpty() && !examPoint.isEmpty()) {
                    updateTerm(id, word, meaning, examPoint);
                }
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

            if (path.equals("/edit") && method.equals("GET")) {
                int id = Integer.parseInt(exchange.getRequestURI().getQuery().substring(3));
                Term term = findTerm(id);
                if (term == null) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                String html = "<!doctype html><html lang='ja'><head><meta charset='UTF-8'>";
                html += "<meta name='viewport' content='width=device-width,initial-scale=1'>";
                html += "<title>用語を編集</title>" + pageStyles() + "</head><body><main class='page'>";
                html += "<header class='hero'><h1>用語を編集</h1><p>登録内容を直して保存できます。</p></header>";
                html += "<section class='panel'><form class='form-grid' method='post' action='/update'>";
                html += "<input type='hidden' name='id' value='" + term.getId() + "'>";
                html += "<div class='field'><label>用語</label><input name='word' value='" + escapeHtml(term.getWord()) + "' required></div>";
                html += "<div class='field'><label>意味</label><input name='meaning' value='" + escapeHtml(term.getMeaning()) + "' required></div>";
                html += "<div class='field wide'><label>試験のポイント</label><input name='examPoint' value='" + escapeHtml(term.getExamPoint()) + "' required></div>";
                html += "<button>変更を保存する</button><a class='action edit' href='/'>キャンセル</a>";
                html += "</form></section></main></body></html>";
                byte[] response = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
                return;
            }

            if (path.equals("/")) {
                String html = "<!doctype html><html lang='ja'><head><meta charset='UTF-8'>";
                html += "<meta name='viewport' content='width=device-width,initial-scale=1'>";
                html += "<title>セキュマネ用語帳</title>" + pageStyles() + "</head><body><main class='page'>";
                html += "<header class='hero'><h1>セキュマネ用語帳</h1><p>答えを隠して、ひとつずつ覚えよう。</p></header>";
                html += "<section class='panel'><h2 class='section-title'>新しい用語を追加</h2>";
                html += "<form class='form-grid' method='post' action='/add'>";
                html += "<div class='field'><label>用語</label><input name='word' placeholder='例：機密性' required></div>";
                html += "<div class='field'><label>意味</label><input name='meaning' placeholder='用語の意味を入力' required></div>";
                html += "<div class='field wide'><label>試験のポイント</label><input name='examPoint' placeholder='覚えておきたいポイント' required></div>";
                html += "<button>用語を追加する</button></form></section>";

                List<Term> terms = selectTerms();
                html += "<section><h2 class='section-title'>復習する用語</h2><ul class='term-list'>";
                for (Term term : terms) {
                    if (!term.isMemorized()) {
                        html += termToHtml(term);
                    }
                }
                html += "</ul></section>";

                int memorizedCount = 0;
                for (Term term : terms) {
                    if (term.isMemorized()) {
                        memorizedCount++;
                    }
                }

                html += "<details class='memorized'><summary>覚えた用語（" + memorizedCount + "件）</summary><ul class='term-list'>";
                for (Term term : terms) {
                    if (term.isMemorized()) {
                        html += termToHtml(term);
                    }
                }
                html += "</ul></details></main></body></html>";
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
