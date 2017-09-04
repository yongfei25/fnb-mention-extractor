Extract food and beverage mentions from wikimedia data dump.
Produce `annotations.txt` that consists of one sentence per line.
Each token of the sentences are tagged with:
- `FNB` Food and beverage entity
- `O` Non food and beverage entity

`FNB` tags have a prefix of either B- (begining of named entity) or I- (continuation of named entity).

Examples:

- **肉/B-FNB 骨/I-FNB 茶/I-FNB** 通/O 常/O 伴/O 白/O 飯/O ,/O **豆/B-FNB 腐/I-FNB** 卜/O 或/O 以/O 油/O 條/O **蘸/B-FNB** 湯/O 來/O 吃/O 。/O
- 中/O 歐/O （/O 歐/O 洲/O 中/O 部/O 地/O 區/O ）/O 的/O **白/B-FNB 菜/I-FNB 卷/I-FNB**
- 在/O 印/O 度/O 尼/O 西/O 亚/O ，/O 一/O 个/O 小/O 号/O 的/O **印/B-FNB 尼/I-FNB 千/I-FNB 层/I-FNB 糕/I-FNB** 就/O 要/O 花/O 费/O 4/O 0/O 0/O ,/O 0/O 0/O 0/O 印/O 尼/O 盾/O （/O 约/O €/O 1/O 2/O ./O 5/O 0/O ）/O 。/
- 英/O 國/O 及/O 紐/O 澳/O 的/O **巧/B-FNB 克/I-FNB 力/I-FNB 布/I-FNB 丁/I-FNB**
- 而/O 在/O 美/O 國/O ，/O **鹼/B-FNB 漬/I-FNB 魚/I-FNB** 常/O 與/O 肉/O 丸/O 同/O 食/O 。/O