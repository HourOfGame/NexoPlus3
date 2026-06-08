# NexoPlus 🎮

> **Advanced ResourcePack & Custom Content Plugin for Minecraft**  
> Hỗ trợ Spigot/Paper 1.16.5 → 1.20.x · Java 17+

[![Build](https://github.com/YOUR_USERNAME/NexoPlus/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR_USERNAME/NexoPlus/actions)

---

## ✨ Tính Năng

| Tính năng | NexoPlus | ItemsAdder |
|-----------|----------|------------|
| Custom Items | ✅ | ✅ |
| Custom Blocks (NOTE_BLOCK) | ✅ 800+ | ✅ |
| Custom Blocks (MUSHROOM) | ✅ | ✅ |
| Custom Armor Textures | ✅ | ✅ |
| Custom Sounds | ✅ | ✅ |
| Custom Fonts/HUD | ✅ | ✅ |
| Custom Recipes (6 loại) | ✅ | ✅ |
| Furniture System | ✅ | ✅ |
| **Built-in Pack Hosting** | ✅ | ❌ |
| **MiniMessage Support** | ✅ | ❌ |
| **SQLite/MySQL Storage** | ✅ | ❌ |
| **Auto CMD Assignment** | ✅ | ❌ |
| **PlaceholderAPI** | ✅ | ✅ |
| **Open Source** | ✅ | ❌ |

---

## 📥 Tải JAR

👉 Vào tab **[Releases](../../releases)** để tải file `.jar` mới nhất.

Hoặc vào **[Actions](../../actions)** → chọn build mới nhất → download artifact.

---

## 🚀 Cài Đặt

1. Tải `NexoPlus-1.0.0.jar` từ Releases
2. Copy vào `plugins/` của server
3. Khởi động server
4. Cấu hình tại `plugins/NexoPlus/config.yml`

---

## 📁 Cấu Trúc Thư Mục

```
plugins/NexoPlus/
├── config.yml
├── items/          ← Định nghĩa custom items (.yml)
├── blocks/         ← Định nghĩa custom blocks (.yml)
├── recipes/        ← Custom recipes (.yml)
├── sounds/         ← File .ogg âm thanh
├── textures/       ← Texture PNG
│   ├── item/
│   ├── block/
│   └── armor/
└── resourcepack/   ← Pack tự động generate ra đây
```

---

## 🗡️ Ví Dụ Item

```yaml
# plugins/NexoPlus/items/my_items.yml
ruby_sword:
  material: DIAMOND_SWORD
  display_name: "<red>✦ Ruby Sword"
  custom_model_data: 1001
  Pack:
    textures: item/ruby_sword.png
  lore:
    - "<gray>Thanh kiếm hồng ngọc"
    - "<red>+15 Attack Damage"
  unbreakable: true
  glow: true
  attributes:
    GENERIC_ATTACK_DAMAGE:
      amount: 15.0
      operation: ADD_NUMBER
      slot: HAND
```

---

## 🧱 Ví Dụ Block

```yaml
# plugins/NexoPlus/blocks/ores.yml
ruby_ore:
  block_type: NOTE_BLOCK
  block_data: 1
  texture: block/ruby_ore.png
  hardness: 3.0
  required_tool: pickaxe
  drops:
    - item_id: "myns:ruby_gem"
      min_amount: 1
      max_amount: 3
      chance: 1.0
```

---

## ⚙️ Commands

| Lệnh | Mô tả |
|------|-------|
| `/nexoplus reload` | Reload plugin |
| `/nexoplus generate` | Build ResourcePack |
| `/nexoplus list` | Xem tất cả items |
| `/give <player> <id> [amount]` | Cho item |
| `/resourcepack send` | Gửi pack cho tất cả |

---

## 🔧 Build từ Source

```bash
git clone https://github.com/YOUR_USERNAME/NexoPlus
cd NexoPlus
mvn clean package
# JAR xuất ra: target/NexoPlus-1.0.0.jar
```

---

## 📄 License

MIT License - Free to use and modify.
