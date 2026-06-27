# Simple Farmer Story

This example shows the smallest useful Chronicle Engine pack:

- right-click a farmer villager to open a dialogue;
- accept a main quest from the farmer;
- kill one zombie;
- receive ten iron ingots;
- buy vanilla food from the farmer shop;
- use emeralds as the shop wallet currency.

The content is stored as a real datapack under `data/simple_farmer_story/chronicle`.

To use it in a modpack, copy this folder into a world's `datapacks` folder, or copy the JSON files into:

`config/chronicle_engine/chronicle_pack`

## Commands

- `/chronicle dialogue start @p simple_farmer_story:farmer_dialogue`
- `/chronicle quest give @p simple_farmer_story:kill_zombie`
- `/chronicle shop open @p simple_farmer_story:farmer_food_shop`
