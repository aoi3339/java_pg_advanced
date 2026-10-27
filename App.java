import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を使えるようにします。
import java.io.IOException; // 追加：ファイルの読み書きで起きるエラーを扱えるようにします。
import java.net.InetSocketAddress; // 待ち受けるポート番号を指定できるようにします。
import java.net.URLDecoder; // 追加：送られた日本語を元に戻せるようにします。
import java.nio.charset.StandardCharsets; // 追加：UTF-8という文字コード（文字の読み取り方式）を使えるようにします。
import java.nio.file.Files; // 追加：ファイルを読み書きできるようにします。
import java.nio.file.Path; // 追加：todos.csvの場所を表せるようにします。
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

    static void save() throws IOException { // 追加：現在のTodo全件をtodos.csvへ保存します。
        List<String> lines = new ArrayList<>(); // 追加：ファイルへ書く行を入れるリストを用意します。
        for (Todo todo : todos) { // 追加：Todoを1件ずつ取り出します。
            String title = todo.getTitle().replace("\r", " ").replace("\n", " "); // 追加：1件を必ず1行で保存できるよう改行を空白にします。
            String done = todo.isDone() ? "1" : "0"; // 追加：完了なら1、未完了なら0にします。
            lines.add(todo.getId() + "," + done + "," + title); // 追加：id、完了状態、タイトルの順で1行を作ります。
        } // 追加：全Todoを保存用の行にする処理を終えます。
        Files.write(Path.of("todos.csv"), lines, StandardCharsets.UTF_8); // 追加：全件をUTF-8でtodos.csvへ書き出します。
    } // 追加：保存の処理を終えます。

    static void load() throws IOException { // 追加：todos.csvからTodo全件を読み込みます。
        Path path = Path.of("todos.csv"); // 追加：読み込むtodos.csvの場所を用意します。
        if (!Files.exists(path)) { // 追加：todos.csvが無いか確かめます。
            return; // 追加：ファイルが無ければ空のまま読み込みを終えます。
        } // 追加：ファイルの有無を確かめる処理を終えます。
        todos.clear(); // 追加：ファイルの内容を正本にするため現在のリストを空にします。
        int maxId = 0; // 追加：読み込んだ中で一番大きいidを覚える変数を用意します。
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) { // 追加：UTF-8で読み込んだ行を1行ずつ処理します。
            String[] values = line.split(",", 3); // 追加：1行をid、完了状態、タイトルの3つに分けます。
            if (values.length != 3) { // 追加：3つの値がそろっているか確かめます。
                continue; // 追加：形式が違う行は読み飛ばします。
            } // 追加：値の数を確かめる処理を終えます。
            try { // 追加：idを数字として読み込めるか試します。
                int id = Integer.parseInt(values[0]); // 追加：先頭の値をidに戻します。
                Todo todo = new Todo(id, values[2]); // 追加：読み込んだidとタイトルでTodoを戻します。
                todo.setDone(values[1].equals("1")); // 追加：完了状態が1なら完了済みに戻します。
                todos.add(todo); // 追加：戻したTodoをリストへ入れます。
                maxId = Math.max(maxId, id); // 追加：一番大きいidを更新します。
            } catch (NumberFormatException e) { // 追加：idが数字でない行を受け止めます。
            } // 追加：idが数字でない行は読み飛ばします。
        } // 追加：全行を読み込む処理を終えます。
        nextId = maxId + 1; // 追加：次の番号を一番大きいidの次にします。
    } // 追加：読み込みの処理を終えます。

    public static void main(String[] args) throws Exception { // プログラムが最初に実行する場所を定義します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートで待ち受けるサーバーを用意します。
        load(); // 追加：サーバーを動かす前にtodos.csvからTodo全件を読み込みます。
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
                    save(); // 追加：Todoを追加した直後に全件を保存します。
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
                                save(); // 追加：Todoを完了にした直後に全件を保存します。
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
                        boolean removed = todos.removeIf(todo -> todo.getId() == id); // 変更：Todoを取り除き、リストが変わったか覚えます。
                        if (removed) { // 追加：Todoが実際に削除されたか確かめます。
                            save(); // 追加：Todoを削除した直後に全件を保存します。
                        } // 追加：削除後に保存する処理を終えます。
                    } catch (NumberFormatException e) { // ★追加：idが数字でない場合を受け止めます。
                    } // ★追加：数字でない場合は何も変えません。
                } // ★追加：idの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // ★追加：戻る場所を「/」に指定します。
                exchange.sendResponseHeaders(303, -1); // ★追加：303で「/」へ移動するよう返します。
                return; // ★追加：削除の処理を終えます。
            } else if (path.equals("/")) { // 追加：「/」なら入力フォームとTodo一覧を表示します。
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
        server.start(); // サーバーの待ち受けを始めます。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことと止め方を表示します。
    } // mainの処理を終えます。
} // Appクラスの定義を終えます。
