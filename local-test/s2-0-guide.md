# S2-0 詳しい手順書（Windows）

キットの README の全手順を、つまずきやすい所まで細かく書いたものです。上から順に進めてください。

---

## ステップ 0:JDK 25 を入れる

Paper 26.3 は Java 25 が必要です。

1. https://adoptium.net/temurin/releases/?version=25 を開く
2.「Windows」「x64」の `.msi` をダウンロードして実行（選択肢は既定のまま進めて OK）
3. コマンドプロンプトを開いて（Windows キー →「cmd」と入力 → Enter）、次を打つ

```
java -version
```

`openjdk version "25...` のように **25** と出れば OK。
もし 25 以外が出る場合は、以降のコマンドの `java` の部分を、JDK 25 のフルパスに置き換えてください（例: `"C:\Program Files\Eclipse Adoptium\jdk-25.0.x.x-hotspot\bin\java.exe"`、バージョン部分は実際のフォルダ名）。

## ステップ 1:フォルダとファイルを準備する

1. エクスプローラーで `C:\` の直下に `mc-test` フォルダを作る
2. ダウンロードした2つを `mc-test` に入れる
   - `paper-26.3-259.jar`（Paper）
   - Geyser の jar → 名前を **`Geyser-Spigot.jar`** に変える
3. テストキット（s2-0-test-kit.zip）を解凍する（右クリック → すべて展開）
4. `mc-test` の中に `plugins` フォルダを作り、次を入れる
   - `Geyser-Spigot.jar`
   - キットの `floodgate-spigot.jar`
5. `plugins` の中に `Geyser-Spigot` フォルダを作り、その中に `extensions` フォルダを作って、キットの `ViveBridgeExtension.jar` を入れる

最終形:

```
C:\mc-test\
  paper-26.3-259.jar
  plugins\
    Geyser-Spigot.jar
    floodgate-spigot.jar
    Geyser-Spigot\
      extensions\
        ViveBridgeExtension.jar
