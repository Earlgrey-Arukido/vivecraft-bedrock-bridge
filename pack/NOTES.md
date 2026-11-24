# リソースパック メモ（S2-1 用）

## 設計（2026-11-24、あるキドの指摘＋しるふぁ/ミーナのレビューで確定）

見た目は「VR プレイヤー本人」のもの。キーは名前ではなく **UUID**（改名で壊れないため）。

- **登録の正は `pack/roster.json`**。パック生成・プラグイン・拡張はすべて同じ roster を読む。
- **ジオメトリは全員共通**（classic 用）。`geometry.vivecraftbedrockbridge.vr_avatar`
  - 四肢は最初から2分割（armRight/forearmRight、legRight/shinRight 等）。ピボットは肩・肘・股関節・膝。
  - 上側セグメントは box UV でスキン領域の上半分をサンプリング。下側セグメントは per-face UV で下半分＋手足の底面を正確に対応付け。
  - 確認事項（S2-1 実機）: per-face UV の north/south が前後逆なら入れ替える。
  - slim 型（腕3px）用ジオメトリは未作成。slim の人が登録される段階で追加する。
- **テクスチャとエンティティ定義は VR プレイヤーごとに1セット**（当面の方式）:
  - 定義 ID: `vivecraftbedrockbridge:vr_avatar_<UUID（ハイフンを _ に置換）>`
  - テクスチャ: `textures/entity/vr_avatar_<UUID>.png`
  - 登録済み: RK3_Vivecraft（8fe06016-…、classic、skin_index 0）のみ。
- **将来の移行候補（S2-2 で実験）**: 定義を1つにし、`skin_index` と `model_type` をエンティティプロパティにして、render controller の配列＋Molang（query.property）でテクスチャ/ジオメトリを選び分ける。成立すれば人の追加がパック更新だけで済む。プロパティは 29+2=31 個になり、Bedrock の上限（32前後という説、要確認）に近い点に注意。
- スキンは Mojang API から取得して焼き込む。登録時に次をチェックする:
  - `SKIN.metadata.model` があれば slim、無ければ classic。
  - 旧形式 64×32 スキンは 64×64 へ変換（右腕・右脚を左へ反転コピー）が必要。
  - オーバーレイ層（hat/jacket 等）の使用有無。**RK3_Vivecraft のスキンはオーバーレイ全領域が完全透過＝未使用（2026-11-24 実測）** ので、オーバーレイ用ボックスは未実装のまま進める。使用している人が登録される際に追加する（inflate 付きボックス＋透過マテリアルの確認込み）。

## 運用ルール（レビューで合意）

- VR プレイヤーは事前登録制。未登録の人は既定スキンで表示し、登録は再参加時に反映される。
- パックを再生成するたびに manifest の version を上げる（上げないとクライアントに古いキャッシュが残る）。
- 接続中クライアントへのパック強制再取得の手段はないため、「パック更新時は再接続が必要」と案内する。
- `/geyser reload` が接続中セッションへ与える影響は要確認（手元PCでの S2-0 残作業の確認項目に追加）。

## その他

- Geyser 側の配布方法（拡張の dataFolder に同梱 / DefineResourcePacks での登録）は S2-1 実装時に確定する。
