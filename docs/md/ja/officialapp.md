<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@40,400,0,0&icon_names=vpn_key" />

## 公式アプリとの連動
kcanotifyを艦これAndroid版と使用するための方法です。

##### 設定方法
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/officialapp_integration.png" width="720"/>

- 設定→Kcanotify設定で次のように設定します。
  - 実行するアプリケーション：<span class="text-danger">艦これAndroid版 (公式)</span>
  - この設定は、サービスがオフになっている状態でのみ変更できます。
- Sniffer(ACTIVE)設定で<span class="text-danger">セキュア通信を読み込む</span>を有効にします。
  - <b>Mitm設定アシスタント</b>で必要な要素を最初にインストールする必要があります。(以下を参照)
  - 必要な要素がインストールされていない場合、Snifferの起動時にアシスタント画面が表示されます。
- 艦これAndroid版では別途設定は不要です。

##### プレイ方法
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/active_sniffer_buttons.png" width="480"/>

- メイン上部の2つのボタン（Sniffer/Service）をOnにした後、下部のGAME STARTボタンを押してゲームを実行します。
  - 正常に動作している状態では、ステータスバーに鍵アイコン(<span class="material-symbols-outlined inline-icon">vpn_key</span>)が表示されます。
  - ゲーム画面が検出されると、画面上に妖精ボタンが表示されます。
- 初めて実行する場合は、次の設定が表示されることがあります。（下のスクリーンショットを参照）
  - VpnServiceに関連する内容を確認した後、接続要求を確認する
  - kcanotify mitm addonのバッテリー最適化を解除
<img src="https://kcanotify-docs.s3.ap-northeast-1.amazonaws.com/ja/sniffer_init.png" width="800"/>

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