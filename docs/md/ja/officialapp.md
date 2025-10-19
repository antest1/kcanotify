<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@40,400,0,0&icon_names=vpn_key" />

## 公式アプリとの連動
kcanotifyを艦これAndroid版と使用するための方法です。

##### 設定方法
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/officialapp_integration.png" width="640"/>

設定 → Kcanotify設定 を開きます。  
  - 実行アプリ：**艦これAndroid版（公式）**  
  - ⚠️ サービスがオンのときは変更できません。止めてから設定してください。  

**ゲーム通信を読み取るには、公式アプリにパッチが必要です。**  
  - **「艦これAndroid版のパッチ方法」** を参照してください。  

Sniffer(ACTIVE)設定で **セキュア通信を読み込む** をオンにします。  
  - **Mitm設定アシスタント** で必要な要素を事前にインストールしてください。  
  - 未インストールの場合、Sniffer起動時にアシスタント画面が自動で表示されます。

---

##### プレイ方法
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/active_sniffer_buttons.png" width="480"/>

メイン上部の Sniffer / Service をオンにして **GAME START** を押します。  
  - 正常時はステータスバーに鍵アイコン（<span class="material-symbols-outlined inline-icon">vpn_key</span>）が表示されます。  
  - ゲーム検出で画面に妖精ボタンが表示されます。  
初回は VpnService の接続許可と kcanotify mitm addon のバッテリー最適化解除を求められる場合があります。  
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/sniffer_init.png" width="640"/>

---

##### 「艦これAndroid版」アプリのパッチ方法

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/common/revanced_icon.png" width="96"/>

このガイドでは、**ReVanced Manager**を使って  
元のAPKファイルにパッチを適用する手順を説明します。  
ReVanced Managerは公式サイトからダウンロードできます。

🔗 [公式サイトはこちら](https://revanced.app/)

###### インストールと設定

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/revanced_settings.png" width="360"/>

ReVanced Managerをインストールして起動。  
設定で <span class="text-danger">「パッチ選択の変更を許可」</span> と  
<span class="text-danger">「共通パッチを表示」</span> をオンにします。

###### 対象アプリとパッチの選択

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/revanced_patcher.png" width="640"/>

画面下の **パッチャー** を開きます。  

「アプリ」欄で一覧から 艦これ を選択。  
🔍 「艦これ」または「kancolle」で検索もできます。  

「パッチ」欄では、<span class="text-danger">「Override certificate pinning」</span> を選び、  
内容を確認して「確認」をタップします。 

###### パッチ実行とインストール

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/revanced_progress.png" width="640"/>

内容を確認して **Patchを実行**。  
完了後、アプリをインストールします。  

💡 既存の艦これは一度削除して再インストール。  
📢 DMMストアで更新がある場合は、  
旧パッチ版を削除して再度パッチを適用してください。

---

##### Mitm設定アシスタント
###### kcanotify mitm addonのインストール
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/common/mitm_icon.png" width="96"/>

- Kcanotifyから渡された艦これ関連の暗号化された通信データを中間から復号化する役割を果たします。
  - このアプリケーションのソースコードは[Github Repository](https://github.com/antest1/kcanotify-mitm)に公開されています。
- 下部の<span class="text-danger">インストール</span>ボタンをクリックすると、APKインストールファイルをダウンロードできます。(上記のアイコンを確認)
- 正常に連動した場合、Kcanotifyに戻ったときに以下の例のように画面に✔️アイコンが表示されます。
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/mitm_addon_install.png" width="640"/>

###### CA証明書のインストール
- 暗号化された通信データを復号化するには、CA証明書をインストールしてください。
  - Androidのセキュリティポリシーのため、設定から手動でインストールする必要があります。
  - Kcanotifyは、艦これゲームプレイに関するデータ以外の個人情報にアクセスしたり、不正に利用したりしません。

<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/certificate_install.png" width="640"/>

- 下部の<span class="text-danger">エクスポート</span>ボタンをクリックして、<code>Kcanotify_CA.crt</code>ファイルを保存します。
- 端末の設定で<span class="text-danger">「証明書」</span>を検索して出てくる項目のうち<span class="text-danger">「CA証明書」</span>を選択した後、証明書をインストールします。
  - ※ 例: <span class="text-primary">セキュリティとプライバシー → その他のセキュリティとプライバシー → 暗号化と認証情報 → 証明書のインストール</span>
  - 証明書を削除したい場合は、[証信頼できる認証情報]設定で[ユーザー]タブをクリックしてから、関連証明書を選択して削除できます。
- 正常に設定されている場合、以下の例のように画面に✔️アイコンが表示されます。
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/certificate_ok.png" width="320"/>