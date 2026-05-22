# ⚙️ Create Diagram — Blueprint Editor

**Create Diagram** is a Minecraft (NeoForge) mod designed to revolutionize the way you plan your factories. Forget paper sketches or randomly placed blocks: this mod provides you with a full in-game flowchart editor, perfectly integrated with the mechanics, recipes, and graphics of the Create mod and all its add-ons (e.g., Create: New Age).

---

## ✨ Key Features

### 🎨 Infinite & Interactive Workspace
- An explorable canvas with smooth **Dynamic Zoom** and **Panning**.
- **20×20 snap-to-grid** system to keep your blueprints perfectly organized.

### 🤖 Smart Machinery & Logic
- **Total EMI Integration:** Renders original textures, formats fluids in millibuckets (mB), and displays accurate drop percentage tooltips.
- **Dynamic Recipe Engine:** Connect inputs to a machine (like a Mixer or Press) and the mod will automatically calculate and enable the correct output ports!
- **Special Renderings:** Accurate visual support (e.g., paired Crushing Wheels, Basins placed under presses).

### 🔀 Advanced Connectivity System (×N Routing)
- Connect machines using elegant **Bézier curves**.
- **Quantity Sliders:** Distribute your logistic flows! If a mixer produces 2 Brass Ingots, you can drag the slider to send ×1 to one belt and ×1 to another.
- **Automatic Garbage Collector:** If you change a machine's recipe, all "orphan" edges carrying items that are no longer produced are automatically and instantly removed.

### 📝 Integrated Annotation Tools
- **UML Sticky Notes** (`Shift + Right Click`): Jot down notes directly in-game with a native text editor (supports cursor blinking, text wrapping, and backspacing).
- **Double Click:** Instantly clone machines or notes.
- **Popup Color Menu:** Change the color of your sticky notes on the fly.

### 🖌️ Freehand Drawing
- Dedicated **Toolbar** (Pen, Straight Line, Eraser).
- **Hold-to-open:** Click and hold a tool to open a quick color palette (White, Yellow, Red, Blue, Green, Orange).

### 💾 Persistent Storage (Data-Driven)
- Utilizes modern Minecraft **DataComponents** and NeoForge's **Custom Packet Payloads**.
- Close the UI and reopen it whenever you want: your entire project (nodes, weighted edges, and freehand drawings) safely travels between Client and Server, saving directly onto the physical Blueprint item in your hand.

### 🌍 Mod-Agnostic (Universal Support)
- The item library (Palette) is **not hardcoded**! The mod automatically reads the Minecraft Registry and filters by tags, instantly supporting any Create expansion added to your modpack.

---

## 🎮 Controls & Shortcuts

| Action | Control |
|---|---|
| Pan Camera | Hold `Right Click` in an empty space and drag |
| Zoom in/out | Mouse Wheel |
| Scroll Menu Categories | Mouse Wheel |
| Add an Object | Drag from the left Palette onto the Canvas |
| Move an Object | Hold `Left Click` on the object and drag |
| Connect Nodes | Hold `Left Click` from a node's output port to another node's input port |
| Balance Resources | `Left Click` on the "×N" badge in the middle of an edge to open the Slider |
| Disconnect Nodes | `Right Click` on a machine's input port |
| Duplicate Node/Note | Quick Double Click on the object |
| Create Sticky Note | `Shift + Right Click` in an empty space |
| Edit Text Note | Single Click on the note and type |
| Delete an Object | `Right Click` on the object |

---

## 🛠️ Technical Requirements

- **Mod Loader:** NeoForge (for Minecraft 1.20.5+ / 1.21+)
- **Required Dependencies:**
  - [Create](https://modrinth.com/mod/create)
  - [EMI](https://modrinth.com/mod/emi) — Essential for recipe resolution, tooltips, and GUI rendering.

---

## 👨‍💻 Developer Notes

This mod is built following strict **Data-Driven** architecture patterns:

- No hardcoded JSON files are overwritten.
- Client-Server communication uses non-blocking `CustomPacketPayload` interfaces.
- Fluid and item flow renderings bypass hardcoded checks, delegating ID analysis to `EmiStack`. If a gas or fluid is detected, it will automatically be formatted with the `mB` suffix.

## 🐛 BUGS

The mod is still in beta, if you have suggestions, bugs to report, or want to contribute, feel free to open an **Issue** or a **Pull Request**!

I am not good in drawing items and stuff, if you want to help me to draw a better logo and items, feel free to contact me!

[![Buy Me A Coffee](https://img.buymeacoffee.com/button-api/?text=Buy%20me%20a%20coffee&emoji=&slug=emalter&button_colour=FFDD00&font_colour=000000&font_family=Bree&outline_colour=000000&coffee_colour=ffffff)](https://www.buymeacoffee.com/emalter)
