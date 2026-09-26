# 重量ごとの販売価格（2026-09-26）

「経営管理 → 売上」の登録・編集画面に追加しました。
- 何kgあたり（例：5）
- その重量の販売価格（円、例：1500）
- 販売数量（セット、例：3）
- 自動計算：合計15kg、売上4,500円

一覧にも「5kgあたり1,500円」「3セット / 15kg」が表示されます。
1セットの販売は数量を1にしてください。重量は小数点以下3桁まで入力できます。
3項目をすべて空欄にすると、従来どおり売上金額だけを入力できます。
既存データに重量・価格を自動で割り当てることはありません。
収支には自動計算した売上金額が反映されます。

## 導入
1. 起動中のコマンドプロンプトで Ctrl+C を押して停止します。
2. 現在のプロジェクトフォルダをコピーしてバックアップします。
3. ZIPを「すべて展開」します。中の「農業管理システム最新版/src」フォルダを、
   C:\Users\yoush\Downloads\farm-calendar-no-login\農業管理システム最新版
   の中にコピーし、同名ファイルを置き換えます。現在のsrcは削除せず上書きしてください。
   application.propertiesを含むsrc内の設定を手元で変更している場合は、以下の5ファイルのみ置き換えてください。
4. 現在のプロジェクトのコマンドプロンプトで実行します。

```bat
set "PATH=%SystemRoot%\System32\WindowsPowerShell\v1.0;%SystemRoot%\System32;%PATH%"
mvnw.cmd clean package
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```

BUILD SUCCESSを確認してから起動コマンドを実行してください。
http://localhost:8083/sales を開きます。必要ならCtrl+F5で再読み込みしてください。
現在のddl-auto=update設定により、起動時に売上テーブルへ3つの任意項目が追加されます。
本体の設定ファイルやDB接続先を変更する必要はありません。

## 変更ファイル（プロジェクトからの相対パス）
- src/main/java/com/example/demo/entity/Sales.java
- src/main/java/com/example/demo/conotroller/SalesController.java
- src/main/resources/templates/sales.html
- src/main/resources/templates/sales_edit.html
- src/main/resources/static/js/sales-pricing.js（新規）

## 確認範囲
- Javaの価格計算ロジックをJPA注釈から切り離した一時ハーネスで実行し、
  5kg/1500円×3、端数重量、従来の金額のみの記録、不完全入力・負数・上限、
  ブラウザ送信金額に依存しない再計算を確認しました。
- JavaScriptの構文、およびDOM模擬テストで計算・編集・空欄・旧形式・上限を確認しました。
- Mavenビルドは依存ファイル配布先の名前解決に失敗し、完了できませんでした。
  Spring全体でのビルド、DBへの保存、ブラウザでの実表示は未確認です。
- 実際のデータベースには接続していません。Renderへの反映も行っていません。
