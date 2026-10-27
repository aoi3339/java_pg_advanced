import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を使えるようにします。
import java.io.IOException; // 追加：ファイルの読み書きで起きるエラーを扱えるようにします。
import java.net.InetSocketAddress; // 待ち受けるポート番号を指定できるようにします。
import java.net.URLDecoder; // 追加：送られた日本語を元に戻せるようにします。
import java.nio.charset.StandardCharsets; // 追加：UTF-8という文字コード（文字の読み取り方式）を使えるようにします。
import java.sql.Connection; // ★変更：SQLiteとの接続を扱えるようにします。
import java.sql.DriverManager; // ★変更：SQLiteへ接続できるようにします。
import java.sql.PreparedStatement; // ★変更：値を安全にSQLへ渡せるようにします。
import java.sql.ResultSet; // ★変更：SELECTの検索結果を読めるようにします。
import java.sql.SQLException; // ★変更：データベースのエラーを扱えるようにします。
import java.sql.Statement; // ★変更：表を作るSQLを実行できるようにします。
import java.util.ArrayList; // 追加：Todoを入れるリストを作れるようにします。
import java.util.List; // 追加：Todoを入れるリストの型（データの種類）を使えるようにします。

class Todo { // ★変更：Todo（やること1件分）のクラスを定義します。
    private final int id; // ★変更：何番かを保存します。
    private final String title; // ★変更：やることを保存します。
    private boolean done; // ★変更：終わったかを保存します。

    Todo(int id, String title) { // ★変更：新しいTodoを作るときに番号とやることを受け取ります。
        this.id = id; // ★変更：受け取った番号を保存します。
        this.title = title; // ★変更：受け取ったやることを保存します。
        this.done = false; // ★変更：最初は終わっていない状態にします。
    }

    int getId() { // ★変更：番号を読み出すメソッド（処理）です。
        return id; // ★変更：番号を返します。
    }

    String getTitle() { // ★変更：やることを読み出すメソッド（処理）です。
        return title; // ★変更：やることを返します。
    }

    boolean isDone() { // ★変更：終わったかを読み出すメソッド（処理）です。
        return done; // ★変更：終わったかを返します。
    }

    void setDone(boolean done) { // ★変更：終わったかを書き換えるメソッド（処理）です。
        this.done = done; // ★変更：受け取った状態に書き換えます。
    }
}

public class App { // Appという名前のクラス（プログラムのまとまり）を定義します。
    static final String DB_URL = "jdbc:sqlite:todos.db"; // ★変更：SQLiteデータベースの接続先を指定します。

    static void createTable() throws SQLException { // ★変更：Todoを保存する表を用意します。
        String sql = "CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER)"; // ★変更：必要な列を持つtodos表の定義です。
        try (Connection connection = DriverManager.getConnection(DB_URL); // ★変更：todos.dbへ接続します。
             Statement statement = connection.createStatement()) { // ★変更：SQLを実行する準備をします。
            statement.executeUpdate(sql); // ★変更：todos表が無い場合だけ作成します。
        } // ★変更：接続とStatementを自動で閉じます。
    } // ★変更：表を用意する処理を終えます。

    static void insertTodo(String title) throws IOException { // ★変更：Todoを1件追加します。
        String sql = "INSERT INTO todos (title, done) VALUES (?, ?)"; // ★変更：追加用のSQLを用意します。
        try (Connection connection = DriverManager.getConnection(DB_URL); // ★変更：todos.dbへ接続します。
             PreparedStatement statement = connection.prepareStatement(sql)) { // ★変更：INSERTを安全に実行する準備をします。
            statement.setString(1, title); // ★変更：1つ目の?へタイトルを入れます。
            statement.setInt(2, 0); // ★変更：2つ目の?へ未完了を表す0を入れます。
            statement.executeUpdate(); // ★変更：INSERTを実行します。
        } catch (SQLException e) { // ★変更：追加時のデータベースエラーを受け止めます。
            throw new IOException(e); // ★変更：HTTP処理で扱えるエラーとして渡します。
        } // ★変更：追加時のエラー処理を終えます。
    } // ★変更：追加処理を終えます。

