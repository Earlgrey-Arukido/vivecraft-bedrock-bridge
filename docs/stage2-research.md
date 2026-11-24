# Stage 2/3 技術調査メモ: Geyserカスタムエンティティ + プロパティ同期

調査日: 2026-11-24 / 出典は各項に記載。事実と推測を区別し、未確認事項は明記する。

## 結論（要約）

**本命ルートとして成立する（条件付き）。** 部品は揃った:
- Geyser Custom Entity API（2.11+、Experimental）で、Java側エンティティの Bedrock 表現をカスタム定義に差し替え可能。差し替えは「置き換え」方式で **Java側にキャリアエンティティが必要** → 現行の防具立てマネキンがそのままキャリアになる
- エンティティプロパティの動的更新は可能で、更新は最大で毎tick（20Hz）にバッチされる実績構成がある（目標 10〜20Hz は上限の内側）
- スキンは**動的同期は不可、静的焼き込みは可能**。VRプレイヤーは あるキド 一人なので、現在のスキンPNGをパックに同梱し、Java版スキンの 64×64 UV配置を再現した人型ジオメトリを使えば「本人の見た目」になる
- 肘・膝はカスタムジオメトリ側のボーン構成次第で自由に持てる → Stage 2（見た目）→ Stage 3（関節）の増分開発と整合

## 1. カスタムエンティティ機能

- Custom Entity API は Geyser API 2.11.0 で導入。Geyser拡張（Extension）が「Javaエンティティタイプを置き換えるBedrockカスタムエンティティ定義」を登録できる
  出典: https://docsearch.algolia.com/mcp/docs/website/geysermc.org （Custom Entity API 概要）
- 制約: ①APIは `@ApiStatus.Experimental`（互換性保証なし） ②JSONマッピングでは使えずコード登録が必須 ③ジオメトリ/テクスチャ/アニメーションを定義したBedrockリソースパックが別途必要
- 中核クラスは `CustomEntityDefinition`。スポーン時イベントで寸法等を変更可能（実装PR #6010 由来、https://github.com/GeyserMC/Geyser/pull/6470）
- **登録は Geyser の起動時ウィンドウ内でのみ有効**。新しいエンティティ識別子や新しい/変更されたプロパティ型は再起動するまで有効化されない（実運用拡張 ResourcePackManager の記録: https://wiki.nightbreak.io/ResourcePackManager/geyser-extension/）
- 登録コードは **Geyser拡張として Geyser プロセス内で動く**必要がある（extensions/ フォルダに jar 配置）。実績構成は「同一jarが Paperプラグイン + Geyser拡張の両方を兼ね、プラグインメッセージで連携」する方式（RSPM がこの形）
- リソースパックは Bedrock ログイン時の Geyser パック配布に乗せる。パックと拡張は「別々の半分」で、両方揃わないと表示されない
- 未確認: 登録イベントの正確なクラス名（第三者コードに `GeyserDefineEntitiesEvent` の参照あり。現行公式APIでの正式名は一次資料で未確認）

## 2. エンティティプロパティの動的更新

- Bedrock 仕様: プロパティは `description.properties` で型（bool/enum/float/int）と共に定義。float/int には range が必須で、範囲外はクランプされる。パック側から読むには `client_sync: true` が必要、読み取りは Molang の `q.property('名前')`
  出典: https://wiki.bedrock.dev/entities/entity-properties.html
- 動的更新は可能で実運用例がある（RSPM拡張がライブのプロパティ上書きを本番運用）。更新は「変更のあったエンティティをtickに1回送る」設計（PR #6010 のコミット "Implement dirty entities to send updates once a tick"）
- カスタムプロパティが Java メタデータ/NBT から**自動で**導出される仕組みは確認できなかった（推測: 存在しない）。実績構成はすべて「プラグインが値を計算し、拡張経由で明示的にセット」→ 本プロジェクトも VSEポーズ値 → プラグインが角度計算 → 拡張へ送信、という明示的フローが前提
- 型・個数の定義は起動時登録が前提。必要数の目安: 頭3 + 腕（上腕・前腕×2）最大12 + 体幹1〜3 + 脚（大腿・脛×2）最大12 → **float約25〜30個**。個数上限・帯域の数値制約は公式資料で未確認。IntegratedPack の整数圧縮エンコードのように、必要なら圧縮で個数を減らす設計が安全側

## 3. Java版プレイヤーからの見え方

