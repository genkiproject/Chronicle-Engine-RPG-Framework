# Chronicle Engine: RPG Framework

Forge 1.20.1 data-driven RPG framework for quests, dialogues, NPC bindings, shops, wallets, quest trackers, and map markers.

Chronicle Engine is intended for modpack authors who want to ship story content without hardcoding their pack-specific questline into the mod jar.

## Data Layout

Built-in datapack JSON resources can be placed under:

- `data/<namespace>/chronicle/dialogues/*.json`
- `data/<namespace>/chronicle/quests/*.json`
- `data/<namespace>/chronicle/npc/*.json`
- `data/<namespace>/chronicle/trades/*.json`
- `data/<namespace>/chronicle/shops/*.json`
- `data/<namespace>/chronicle/wallet/*.json`

Editable external content is loaded from the Minecraft instance config folder:

- `config/chronicle_engine/chronicle_pack/dialogues/*.json`
- `config/chronicle_engine/chronicle_pack/quests/*.json`
- `config/chronicle_engine/chronicle_pack/npc/*.json`
- `config/chronicle_engine/chronicle_pack/trades/*.json`
- `config/chronicle_engine/chronicle_pack/shops/*.json`
- `config/chronicle_engine/chronicle_pack/wallet/*.json`

External `chronicle_pack` files are loaded after built-in resources, so they override same-id bundled data.

## Commands

- `/chronicle reload`
- `/chronicle dialogue start <players> <dialogue_id>`
- `/chronicle quest give <players> <quest_id>`
- `/chronicle quest complete <players> <quest_id>`
- `/chronicle quest reset <players> <quest_id>`
- `/chronicle quest reset_all [players]`
- `/chronicle quest journal`
- `/chronicle shop open <players> <shop_id>`

## Dialogue Options

Dialogue JSON files may set top-level `allowEscClose`.

- Omit it, or set it to `true`, to keep normal ESC closing behavior.
- Set it to `false` for mandatory first-run or tutorial dialogues that must be completed through a choice button.

## Content Tools

The browser-based no-code maker lives in:

- `tools/chronicle_maker/index.html`

It can scan a selected modpack folder, infer mod namespaces from `mods`, collect useful id suggestions from `kubejs` and existing Chronicle config files, edit quests/dialogues/NPC bindings/shops/wallets through forms, validate the result, and write files directly to `config/chronicle_engine/chronicle_pack`.

Chronicle Maker defaults to English and includes Simplified Chinese and Japanese UI options.

## Example Pack

A small example pack is provided in:

- `examples/simple_farmer_story`

It binds a farmer villager to a dialogue, starts a main quest to kill one zombie, rewards ten iron ingots, adds a farmer food shop, and uses emeralds as shop currency.

## License

Chronicle Engine: RPG Framework is released under the MIT License.
