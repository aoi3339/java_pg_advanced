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
    private final int learningOrder;
    private final String relatedTerms;
    private boolean memorized;

    Term(int id, String word, String meaning, String examPoint, String category, int importance,
         int learningOrder, String relatedTerms) {
        this.id = id;
        this.word = word;
        this.meaning = meaning;
        this.examPoint = examPoint;
        this.category = category;
        this.importance = importance;
        this.learningOrder = learningOrder;
        this.relatedTerms = relatedTerms;
        this.memorized = false;
    }

    int getId() { return id; }
    String getWord() { return word; }
    String getMeaning() { return meaning; }
    String getExamPoint() { return examPoint; }
    String getCategory() { return category; }
    int getImportance() { return importance; }
    int getLearningOrder() { return learningOrder; }
    String getRelatedTerms() { return relatedTerms; }
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
                + "learning_order INTEGER NOT NULL DEFAULT 999, "
                + "related_terms TEXT NOT NULL DEFAULT '', "
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
            if (!columnExists(connection, "learning_order")) {
                statement.executeUpdate("ALTER TABLE terms ADD COLUMN learning_order INTEGER NOT NULL DEFAULT 999");
            }
            if (!columnExists(connection, "related_terms")) {
                statement.executeUpdate("ALTER TABLE terms ADD COLUMN related_terms TEXT NOT NULL DEFAULT ''");
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

    static void insertTerm(String word, String meaning, String examPoint, String category, int importance,
                           int learningOrder, String relatedTerms) throws IOException {
        String sql = "INSERT INTO terms (word, meaning, exam_point, category, importance, learning_order, related_terms, memorized) VALUES (?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, word);
            statement.setString(2, meaning);
            statement.setString(3, examPoint);
            statement.setString(4, category);
            statement.setInt(5, importance);
            statement.setInt(6, learningOrder);
            statement.setString(7, relatedTerms);
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

    static void insertStudyTerms() throws IOException {
        String[][] terms = {
                {"情報資産", "組織にとって価値があり、保護すべき情報や情報システム、設備、人材などの総称。", "情報資産を洗い出し、管理責任者、重要度、取扱方法を明確にすることがリスク管理の出発点。", "セキュリティ管理", "3"},
                {"脅威", "情報資産に損害を与える可能性がある原因。攻撃者、マルウェア、災害、誤操作などが該当する。", "脅威が脆弱性を利用するとインシデントにつながる。脅威は原因、脆弱性は弱点。", "情報セキュリティ", "3"},
                {"SIEM", "複数の機器やシステムのログを収集・一元管理し、相関分析によって攻撃の兆候や異常を検知する仕組み。", "単独のログでは見つけにくい事象を横断的に分析する。SOCでの監視やインシデント検知に活用される。", "情報セキュリティ", "3"},
                {"ゼロトラスト", "社内外を問わず全てのアクセスを無条件には信用せず、利用者や端末を継続的に確認する考え方。", "内部ネットワークも安全とは限らないことを前提とし、最小権限や多要素認証などを組み合わせる。", "情報セキュリティ", "3"},
                {"CVSS", "脆弱性の深刻度を共通の尺度で評価する仕組み。基本評価、現状評価、環境評価の基準がある。", "現状評価は攻撃コードや対策状況によって時間とともに変化し、環境評価は利用組織の環境によって異なる。", "セキュリティ管理", "3"},
                {"デジタルフォレンジックス", "インシデント発生時に、法的証拠となり得るデータを適切に収集・保全し、調査・分析する活動。", "証拠の完全性を保ち、誰がいつ扱ったかという記録を残す。原因究明だけでなく証拠保全が重要。", "セキュリティ管理", "3"},
                {"IDS・IPS", "IDSは不正な通信を検知して通知し、IPSは検知した通信を自動的に遮断する仕組み。", "IDSは検知、IPSは検知と防御。ファイアウォールの許可通信に含まれる攻撃も検査できる。", "情報セキュリティ", "3"},
                {"UTM", "ファイアウォール、IDS・IPS、アンチウイルスなど複数のセキュリティ機能を一台に統合した機器。", "運用を一元化しやすい一方、単一障害点や性能不足にならないよう設計する。", "情報セキュリティ", "2"},
                {"サイバーキルチェーン", "攻撃者の行動を偵察、武器化、配送、攻撃、侵入、遠隔操作、目的実行などの段階に分けて捉える考え方。", "各段階で攻撃を検知・阻止し、最終目的の達成を防ぐために利用する。", "情報セキュリティ", "3"},
                {"リスクベース認証", "アクセス元、端末、時間帯、行動などからリスクを判定し、必要に応じて追加認証を要求する方式。", "通常と異なるアクセスだけ認証を強化する。多要素認証は異なる認証要素を組み合わせる方式。", "情報セキュリティ", "3"},
                {"標的型攻撃", "特定の組織や個人を狙い、業務に関係する内容を装ったメールなどで侵入を試みる攻撃。", "不審な添付ファイルやURLを開かない教育に加え、侵入を前提とした多層防御と監視が必要。", "情報セキュリティ", "3"},
                {"ソーシャルエンジニアリング", "人の心理や行動上の隙を利用して、秘密情報を聞き出したり不正行為へ誘導したりする手法。", "なりすまし、のぞき見、ゴミ箱あさりなどが該当する。技術対策だけでなく教育と手順が重要。", "情報セキュリティ", "3"},
                {"内部不正・不正のトライアングル", "内部者による不正は、動機・プレッシャー、機会、正当化の三要素がそろうと発生しやすいという考え方。", "権限管理や職務分離で機会を減らし、相談体制や教育なども組み合わせて防止する。", "セキュリティ管理", "3"},
                {"情報の分類・ラベル付け", "情報を重要度や機密性に応じて分類し、取扱区分をラベルなどで明示する管理策。", "社外秘などを明示して取扱ルールを周知し、知らなかったという状態を防ぐ。", "セキュリティ管理", "3"},
                {"委託先管理", "業務や情報処理を外部へ委託する際に、選定、契約、監督、終了までセキュリティを管理する活動。", "委託しても管理責任がなくなるわけではない。契約に対策、事故報告、再委託などを定める。", "セキュリティ管理", "3"},
                {"情報セキュリティ監査", "情報セキュリティ対策や管理の状況を、独立した立場から証拠に基づいて評価する活動。", "監査人の独立性と客観性が重要。発見事項を報告し、改善につなげる。", "セキュリティ管理", "2"},
                {"ISO/IEC 27001・JIS Q 27001", "ISMSを構築・運用する組織に対する要求事項を定めた国際規格と、その日本産業規格。", "リスクに基づいて管理策を選び、継続的に改善する。ISO/IEC 27002は管理策の実践指針。", "セキュリティ管理", "3"},
                {"アンチパスバック", "入室記録のないカードでの退室や、退室記録のないカードでの再入室を許可しない入退室管理方式。", "カードの貸し借りや共連れへの対策として用いる。入室と退室の対応関係を管理する。", "情報セキュリティ", "2"},
                {"匿名加工情報・仮名加工情報", "匿名加工情報は特定の個人を識別できず復元できないよう加工した情報。仮名加工情報は他の情報と照合しなければ個人を識別できないよう加工した情報。", "匿名加工情報と仮名加工情報では利用や第三者提供に関する規律が異なる。加工方法と目的を区別する。", "法務・コンプライアンス", "3"},
                {"不正アクセス禁止法", "他人のID・パスワードを不正利用する行為や、アクセス制御を回避して侵入する行為などを禁止する法律。", "不正アクセス行為だけでなく、他人の認証情報を不正に取得・提供する行為なども規制対象。", "法務・コンプライアンス", "3"}
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
                for (int i = 0; i < 4; i++) insert.setString(i + 1, term[i]);
                insert.setInt(5, Integer.parseInt(term[4]));
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    static void applyLearningData() throws IOException {
        String[][] learningData = {
                {"情報資産", "1", "機密性・完全性・可用性（CIA）, 情報の分類・ラベル付け"},
                {"機密性・完全性・可用性（CIA）", "2", "情報資産, 脅威, 脆弱性"}, {"脅威", "3", "脆弱性, リスクアセスメント"},
                {"脆弱性", "4", "脅威, CVSS, ゼロデイ攻撃"}, {"リスクアセスメント", "5", "脅威, 脆弱性, リスク対応"},
                {"リスク対応", "6", "リスクアセスメント, ISMS（情報セキュリティマネジメントシステム）"},
                {"情報セキュリティポリシー", "7", "ISMS（情報セキュリティマネジメントシステム）, 情報の分類・ラベル付け"},
                {"ISMS（情報セキュリティマネジメントシステム）", "8", "情報セキュリティポリシー, ISO/IEC 27001・JIS Q 27001"},
                {"ISO/IEC 27001・JIS Q 27001", "9", "ISMS（情報セキュリティマネジメントシステム）, 情報セキュリティ監査"},
                {"情報の分類・ラベル付け", "10", "情報資産, アクセス制御"}, {"内部不正・不正のトライアングル", "11", "アクセス制御, 情報セキュリティ監査"},
                {"委託先管理", "12", "リスクアセスメント, 情報セキュリティ監査"}, {"情報セキュリティ監査", "13", "ISMS（情報セキュリティマネジメントシステム）, 委託先管理"},
                {"アクセス制御", "14", "多要素認証（MFA）, ゼロトラスト"}, {"多要素認証（MFA）", "15", "アクセス制御, リスクベース認証"},
                {"リスクベース認証", "16", "多要素認証（MFA）, ゼロトラスト"}, {"ゼロトラスト", "17", "アクセス制御, 多要素認証（MFA）"},
                {"アンチパスバック", "18", "アクセス制御, 多要素認証（MFA）"}, {"ハッシュ関数", "19", "デジタル署名, 暗号化"},
                {"暗号化", "20", "ハッシュ関数, デジタル署名, TLS"}, {"デジタル署名", "21", "ハッシュ関数, デジタル証明書（公開鍵証明書）"},
                {"デジタル証明書（公開鍵証明書）", "22", "デジタル署名, TLS"}, {"TLS", "23", "暗号化, デジタル証明書（公開鍵証明書）"},
                {"DNS", "24", "TLS, ファイアウォール"}, {"ファイアウォール", "25", "DMZ, WAF（Web Application Firewall）, IDS・IPS"},
                {"DMZ", "26", "ファイアウォール, VPN"}, {"VPN", "27", "暗号化, TLS"}, {"WAF（Web Application Firewall）", "28", "SQLインジェクション, クロスサイトスクリプティング（XSS）"},
                {"IDS・IPS", "29", "ファイアウォール, SIEM"}, {"UTM", "30", "ファイアウォール, IDS・IPS"}, {"SIEM", "31", "IDS・IPS, CSIRT（シーサート）, インシデント対応"},
                {"マルウェア", "32", "ランサムウェア, ゼロデイ攻撃"}, {"フィッシング", "33", "ソーシャルエンジニアリング, 標的型攻撃"},
                {"ソーシャルエンジニアリング", "34", "フィッシング, 内部不正・不正のトライアングル"}, {"標的型攻撃", "35", "フィッシング, サイバーキルチェーン"},
                {"クロスサイトスクリプティング（XSS）", "36", "WAF（Web Application Firewall）, SQLインジェクション"}, {"SQLインジェクション", "37", "WAF（Web Application Firewall）, クロスサイトスクリプティング（XSS）"},
                {"ランサムウェア", "38", "マルウェア, バックアップ"}, {"ゼロデイ攻撃", "39", "脆弱性, CVSS"}, {"CVSS", "40", "脆弱性, ゼロデイ攻撃"},
                {"サイバーキルチェーン", "41", "標的型攻撃, SIEM"}, {"インシデント対応", "42", "CSIRT（シーサート）, デジタルフォレンジックス"},
                {"CSIRT（シーサート）", "43", "インシデント対応, SIEM"}, {"デジタルフォレンジックス", "44", "インシデント対応, CSIRT（シーサート）"},
                {"バックアップ", "45", "ランサムウェア, RTO・RPO"}, {"RTO・RPO", "46", "バックアップ, BCP（事業継続計画）"},
                {"BCP（事業継続計画）", "47", "RTO・RPO, バックアップ"}, {"個人情報保護法", "48", "匿名加工情報・仮名加工情報"},
                {"匿名加工情報・仮名加工情報", "49", "個人情報保護法"}, {"不正アクセス禁止法", "50", "アクセス制御, SQLインジェクション"}
        };
        String sql = "UPDATE terms SET learning_order = ?, related_terms = ? WHERE word = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String[] item : learningData) {
                statement.setInt(1, Integer.parseInt(item[1]));
                statement.setString(2, item[2]);
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

    static void updateTerm(int id, String word, String meaning, String examPoint, String category, int importance,
                           int learningOrder, String relatedTerms) throws IOException {
        String sql = "UPDATE terms SET word = ?, meaning = ?, exam_point = ?, category = ?, importance = ?, learning_order = ?, related_terms = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, word);
            statement.setString(2, meaning);
            statement.setString(3, examPoint);
            statement.setString(4, category);
            statement.setInt(5, importance);
            statement.setInt(6, learningOrder);
            statement.setString(7, relatedTerms);
            statement.setInt(8, id);
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
        String sql = "SELECT id, word, meaning, exam_point, category, importance, learning_order, related_terms, memorized "
                + "FROM terms ORDER BY learning_order, id";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                Term term = new Term(results.getInt("id"), results.getString("word"),
                        results.getString("meaning"), results.getString("exam_point"),
                        results.getString("category"), results.getInt("importance"),
                        results.getInt("learning_order"), results.getString("related_terms"));
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
            json.append(",\"learningOrder\":").append(term.getLearningOrder());
            json.append(",\"relatedTerms\":\"").append(escapeJson(term.getRelatedTerms()));
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

    static Term findTermByWord(String word, List<Term> terms) {
        for (Term term : terms) {
            if (term.getWord().equals(word)) return term;
        }
        return null;
    }

    static String relatedTermsToHtml(Term term, List<Term> terms) {
        StringBuilder html = new StringBuilder();
        for (String word : term.getRelatedTerms().split(",")) {
            String trimmedWord = word.trim();
            if (trimmedWord.isEmpty()) continue;
            if (html.length() > 0) html.append("、");
            Term related = findTermByWord(trimmedWord, terms);
            if (related == null) {
                html.append(escapeHtml(trimmedWord));
            } else {
                html.append("<a href='/term?id=").append(related.getId()).append("'>")
                        .append(escapeHtml(trimmedWord)).append("</a>");
            }
        }
        return html.toString();
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

    static String termToHtml(Term term, List<Term> terms) {
        String html = "<li class='term-card'><div class='term-meta'><span class='study-number'>学習 " + term.getLearningOrder() + "</span>";
        html += "<span class='category'>" + escapeHtml(term.getCategory()) + "</span>";
        html += "<span class='importance'>重要度 " + "★".repeat(term.getImportance()) + "</span></div>";
        html += "<div class='term-title'><strong>" + escapeHtml(term.getWord()) + "とは？</strong>";
        html += term.isMemorized() ? "<span class='badge'>覚えた</span>" : "<span class='badge learning'>学習中</span>";
        html += "</div><details class='answer'><summary>答えを見る</summary><div class='answer-body'><p>";
        html += escapeHtml(term.getMeaning());
        html += "</p><h3>試験のポイント</h3><p>";
        html += escapeHtml(term.getExamPoint());
        html += "</p>";
        if (!term.getRelatedTerms().isEmpty()) {
            html += "<h3>関連用語</h3><p class='related'>" + relatedTermsToHtml(term, terms) + "</p>";
        }
        html += "</div></details><div class='actions'>";
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
                + ".term-meta{display:flex;align-items:center;gap:10px;margin-bottom:8px;font-size:12px;font-weight:700}.study-number{padding:3px 8px;border-radius:999px;background:#173b72;color:#fff}.category{color:#315f9d}.importance{margin-left:auto;color:#d27a00}"
                + ".category-section{margin:0 0 36px}.category-title{padding-bottom:8px;border-bottom:2px solid #d9e5f5;color:#173b72}"
                + ".term-title{display:flex;align-items:center;justify-content:space-between;gap:12px;font-size:18px}"
                + ".badge{flex:none;padding:3px 9px;border-radius:999px;background:#def6e8;color:#187443;font-size:12px;font-weight:700}"
                + ".badge.learning{background:#e6effd;color:#245da9}"
                + ".answer{margin:14px 0}.answer summary{cursor:pointer;color:#2367c7;font-weight:700}"
                + ".answer-body{margin-top:12px;padding:14px 16px;background:#f6f9fd;border-left:4px solid #79a7e7;border-radius:8px}"
                + ".answer-body p{margin:0;white-space:pre-line}.answer-body h3{margin:14px 0 4px;font-size:14px;color:#52647e}"
                + ".related{color:#315f9d;font-weight:600}.related a{color:#2367c7;text-decoration:underline;text-underline-offset:3px}"
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
        insertStudyTerms();
        applyLearningData();

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
                int learningOrder = Integer.parseInt(formValue(body, "learningOrder"));
                String relatedTerms = formValue(body, "relatedTerms");
                if (!word.isEmpty() && !meaning.isEmpty() && !examPoint.isEmpty() && !category.isEmpty()) {
                    insertTerm(word, meaning, examPoint, category, importance, learningOrder, relatedTerms);
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
                int learningOrder = Integer.parseInt(formValue(body, "learningOrder"));
                String relatedTerms = formValue(body, "relatedTerms");
                if (!word.isEmpty() && !meaning.isEmpty() && !examPoint.isEmpty() && !category.isEmpty()) {
                    updateTerm(id, word, meaning, examPoint, category, importance, learningOrder, relatedTerms);
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

            if (path.equals("/term") && method.equals("GET")) {
                int id = Integer.parseInt(exchange.getRequestURI().getQuery().substring(3));
                List<Term> terms = selectTerms();
                Term term = null;
                for (Term candidate : terms) {
                    if (candidate.getId() == id) term = candidate;
                }
                if (term == null) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                String html = "<!doctype html><html lang='ja'><head><meta charset='UTF-8'>";
                html += "<meta name='viewport' content='width=device-width,initial-scale=1'>";
                html += "<title>" + escapeHtml(term.getWord()) + " - セキュマネ用語帳</title>" + pageStyles();
                html += "</head><body><main class='page'><header class='hero'><h1>関連用語</h1>";
                html += "<p><a class='action edit' href='/'>← おすすめ学習順へ戻る</a></p></header>";
                html += "<ul class='term-list'>" + termToHtml(term, terms) + "</ul></main></body></html>";
                byte[] response = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.getResponseBody().close();
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
                html += "<div class='field'><label>学習順</label><input type='number' min='1' name='learningOrder' value='" + term.getLearningOrder() + "' required></div>";
                html += "<div class='field wide'><label>関連用語</label><input name='relatedTerms' value='" + escapeHtml(term.getRelatedTerms()) + "' placeholder='カンマ区切りで入力'></div>";
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
                html += "<div class='field'><label>学習順</label><input type='number' min='1' name='learningOrder' value='999' required></div>";
                html += "<div class='field wide'><label>関連用語</label><input name='relatedTerms' placeholder='例：暗号化, ハッシュ関数'></div>";
                html += "<button>用語を追加する</button></form></section>";

                List<Term> terms = selectTerms();
                html += "<section><h2 class='section-title'>おすすめ学習順</h2><ul class='term-list'>";
                for (Term term : terms) {
                    if (!term.isMemorized()) html += termToHtml(term, terms);
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
                        html += termToHtml(term, terms);
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