- Geyser は Bedrock クライアントごとのセッション変換であり、Java クライアントは通らない。カスタムエンティティは Java 側に一切影響せず、**Java プレイヤーにはキャリア（防具立て）がバニラの姿で見える**
- 含意: Phase 3 の出し分け（Java にはマネキンを隠す、Bedrock には本人モデルを隠す）は Stage 2 でもそのまま必要

## 4. スキンテクスチャ

- カスタムエンティティのテクスチャはパック内の固定テクスチャ参照が前提で、実行時に任意プレイヤーのスキンを差し込む公式手段は資料上存在しない
- Geyser の「カスタムスカル」は、事前登録したスキンのジオメトリ+テクスチャを含むパックを起動時に自動生成する前例（対象はスカル専用。全身への流用は未確認）: https://geysermc.org/crp/wiki/geyser/custom-skulls/
- Bedrock ジオメトリは UV を自由に定義できるため、Java版スキンの UV配置を再現した人型ジオメトリ + 本人のスキンPNG で本人再現が可能
- 運用制約: スキンを変えたらパックの作り直し + Bedrock 側の再ダウンロードが必要

## 5. ボトルネック（重大順）

1. **APIの実験性 × 自動更新**: 本サーバーは GeyserUpdater で Geyser/Floodgate が定期自動更新される構成。「Geyserを更新したら拡張が壊れた」は実運用でも起きている。**Stage 2 着手時は自動更新の停止 or バージョン固定を検討すべき（最重要）**
2. **二重開発**: Paperプラグイン + Geyser拡張 + Bedrockパック の3点セットになる。現行の単一プラグイン構成からの工数増は避けられない
3. **起動時登録ウィンドウ**: 定義とプロパティ型は起動時に確定。プロパティ設計（個数・range）は先に固めること
4. **負荷の実測がない**: float約30個 × 10〜20Hz × viewer数 の裏付けなし。Altare無料枠 + ストックホルム遅延では、更新 10Hz + クライアント側補間が現実解になる可能性（推測）

## 6. 代替案の評価

- 案A（本命）: 自前の一体jar（プラグイン+拡張）。自由度最大、バージョン追従コストは自前
- 案B: ResourcePackManager のブリッジに乗る。保守は楽だが、任意角度のプロパティ連続ストリーミングを API から行えるか未確認（採用前に仕様確認必須）
- 案C: 防具立て+IntegratedPack の延長（前腕専用キャリア分離など）。Stage 2/3 の要件を満たさない
- 案D: 偽プレイヤー方式。スキンは付くが関節を任意ポーズで駆動できないため不採用

## 7. 次に実地で確認すべきこと

1. 使用中の Geyser ビルド（2.12.1-b1307）に Custom Entity API のクラス群が含まれているか（拡張の最小スケルトンで起動ログ確認）
2. 登録イベントとプロパティ上書き API の正確なシグネチャ（Javadoc/ソースで確定）
3. プロパティ float×約30個を 10〜20Hz で更新した際の Altare 環境での負荷と滑らかさの実測

## 追記（2026-11-24）: S2-0 第一確認の結果

使用中と同一ビルドの Geyser-Spigot **2.12.1-b1307** の jar を公式ダウンロードから取得し、クラス一覧を確認した。

- `org.geysermc.geyser.api.entity.custom.CustomEntityDefinition` が存在 → **Custom Entity API は使用中ビルドに含まれている**
- 登録イベントは **`org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent`** で確定（第1項の未確認事項を解消）
- プロパティ API も存在: `api.entity.property.GeyserEntityProperty`、`type.GeyserFloatEntityProperty`、`BatchPropertyUpdater`（一括更新用）
- `org.geysermc.geyser.api.GeyserApi` も jar 内にあり、同一 JVM からの API アクセスの前提は満たす（プラグインから直接呼べるかの実証は S2-0 本体で）

## 追記2（2026-11-24）: API シグネチャの確定（javap による一次確認）

同一ビルド jar に対して javap で API の実シグネチャを確認した。