    static void completeTodo(int id) throws IOException { // ★変更：指定されたTodoを完了にします。
        String sql = "UPDATE todos SET done = ? WHERE id = ?"; // ★変更：完了状態を更新するSQLを用意します。
        try (Connection connection = DriverManager.getConnection(DB_URL); // ★変更：todos.dbへ接続します。
             PreparedStatement statement = connection.prepareStatement(sql)) { // ★変更：UPDATEを安全に実行する準備をします。
            statement.setInt(1, 1); // ★変更：1つ目の?へ完了を表す1を入れます。
            statement.setInt(2, id); // ★変更：2つ目の?へ対象のidを入れます。
            statement.executeUpdate(); // ★変更：UPDATEを実行します。
        } catch (SQLException e) { // ★変更：更新時のデータベースエラーを受け止めます。
            throw new IOException(e); // ★変更：HTTP処理で扱えるエラーとして渡します。
        } // ★変更：更新時のエラー処理を終えます。
    } // ★変更：完了処理を終えます。

    static void deleteTodo(int id) throws IOException { // ★変更：指定されたTodoを削除します。
        String sql = "DELETE FROM todos WHERE id = ?"; // ★変更：削除用のSQLを用意します。
        try (Connection connection = DriverManager.getConnection(DB_URL); // ★変更：todos.dbへ接続します。
             PreparedStatement statement = connection.prepareStatement(sql)) { // ★変更：DELETEを安全に実行する準備をします。
            statement.setInt(1, id); // ★変更：?へ削除するTodoのidを入れます。
            statement.executeUpdate(); // ★変更：DELETEを実行します。
        } catch (SQLException e) { // ★変更：削除時のデータベースエラーを受け止めます。
            throw new IOException(e); // ★変更：HTTP処理で扱えるエラーとして渡します。
        } // ★変更：削除時のエラー処理を終えます。
    } // ★変更：削除処理を終えます。

    static List<Todo> selectTodos() throws IOException { // ★変更：todos表から全Todoを読み出します。
        List<Todo> todos = new ArrayList<>(); // ★変更：検索結果を入れるリストを用意します。
        String sql = "SELECT id, title, done FROM todos ORDER BY id"; // ★変更：id順で全件を読むSQLを用意します。
        try (Connection connection = DriverManager.getConnection(DB_URL); // ★変更：todos.dbへ接続します。
             PreparedStatement statement = connection.prepareStatement(sql); // ★変更：SELECTを実行する準備をします。
             ResultSet results = statement.executeQuery()) { // ★変更：SELECTを実行して検索結果を受け取ります。
            while (results.next()) { // ★変更：検索結果を1件ずつ取り出します。
                Todo todo = new Todo(results.getInt("id"), results.getString("title")); // ★変更：idとtitleからTodoを作ります。
                todo.setDone(results.getInt("done") == 1); // ★変更：doneが1なら完了済みにします。
                todos.add(todo); // ★変更：読み出したTodoを一覧へ加えます。
            } // ★変更：検索結果を読む処理を終えます。
        } catch (SQLException e) { // ★変更：検索時のデータベースエラーを受け止めます。
            throw new IOException(e); // ★変更：HTTP処理で扱えるエラーとして渡します。
        } // ★変更：検索時のエラー処理を終えます。
        return todos; // ★変更：データベースから読み出したTodo一覧を返します。
    } // ★変更：一覧取得処理を終えます。

    static String todosToJson(List<Todo> todos) { // Todo一覧をJSON配列へ変換します。
        StringBuilder json = new StringBuilder("["); // JSON配列の開始記号を追加します。
        for (int i = 0; i < todos.size(); i++) { // 全Todoを先頭から順番に処理します。
            Todo todo = todos.get(i); // 現在処理するTodoを取得します。
            if (i > 0) { // 2件目以降か確認します。
                json.append(","); // Todo同士を区切るカンマを追加します。
            } // カンマが必要かの確認を終えます。
            json.append("{\"title\":\""); // titleプロパティの開始部分を追加します。
            json.append(escapeJson(todo.getTitle())); // エスケープしたタイトルを追加します。
            json.append("\",\"done\":"); // doneプロパティの開始部分を追加します。
            json.append(todo.isDone()); // 完了状態をtrueまたはfalseで追加します。
            json.append("}"); // Todoオブジェクトの終了記号を追加します。
        } // 全Todoの変換を終えます。
        json.append("]"); // JSON配列の終了記号を追加します。
        return json.toString(); // 完成したJSON文字列を返します。
    } // Todo一覧をJSONへ変換する処理を終えます。