```

## ステップ 2:初回起動と EULA

1. `mc-test` フォルダを開いた状態で、エクスプローラーのアドレスバーに `cmd` と打って Enter（そのフォルダでコマンドプロンプトが開きます）
2. 次を打って起動

```
java -Xmx2G -jar paper-26.3-259.jar nogui
```

3.「You need to agree to the EULA」のような文が出て止まります。`mc-test` にできた `eula.txt` をメモ帳で開き、`eula=false` を `eula=true` に書き換えて保存
4. もう一度同じコマンドで起動。ワールド生成が進み、**`Done (...)! For help, type "help"`** と出たら起動完了
5. コマンドプロンプト側に `stop` と打って Enter、サーバーを止める

※ Windows ファイアウォールの確認ダイアログが出たら「アクセスを許可する」を選んでください（出ない場合もあります）。

## ステップ 3:Geyser の設定を変える

1. `plugins\Geyser-Spigot\config.yml` をメモ帳で開く
2. Ctrl+F で `auth-type` を検索する
3. `auth-type: online` となっている行を **`auth-type: floodgate`** に書き換えて保存

## ステップ 4:本番起動と拡張ログの確認

1. ステップ2と同じコマンドで起動する

```
java -Xmx2G -jar paper-26.3-259.jar nogui
```

2. 流れるログの中に、次の3行があることを確認する（コンソールで見ても、`logs\latest.log` をメモ帳で開いて Ctrl+F で `ViveBridge` と検索しても OK）

```
[ViveBridge-S2-0] extension initialized
[ViveBridge-S2-0] registered custom entity definition vivecraftbedrockbridge:vr_avatar
[ViveBridge-S2-0] registered 29 float properties
```

3. Geyser の起動完了の文（`Done` の前後）でエラーが止まっていないことも確認する

## ステップ 5:PC のローカル IP を調べる

1. コマンドプロンプトで次を打つ

```
ipconfig
```

2.「IPv4 アドレス」に書かれた `192.168.` で始まる数字（例: `192.168.1.5`）をメモする。これがスマホから接続するときのアドレスです

## ステップ 6:参加する

**Java 版（いつものアカウント）**

1. マルチプレイ → サーバーを追加する
2. サーバーアドレスに `localhost` と入れて参加

**統合版（スマホ）**

1. スマホと PC が **同じ Wi-Fi** につながっていることを確認
2. マイクラ統合版 → 遊ぶ → サーバー → サーバーを追加する
3. アドレスにステップ5の IP、ポートに `19132` を入れて参加
4. Floodgate 経由なので Java アカウントの入力は不要です。プレイヤー名の頭に `.` が付いた状態で入ります

## ステップ 7:対照実験（通常の防具立て）

1. Java 版のゲーム内で、統合版から見える場所に立って次を打つ（チャット欄に `/` から入力）

```
/summon minecraft:armor_stand ~ ~ ~
```

※ OP 権限が無くて打てない場合は、サーバーのコンソール側で同じ文（先頭の `/` なし）を打ってください。その場合、防具立てはワールドのスポーン地点付近に出ます。

2. サーバーのコンソール（または `logs\latest.log`）で `ViveBridge` を検索し、次の形の行を探す

```
[ViveBridge-S2-0] spawn uuid=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx entityId=... type=... match=false
```

3. **`match=false` であること**を確認し、**uuid の部分をコピーしてメモ**する（次のステップで使います）

## ステップ 8:UUID を登録して差し替え確認

1. サーバーは止めないでください
2. `plugins\Geyser-Spigot\extensions\` の中を見ます。初回起動で拡張用のデータフォルダ（`VivecraftBedrockBridge` という名前）ができているはずなので、その中にキットの `mannequins.txt` を入れます
   - フォルダが見当たらない場合は、`extensions` の中に `VivecraftBedrockBridge` フォルダを自分で作って入れてください
3. `mannequins.txt` をメモ帳で開き、ステップ7でメモした uuid を **1行だけ** 書いて保存（前後に空白や余分な行を入れない）
4. スマホの統合版を一度サーバーから切断し、再参加する
5. ログで `ViveBridge` を検索し、次の2行が出れば成功です

```
[ViveBridge-S2-0] spawn uuid=（同じ uuid） ... match=true
[ViveBridge-S2-0] definition swapped for uuid=（同じ uuid）
```

※ 統合版で見た目が変わらなくて正常です。このテストはログで判定します。

## ステップ 9:`/geyser reload` の確認

1. スマホをつないだまま、**サーバーのコンソール**に次を打つ（ゲーム内チャットではありません）

```
geyser reload
```

2. 次の3点をメモする（スクショでも OK）
   - スマホは切断されたか、そのまま遊べたか
   - コンソールにエラーが出たか（出たらその文面）

## ステップ 10:再起動チェック

1. コンソールに `stop` と打って止める
2. ステップ4と同じコマンドで再び起動する
3. ステップ4の登録ログ3行が再び出ることを確認
4. スマホで再参加し、ステップ8と同じ `match=true` の行が再び出ることを確認

## ステップ 11:ログを送る

`mc-test\logs\latest.log` を、おるたなっちとのチャットにそのまま添付して送ってください。
途中のステップでエラーになって進めなくなっても、無理に直さず、そのまま `latest.log` を送ってくれれば大丈夫です。

## チェックリスト

- [ ] 拡張の登録ログ3行が出た
- [ ] Geyser がエラーなく起動完了した
- [ ] 通常の防具立てで `match=false`（対照実験）
- [ ] mannequins.txt 登録後、同じ防具立てで `match=true` ＋ `definition swapped`
- [ ] `/geyser reload` の挙動を記録した
- [ ] 再起動後も登録ログと match が再現した

## つまずきやすい所

| 症状 | 対処 |
|---|---|
| `java -version` が 25 にならない | ステップ0のフルパス指定で起動する |
| スマホでサーバーが見つからない | 同じ Wi-Fi か確認。ファイアウォールの許可ダイアログで「許可」を選んだか確認 |
| 防具立てを出してもログ行が出ない | 統合版（スマホ）が接続した状態で、スマホの近くに防具立てが出ているか確認（スポーンは統合版プレイヤーが受け取るときに発生します） |
| `mannequins.txt` の置き場所が分からない | ステップ8-2 の通り。extensions 内に `VivecraftBedrockBridge` フォルダを作って入れて OK |
| Geyser がエラーで止まる | 先に進めず、`logs\latest.log` を送ってください |
