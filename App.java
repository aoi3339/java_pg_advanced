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
    private final String category;
    private final int importance;
    private boolean memorized;

    Term(int id, String word, String meaning, String examPoint, String category, int importance) {
        this.id = id;
        this.word = word;
        this.meaning = meaning;
        this.examPoint = examPoint;
        this.category = category;
        this.importance = importance;
        this.memorized = false;
    }

    int getId() { return id; }
    String getWord() { return word; }
    String getMeaning() { return meaning; }
    String getExamPoint() { return examPoint; }
    String getCategory() { return category; }
    int getImportance() { return importance; }
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
                + "category TEXT NOT NULL DEFAULT '情報セキュリティ', "
                + "importance INTEGER NOT NULL DEFAULT 2, "
                + "memorized INTEGER NOT NULL DEFAULT 0)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            if (!columnExists(connection, "category")) {
                statement.executeUpdate("ALTER TABLE terms ADD COLUMN category TEXT NOT NULL DEFAULT '情報セキュリティ'");
            }
            if (!columnExists(connection, "importance")) {
                statement.executeUpdate("ALTER TABLE terms ADD COLUMN importance INTEGER NOT NULL DEFAULT 2");
            }
        }
    }

    static boolean columnExists(Connection connection, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("PRAGMA table_info(terms)")) {
            while (results.next()) {
                if (columnName.equals(results.getString("name"))) return true;
            }
        }
        return false;
    }

    static void insertTerm(String word, String meaning, String examPoint, String category, int importance) throws IOException {
        String sql = "INSERT INTO terms (word, meaning, exam_point, category, importance, memorized) VALUES (?, ?, ?, ?, ?, 0)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, word);
            statement.setString(2, meaning);
            statement.setString(3, examPoint);
            statement.setString(4, category);
            statement.setInt(5, importance);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static void insertInitialTerms() throws IOException {
        String[][] initialTerms = {
                {
                        "SQLインジェクション",
                        "Webアプリケーションの入力欄などに不正なSQL文を入力し、データベースを不正操作する攻撃。情報の窃取・改ざん・削除などを引き起こす。対策にはプレースホルダの利用が有効。",
                        "プレースホルダを利用し、SQL文と入力データを分離することで、不正なSQL文の実行を防ぐ。\n\n【混同しやすい用語：クロスサイトスクリプティング（XSS）】\nSQLインジェクションはデータベースを不正操作する攻撃。XSSは主に利用者のブラウザ上で不正なスクリプトを実行させる攻撃。"
                },
                {
                        "リスクアセスメント",
                        "情報資産に対する脅威や脆弱性を特定し、リスクの発生可能性や影響度を分析・評価する活動。対策の優先順位を判断するために実施する。",
                        "「リスク特定 → リスク分析 → リスク評価」の順に実施する。リスク対応はアセスメント後の工程。\n\n【混同しやすい用語：リスク対応】\nリスクアセスメントはリスクを分析・評価する活動。リスク対応はリスクの低減・回避・共有・保有などの対処方法を選択し、実施する活動。"
                },
                {
                        "ISMS（情報セキュリティマネジメントシステム）",
                        "情報資産の機密性・完全性・可用性を維持するため、組織が情報セキュリティを継続的に管理・改善する仕組み。国際規格ISO/IEC 27001に要求事項が定められている。",
                        "ISMSは技術的対策だけでなく、組織の管理体制や運用も含む。PDCAサイクルによる継続的改善が重要。"
                },
                {
                        "機密性・完全性・可用性（CIA）",
                        "情報セキュリティの基本となる三要素。機密性は許可された者だけが情報にアクセスできること、完全性は情報の正確性と完全性を保つこと、可用性は必要なときに利用できること。",
                        "情報漏えいは「機密性」、データ改ざんは「完全性」、システム停止は「可用性」に関わる。"
                },
                {
                        "CSIRT（シーサート）",
                        "組織内で発生した情報セキュリティインシデントに対応する専門チーム。事故の分析、被害拡大の防止、復旧支援、再発防止策の検討などを行う。",
                        "CSIRTはインシデント対応を担う組織。SOCは主にシステムやネットワークの監視・脅威検知を担う。\n\n【混同しやすい用語：SOC（ソック）】\nCSIRTはセキュリティ事故への対応が中心。SOCはシステムの監視や不審な通信の検知・分析が中心。"
                },
                {
                        "多要素認証（MFA）",
                        "知識情報・所持情報・生体情報のうち、異なる種類の認証要素を二つ以上組み合わせる認証方式。パスワード漏えい時の不正ログイン防止に有効。",
                        "パスワード＋秘密の質問は、どちらも知識情報なので多要素認証ではない。\n\n【混同しやすい用語：2段階認証】\n多要素認証は異なる種類の認証要素を組み合わせる方式。2段階認証は認証を2段階で行う方式で、同じ種類の要素でも成立する。"
                },
                {
                        "フィッシング",
                        "実在する企業や金融機関を装ったメールや偽サイトを使い、ID・パスワード・クレジットカード情報などを不正に取得する攻撃手法。",
                        "特定の組織や個人を狙う「スピアフィッシング」、SMSを悪用する「スミッシング」も出題される。"
                },
                {
                        "ランサムウェア",
                        "ファイルの暗号化やシステムの利用制限を行い、復旧と引き換えに金銭を要求するマルウェア。窃取した情報の公開を脅迫材料にする場合もある。",
                        "データの暗号化に加え、窃取した情報の公開をちらつかせる手法を「二重脅迫」と呼ぶ。"
                },
                {
                        "デジタル署名",
                        "電子データの作成者の真正性と改ざんの有無を確認する技術。送信者の秘密鍵で署名を生成し、受信者が送信者の公開鍵で検証する。",
                        "デジタル署名は「秘密鍵で署名・公開鍵で検証」。データの暗号化による機密性の確保とは目的が異なる。\n\n【混同しやすい用語：暗号化】\nデジタル署名は送信者の真正性や改ざんの有無を確認する技術。暗号化は第三者にデータの内容を読まれないようにする技術。"
                },
                {
                        "バックアップ",
                        "障害・誤操作・災害・マルウェア感染などによるデータ消失に備え、複製を別の場所に保存すること。定期的な取得と復元可能かの確認が重要。",
                        "フルバックアップは全データ、差分バックアップは直近のフルバックアップ以降の変更データを保存する。"
                }
        };

        String existsSql = "SELECT 1 FROM terms WHERE word = ? LIMIT 1";
        String insertSql = "INSERT INTO terms (word, meaning, exam_point, memorized) VALUES (?, ?, ?, 0)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement existsStatement = connection.prepareStatement(existsSql);
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            for (String[] term : initialTerms) {
                existsStatement.setString(1, term[0]);
                try (ResultSet result = existsStatement.executeQuery()) {
                    if (result.next()) continue;
                }
                insertStatement.setString(1, term[0]);
                insertStatement.setString(2, term[1]);
                insertStatement.setString(3, term[2]);
                insertStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static void insertAdditionalTerms() throws IOException {
        String[][] terms = {
                {"クロスサイトスクリプティング（XSS）", "Webページへ不正なスクリプトを埋め込み、閲覧した利用者のブラウザ上で実行させる攻撃。Cookieの窃取や偽画面への誘導などに悪用される。", "出力時のエスケープ処理や入力値の検証が基本対策。SQLインジェクションがDBを狙うのに対し、XSSは利用者のブラウザを狙う。", "情報セキュリティ", "3"},
                {"マルウェア", "コンピュータや利用者に不利益を与える目的で作られた悪意あるソフトウェアの総称。ウイルス、ワーム、トロイの木馬、ランサムウェアなどを含む。", "ウイルスは他のファイルへ寄生し、ワームは単独でネットワークを介して自己増殖する。トロイの木馬は正規ソフトを装う。", "情報セキュリティ", "3"},
                {"脆弱性", "ソフトウェアやシステムの設計・実装・運用上の弱点で、攻撃に悪用される可能性があるもの。", "発見後は修正プログラムの適用や回避策を実施する。脅威は損害を生じさせる原因、脆弱性はそれにつけ込まれる弱点。", "情報セキュリティ", "3"},
                {"暗号化", "データを第三者が読めない形式へ変換し、機密性を確保する技術。共通鍵暗号方式と公開鍵暗号方式が代表的。", "共通鍵暗号は処理が速いが鍵配送が課題。公開鍵暗号は鍵配送が容易だが処理が比較的遅い。", "情報セキュリティ", "3"},
                {"ハッシュ関数", "入力データから固定長のハッシュ値を生成する一方向性の関数。改ざん検知やパスワード保存などに利用される。", "同じ入力から同じ値を生成し、ハッシュ値から元データを復元することは困難。暗号化と異なり復号しない。", "情報セキュリティ", "3"},
                {"ファイアウォール", "ネットワークの境界で通信を監視し、設定したルールに基づいて許可または遮断する仕組み。", "送信元・宛先IPアドレスやポート番号などで通信を制御する。Webアプリへの攻撃対策にはWAFが用いられる。", "ネットワーク・システム", "3"},
                {"WAF（Web Application Firewall）", "Webアプリケーションへの通信内容を検査し、SQLインジェクションやXSSなどの攻撃を検知・遮断する仕組み。", "一般的なファイアウォールがIPアドレスやポートを主に見るのに対し、WAFはHTTP通信の内容を検査する。", "情報セキュリティ", "3"},
                {"TLS", "インターネット上の通信を暗号化し、盗聴や改ざん、なりすましを防ぐためのプロトコル。HTTPSで利用される。", "サーバ証明書で接続先を確認し、通信内容を暗号化する。SSLはTLSの前身で、現在はTLSが用いられる。", "ネットワーク・システム", "3"},
                {"インシデント対応", "情報セキュリティ事故の発生を検知し、初動対応、封じ込め、根絶、復旧、再発防止を行う一連の活動。", "証拠保全を意識し、被害拡大の防止と原因究明を進める。事前に連絡体制や手順を定めておく。", "セキュリティ管理", "3"},
                {"情報セキュリティポリシー", "組織が情報セキュリティへ取り組む方針や行動基準を定めた文書体系。基本方針、対策基準、実施手順などで構成される。", "経営者が方針を示し、従業員へ周知し、環境変化に応じて定期的に見直す。", "セキュリティ管理", "3"},
                {"アクセス制御", "利用者やプログラムに対し、情報資産へのアクセスを許可または制限する仕組み。認証後に権限を確認する。", "最小権限の原則では、業務に必要な最小限の権限だけを付与する。認証は本人確認、認可は操作権限の決定。", "情報セキュリティ", "3"},
                {"VPN", "インターネットなどの公衆網上に暗号化された仮想的な専用ネットワークを構築する技術。", "拠点間接続やリモートアクセスに利用する。通信経路を暗号化するが、端末自体の安全対策も別途必要。", "ネットワーク・システム", "2"},
                {"DMZ", "外部ネットワークと内部ネットワークの間に設ける隔離された領域。公開Webサーバやメールサーバなどを配置する。", "公開サーバが侵害されても内部ネットワークへ直接侵入されにくくする。非武装地帯とも呼ばれる。", "ネットワーク・システム", "2"},
                {"DNS", "ドメイン名とIPアドレスを対応付ける仕組み。利用者が名前でサーバへアクセスできるようにする。", "DNSキャッシュポイズニングは偽の名前解決情報を登録し、不正サイトへ誘導する攻撃。DNSSECは応答の正当性を検証する。", "ネットワーク・システム", "2"},
                {"デジタル証明書（公開鍵証明書）", "公開鍵が特定の本人や組織のものであることを、認証局が電子的に証明するもの。", "認証局（CA）が証明書を発行する。PKIは証明書や公開鍵を安全に管理・利用するための基盤。", "情報セキュリティ", "2"},
                {"ゼロデイ攻撃", "脆弱性が発見されてから修正プログラムが提供・適用されるまでの間に、その脆弱性を悪用する攻撃。", "修正前の脆弱性を狙うため、パッチ適用だけでは間に合わない場合がある。多層防御や監視が重要。", "情報セキュリティ", "2"},
                {"BCP（事業継続計画）", "災害や事故が発生しても重要業務を継続し、または目標時間内に復旧するための計画。", "平常時に優先業務、代替手段、連絡体制、復旧手順を定め、訓練と見直しを行う。", "セキュリティ管理", "2"},
                {"RTO・RPO", "RTOは障害発生後に業務を復旧させるまでの目標時間、RPOはどの時点までデータを戻せればよいかを示す目標復旧時点。", "RTOは時間、RPOは失ってよいデータ量に関係する。バックアップ間隔はRPOを考慮して決める。", "セキュリティ管理", "2"},
                {"リスク対応", "評価したリスクに対して、低減、回避、共有、保有のいずれかを選択し実施する活動。", "保険への加入はリスク共有、危険な業務の中止は回避、対策の導入は低減、受容する判断は保有。", "セキュリティ管理", "2"},
                {"個人情報保護法", "個人情報を取り扱う事業者が守るべき義務などを定め、個人の権利利益を保護する法律。", "利用目的を特定・通知し、目的の範囲内で適正に利用する。漏えい防止のため安全管理措置を講じる。", "法務・コンプライアンス", "2"}
        };

        String existsSql = "SELECT 1 FROM terms WHERE word = ? LIMIT 1";
        String insertSql = "INSERT INTO terms (word, meaning, exam_point, category, importance, memorized) VALUES (?, ?, ?, ?, ?, 0)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement exists = connection.prepareStatement(existsSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (String[] term : terms) {
                exists.setString(1, term[0]);
                try (ResultSet result = exists.executeQuery()) {
                    if (result.next()) continue;
                }
                insert.setString(1, term[0]);
                insert.setString(2, term[1]);
                insert.setString(3, term[2]);
                insert.setString(4, term[3]);
                insert.setInt(5, Integer.parseInt(term[4]));
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static void classifyExistingTerms() throws IOException {
        String[][] classifications = {
                {"SQLインジェクション", "情報セキュリティ", "3"}, {"リスクアセスメント", "セキュリティ管理", "3"},
                {"ISMS（情報セキュリティマネジメントシステム）", "セキュリティ管理", "3"}, {"機密性・完全性・可用性（CIA）", "情報セキュリティ", "3"},
                {"CSIRT（シーサート）", "セキュリティ管理", "3"}, {"多要素認証（MFA）", "情報セキュリティ", "3"},
                {"フィッシング", "情報セキュリティ", "3"}, {"ランサムウェア", "情報セキュリティ", "3"},
                {"デジタル署名", "情報セキュリティ", "3"}, {"バックアップ", "ネットワーク・システム", "2"}
        };
        String sql = "UPDATE terms SET category = ?, importance = ? WHERE word = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String[] item : classifications) {
                statement.setString(1, item[1]);
                statement.setInt(2, Integer.parseInt(item[2]));
                statement.setString(3, item[0]);
                statement.executeUpdate();
            }
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

    static void updateTerm(int id, String word, String meaning, String examPoint, String category, int importance) throws IOException {
        String sql = "UPDATE terms SET word = ?, meaning = ?, exam_point = ?, category = ?, importance = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, word);
            statement.setString(2, meaning);
            statement.setString(3, examPoint);
            statement.setString(4, category);
            statement.setInt(5, importance);
            statement.setInt(6, id);
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
        String sql = "SELECT id, word, meaning, exam_point, category, importance, memorized FROM terms "
                + "ORDER BY CASE category WHEN '情報セキュリティ' THEN 1 WHEN 'セキュリティ管理' THEN 2 "
                + "WHEN 'ネットワーク・システム' THEN 3 WHEN '法務・コンプライアンス' THEN 4 ELSE 5 END, importance DESC, id";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                Term term = new Term(results.getInt("id"), results.getString("word"),
                        results.getString("meaning"), results.getString("exam_point"),
                        results.getString("category"), results.getInt("importance"));
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
            json.append("\",\"category\":\"").append(escapeJson(term.getCategory()));
            json.append("\",\"importance\":").append(term.getImportance());
            json.append(",\"memorized\":").append(term.isMemorized()).append("}");
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

    static String categoryOptions(String selected) {
        String[] categories = {"情報セキュリティ", "セキュリティ管理", "ネットワーク・システム", "法務・コンプライアンス"};
        StringBuilder options = new StringBuilder();
        for (String category : categories) {
            options.append("<option value='").append(category).append("'");
            if (category.equals(selected)) options.append(" selected");
            options.append(">").append(category).append("</option>");
        }
        return options.toString();
    }

    static String termToHtml(Term term) {
        String html = "<li class='term-card'><div class='term-meta'><span class='category'>" + escapeHtml(term.getCategory()) + "</span>";
        html += "<span class='importance'>重要度 " + "★".repeat(term.getImportance()) + "</span></div>";
        html += "<div class='term-title'><strong>" + escapeHtml(term.getWord()) + "とは？</strong>";
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
                + "select{width:100%;padding:12px 14px;border:1px solid #cbd6e5;border-radius:10px;background:#fff;font:inherit}"
                + "input:focus{border-color:#3978d4;box-shadow:0 0 0 3px rgba(57,120,212,.14)}"
                + "button{grid-column:1/-1;padding:13px;border:0;border-radius:10px;background:#2367c7;color:#fff;font:inherit;font-weight:700;cursor:pointer}"
                + "button:hover{background:#1957ad}"
                + ".section-title{margin:0 0 14px;font-size:22px;color:#173b72}"
                + ".term-list{display:grid;gap:16px;margin:0 0 34px;padding:0;list-style:none}"
                + ".term-card{padding:20px;background:#fff;border:1px solid #dfe7f2;border-radius:16px;box-shadow:0 5px 18px rgba(38,73,120,.06)}"
                + ".term-meta{display:flex;justify-content:space-between;gap:10px;margin-bottom:8px;font-size:12px;font-weight:700}.category{color:#315f9d}.importance{color:#d27a00}"
                + ".category-section{margin:0 0 36px}.category-title{padding-bottom:8px;border-bottom:2px solid #d9e5f5;color:#173b72}"
                + ".term-title{display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:18px}"
                + ".badge{flex:none;padding:3px 9px;border-radius:999px;background:#def6e8;color:#187443;font-size:12px;font-weight:700}"
                + ".badge.learning{background:#e6effd;color:#245da9}"
                + ".answer{margin:14px 0}.answer summary{cursor:pointer;color:#2367c7;font-weight:700}"
                + ".answer-body{margin-top:12px;padding:14px 16px;background:#f6f9fd;border-left:4px solid #79a7e7;border-radius:8px}"
                + ".answer-body p{margin:0;white-space:pre-line}.answer-body h3{margin:14px 0 4px;font-size:14px;color:#52647e}"
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
        insertInitialTerms();
        classifyExistingTerms();
        insertAdditionalTerms();

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String word = formValue(body, "word");
                String meaning = formValue(body, "meaning");
                String examPoint = formValue(body, "examPoint");
                String category = formValue(body, "category");
                int importance = Integer.parseInt(formValue(body, "importance"));
                if (!word.isEmpty() && !meaning.isEmpty() && !examPoint.isEmpty() && !category.isEmpty()) {
                    insertTerm(word, meaning, examPoint, category, importance);
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
                String category = formValue(body, "category");
                int importance = Integer.parseInt(formValue(body, "importance"));
                if (!word.isEmpty() && !meaning.isEmpty() && !examPoint.isEmpty() && !category.isEmpty()) {
                    updateTerm(id, word, meaning, examPoint, category, importance);
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
                html += "<div class='field'><label>カテゴリ</label><select name='category'>" + categoryOptions(term.getCategory()) + "</select></div>";
                html += "<div class='field'><label>重要度</label><select name='importance'>";
                for (int level = 3; level >= 1; level--) {
                    html += "<option value='" + level + "'" + (term.getImportance() == level ? " selected" : "") + ">" + "★".repeat(level) + "</option>";
                }
                html += "</select></div>";
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
                html += "<div class='field'><label>カテゴリ</label><select name='category'>" + categoryOptions("情報セキュリティ") + "</select></div>";
                html += "<div class='field'><label>重要度</label><select name='importance'><option value='3'>★★★</option><option value='2' selected>★★</option><option value='1'>★</option></select></div>";
                html += "<button>用語を追加する</button></form></section>";

                List<Term> terms = selectTerms();
                html += "<section><h2 class='section-title'>復習する用語</h2>";
                String[] categories = {"情報セキュリティ", "セキュリティ管理", "ネットワーク・システム", "法務・コンプライアンス"};
                for (String category : categories) {
                    boolean hasTerms = false;
                    for (Term term : terms) {
                        if (!term.isMemorized() && category.equals(term.getCategory())) hasTerms = true;
                    }
                    if (!hasTerms) continue;
                    html += "<div class='category-section'><h3 class='category-title'>" + category + "</h3><ul class='term-list'>";
                    for (Term term : terms) {
                        if (!term.isMemorized() && category.equals(term.getCategory())) html += termToHtml(term);
                    }
                    html += "</ul></div>";
                }
                html += "</section>";

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