    static String escapeJson(String value) { // 文字列をJSON用にエスケープします。
        if (value == null) { // 文字列がnullか確認します。
            return ""; // nullの場合は空文字列として返します。
        } // nullの確認を終えます。
        StringBuilder escaped = new StringBuilder(); // エスケープ後の文字列を組み立てます。
        for (int i = 0; i < value.length(); i++) { // 元の文字列を1文字ずつ処理します。
            char character = value.charAt(i); // 現在処理する文字を取得します。
            switch (character) { // 特別な処理が必要な文字を判定します。
                case '"' -> escaped.append("\\\""); // ダブルクォートをエスケープします。
                case '\\' -> escaped.append("\\\\"); // バックスラッシュをエスケープします。
                case '\n' -> escaped.append("\\n"); // 改行をエスケープします。
                case '\r' -> escaped.append("\\r"); // 復帰文字をエスケープします。
                case '\t' -> escaped.append("\\t"); // タブをエスケープします。
                case '\b' -> escaped.append("\\b"); // バックスペースをエスケープします。
                case '\f' -> escaped.append("\\f"); // フォームフィードをエスケープします。
                default -> { // それ以外の文字を処理します。
                    if (character < 0x20) { // JSONに直接書けない制御文字か確認します。
                        escaped.append(String.format("\\u%04x", (int) character)); // 制御文字をUnicode表記へ変換します。
                    } else { // 通常の文字の場合を処理します。
                        escaped.append(character); // 文字をそのまま追加します。
                    } // 制御文字かどうかの確認を終えます。
                } // 通常文字と制御文字の処理を終えます。
            } // 1文字分の判定を終えます。
        } // 全文字のエスケープを終えます。
        return escaped.toString(); // 完成したエスケープ済み文字列を返します。
    } // JSON用のエスケープ処理を終えます。

