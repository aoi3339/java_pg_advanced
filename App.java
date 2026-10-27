import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を使えるようにします。
import java.net.InetSocketAddress; // 待ち受けるポート番号を指定できるようにします。
import java.net.URLDecoder; // 追加：送られた日本語を元に戻せるようにします。
import java.nio.charset.StandardCharsets; // 追加：UTF-8という文字コード（文字の読み取り方式）を使えるようにします。
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
    static List<Todo> todos = new ArrayList<>(); // ★変更：Todoをためるリストをmainの外に置きます。
    static int nextId = 1; // ★変更：次に振る番号を1から始めます。

    public static void main(String[] args) throws Exception { // プログラムが最初に実行する場所を定義します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートで待ち受けるサーバーを用意します。
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更：1番のサンプルを追加します。
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更：2番のサンプルを作ります。
        egg.setDone(false); // ★変更：確認のため、卵を買うTodoを一時的に終わっていない状態にします。
        todos.add(egg); // ★変更：卵を買うTodoをリストに追加します。
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
                    todos.add(new Todo(nextId, title)); // ★変更：次の番号とtitleでTodoを作り、リストに足します。
                    nextId++; // ★変更：次に振る番号を1つ増やします。
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
                        for (Todo todo : todos) { // ★追加：Todoを1件ずつ確認します。
                            if (todo.getId() == id) { // ★追加：idが一致するか確かめます。
                                todo.setDone(true); // ★追加：一致したTodoを完了にします。
                                break; // ★追加：一致したので繰り返しを抜けます。
                            } // ★追加：idが一致したときの処理を終えます。
                        } // ★追加：Todoを1件ずつ見る処理を終えます。
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
                        todos.removeIf(todo -> todo.getId() == id); // ★追加：idが一致するTodoをリストから取り除きます。
                    } catch (NumberFormatException e) { // ★追加：idが数字でない場合を受け止めます。
                    } // ★追加：数字でない場合は何も変えません。
                } // ★追加：idの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // ★追加：戻る場所を「/」に指定します。
                exchange.sendResponseHeaders(303, -1); // ★追加：303で「/」へ移動するよう返します。
                return; // ★追加：削除の処理を終えます。
            } else if (path.equals("/")) { // 追加：「/」なら入力フォームとTodo一覧を表示します。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // 追加：入力フォームとHTMLの箇条書きを始めます。
                for (Todo todo : todos) { // ★変更：Todoを1件ずつ取り出します。
                    String mark = ""; // ★変更：最初は印を空にします。
                    if (todo.isDone()) { // ★変更：終わったTodoかを確かめます。
                        mark = " ✔"; // ★変更：終わったTodoにだけ印を付けます。
                    }
                    html += "<li>" + todo.getTitle() + mark + " <a href='/done?id=" + todo.getId() + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加：Todoのidを入れた完了と削除のリンクを足します。
                } // 追加：繰り返しを終えます。
                html += "</ul>"; // 追加：HTMLの箇条書きを閉じます。
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
        server.start(); // サーバーの待ち受けを始めます。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことと止め方を表示します。
    } // mainの処理を終えます。
} // Appクラスの定義を終えます。