- **登録**: `GeyserDefineEntitiesEvent.register(CustomEntityDefinition)`。定義は `CustomEntityDefinition.of(Identifier | String)` で作る
- **プロパティ登録**: 専用イベント `GeyserDefineEntityPropertiesEvent` があり、`registerFloatProperty(エンティティIdentifier, プロパティIdentifier, min, max, デフォルト値)` で float プロパティを登録できる（int / boolean / enum 用も同形）。設計書の range [-360, 360] はこの min/max にそのまま渡せる
- **個別割り当ての鍵**: `SessionSpawnEntityEvent`（Bedrock セッションごとのスポーンイベント）に **`definition()` の setter** があり、スポーンする個体ごとに定義を差し替えられる。`preSpawnConsumer(Consumer<GeyserEntity>)` でスポーン直前の個体にも触れる → 「タイプ単位でしか置き換えできない」という最悪ケースは回避できる見込みが立った
- **プロパティ更新**: `GeyserEntity.updateProperty(property, value)` と `updatePropertiesBatched(Consumer<BatchPropertyUpdater>)` が個体単位で用意されている（ストリーミング経路の実在を確認）
- 残る未確認: SessionSpawnEntityEvent の時点で「自前のマネキン個体」をどう識別するか（preSpawnConsumer 経由の GeyserEntity のデータ（名前等）で判別できるか）、および Paper プラグイン → 拡張への値の受け渡し方法（プラグインメッセージ or 直接 API）

## 追記3（2026-11-24）: 識別方法の確認

- `GeyserEntityDataTypes` の定数を確認した範囲では **名前（NAME）に相当するデータ型は存在しない**。スポーン時点で名前でマネキンを判別する案は採れない
- 代替の本命: `GeyserEntity.uuid()` / `javaId()` による照合。Paper プラグインは生成したマネキンの UUID を知っているため、それを Geyser 拡張へ共有し、`SessionSpawnEntityEvent` / `preSpawnConsumer` で UUID が一致した個体だけ定義を差し替える
- 順序の注意: マネキン生成直後に UUID を拡張へ渡せば、統合版クライアントへのスポーン送信より前に共有が間に合う想定（実地確認は S2-0）

## 追記4（2026-11-24）: スポーンイベントから取れる情報（識別設計の確定）

javap でイベント階層を確認した。

- `ServerSpawnEntityEvent`（`SessionSpawnEntityEvent` の Java 側サブクラス）は、イベント自体に **`uuid()`、`entityId()`（Java のエンティティID）、`entityType()`** を持つ。preSpawnConsumer を経由しなくても個体識別ができる
- `SessionSpawnEntityEvent` 側は `definition()` の取得・差し替えと `connection()`（対象 Bedrock セッション）
- `preSpawnConsumer` の `GeyserEntity` からは position / geyserId / データ値が取れ、プロパティ更新（updateProperty）もここから繋がる
- Bukkit の Entity や PersistentDataContainer は Geyser 側イベントからは引けない（Geyser 独自表現のため）→ 印付け方式（方法B）は不採用
- **識別設計の確定**: プラグインがスポーン時にマネキン UUID を登録 → 拡張が `ServerSpawnEntityEvent.uuid()` と照合 → 一致したら `definition(カスタム定義)` で差し替え。マネキンUUID ↔ プレイヤーUUID の対応表はプラグイン側の既存マップがそのまま使える

## 追記5（2026-11-24）: S2-0 を ALTERNA の VM で前倒し実施した結果

本番と同一構成（Paper 26.3-259 + 固定した Geyser 2.12.1-b1307）を VM 内に構築し、拡張スケルトン（定義登録・プロパティ29個登録・ServerSpawnEntityEvent の UUID 照合ログ、`local-test/` に保存）を読み込ませた。

- **確認できた**: 拡張 jar は実物 Geyser に認識され「Enabled extension VivecraftBedrockBridge / Loaded 1 extension(s)」まで到達。extension.yml の形式（api: 2.12.0）も受理された
- **VM ではここが限界**: Geyser 本体の初期化が、暗号化セットアップ用の discovery 取得（client.discovery.minecraft-services.net への Java の HTTPS）で失敗し、定義登録イベントの発火前に停止する。切り分けの結論として、これはコードの問題ではなく **VM の出口プロキシと Java の HTTP トンネリングの相性**（python では同 URL が正常に取得できることを確認済み）。ユーザー PC や Altare のような通常回線では発生しない
- **残りの S2-0 確認**（定義登録ハンドラの発火、UUID 照合での差し替え、対照実験、再起動後の再登録）は、ユーザー PC の Paper + Geyser で実施する。手順とチェックリストは stage2-prototype-design.md の S2-0 節のとおり
- 副産物: MC 26.1 以降の Paper は **Java 25 以上**が必要（Temurin 25 を ~/tools/jdk-25 に用意済み）。Paperclip の Mojang jar 取得も Java では失敗するため、python で取得して cache/ に配置する方式で回避した