    public static void main(String[] args) throws Exception { // プログラムが最初に実行する場所を定義します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートで待ち受けるサーバーを用意します。
        createTable(); // ★変更：サーバーを動かす前にtodos表を用意します。
        server.createContext("/", exchange -> { // 「/」へのアクセスを処理します。
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String method = exchange.getRequestMethod(); // 追加：GETかPOSTかという送信方法を取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 移動：通常は文字をUTF-8のプレーンテキスト（飾りのない文字）として返します。
            if (path.equals("/add") && method.equals("POST")) { // 追加：/addへPOSTで送られたか確かめます。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 追加：送られた中身をUTF-8で受け取ります。
                String value = body.substring(5); // 追加：「todo=」の5文字を除きます。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更：日本語に戻した文字をtitleに入れます。
                if (!title.isEmpty()) { // ★変更：titleが空ではないか確かめます。
                    insertTodo(title); // ★変更：PreparedStatementのINSERTでTodoを追加します。
                } // 追加：空の入力を除く処理を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // 追加：戻る場所を「/」に指定します。
                exchange.sendResponseHeaders(303, -1); // 追加：303で「/」へ移動するよう返します。
                exchange.close(); // 追加：今回の通信を閉じます。
                return; // 追加：この分岐の処理を終えます。
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加：完了リンクへのアクセスを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★追加：URLのid部分を受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加：idが付いているか確かめます。
                    try { // ★追加：数字に変えられるか試します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加：idの文字を数に変えます。
                        completeTodo(id); // ★変更：PreparedStatementのUPDATEでTodoを完了にします。
                    } catch (NumberFormatException e) { // ★追加：idが数字でない場合を受け止めます。
                    } // ★追加：数字でない場合は何も変えません。
                } // ★追加：idの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // ★追加：戻る場所を「/」に指定します。
                exchange.sendResponseHeaders(303, -1); // ★追加：303で「/」へ移動するよう返します。
                return; // ★追加：完了の処理を終えます。
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加：削除リンクへのアクセスを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★追加：URLのid部分を受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加：idが付いているか確かめます。
                    try { // ★追加：数字に変えられるか試します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加：idの文字を数に変えます。
                        deleteTodo(id); // ★変更：PreparedStatementのDELETEでTodoを削除します。
                    } catch (NumberFormatException e) { // ★追加：idが数字でない場合を受け止めます。
                    } // ★追加：数字でない場合は何も変えません。
                } // ★追加：idの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // ★追加：戻る場所を「/」に指定します。
                exchange.sendResponseHeaders(303, -1); // ★追加：303で「/」へ移動するよう返します。
                return; // ★追加：削除の処理を終えます。
            } else if (path.equals("/")) { // 追加：「/」なら入力フォームとTodo一覧を表示します。
                List<Todo> todos = selectTodos(); // ★変更：PreparedStatementのSELECTで表示するTodoを読み出します。
                String html = "<html><head><meta charset='UTF-8'><style>body { max-width: 700px; margin: 40px auto; padding: 0 20px; font-size: 18px; }</style></head><body>"; // ★変更：幅・余白・文字サイズだけを整えます。
                html += "<h1>今日のTodoリスト</h1>"; // ★変更：ページの見出しを表示します。
                html += "<form method='post' action='/add'><input name='todo'><button>追加</button></form>"; // ★変更：入力フォームを表示します。
                if (todos.isEmpty()) { // ★変更：Todoが0件か確かめます。
                    html += "<p>やることは、いまゼロです</p>"; // ★変更：0件のときの文を表示します。
                }
                html += "<ul>"; // ★変更：HTMLの箇条書きを始めます。
                for (Todo todo : todos) { // ★変更：Todoを1件ずつ取り出します。
                    String mark = ""; // ★追加：最初は印を空にします。
                    String titleStyle = ""; // ★追加：未完了のTodoには色を付けません。
                    if (todo.isDone()) { // ★変更：終わったTodoかを確かめます。
                        mark = " <span style='color: red;'>✔</span>"; // ★追加：終わったTodoの印を赤文字にします。
                        titleStyle = " style='color: lightblue;'"; // ★追加：終わったTodoの文字を薄い水色にします。
                    } // ★追加：終わったTodoの見た目を決める処理を終えます。
                    html += "<li><span" + titleStyle + ">" + todo.getTitle() + "</span>" + mark + " <a href='/done?id=" + todo.getId() + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加：Todoの文字色、印、完了と削除のリンクを表示します。
                } // 追加：繰り返しを終えます。
                html += "</ul></body></html>"; // ★変更：HTMLの箇条書きとページを閉じます。
                message = html; // 追加：組み立てたHTMLを返す内容にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // 移動：「/」だけHTMLをUTF-8として返します。
            } else { // ★変更：/addと/以外を受け止めます。
                message = "ページが見つかりません"; // ★変更：見つからないことを返します。
            }
            byte[] body = message.getBytes("UTF-8"); // 文字を送信用のバイト列（データの並び）に変えます。
            exchange.sendResponseHeaders(200, body.length); // 成功を表す200とデータの長さを返します。
            exchange.getResponseBody().write(body); // 本文のデータをブラウザへ送ります。
            exchange.getResponseBody().close(); // 送信を終えて本文を閉じます。
        }); // 「/」への処理の登録を終えます。
        server.createContext("/api/todos", exchange -> { // Todo一覧APIの入口を登録します。
            if (!exchange.getRequestMethod().equals("GET")) { // GET以外のリクエストか確認します。
                exchange.sendResponseHeaders(405, -1); // GET以外には405 Method Not Allowedを返します。
                exchange.close(); // 通信を終了します。
                return; // 以降の処理を行わず終了します。
            } // リクエスト方式の確認を終えます。
            String json = todosToJson(selectTodos()); // データベースの全TodoをJSONへ変換します。
            byte[] body = json.getBytes(StandardCharsets.UTF_8); // JSONをUTF-8のバイト列へ変換します。
            exchange.getResponseHeaders().set("Content-Type", "application/json"); // charsetなしのJSON形式を指定します。
            exchange.sendResponseHeaders(200, body.length); // 成功を表す200とデータサイズを返します。
            exchange.getResponseBody().write(body); // JSONをレスポンス本文へ書き込みます。
            exchange.getResponseBody().close(); // レスポンス本文を閉じて送信を完了します。
        }); // Todo一覧APIの登録を終えます。
        server.start(); // サーバーの待ち受けを始めます。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことと止め方を表示します。
    } // mainの処理を終えます。
} // Appクラスの定義を終えます。
