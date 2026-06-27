const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

const COMMON_ITEMS = [
  "minecraft:emerald",
  "minecraft:iron_ingot",
  "minecraft:gold_ingot",
  "minecraft:diamond",
  "minecraft:bread",
  "minecraft:apple",
  "minecraft:potion",
  "minecraft:enchanted_book"
];

const COMMON_ENTITIES = [
  "minecraft:player",
  "minecraft:villager",
  "minecraft:zombie",
  "minecraft:skeleton",
  "minecraft:spider",
  "minecraft:creeper",
  "minecraft:enderman"
];

const VILLAGER_PROFESSIONS = [
  "none",
  "nitwit",
  "farmer",
  "fisherman",
  "shepherd",
  "fletcher",
  "librarian",
  "cartographer",
  "cleric",
  "armorer",
  "weaponsmith",
  "toolsmith",
  "butcher",
  "leatherworker",
  "mason"
];

const state = {
  projectHandle: null,
  projectName: "",
  activeView: "setup",
  language: localStorage.getItem("chronicleMakerLanguage") || "en_us",
  selected: {
    quests: "",
    dialogues: "",
    npcs: "",
    shops: "",
    wallets: ""
  },
  settings: {
    namespace: "my_story",
    packName: "My Chronicle Story",
    exportMode: "config",
    defaultCategory: "main"
  },
  data: {
    quests: [],
    dialogues: [],
    npcs: [],
    shops: [],
    wallets: []
  },
  hints: {
    mods: [],
    items: [...COMMON_ITEMS],
    entities: [...COMMON_ENTITIES],
    quests: [],
    dialogues: [],
    shops: [],
    flags: []
  }
};

const viewCopy = {
  setup: ["Project Setup", "Select a modpack project. The editor will read mods and the CE:RF config folder."],
  quests: ["Quest Editing", "Build main quests or side quests with phases, objectives, and rewards."],
  dialogues: ["Dialogue Editing", "Create NPC lines, choices, and choice actions."],
  npcs: ["NPC Bindings", "Bind entity types or villager professions to dialogues."],
  shops: ["Shop Editing", "Create purchasable content with categories and shop entries."],
  wallets: ["Wallet Currencies", "Bind one or more items as CE:RF wallet currencies."],
  export: ["Export And Validate", "Check missing references and write files into the project folder."]
};

const LANGUAGE_META = {
  en_us: { htmlLang: "en", label: "English" },
  zh_cn: { htmlLang: "zh-CN", label: "简体中文" },
  ja_jp: { htmlLang: "ja", label: "日本語" }
};

const PHRASES = [
  ["Chronicle Engine Pack Maker", "Chronicle Engine Pack Maker", "Chronicle Engine Pack Maker"],
  ["CE:RF Datapack Editor", "CE:RF 数据包编辑器", "CE:RF データパックエディター"],
  ["Language", "语言", "言語"],
  ["Project", "项目", "プロジェクト"],
  ["Quests", "任务", "クエスト"],
  ["Dialogues", "对话", "会話"],
  ["Shops", "商店", "ショップ"],
  ["Wallets", "钱包", "ウォレット"],
  ["Export", "导出", "エクスポート"],
  ["No project selected", "未选择项目", "プロジェクト未選択"],
  ["You can use the example first, or select a modpack root folder.", "可先使用示例，也可选择整合包根目录。", "先にサンプルを使うか、Modpack のルートフォルダーを選択できます。"],
  ["Project Setup", "项目设置", "プロジェクト設定"],
  ["Select a modpack project. The editor will read mods and the CE:RF config folder.", "选择整合包项目，编辑器会读取 mods 与 CE:RF 配置目录。", "Modpack プロジェクトを選択すると、エディターが mods と CE:RF 設定フォルダーを読み込みます。"],
  ["Quest Editing", "任务编辑", "クエスト編集"],
  ["Build main quests or side quests with phases, objectives, and rewards.", "用阶段、目标和奖励搭出主线或支线任务。", "フェーズ、目標、報酬でメインクエストやサイドクエストを作成します。"],
  ["Dialogue Editing", "对话编辑", "会話編集"],
  ["Create NPC lines, choices, and choice actions.", "创建 NPC 台词、选项和选项触发动作。", "NPC の台詞、選択肢、選択時のアクションを作成します。"],
  ["NPC Bindings", "NPC 绑定", "NPC バインド"],
  ["Bind entity types or villager professions to dialogues.", "把实体类型或村民职业绑定到指定对话。", "エンティティ種別や村人の職業を会話にバインドします。"],
  ["Shop Editing", "商店编辑", "ショップ編集"],
  ["Create purchasable content with categories and shop entries.", "用分类和商品项创建可购买内容。", "カテゴリーと商品項目で購入可能な内容を作成します。"],
  ["Wallet Currencies", "钱包货币", "ウォレット通貨"],
  ["Bind one or more items as CE:RF wallet currencies.", "指定一个或多个物品作为 CE:RF 钱包货币。", "1つ以上のアイテムを CE:RF ウォレット通貨として指定します。"],
  ["Export And Validate", "导出与检查", "エクスポートと検証"],
  ["Check missing references and write files into the project folder.", "检查缺失引用并写入项目文件夹。", "不足している参照を確認し、プロジェクトフォルダーへ書き込みます。"],
  ["Load Example", "载入示例", "サンプルを読み込む"],
  ["Write Project", "写入项目", "プロジェクトへ書き込む"],
  ["Project Source", "项目来源", "プロジェクト元"],
  ["Use Chromium or Edge for best local file access", "推荐使用 Chromium / Edge 打开此文件", "ローカルファイル操作には Chromium / Edge を推奨します"],
  ["Select Modpack Project Folder", "选择整合包项目文件夹", "Modpack プロジェクトフォルダーを選択"],
  ["Read Existing CE:RF Config", "读取已有 CE:RF 配置", "既存の CE:RF 設定を読み込む"],
  ["After selecting a project, the editor scans", "选择后会扫描", "プロジェクト選択後、エディターは次をスキャンします"],
  ["and", "和", "と"],
  ["If your browser cannot write folders, use the JSON preview on the right to save manually.", "浏览器不支持目录写入时，仍可使用右侧 JSON 预览手动保存。", "ブラウザーがフォルダー書き込みに対応していない場合は、右側の JSON プレビューから手動保存できます。"],
  ["Basic Info", "基础信息", "基本情報"],
  ["Controls generated file names and namespaces", "影响生成文件名和命名空间", "生成されるファイル名と名前空間に影響します"],
  ["Namespace", "命名空间", "名前空間"],
  ["Pack Name", "包名称", "パック名"],
  ["Output Mode", "输出方式", "出力方式"],
  ["Write to config/chronicle_engine/chronicle_pack", "写入 config/chronicle_engine/chronicle_pack", "config/chronicle_engine/chronicle_pack へ書き込む"],
  ["Generate a standard datapack folder", "生成标准 datapack 文件夹", "標準 datapack フォルダーを生成"],
  ["Default Quest Category", "任务默认类别", "デフォルトのクエストカテゴリー"],
  ["Scan Results", "扫描结果", "スキャン結果"],
  ["Used for suggestions only. Your files are not changed automatically.", "用于下拉建议，不会自动修改你的文件", "候補表示のみに使用され、ファイルは自動変更されません。"],
  ["Detected Mod Namespaces", "检测到的模组命名空间", "検出された Mod 名前空間"],
  ["Available References", "可引用的数据", "利用可能な参照"],
  ["Item Suggestions", "物品建议", "アイテム候補"],
  ["Quest List", "任务列表", "クエスト一覧"],
  ["Add", "新增", "追加"],
  ["Kill Quest", "击杀任务", "討伐クエスト"],
  ["Collect Quest", "收集任务", "収集クエスト"],
  ["Dialogue Progress", "对话推进", "会話進行"],
  ["Dialogue List", "对话列表", "会話一覧"],
  ["Validate And Write", "检查与写入", "検証と書き込み"],
  ["Directories and JSON files are generated before writing", "写入前会自动生成目录和 JSON 文件", "書き込み前にディレクトリと JSON ファイルを生成します"],
  ["Validate Data", "检查数据", "データを検証"],
  ["Download File Index", "下载文件清单", "ファイル一覧をダウンロード"],
  ["JSON Preview", "JSON 预览", "JSON プレビュー"],
  ["No manual writing required. Use this only for confirmation.", "无需手写，仅用于确认", "手書き不要です。確認用として使用してください。"],
  ["Select an item on the left", "请选择左侧项目", "左側の項目を選択してください"],
  ["You can also click “Add”; the editor will create a ready-to-use basic template.", "也可以点击“新增”，编辑器会创建一个可直接使用的基础模板。", "「追加」を押すと、すぐ使える基本テンプレートを作成できます。"],
  ["No content yet.", "暂无内容。", "まだ内容がありません。"],
  ["No mods scanned yet", "尚未扫描 mods", "mods はまだスキャンされていません"],
  ["Can read existing config and write changes back to this project.", "可以读取已有配置并写回项目。", "既存設定を読み込み、変更をこのプロジェクトへ書き戻せます。"],
  ["Built-in example loaded.", "已载入内置示例。", "内蔵サンプルを読み込みました。"],
  ["This browser cannot write folders. Open this HTML with a recent Edge or Chrome.", "当前浏览器不支持目录写入。请使用新版 Edge / Chrome 打开此 HTML。", "このブラウザーはフォルダー書き込みに対応していません。新しい Edge / Chrome でこの HTML を開いてください。"],
  ["Select a modpack project folder first.", "请先选择整合包项目文件夹。", "先に Modpack プロジェクトフォルダーを選択してください。"],
  ["No config/chronicle_engine/chronicle_pack folder was found. You can create content first, then write it.", "没有找到 config/chronicle_engine/chronicle_pack。你可以先创建内容再写入。", "config/chronicle_engine/chronicle_pack が見つかりません。先に内容を作成してから書き込めます。"],
  ["Found the CE:RF config folder, but no readable JSON files were found.", "找到了 CE:RF 配置目录，但里面没有可读取的 JSON。", "CE:RF 設定フォルダーは見つかりましたが、読み込める JSON がありません。"],
  ["Select a project folder first. If folder permission is unavailable, copy the JSON preview on the Export page.", "请先选择项目文件夹。没有目录权限时，可在导出页复制 JSON 预览。", "先にプロジェクトフォルダーを選択してください。フォルダー権限がない場合は、エクスポート画面の JSON プレビューをコピーしてください。"],
  ["Validation passed: no issues blocking writes were found.", "检查通过：没有发现阻止写入的问题。", "検証成功：書き込みを妨げる問題はありません。"],
  ["Quest", "任务", "クエスト"],
  ["Quest ID", "任务 ID", "クエスト ID"],
  ["Display Name", "显示名称", "表示名"],
  ["Category", "分类", "カテゴリー"],
  ["Sort Order", "排序", "並び順"],
  ["Mode", "模式", "モード"],
  ["Phase Progression", "阶段推进", "フェーズ進行"],
  ["Simple Quest", "简单任务", "シンプルクエスト"],
  ["Initial Phase", "初始阶段", "初期フェーズ"],
  ["Repeatable", "可重复", "繰り返し可能"],
  ["Team Sync", "团队同步", "チーム同期"],
  ["Quest Description", "任务描述", "クエスト説明"],
  ["Flag Settings", "旗标设置", "フラグ設定"],
  ["Set Flags On Accept", "接取时设置旗标", "受注時に設定するフラグ"],
  ["Set Flags On Complete", "完成时设置旗标", "完了時に設定するフラグ"],
  ["Unlock Conditions", "解锁条件", "解放条件"],
  ["Completion Rewards", "完成奖励", "完了報酬"],
  ["Quest Phases", "任务阶段", "クエストフェーズ"],
  ["Quest Route Map", "任务路线图", "クエストルートマップ"],
  ["No phases yet. Add a phase first.", "还没有阶段，先新增一个阶段。", "フェーズがありません。先にフェーズを追加してください。"],
  ["Click a phase card to jump to its editor block. Choose branches in \"After Completion\".", "点击阶段卡片会跳到对应编辑块。分支走向在“完成后跳转”里选择。", "フェーズカードをクリックすると該当編集ブロックへ移動します。分岐は「完了後の移動」で選択します。"],
  ["Finish Quest", "完成任务", "クエスト完了"],
  ["No phase ID set", "未设置阶段 ID", "フェーズ ID 未設定"],
  ["Finish Quest / No Further Transition", "完成任务 / 不再跳转", "クエスト完了 / これ以上移動しない"],
  ["Go to Finish Quest", "跳转到 完成任务", "クエスト完了へ移動"],
  ["Enter the next step when the condition is met.", "满足条件后进入下一步", "条件を満たすと次のステップへ進みます。"],
  ["Unnamed Phase", "未命名阶段", "無名フェーズ"],
  ["Phase ID", "阶段 ID", "フェーズ ID"],
  ["Phase Name", "阶段名称", "フェーズ名"],
  ["Phase Description", "阶段描述", "フェーズ説明"],
  ["Phase Story", "阶段剧情", "フェーズ物語"],
  ["Phase Flags", "阶段旗标", "フェーズフラグ"],
  ["Set Flags On Phase Enter", "进入阶段设置旗标", "フェーズ開始時に設定するフラグ"],
  ["Set Flags On Phase Complete", "完成阶段设置旗标", "フェーズ完了時に設定するフラグ"],
  ["Objectives", "目标", "目標"],
  ["Add Objective", "新增目标", "目標を追加"],
  ["Phase Rewards", "阶段奖励", "フェーズ報酬"],
  ["After Completion", "完成后跳转", "完了後の移動"],
  ["Add Transition", "新增跳转", "移動を追加"],
  ["Objective Type", "目标类型", "目標タイプ"],
  ["Target ID", "目标 ID", "対象 ID"],
  ["Required Count", "需要数量", "必要数"],
  ["Display Text", "显示文本", "表示テキスト"],
  ["Hidden Objective", "隐藏目标", "非表示目標"],
  ["Optional Objective", "可选目标", "任意目標"],
  ["Marker Dimension", "标记维度", "マーカーのディメンション"],
  ["Marker X", "标记 X", "マーカー X"],
  ["Marker Y", "标记 Y", "マーカー Y"],
  ["Marker Z", "标记 Z", "マーカー Z"],
  ["Target Phase", "目标阶段", "移動先フェーズ"],
  ["Condition", "条件", "条件"],
  ["Condition Type", "条件类型", "条件タイプ"],
  ["Flag", "旗标", "フラグ"],
  ["Name Contains", "名称包含", "名前に含む文字"],
  ["Villager Profession", "村民职业", "村人の職業"],
  ["Always true. No extra parameters are needed.", "始终满足，不需要额外参数。", "常に成立します。追加パラメーターは不要です。"],
  ["Add Condition", "新增条件", "条件を追加"],
  ["Add Cost", "新增花费", "コストを追加"],
  ["Add Reward", "新增奖励", "報酬を追加"],
  ["Type", "类型", "タイプ"],
  ["Item ID", "物品 ID", "アイテム ID"],
  ["Count", "数量", "数"],
  ["Enchantment ID", "附魔 ID", "エンチャント ID"],
  ["Level", "等级", "レベル"],
  ["Potion Item", "药水物品", "ポーションアイテム"],
  ["Potion ID", "药水 ID", "ポーション ID"],
  ["Command", "命令", "コマンド"],
  ["Random rewards can be adjusted with weights after export. In no-code mode, fixed rewards are recommended first.", "随机奖励可在导出后进阶调整权重。零基础模式建议先使用固定奖励。", "ランダム報酬の重みはエクスポート後に調整できます。ノーコードではまず固定報酬を推奨します。"],
  ["Action Type", "动作类型", "アクションタイプ"],
  ["Add Action", "新增动作", "アクションを追加"],
  ["Close the current dialogue. No extra parameters are needed.", "关闭当前对话，不需要额外参数。", "現在の会話を閉じます。追加パラメーターは不要です。"],
  ["Dialogue", "对话", "会話"],
  ["Dialogue ID", "对话 ID", "会話 ID"],
  ["Default NPC Name", "NPC 默认显示名", "NPC デフォルト表示名"],
  ["Start Node ID", "起始节点 ID", "開始ノード ID"],
  ["Nodes", "节点", "ノード"],
  ["Add Node", "新增节点", "ノードを追加"],
  ["Node ID", "节点 ID", "ノード ID"],
  ["Line", "台词", "台詞"],
  ["Choices", "选项", "選択肢"],
  ["Add Choice", "新增选项", "選択肢を追加"],
  ["Choice ID", "选项 ID", "選択肢 ID"],
  ["Button Text", "按钮文字", "ボタン文字"],
  ["Next Node ID", "跳到节点 ID", "次のノード ID"],
  ["Display Conditions", "显示条件", "表示条件"],
  ["Triggered Actions", "触发动作", "発動アクション"],
  ["Close or run actions", "关闭或执行动作", "閉じる、またはアクション実行"],
  ["File Name", "文件名", "ファイル名"],
  ["Entity Type", "实体类型", "エンティティタイプ"],
  ["Dialogue Distance", "对话距离", "会話距離"],
  ["Cancel Vanilla Interaction", "取消原版交互", "バニラのインタラクションを無効化"],
  ["Look At Player", "看向玩家", "プレイヤーを見る"],
  ["Stop Moving", "停止移动", "移動を止める"],
  ["Binding Rules", "绑定规则", "バインドルール"],
  ["Add Binding", "新增绑定", "バインドを追加"],
  ["Binding ID", "绑定 ID", "バインド ID"],
  ["NBT Dialogue Field", "NBT 对话字段", "NBT 会話フィールド"],
  ["Priority", "优先级", "優先度"],
  ["Match Condition", "匹配条件", "一致条件"],
  ["No dialogue set", "未设置对话", "会話未設定"],
  ["No item set", "未设置物品", "アイテム未設定"],
  ["No enchantment set", "未设置附魔", "エンチャント未設定"],
  ["No command set", "未设置命令", "コマンド未設定"],
  ["No quest set", "未设置任务", "クエスト未設定"],
  ["No interaction target set", "未设置交互目标", "インタラクト対象未設定"],
  ["No flag set", "未设置旗标", "フラグ未設定"],
  ["Shop", "商店", "ショップ"],
  ["Shop ID", "商店 ID", "ショップ ID"],
  ["Description", "描述", "説明"],
  ["Open Condition", "打开条件", "開始条件"],
  ["Categories", "分类", "カテゴリー"],
  ["Add Category", "新增分类", "カテゴリーを追加"],
  ["Color", "颜色", "色"],
  ["Entries", "商品", "商品"],
  ["Add Entry", "新增商品", "商品を追加"],
  ["Entry ID", "商品 ID", "商品 ID"],
  ["Visible Condition", "可见条件", "表示条件"],
  ["Costs", "花费", "コスト"],
  ["Rewards", "获得", "入手"],
  ["Uncategorized", "未分类", "未分類"],
  ["Currency ID", "货币 ID", "通貨 ID"],
  ["Primary Item ID", "主物品 ID", "主アイテム ID"],
  ["Depositable Currency Item IDs", "可存入的钱币物品 ID", "預け入れ可能な通貨アイテム ID"],
  ["One ID or flag per line.", "每行一个 ID 或旗标。", "1行に1つの ID またはフラグを入力します。"],
  ["Kill Entity", "击杀生物", "エンティティ討伐"],
  ["Inventory Has Item", "背包持有物品", "インベントリ内アイテム所持"],
  ["Offer Item", "交付物品", "アイテム納品"],
  ["Interact With NPC/Target", "与 NPC/目标交互", "NPC/対象とインタラクト"],
  ["Complete Advancement", "完成成就/进度", "進捗達成"],
  ["Always True", "始终满足", "常に成立"],
  ["Has Flag", "拥有旗标", "フラグ所持"],
  ["Quest Not Started", "任务未开始", "クエスト未開始"],
  ["Quest In Progress", "任务进行中", "クエスト進行中"],
  ["Quest Is In Phase", "任务处于阶段", "クエストがフェーズ中"],
  ["Entity Name Matches", "实体名称匹配", "エンティティ名一致"],
  ["Item", "物品", "アイテム"],
  ["Enchanted Book", "附魔书", "エンチャント本"],
  ["Potion", "药水", "ポーション"],
  ["Run Command", "执行命令", "コマンド実行"],
  ["Close Dialogue", "关闭对话", "会話を閉じる"],
  ["Start Quest", "接取任务", "クエスト受注"],
  ["Notify Interaction Objective", "推进交互目标", "インタラクト目標を進行"],
  ["Set Flag", "设置旗标", "フラグ設定"],
  ["Open Shop", "打开商店", "ショップを開く"],
  ["Give Item", "给予物品", "アイテム付与"],
  ["New Quest", "新的任务", "新しいクエスト"],
  ["Write the quest description here.", "这里填写任务介绍。", "ここにクエスト説明を書きます。"],
  ["Collect Supplies", "收集物资", "物資収集"],
  ["Collect the required items and complete the request.", "收集指定物品并完成委托。", "指定アイテムを集めて依頼を完了します。"],
  ["Collect 10 Iron Ingots", "收集铁锭 10 个", "鉄インゴットを10個集める"],
  ["Visit NPC", "拜访 NPC", "NPC を訪ねる"],
  ["Talk to the specified NPC to progress the quest.", "与指定 NPC 对话来推进任务。", "指定 NPC と会話してクエストを進行します。"],
  ["Go Talk", "前往交谈", "会話しに行く"],
  ["Talk to the target NPC", "与目标 NPC 对话", "対象 NPC と会話する"],
  ["Clear Zombies", "清理僵尸", "ゾンビ退治"],
  ["Defeat one zombie, then claim the reward.", "击败一只僵尸，然后领取奖励。", "ゾンビを1体倒して報酬を受け取ります。"],
  ["Kill Zombie", "击杀僵尸", "ゾンビを倒す"],
  ["Kill 1 Zombie", "击杀僵尸 1 次", "ゾンビを1体倒す"],
  ["Task Phase", "任务阶段", "タスクフェーズ"],
  ["Explain what the player should do now.", "说明玩家现在要做什么。", "プレイヤーが今すべきことを説明します。"],
  ["Complete Objective", "完成目标", "目標を達成"],
  ["Villager", "村民", "村人"],
  ["Hello, need any help?", "你好，需要帮忙吗？", "こんにちは、手伝いが必要ですか？"],
  ["Leave", "离开", "退出"],
  ["New Shop", "新的商店", "新しいショップ"],
  ["Sells some items.", "售卖一些物品。", "いくつかのアイテムを販売します。"],
  ["General", "常用", "一般"],
  ["Bread", "面包", "パン"],
  ["Emerald", "绿宝石", "エメラルド"],
  ["Currency", "货币", "通貨"]
];

const PHRASE_MAPS = {};

function phraseMap(language) {
  if (PHRASE_MAPS[language]) return PHRASE_MAPS[language];
  const targetIndex = language === "zh_cn" ? 1 : language === "ja_jp" ? 2 : 0;
  const map = new Map();
  for (const row of PHRASES) {
    for (const source of row) map.set(source, row[targetIndex]);
  }
  PHRASE_MAPS[language] = map;
  return map;
}

function tr(text) {
  const value = String(text ?? "");
  return phraseMap(state.language).get(value) || value;
}

function translateText(text) {
  const leading = text.match(/^\s*/)?.[0] || "";
  const trailing = text.match(/\s*$/)?.[0] || "";
  const body = text.trim();
  if (!body) return text;
  return leading + translateBody(body) + trailing;
}

function translateBody(body) {
  const exact = phraseMap(state.language).get(body);
  if (exact) return exact;
  return translatePattern(body);
}

function translatePattern(body) {
  const language = state.language;
  const objectiveWord = tr("Objectives").toLowerCase();
  const transitionWord = language === "zh_cn" ? "跳转" : language === "ja_jp" ? "移動" : "transitions";
  const choiceWord = language === "zh_cn" ? "选项" : language === "ja_jp" ? "選択肢" : "choices";
  return body
    .replace(/^(\d+) 阶段$/, (_, count) => language === "zh_cn" ? `${count} 阶段` : language === "ja_jp" ? `${count} フェーズ` : `${count} phases`)
    .replace(/^(\d+) 节点$/, (_, count) => language === "zh_cn" ? `${count} 节点` : language === "ja_jp" ? `${count} ノード` : `${count} nodes`)
    .replace(/^(\d+) 绑定$/, (_, count) => language === "zh_cn" ? `${count} 绑定` : language === "ja_jp" ? `${count} バインド` : `${count} bindings`)
    .replace(/^(\d+) 商品$/, (_, count) => language === "zh_cn" ? `${count} 商品` : language === "ja_jp" ? `${count} 商品` : `${count} entries`)
    .replace(/^(\d+) 任务$/, (_, count) => language === "zh_cn" ? `${count} 任务` : language === "ja_jp" ? `${count} クエスト` : `${count} quests`)
    .replace(/^(\d+) 对话$/, (_, count) => language === "zh_cn" ? `${count} 对话` : language === "ja_jp" ? `${count} 会話` : `${count} dialogues`)
    .replace(/^(\d+) 商店$/, (_, count) => language === "zh_cn" ? `${count} 商店` : language === "ja_jp" ? `${count} ショップ` : `${count} shops`)
    .replace(/^(\d+) 物品建议$/, (_, count) => language === "zh_cn" ? `${count} 物品建议` : language === "ja_jp" ? `${count} アイテム候補` : `${count} item suggestions`)
    .replace(/^(\d+) 次$/, (_, count) => language === "zh_cn" ? `${count} 次` : language === "ja_jp" ? `${count} 回` : `${count} times`)
    .replace(/^(\d+) 个目标 · 下一步：(.+)$/, (_, count, next) => language === "zh_cn" ? `${count} 个目标 · 下一步：${translateBody(next)}` : language === "ja_jp" ? `${count} 目標 · 次：${translateBody(next)}` : `${count} ${objectiveWord} · Next: ${translateBody(next)}`)
    .replace(/^(\d+) 个目标 · (\d+) 个跳转$/, (_, objectives, transitions) => language === "zh_cn" ? `${objectives} 个目标 · ${transitions} 个跳转` : language === "ja_jp" ? `${objectives} 目標 · ${transitions} ${transitionWord}` : `${objectives} objectives · ${transitions} transitions`)
    .replace(/^(\d+) 个选项$/, (_, count) => language === "zh_cn" ? `${count} 个选项` : language === "ja_jp" ? `${count} ${choiceWord}` : `${count} choices`)
    .replace(/^(\d+) 个接取旗标 · (\d+) 个完成旗标$/, (_, accept, complete) => language === "zh_cn" ? `${accept} 个接取旗标 · ${complete} 个完成旗标` : language === "ja_jp" ? `${accept} 受注フラグ · ${complete} 完了フラグ` : `${accept} accept flags · ${complete} completion flags`)
    .replace(/^(\d+) 个进入旗标 · (\d+) 个完成旗标$/, (_, enter, complete) => language === "zh_cn" ? `${enter} 个进入旗标 · ${complete} 个完成旗标` : language === "ja_jp" ? `${enter} 開始フラグ · ${complete} 完了フラグ` : `${enter} enter flags · ${complete} completion flags`)
    .replace(/^(.+) → 完成任务$/, (_, from) => `${from} → ${tr("Finish Quest")}`)
    .replace(/^跳转到 (.+)$/, (_, target) => language === "zh_cn" ? `跳转到 ${translateBody(target)}` : language === "ja_jp" ? `${translateBody(target)} へ移動` : `Go to ${translateBody(target)}`)
    .replace(/^跳到 (.+)$/, (_, target) => language === "zh_cn" ? `跳到 ${target}` : language === "ja_jp" ? `${target} へ移動` : `Go to ${target}`)
    .replace(/^缺失的阶段：(.+)$/, (_, target) => language === "zh_cn" ? `缺失的阶段：${target}` : language === "ja_jp" ? `欠落フェーズ：${target}` : `Missing phase: ${target}`)
    .replace(/^已选择项目：(.+)$/, (_, name) => language === "zh_cn" ? `已选择项目：${name}` : language === "ja_jp" ? `プロジェクトを選択しました：${name}` : `Project selected: ${name}`)
    .replace(/^选择项目失败：(.+)$/, (_, message) => language === "zh_cn" ? `选择项目失败：${message}` : language === "ja_jp" ? `プロジェクト選択に失敗：${message}` : `Failed to select project: ${message}`)
    .replace(/^已读取 (\d+) 个 CE:RF JSON 文件。$/, (_, count) => language === "zh_cn" ? `已读取 ${count} 个 CE:RF JSON 文件。` : language === "ja_jp" ? `${count} 個の CE:RF JSON ファイルを読み込みました。` : `Read ${count} CE:RF JSON files.`)
    .replace(/^已写入 (\d+) 个文件。$/, (_, count) => language === "zh_cn" ? `已写入 ${count} 个文件。` : language === "ja_jp" ? `${count} 個のファイルを書き込みました。` : `Wrote ${count} files.`)
    .replace(/^写入失败：(.+)$/, (_, message) => language === "zh_cn" ? `写入失败：${message}` : language === "ja_jp" ? `書き込み失敗：${message}` : `Write failed: ${message}`)
    .replace(/^错误：任务 ID 不合法：(.+)$/, (_, id) => language === "zh_cn" ? `错误：任务 ID 不合法：${id}` : language === "ja_jp" ? `エラー：クエスト ID が不正です：${id}` : `Error: invalid quest ID: ${id}`)
    .replace(/^错误：任务 (.+) 没有阶段。$/, (_, id) => language === "zh_cn" ? `错误：任务 ${id} 没有阶段。` : language === "ja_jp" ? `エラー：クエスト ${id} にフェーズがありません。` : `Error: quest ${id} has no phases.`)
    .replace(/^错误：任务 (.+) 的初始阶段不存在。$/, (_, id) => language === "zh_cn" ? `错误：任务 ${id} 的初始阶段不存在。` : language === "ja_jp" ? `エラー：クエスト ${id} の初期フェーズが存在しません。` : `Error: quest ${id} has a missing initial phase.`)
    .replace(/^错误：任务 (.+) 有阶段缺少 ID。$/, (_, id) => language === "zh_cn" ? `错误：任务 ${id} 有阶段缺少 ID。` : language === "ja_jp" ? `エラー：クエスト ${id} に ID のないフェーズがあります。` : `Error: quest ${id} has a phase without an ID.`)
    .replace(/^错误：任务 (.+) 的目标缺少类型或目标 ID。$/, (_, id) => language === "zh_cn" ? `错误：任务 ${id} 的目标缺少类型或目标 ID。` : language === "ja_jp" ? `エラー：クエスト ${id} の目標にタイプまたは対象 ID がありません。` : `Error: quest ${id} has an objective without a type or target ID.`)
    .replace(/^错误：对话 ID 不合法：(.+)$/, (_, id) => language === "zh_cn" ? `错误：对话 ID 不合法：${id}` : language === "ja_jp" ? `エラー：会話 ID が不正です：${id}` : `Error: invalid dialogue ID: ${id}`)
    .replace(/^错误：对话 (.+) 的起始节点不存在。$/, (_, id) => language === "zh_cn" ? `错误：对话 ${id} 的起始节点不存在。` : language === "ja_jp" ? `エラー：会話 ${id} の開始ノードが存在しません。` : `Error: dialogue ${id} has a missing start node.`)
    .replace(/^错误：有 NPC 绑定缺少文件名。$/, () => language === "zh_cn" ? "错误：有 NPC 绑定缺少文件名。" : language === "ja_jp" ? "エラー：ファイル名のない NPC バインドがあります。" : "Error: an NPC binding is missing a file name.")
    .replace(/^错误：NPC (.+) 缺少实体类型。$/, (_, id) => language === "zh_cn" ? `错误：NPC ${id} 缺少实体类型。` : language === "ja_jp" ? `エラー：NPC ${id} にエンティティタイプがありません。` : `Error: NPC ${id} is missing an entity type.`)
    .replace(/^错误：商店 ID 不合法：(.+)$/, (_, id) => language === "zh_cn" ? `错误：商店 ID 不合法：${id}` : language === "ja_jp" ? `エラー：ショップ ID が不正です：${id}` : `Error: invalid shop ID: ${id}`)
    .replace(/^错误：商店 (.+) 有商品缺少 ID。$/, (_, id) => language === "zh_cn" ? `错误：商店 ${id} 有商品缺少 ID。` : language === "ja_jp" ? `エラー：ショップ ${id} に ID のない商品があります。` : `Error: shop ${id} has an entry without an ID.`)
    .replace(/^警告：商品 (.+) 没有花费。$/, (_, id) => language === "zh_cn" ? `警告：商品 ${id} 没有花费。` : language === "ja_jp" ? `警告：商品 ${id} にコストがありません。` : `Warning: entry ${id} has no costs.`)
    .replace(/^警告：商品 (.+) 没有获得物品。$/, (_, id) => language === "zh_cn" ? `警告：商品 ${id} 没有获得物品。` : language === "ja_jp" ? `警告：商品 ${id} に入手アイテムがありません。` : `Warning: entry ${id} has no rewards.`)
    .replace(/^错误：钱包 (.+) 没有绑定物品。$/, (_, id) => language === "zh_cn" ? `错误：钱包 ${id} 没有绑定物品。` : language === "ja_jp" ? `エラー：ウォレット ${id} にバインドされたアイテムがありません。` : `Error: wallet ${id} has no bound items.`)
    .replace(/^错误：(.+)$/, (_, message) => language === "zh_cn" ? `错误：${message}` : language === "ja_jp" ? `エラー：${message}` : `Error: ${message}`)
    .replace(/^警告：(.+)$/, (_, message) => language === "zh_cn" ? `警告：${message}` : language === "ja_jp" ? `警告：${message}` : `Warning: ${message}`);
}

function localizePage(root = document.body) {
  document.documentElement.lang = LANGUAGE_META[state.language]?.htmlLang || "en";
  document.title = tr("Chronicle Engine Pack Maker");
  const languageSelect = $("#languageSelect");
  if (languageSelect) languageSelect.value = state.language;
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      const parent = node.parentElement;
      if (!parent) return NodeFilter.FILTER_REJECT;
      if (parent.closest("script, style, textarea, input, code, #languageSelect")) return NodeFilter.FILTER_REJECT;
      return node.nodeValue.trim() ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
    }
  });
  const nodes = [];
  while (walker.nextNode()) nodes.push(walker.currentNode);
  for (const node of nodes) node.nodeValue = translateText(node.nodeValue);
}

function init() {
  wireStaticEvents();
  loadExample();
  renderAll();
}

function wireStaticEvents() {
  $("#languageSelect").value = state.language;
  $("#languageSelect").addEventListener("change", (event) => {
    state.language = event.target.value;
    localStorage.setItem("chronicleMakerLanguage", state.language);
    renderAll();
  });

  $$(".nav-item").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });

  $("#pickProjectBtn").addEventListener("click", pickProject);
  $("#importConfigBtn").addEventListener("click", importConfigPack);
  $("#loadExampleBtn").addEventListener("click", () => {
    loadExample();
    renderAll();
    showMessages(["已载入内置示例。"]);
  });
  $("#saveAllBtn").addEventListener("click", saveAll);
  $("#saveAllBtn2").addEventListener("click", saveAll);
  $("#validateBtn").addEventListener("click", () => showMessages(validateData()));
  $("#downloadIndexBtn").addEventListener("click", downloadFileIndex);

  $("#namespaceInput").addEventListener("input", (event) => {
    state.settings.namespace = normalizeNamespace(event.target.value);
    event.target.value = state.settings.namespace;
    renderPreview();
  });
  $("#packNameInput").addEventListener("input", (event) => {
    state.settings.packName = event.target.value;
    renderPreview();
  });
  $("#exportModeInput").addEventListener("change", (event) => {
    state.settings.exportMode = event.target.value;
    renderPreview();
  });
  $("#defaultCategoryInput").addEventListener("input", (event) => {
    state.settings.defaultCategory = event.target.value.trim() || "main";
  });

  $("#addQuestBtn").addEventListener("click", () => {
    const quest = makeQuest(uniqueLocalId("new_quest", state.data.quests.map((quest) => fileNameFromId(quest.id))));
    state.data.quests.push(quest);
    selectRecord("quests", quest.id);
  });
  $$("[data-quest-template]").forEach((button) => {
    button.addEventListener("click", () => {
      const quest = makeQuestFromTemplate(button.dataset.questTemplate);
      state.data.quests.push(quest);
      selectRecord("quests", quest.id);
    });
  });
  $("#addDialogueBtn").addEventListener("click", () => {
    const dialogue = makeDialogue(uniqueLocalId("new_dialogue", state.data.dialogues.map((dialogue) => fileNameFromId(dialogue.id))));
    state.data.dialogues.push(dialogue);
    selectRecord("dialogues", dialogue.id);
  });
  $("#addNpcBtn").addEventListener("click", () => {
    const npc = makeNpc(uniqueLocalId("villager_npc", state.data.npcs.map((npc) => npc.localId)));
    state.data.npcs.push(npc);
    selectRecord("npcs", npc.localId);
  });
  $("#addShopBtn").addEventListener("click", () => {
    const shop = makeShop(uniqueLocalId("new_shop", state.data.shops.map((shop) => fileNameFromId(shop.shopId))));
    state.data.shops.push(shop);
    selectRecord("shops", shop.shopId);
  });
  $("#addWalletBtn").addEventListener("click", () => {
    const wallet = makeWallet("minecraft:emerald");
    state.data.wallets.push(wallet);
    selectRecord("wallets", wallet.currencyId);
  });

  $("#previewSelect").addEventListener("change", renderPreview);
}

function switchView(view) {
  state.activeView = view;
  $$(".nav-item").forEach((button) => button.classList.toggle("active", button.dataset.view === view));
  $$(".view").forEach((panel) => panel.classList.toggle("active", panel.dataset.viewPanel === view));
  $("#viewTitle").textContent = viewCopy[view][0];
  $("#viewSubtitle").textContent = viewCopy[view][1];
  if (view === "export") {
    renderPreviewList();
    renderPreview();
  }
  localizePage();
}

function renderAll() {
  $("#namespaceInput").value = state.settings.namespace;
  $("#packNameInput").value = state.settings.packName;
  $("#exportModeInput").value = state.settings.exportMode;
  $("#defaultCategoryInput").value = state.settings.defaultCategory;
  $("#viewTitle").textContent = viewCopy[state.activeView][0];
  $("#viewSubtitle").textContent = viewCopy[state.activeView][1];
  renderProjectStatus();
  renderHints();
  renderLists();
  renderEditors();
  renderPreviewList();
  renderPreview();
  localizePage();
}

function renderProjectStatus() {
  $("#projectDot").classList.toggle("ready", Boolean(state.projectHandle));
  $("#projectStatus").textContent = state.projectHandle ? state.projectName : "No project selected";
  $("#projectHint").textContent = state.projectHandle
    ? "Can read existing config and write changes back to this project."
    : "You can use the example first, or select a modpack root folder.";
}

function renderHints() {
  $("#modChips").innerHTML = state.hints.mods.length
    ? state.hints.mods.map((mod) => `<span class="chip">${escapeHtml(mod)}</span>`).join("")
    : `<span class="chip">No mods scanned yet</span>`;
  $("#questCount").textContent = state.data.quests.length;
  $("#dialogueCount").textContent = state.data.dialogues.length;
  $("#shopCount").textContent = state.data.shops.length;
  $("#itemHintCount").textContent = state.hints.items.length;
  fillDatalist("itemSuggestions", state.hints.items);
  fillDatalist("entitySuggestions", state.hints.entities);
  fillDatalist("questSuggestions", state.hints.quests);
  fillDatalist("dialogueSuggestions", state.hints.dialogues);
  fillDatalist("shopSuggestions", state.hints.shops);
  fillDatalist("flagSuggestions", state.hints.flags);
}

function fillDatalist(id, values) {
  const list = $(`#${id}`);
  list.innerHTML = unique(values)
    .sort((a, b) => a.localeCompare(b))
    .slice(0, 600)
    .map((value) => `<option value="${escapeHtml(value)}"></option>`)
    .join("");
}

function renderLists() {
  renderList("quests", "#questList", state.data.quests, (quest) => ({
    id: quest.id,
    title: quest.displayName || quest.id,
    meta: `${quest.mode} · ${quest.phases.length} 阶段`
  }));
  renderList("dialogues", "#dialogueList", state.data.dialogues, (dialogue) => ({
    id: dialogue.id,
    title: dialogue.defaultNpc || dialogue.id,
    meta: `${dialogue.nodes.length} 节点`
  }));
  renderList("npcs", "#npcList", state.data.npcs, (npc) => ({
    id: npc.localId,
    title: npc.entityType || npc.localId,
    meta: `${npc.bindings.length} 绑定`
  }));
  renderList("shops", "#shopList", state.data.shops, (shop) => ({
    id: shop.shopId,
    title: shop.displayName || shop.shopId,
    meta: `${shop.entries.length} 商品`
  }));
  renderList("wallets", "#walletList", state.data.wallets, (wallet) => ({
    id: wallet.currencyId,
    title: wallet.displayName || wallet.currencyId,
    meta: wallet.itemIds.join(", ")
  }));
}

function renderList(kind, selector, rows, describe) {
  const container = $(selector);
  if (!rows.length) {
    container.innerHTML = `<div class="empty-state"><p>暂无内容。</p></div>`;
    return;
  }
  container.innerHTML = rows.map((row) => {
    const item = describe(row);
    const active = state.selected[kind] === item.id ? "active" : "";
    return `
      <div class="list-item ${active}" role="button" tabindex="0" data-select-kind="${kind}" data-select-id="${escapeHtml(item.id)}">
        <span><strong>${escapeHtml(item.title)}</strong><span>${escapeHtml(item.meta)}</span></span>
        <button class="delete-button" data-delete-kind="${kind}" data-delete-id="${escapeHtml(item.id)}">×</button>
      </div>
    `;
  }).join("");
  $$("[data-select-kind]", container).forEach((button) => {
    button.addEventListener("click", (event) => {
      if (event.target.closest("[data-delete-kind]")) return;
      selectRecord(button.dataset.selectKind, button.dataset.selectId);
    });
  });
  $$("[data-delete-kind]", container).forEach((button) => {
    button.addEventListener("click", (event) => {
      event.stopPropagation();
      deleteRecord(button.dataset.deleteKind, button.dataset.deleteId);
    });
  });
}

function renderEditors() {
  renderQuestEditor();
  renderDialogueEditor();
  renderNpcEditor();
  renderShopEditor();
  renderWalletEditor();
}

function renderQuestEditor() {
  const quest = findById(state.data.quests, "id", state.selected.quests);
  const root = $("#questEditor");
  if (!quest) return renderEmpty(root);
  const initialPhaseOptions = phaseTargetOptions(quest, quest.initialPhaseId, false);
  root.innerHTML = `
    ${sectionTitle("任务", quest.id)}
    <div class="form-grid two">
      ${field("任务 ID", "id", quest.id, "questSuggestions")}
      ${field("显示名称", "displayName", quest.displayName)}
      ${field("分类", "category", quest.category)}
      ${field("排序", "sortOrder", quest.sortOrder, "", "number")}
      ${selectField("模式", "mode", quest.mode, [["PROGRESSION", "阶段推进"], ["SIMPLE", "简单任务"]])}
      ${selectField("初始阶段", "initialPhaseId", quest.initialPhaseId, initialPhaseOptions)}
      ${checkField("可重复", "repeatable", quest.repeatable)}
      ${checkField("团队同步", "teamSync", quest.teamSync)}
    </div>
    ${textareaField("任务描述", "description", quest.description)}
    ${renderQuestRouteMap(quest)}
    ${foldBlock("旗标设置", `${(quest.flagsToSetOnAccept || []).length} 个接取旗标 · ${(quest.flagsToSetOnComplete || []).length} 个完成旗标`, `
      ${stringListField("接取时设置旗标", "flagsToSetOnAccept", quest.flagsToSetOnAccept)}
      ${stringListField("完成时设置旗标", "flagsToSetOnComplete", quest.flagsToSetOnComplete)}
    `, { className: "compact-fold", open: false })}
    ${renderConditionSection("解锁条件", quest.unlockConditions, "quest.unlockConditions")}
    ${renderRewardSection("完成奖励", quest.completionRewards, "quest.completionRewards")}
    <div class="subsection">
      <div class="row-block-head">
        <h4>任务阶段</h4>
        <button class="small-button" data-action="quest:addPhase">新增阶段</button>
      </div>
      <div class="row-list">
        ${quest.phases.map((phase, phaseIndex) => renderPhaseBlock(quest, phase, phaseIndex)).join("")}
      </div>
    </div>
  `;
  bindEditor(root, quest, "quests", "id");
}

function renderQuestRouteMap(quest) {
  const phases = quest.phases || [];
  const phaseIds = new Set(phases.map((phase) => phase.phaseId).filter(Boolean));
  if (!phases.length) {
    return `
      <div class="route-panel">
        <div class="route-head">
          <h4>任务路线图</h4>
          <span>还没有阶段，先新增一个阶段。</span>
        </div>
      </div>
    `;
  }
  const nodes = phases.map((phase, index) => {
    const title = phase.displayName || phase.phaseId || `阶段 ${index + 1}`;
    const targets = (phase.transitions || []).map((transition) => transition.targetPhaseId).filter(Boolean);
    const targetText = targets.length ? targets.join(" / ") : "完成任务";
    const classes = ["route-node", phase.phaseId === quest.initialPhaseId ? "initial" : ""].filter(Boolean).join(" ");
    return `
      <button class="${classes}" type="button" data-focus-phase="${index}">
        <span>${escapeHtml(title)}</span>
        <small>${escapeHtml(phase.phaseId || "未设置阶段 ID")}</small>
        <em>${escapeHtml((phase.objectives || []).length)} 个目标 · 下一步：${escapeHtml(targetText)}</em>
      </button>
    `;
  }).join("");
  const edges = phases.flatMap((phase) => {
    const from = phase.displayName || phase.phaseId || "未命名阶段";
    const transitions = phase.transitions || [];
    if (!transitions.length) return [{ from, to: "完成任务", missing: false }];
    return transitions.map((transition) => ({
      from,
      to: transition.targetPhaseId || "完成任务",
      missing: Boolean(transition.targetPhaseId && !phaseIds.has(transition.targetPhaseId))
    }));
  });
  return `
    <div class="route-panel">
      <div class="route-head">
        <h4>任务路线图</h4>
        <span>点击阶段卡片会跳到对应编辑块。分支走向在“完成后跳转”里选择。</span>
      </div>
      <div class="route-board">${nodes}</div>
      <div class="route-edges">
        ${edges.map((edge) => `<span class="route-chip ${edge.missing ? "missing" : ""}">${escapeHtml(edge.from)} → ${escapeHtml(edge.to)}</span>`).join("")}
      </div>
    </div>
  `;
}

function renderPhaseBlock(quest, phase, phaseIndex) {
  const content = `
      <div class="form-grid two">
        ${field("阶段 ID", `phases.${phaseIndex}.phaseId`, phase.phaseId)}
        ${field("阶段名称", `phases.${phaseIndex}.displayName`, phase.displayName)}
      </div>
      ${textareaField("阶段描述", `phases.${phaseIndex}.description`, phase.description)}
      ${textareaField("阶段剧情", `phases.${phaseIndex}.story`, phase.story)}
      ${foldBlock("阶段旗标", `${(phase.flagsToSetOnEnter || []).length} 个进入旗标 · ${(phase.flagsToSetOnComplete || []).length} 个完成旗标`, `
        ${stringListField("进入阶段设置旗标", `phases.${phaseIndex}.flagsToSetOnEnter`, phase.flagsToSetOnEnter)}
        ${stringListField("完成阶段设置旗标", `phases.${phaseIndex}.flagsToSetOnComplete`, phase.flagsToSetOnComplete)}
      `, { className: "compact-fold", open: false })}
      <div class="subsection">
        <div class="row-block-head">
          <h4>目标</h4>
          <button class="small-button" data-action="quest:addObjective" data-phase="${phaseIndex}">新增目标</button>
        </div>
        <div class="row-list">
          ${phase.objectives.map((objective, objectiveIndex) => renderObjectiveBlock(objective, phaseIndex, objectiveIndex)).join("")}
        </div>
      </div>
      ${renderRewardSection("阶段奖励", phase.phaseRewards, `quest.phases.${phaseIndex}.phaseRewards`)}
      <div class="subsection">
        <div class="row-block-head">
          <h4>完成后跳转</h4>
          <button class="small-button" data-action="quest:addTransition" data-phase="${phaseIndex}">新增跳转</button>
        </div>
        <div class="row-list">
          ${phase.transitions.map((transition, transitionIndex) => renderTransitionBlock(quest, transition, phaseIndex, transitionIndex)).join("")}
        </div>
      </div>
  `;
  const meta = `${(phase.objectives || []).length} 个目标 · ${(phase.transitions || []).length || 0} 个跳转`;
  return foldBlock(phase.displayName || phase.phaseId || `阶段 ${phaseIndex + 1}`, meta, content, {
    className: "phase-fold",
    open: phaseIndex === 0 || phase.phaseId === quest.initialPhaseId,
    attrs: `data-phase-block="${phaseIndex}"`,
    tools: `<button class="delete-button" data-action="quest:removePhase" data-phase="${phaseIndex}">×</button>`
  });
}

function renderObjectiveBlock(objective, phaseIndex, objectiveIndex) {
  const base = `phases.${phaseIndex}.objectives.${objectiveIndex}`;
  const content = `
      <div class="form-grid two">
        ${selectField("目标类型", `${base}.type`, objective.type, objectiveTypeOptions())}
        ${field("目标 ID", `${base}.targetId`, objective.targetId, objective.type === "COLLECT" || objective.type === "OFFER" ? "itemSuggestions" : "entitySuggestions")}
        ${field("需要数量", `${base}.requiredCount`, objective.requiredCount, "", "number")}
        ${field("显示文本", `${base}.displayText`, objective.displayText)}
        ${checkField("隐藏目标", `${base}.hidden`, objective.hidden)}
        ${checkField("可选目标", `${base}.optional`, objective.optional)}
      </div>
      <div class="form-grid two">
        ${field("标记维度", `${base}.extraData.dimension`, objective.extraData.dimension || "")}
        ${field("标记 X", `${base}.extraData.x`, objective.extraData.x ?? "", "", "number")}
        ${field("标记 Y", `${base}.extraData.y`, objective.extraData.y ?? "", "", "number")}
        ${field("标记 Z", `${base}.extraData.z`, objective.extraData.z ?? "", "", "number")}
      </div>
  `;
  return foldBlock(objective.displayText || objective.type || "目标", `${objective.requiredCount ?? 1} 次`, content, {
    className: "compact-fold",
    open: false,
    tools: `<button class="delete-button" data-action="quest:removeObjective" data-phase="${phaseIndex}" data-index="${objectiveIndex}">×</button>`
  });
}

function renderTransitionBlock(quest, transition, phaseIndex, transitionIndex) {
  const content = `
      ${selectField("目标阶段", `phases.${phaseIndex}.transitions.${transitionIndex}.targetPhaseId`, transition.targetPhaseId, phaseTargetOptions(quest, transition.targetPhaseId, true))}
      ${renderSingleCondition("条件", transition.condition, `quest.phases.${phaseIndex}.transitions.${transitionIndex}.condition`)}
  `;
  return foldBlock(`跳转到 ${transition.targetPhaseId || "完成任务"}`, "满足条件后进入下一步", content, {
    className: "compact-fold",
    open: false,
    tools: `<button class="delete-button" data-action="quest:removeTransition" data-phase="${phaseIndex}" data-index="${transitionIndex}">×</button>`
  });
}

function renderDialogueEditor() {
  const dialogue = findById(state.data.dialogues, "id", state.selected.dialogues);
  const root = $("#dialogueEditor");
  if (!dialogue) return renderEmpty(root);
  root.innerHTML = `
    ${sectionTitle("对话", dialogue.id)}
    <div class="form-grid two">
      ${field("对话 ID", "id", dialogue.id, "dialogueSuggestions")}
      ${field("NPC 默认显示名", "defaultNpc", dialogue.defaultNpc)}
      ${field("起始节点 ID", "startNodeId", dialogue.startNodeId)}
    </div>
    <div class="subsection">
      <div class="row-block-head">
        <h4>节点</h4>
        <button class="small-button" data-action="dialogue:addNode">新增节点</button>
      </div>
      <div class="row-list">
        ${dialogue.nodes.map((node, nodeIndex) => renderDialogueNode(node, nodeIndex)).join("")}
      </div>
    </div>
  `;
  bindEditor(root, dialogue, "dialogues", "id");
}

function renderDialogueNode(node, nodeIndex) {
  const content = `
      <div class="form-grid two">
        ${field("节点 ID", `nodes.${nodeIndex}.nodeId`, node.nodeId)}
      </div>
      ${textareaField("台词", `nodes.${nodeIndex}.text`, node.text)}
      <div class="subsection">
        <div class="row-block-head">
          <h4>选项</h4>
          <button class="small-button" data-action="dialogue:addChoice" data-node="${nodeIndex}">新增选项</button>
        </div>
        <div class="row-list">
          ${node.choices.map((choice, choiceIndex) => renderDialogueChoice(choice, nodeIndex, choiceIndex)).join("")}
        </div>
      </div>
  `;
  return foldBlock(node.nodeId || `节点 ${nodeIndex + 1}`, `${(node.choices || []).length} 个选项`, content, {
    className: "dialogue-fold",
    open: nodeIndex === 0,
    tools: `<button class="delete-button" data-action="dialogue:removeNode" data-node="${nodeIndex}">×</button>`
  });
}

function renderDialogueChoice(choice, nodeIndex, choiceIndex) {
  const base = `nodes.${nodeIndex}.choices.${choiceIndex}`;
  const content = `
      <div class="form-grid two">
        ${field("选项 ID", `${base}.choiceId`, choice.choiceId)}
        ${field("按钮文字", `${base}.text`, choice.text)}
        ${field("跳到节点 ID", `${base}.nextNodeId`, choice.nextNodeId)}
      </div>
      ${renderConditionSection("显示条件", choice.conditions, `dialogue.nodes.${nodeIndex}.choices.${choiceIndex}.conditions`)}
      ${renderActionSection("触发动作", choice.actions, `dialogue.nodes.${nodeIndex}.choices.${choiceIndex}.actions`, nodeIndex, choiceIndex)}
  `;
  return foldBlock(choice.text || choice.choiceId || `选项 ${choiceIndex + 1}`, choice.nextNodeId ? `跳到 ${choice.nextNodeId}` : "关闭或执行动作", content, {
    className: "compact-fold",
    open: false,
    tools: `<button class="delete-button" data-action="dialogue:removeChoice" data-node="${nodeIndex}" data-index="${choiceIndex}">×</button>`
  });
}

function renderNpcEditor() {
  const npc = findById(state.data.npcs, "localId", state.selected.npcs);
  const root = $("#npcEditor");
  if (!npc) return renderEmpty(root);
  root.innerHTML = `
    ${sectionTitle("NPC 绑定", npc.localId)}
    <div class="form-grid two">
      ${field("文件名", "localId", npc.localId)}
      ${field("实体类型", "entityType", npc.entityType, "entitySuggestions")}
      ${field("对话距离", "dialogueDistance", npc.dialogueDistance, "", "number")}
      ${checkField("取消原版交互", "cancelVanillaInteract", npc.cancelVanillaInteract)}
      ${checkField("看向玩家", "shouldLookAtPlayer", npc.shouldLookAtPlayer)}
      ${checkField("停止移动", "shouldStopMoving", npc.shouldStopMoving)}
    </div>
    <div class="subsection">
      <div class="row-block-head">
        <h4>绑定规则</h4>
        <button class="small-button" data-action="npc:addBinding">新增绑定</button>
      </div>
      <div class="row-list">
        ${npc.bindings.map((binding, index) => renderNpcBinding(binding, index)).join("")}
      </div>
    </div>
  `;
  bindEditor(root, npc, "npcs", "localId");
}

function renderNpcBinding(binding, index) {
  const content = `
      <div class="form-grid two">
        ${field("绑定 ID", `bindings.${index}.bindingId`, binding.bindingId)}
        ${field("对话 ID", `bindings.${index}.dialogueId`, binding.dialogueId, "dialogueSuggestions")}
        ${field("NBT 对话字段", `bindings.${index}.dialogueIdFromNbt`, binding.dialogueIdFromNbt)}
        ${field("优先级", `bindings.${index}.priority`, binding.priority, "", "number")}
      </div>
      ${renderSingleCondition("匹配条件", binding.condition, `npc.bindings.${index}.condition`, true)}
  `;
  return foldBlock(binding.bindingId || `绑定 ${index + 1}`, binding.dialogueId || "未设置对话", content, {
    className: "compact-fold",
    open: index === 0,
    tools: `<button class="delete-button" data-action="npc:removeBinding" data-index="${index}">×</button>`
  });
}

function renderShopEditor() {
  const shop = findById(state.data.shops, "shopId", state.selected.shops);
  const root = $("#shopEditor");
  if (!shop) return renderEmpty(root);
  root.innerHTML = `
    ${sectionTitle("商店", shop.shopId)}
    <div class="form-grid two">
      ${field("商店 ID", "shopId", shop.shopId, "shopSuggestions")}
      ${field("显示名称", "displayName", shop.displayName)}
    </div>
    ${textareaField("描述", "description", shop.description)}
    ${renderSingleCondition("打开条件", shop.openCondition, "shop.openCondition")}
    <div class="subsection">
      <div class="row-block-head">
        <h4>分类</h4>
        <button class="small-button" data-action="shop:addCategory">新增分类</button>
      </div>
      <div class="row-list">
        ${shop.categories.map((category, index) => renderShopCategory(category, index)).join("")}
      </div>
    </div>
    <div class="subsection">
      <div class="row-block-head">
        <h4>商品</h4>
        <button class="small-button" data-action="shop:addEntry">新增商品</button>
      </div>
      <div class="row-list">
        ${shop.entries.map((entry, index) => renderShopEntry(entry, index)).join("")}
      </div>
    </div>
  `;
  bindEditor(root, shop, "shops", "shopId");
}

function renderShopCategory(category, index) {
  const content = `
      <div class="form-grid two">
        ${field("分类 ID", `categories.${index}.categoryId`, category.categoryId)}
        ${field("显示名称", `categories.${index}.displayName`, category.displayName)}
        ${field("排序", `categories.${index}.sortOrder`, category.sortOrder, "", "number")}
        ${selectField("颜色", `categories.${index}.formatting`, category.formatting, colorOptions())}
      </div>
  `;
  return foldBlock(category.displayName || category.categoryId || `分类 ${index + 1}`, category.categoryId || "未设置 ID", content, {
    className: "compact-fold",
    open: false,
    tools: `<button class="delete-button" data-action="shop:removeCategory" data-index="${index}">×</button>`
  });
}

function renderShopEntry(entry, index) {
  const content = `
      <div class="form-grid two">
        ${field("商品 ID", `entries.${index}.entryId`, entry.entryId)}
        ${field("显示名称", `entries.${index}.displayName`, entry.displayName)}
        ${field("分类 ID", `entries.${index}.category`, entry.category)}
        ${field("排序", `entries.${index}.sortOrder`, entry.sortOrder, "", "number")}
      </div>
      ${renderSingleCondition("可见条件", entry.visibleCondition, `shop.entries.${index}.visibleCondition`)}
      ${renderRewardSection("花费", entry.costs, `shop.entries.${index}.costs`, true)}
      ${renderRewardSection("获得", entry.rewards, `shop.entries.${index}.rewards`)}
  `;
  return foldBlock(entry.displayName || entry.entryId || `商品 ${index + 1}`, entry.category || "未分类", content, {
    className: "shop-entry-fold",
    open: false,
    tools: `<button class="delete-button" data-action="shop:removeEntry" data-index="${index}">×</button>`
  });
}

function renderWalletEditor() {
  const wallet = findById(state.data.wallets, "currencyId", state.selected.wallets);
  const root = $("#walletEditor");
  if (!wallet) return renderEmpty(root);
  root.innerHTML = `
    ${sectionTitle("钱包货币", wallet.currencyId)}
    <div class="form-grid two">
      ${field("货币 ID", "currencyId", wallet.currencyId)}
      ${field("显示名称", "displayName", wallet.displayName)}
      ${field("主物品 ID", "itemId", wallet.itemId, "itemSuggestions")}
      ${field("排序", "sortOrder", wallet.sortOrder, "", "number")}
    </div>
    ${stringListField("可存入的钱币物品 ID", "itemIds", wallet.itemIds, "itemSuggestions")}
  `;
  bindEditor(root, wallet, "wallets", "currencyId");
}

function renderConditionSection(title, conditions, path) {
  return `
    <div class="subsection">
      <div class="row-block-head">
        <h4>${escapeHtml(title)}</h4>
        <button class="small-button" data-action="condition:add" data-path="${escapeHtml(path)}">新增条件</button>
      </div>
      <div class="row-list">
        ${conditions.map((condition, index) => renderConditionBlock(condition, `${path}.${index}`, index)).join("")}
      </div>
    </div>
  `;
}

function renderSingleCondition(title, condition, path, allowVillager = false) {
  return `
    <div class="subsection">
      <h4>${escapeHtml(title)}</h4>
      ${renderConditionBlock(condition || { condition: "chronicle_engine:always" }, path, -1, allowVillager)}
    </div>
  `;
}

function renderConditionBlock(condition, path, index, allowVillager = false) {
  const type = condition.condition || "chronicle_engine:always";
  const selector = `
    <select data-condition-path="${escapeHtml(path)}">
      ${conditionOptions(allowVillager).map(([value, label]) => `<option value="${value}" ${type === value ? "selected" : ""}>${label}</option>`).join("")}
    </select>
  `;
  const remove = index >= 0 ? `<button class="delete-button" data-action="condition:remove" data-path="${escapeHtml(path)}">×</button>` : "";
  const content = `
      <label>条件类型${selector}</label>
      <div class="form-grid two">
        ${conditionFields(condition, path, allowVillager)}
      </div>
  `;
  return foldBlock("条件", conditionLabel(type, allowVillager), content, {
    className: "compact-fold",
    open: index < 0,
    tools: remove
  });
}

function conditionFields(condition, path) {
  const type = condition.condition || "chronicle_engine:always";
  if (type === "chronicle_engine:has_flag") {
    return field("旗标", `${path}.flag`, condition.flag || "", "flagSuggestions");
  }
  if (type === "chronicle_engine:quest_accepted" || type === "chronicle_engine:quest_not_started") {
    return field("任务 ID", `${path}.questId`, condition.questId || "", "questSuggestions");
  }
  if (type === "chronicle_engine:quest_phase") {
    return field("任务 ID", `${path}.questId`, condition.questId || "", "questSuggestions")
      + field("阶段 ID", `${path}.phaseId`, condition.phaseId || "");
  }
  if (type === "chronicle_engine:entity_name") {
    return field("名称包含", `${path}.namePattern`, condition.namePattern || "");
  }
  if (type === "chronicle_engine:villager_profession") {
    return selectField("村民职业", `${path}.profession`, condition.profession || "farmer", VILLAGER_PROFESSIONS.map((value) => [value, value]));
  }
  return `<p class="note">始终满足，不需要额外参数。</p>`;
}

function renderRewardSection(title, rewards, path, costMode = false) {
  return `
    <div class="subsection">
      <div class="row-block-head">
        <h4>${escapeHtml(title)}</h4>
        <button class="small-button" data-action="reward:add" data-path="${escapeHtml(path)}">新增${costMode ? "花费" : "奖励"}</button>
      </div>
      <div class="row-list">
        ${rewards.map((reward, index) => renderRewardBlock(reward, `${path}.${index}`, index)).join("")}
      </div>
    </div>
  `;
}

function renderRewardBlock(reward, path) {
  const type = reward.type || "item";
  const content = `
      <label>类型
        <select data-reward-path="${escapeHtml(path)}">
          ${rewardOptions().map(([value, label]) => `<option value="${value}" ${type === value ? "selected" : ""}>${label}</option>`).join("")}
        </select>
      </label>
      <div class="form-grid two">
        ${rewardFields(reward, path)}
      </div>
  `;
  return foldBlock(rewardLabel(type), rewardSummary(reward), content, {
    className: "compact-fold",
    open: false,
    tools: `<button class="delete-button" data-action="reward:remove" data-path="${escapeHtml(path)}">×</button>`
  });
}

function rewardFields(reward, path) {
  const type = reward.type || "item";
  if (type === "item" || type === "give_item") {
    return field("物品 ID", `${path}.itemId`, reward.itemId || "", "itemSuggestions")
      + field("数量", `${path}.count`, reward.count ?? 1, "", "number")
      + field("NBT", `${path}.nbt`, reward.nbt || "");
  }
  if (type === "enchanted_book") {
    return field("附魔 ID", `${path}.enchantmentId`, reward.enchantmentId || "")
      + field("等级", `${path}.level`, reward.level ?? 1, "", "number");
  }
  if (type === "potion") {
    return field("药水物品", `${path}.itemId`, reward.itemId || "minecraft:potion", "itemSuggestions")
      + field("药水 ID", `${path}.potionId`, reward.potionId || "minecraft:water")
      + field("数量", `${path}.count`, reward.count ?? 1, "", "number");
  }
  if (type === "command" || type === "run_command") {
    return field("命令", `${path}.command`, reward.command || "say hello {player}");
  }
  return `<p class="note">随机奖励可在导出后进阶调整权重。零基础模式建议先使用固定奖励。</p>`;
}

function renderActionSection(title, actions, path) {
  return `
    <div class="subsection">
      <div class="row-block-head">
        <h4>${escapeHtml(title)}</h4>
        <button class="small-button" data-action="action:add" data-path="${escapeHtml(path)}">新增动作</button>
      </div>
      <div class="row-list">
        ${actions.map((action, index) => renderActionBlock(action, `${path}.${index}`)).join("")}
      </div>
    </div>
  `;
}

function renderActionBlock(action, path) {
  const type = action.type || "close";
  const content = `
      <label>动作类型
        <select data-action-path="${escapeHtml(path)}">
          ${actionOptions().map(([value, label]) => `<option value="${value}" ${type === value ? "selected" : ""}>${label}</option>`).join("")}
        </select>
      </label>
      <div class="form-grid two">
        ${actionFields(action, path)}
      </div>
  `;
  return foldBlock(actionLabel(type), actionSummary(action), content, {
    className: "compact-fold",
    open: false,
    tools: `<button class="delete-button" data-action="action:remove" data-path="${escapeHtml(path)}">×</button>`
  });
}

function actionFields(action, path) {
  const type = action.type || "close";
  if (type === "start_quest") {
    return field("任务 ID", `${path}.questId`, action.questId || "", "questSuggestions");
  }
  if (type === "notify_interact") {
    return field("交互目标 ID", `${path}.targetId`, action.targetId || "");
  }
  if (type === "set_flag") {
    return field("旗标", `${path}.flag`, action.flag || action.flagName || "", "flagSuggestions");
  }
  if (type === "open_shop" || type === "open_trade") {
    return field("商店 ID", `${path}.shopId`, action.shopId || "", "shopSuggestions");
  }
  if (type === "item" || type === "give_item") {
    return rewardFields(action, path);
  }
  if (type === "command" || type === "run_command") {
    return field("命令", `${path}.command`, action.command || "say hello {player}");
  }
  return `<p class="note">关闭当前对话，不需要额外参数。</p>`;
}

function bindEditor(root, record, collection, idKey) {
  $$("[data-bind]", root).forEach((input) => {
    input.addEventListener("input", () => {
      const value = readInputValue(input);
      setEditorPath(record, input.dataset.bind, value);
      if (input.dataset.bind === idKey) {
        state.selected[collection] = value;
      }
      refreshDerivedHints();
      renderLists();
      renderPreviewList();
      renderPreview();
    });
    input.addEventListener("change", () => {
      const value = readInputValue(input);
      setEditorPath(record, input.dataset.bind, value);
      if (input.dataset.bind === idKey) {
        state.selected[collection] = value;
      }
      renderAll();
    });
  });

  $$("[data-string-list]", root).forEach((input) => {
    input.addEventListener("input", () => {
      setPath(record, input.dataset.stringList, splitLines(input.value));
      refreshDerivedHints();
      renderPreview();
    });
  });

  $$("[data-condition-path]", root).forEach((select) => {
    select.addEventListener("change", () => {
      setAnyPath(select.dataset.conditionPath, defaultCondition(select.value));
      renderAll();
    });
  });
  $$("[data-reward-path]", root).forEach((select) => {
    select.addEventListener("change", () => {
      setAnyPath(select.dataset.rewardPath, defaultReward(select.value));
      renderAll();
    });
  });
  $$("[data-action-path]", root).forEach((select) => {
    select.addEventListener("change", () => {
      setAnyPath(select.dataset.actionPath, defaultAction(select.value));
      renderAll();
    });
  });

  $$("[data-action]", root).forEach((button) => {
    button.addEventListener("click", (event) => {
      event.preventDefault();
      event.stopPropagation();
      handleEditorAction(button.dataset.action, button.dataset);
    });
  });

  $$("[data-focus-phase]", root).forEach((button) => {
    button.addEventListener("click", () => {
      const target = root.querySelector(`[data-phase-block="${button.dataset.focusPhase}"]`);
      if (!target) return;
      target.open = true;
      target.scrollIntoView({ behavior: "smooth", block: "start" });
      target.classList.add("attention-pulse");
      window.setTimeout(() => target.classList.remove("attention-pulse"), 900);
    });
  });
}

function setEditorPath(record, path, value) {
  if (/^(quest|dialogue|npc|shop)\./.test(path)) {
    setAnyPath(path, value);
  } else {
    setPath(record, path, value);
  }
}

function handleEditorAction(action, dataset) {
  const quest = findById(state.data.quests, "id", state.selected.quests);
  const dialogue = findById(state.data.dialogues, "id", state.selected.dialogues);
  const npc = findById(state.data.npcs, "localId", state.selected.npcs);
  const shop = findById(state.data.shops, "shopId", state.selected.shops);
  if (action === "quest:addPhase" && quest) {
    const id = uniqueLocalId("phase", quest.phases.map((phase) => phase.phaseId));
    quest.phases.push(makePhase(id));
    quest.initialPhaseId ||= id;
  } else if (action === "quest:removePhase" && quest) {
    quest.phases.splice(Number(dataset.phase), 1);
    quest.initialPhaseId = quest.phases[0]?.phaseId || "";
  } else if (action === "quest:addObjective" && quest) {
    quest.phases[Number(dataset.phase)].objectives.push(makeObjective("KILL"));
  } else if (action === "quest:removeObjective" && quest) {
    quest.phases[Number(dataset.phase)].objectives.splice(Number(dataset.index), 1);
  } else if (action === "quest:addTransition" && quest) {
    quest.phases[Number(dataset.phase)].transitions.push({ targetPhaseId: "", condition: defaultCondition("chronicle_engine:always") });
  } else if (action === "quest:removeTransition" && quest) {
    quest.phases[Number(dataset.phase)].transitions.splice(Number(dataset.index), 1);
  } else if (action === "dialogue:addNode" && dialogue) {
    dialogue.nodes.push(makeDialogueNode(uniqueLocalId("node", dialogue.nodes.map((node) => node.nodeId))));
  } else if (action === "dialogue:removeNode" && dialogue) {
    dialogue.nodes.splice(Number(dataset.node), 1);
    dialogue.startNodeId = dialogue.nodes[0]?.nodeId || "root";
  } else if (action === "dialogue:addChoice" && dialogue) {
    dialogue.nodes[Number(dataset.node)].choices.push(makeChoice());
  } else if (action === "dialogue:removeChoice" && dialogue) {
    dialogue.nodes[Number(dataset.node)].choices.splice(Number(dataset.index), 1);
  } else if (action === "npc:addBinding" && npc) {
    npc.bindings.push(makeBinding());
  } else if (action === "npc:removeBinding" && npc) {
    npc.bindings.splice(Number(dataset.index), 1);
  } else if (action === "shop:addCategory" && shop) {
    shop.categories.push(makeCategory(uniqueLocalId("category", shop.categories.map((category) => category.categoryId))));
  } else if (action === "shop:removeCategory" && shop) {
    shop.categories.splice(Number(dataset.index), 1);
  } else if (action === "shop:addEntry" && shop) {
    shop.entries.push(makeShopEntry());
  } else if (action === "shop:removeEntry" && shop) {
    shop.entries.splice(Number(dataset.index), 1);
  } else if (action === "condition:add") {
    const target = getAnyPath(dataset.path);
    if (Array.isArray(target)) target.push(defaultCondition("chronicle_engine:always"));
  } else if (action === "condition:remove") {
    removeAnyPath(dataset.path);
  } else if (action === "reward:add") {
    const target = getAnyPath(dataset.path);
    if (Array.isArray(target)) target.push(defaultReward("item"));
  } else if (action === "reward:remove") {
    removeAnyPath(dataset.path);
  } else if (action === "action:add") {
    const target = getAnyPath(dataset.path);
    if (Array.isArray(target)) target.push(defaultAction("close"));
  } else if (action === "action:remove") {
    removeAnyPath(dataset.path);
  }
  refreshDerivedHints();
  renderAll();
}

function renderEmpty(root) {
  root.innerHTML = $("#emptyEditorTemplate").innerHTML;
}

function sectionTitle(title, id) {
  return `<div class="section-title"><h3>${escapeHtml(title)}</h3><span>${escapeHtml(id)}</span></div>`;
}

function foldBlock(title, meta, content, options = {}) {
  const className = ["fold-block", options.className || ""].filter(Boolean).join(" ");
  const openAttr = options.open === false ? "" : " open";
  const extraAttrs = options.attrs ? ` ${options.attrs}` : "";
  const metaHtml = meta ? `<span class="fold-meta">${escapeHtml(meta)}</span>` : "";
  const tools = options.tools ? `<span class="fold-tools">${options.tools}</span>` : "";
  return `
    <details class="${className}"${openAttr}${extraAttrs}>
      <summary class="fold-summary">
        <span class="fold-title">${escapeHtml(title)}</span>
        ${metaHtml}
        ${tools}
      </summary>
      <div class="fold-body">
        ${content}
      </div>
    </details>
  `;
}

function phaseTargetOptions(quest, currentValue = "", includeComplete = true) {
  const options = [];
  if (includeComplete) options.push(["", "完成任务 / 不再跳转"]);
  (quest.phases || []).forEach((phase, index) => {
    const value = phase.phaseId || "";
    if (!value) return;
    const label = `${phase.displayName || `阶段 ${index + 1}`} (${value})`;
    options.push([value, label]);
  });
  if (currentValue && !options.some(([value]) => value === currentValue)) {
    options.push([currentValue, `缺失的阶段：${currentValue}`]);
  }
  return options;
}

function field(labelText, bind, value, datalist = "", type = "text") {
  return `<label>${escapeHtml(labelText)}
    <input data-bind="${escapeHtml(bind)}" type="${type}" value="${escapeAttr(value ?? "")}" ${datalist ? `list="${datalist}"` : ""}>
  </label>`;
}

function textareaField(labelText, bind, value) {
  return `<label class="wide-field">${escapeHtml(labelText)}
    <textarea data-bind="${escapeHtml(bind)}">${escapeHtml(value ?? "")}</textarea>
  </label>`;
}

function selectField(labelText, bind, value, options) {
  return `<label>${escapeHtml(labelText)}
    <select data-bind="${escapeHtml(bind)}">
      ${options.map(([optionValue, optionLabel]) => `<option value="${escapeAttr(optionValue)}" ${value === optionValue ? "selected" : ""}>${escapeHtml(optionLabel)}</option>`).join("")}
    </select>
  </label>`;
}

function checkField(labelText, bind, value) {
  return `<label class="check-row">
    <input data-bind="${escapeHtml(bind)}" data-kind="boolean" type="checkbox" ${value ? "checked" : ""}>
    ${escapeHtml(labelText)}
  </label>`;
}

function stringListField(labelText, bind, values, datalist = "") {
  const value = Array.isArray(values) ? values.join("\n") : "";
  return `<label class="string-list-field">${escapeHtml(labelText)}
    <textarea data-string-list="${escapeHtml(bind)}" ${datalist ? `list="${datalist}"` : ""}>${escapeHtml(value)}</textarea>
    <span class="note">每行一个 ID 或旗标。</span>
  </label>`;
}

function readInputValue(input) {
  if (input.dataset.kind === "boolean") {
    return input.checked;
  }
  if (input.type === "number") {
    return input.value === "" ? "" : Number(input.value);
  }
  return input.value;
}

function selectRecord(kind, id) {
  state.selected[kind] = id;
  renderAll();
}

function deleteRecord(kind, id) {
  const idKey = kind === "npcs" ? "localId" : kind === "shops" ? "shopId" : kind === "wallets" ? "currencyId" : "id";
  state.data[kind] = state.data[kind].filter((record) => record[idKey] !== id);
  state.selected[kind] = state.data[kind][0]?.[idKey] || "";
  refreshDerivedHints();
  renderAll();
}

function makeQuest(localName) {
  const id = namespaced(localName);
  const phase = makePhase("start");
  return {
    id,
    category: state.settings.defaultCategory,
    displayName: tr("New Quest"),
    description: tr("Write the quest description here."),
    sortOrder: 0,
    repeatable: false,
    teamSync: false,
    mode: "PROGRESSION",
    initialPhaseId: phase.phaseId,
    unlockConditions: [defaultCondition("chronicle_engine:always")],
    flagsToSetOnAccept: [],
    flagsToSetOnComplete: [],
    completionRewards: [],
    phases: [phase]
  };
}

function makeQuestFromTemplate(template) {
  if (template === "collect") {
    const quest = makeQuest("collect_items");
    quest.displayName = tr("Collect Supplies");
    quest.description = tr("Collect the required items and complete the request.");
    quest.phases[0].displayName = tr("Collect Supplies");
    quest.phases[0].objectives = [makeObjective("COLLECT", "minecraft:iron_ingot", 10, tr("Collect 10 Iron Ingots"))];
    quest.completionRewards = [defaultReward("item", "minecraft:emerald", 8)];
    return quest;
  }
  if (template === "interact") {
    const quest = makeQuest("talk_to_npc");
    quest.displayName = tr("Visit NPC");
    quest.description = tr("Talk to the specified NPC to progress the quest.");
    quest.phases[0].displayName = tr("Go Talk");
    quest.phases[0].objectives = [makeObjective("INTERACT", namespaced("npc_talk"), 1, tr("Talk to the target NPC"))];
    return quest;
  }
  const quest = makeQuest("kill_zombie");
  quest.displayName = tr("Clear Zombies");
  quest.description = tr("Defeat one zombie, then claim the reward.");
  quest.teamSync = true;
  quest.phases[0].displayName = tr("Kill Zombie");
  quest.phases[0].objectives = [makeObjective("KILL", "minecraft:zombie", 1, tr("Kill 1 Zombie"))];
  quest.completionRewards = [defaultReward("item", "minecraft:iron_ingot", 10)];
  return quest;
}

function makePhase(id) {
  return {
    phaseId: id,
    displayName: tr("Task Phase"),
    description: tr("Explain what the player should do now."),
    story: "",
    flagsToSetOnEnter: [],
    flagsToSetOnComplete: [],
    objectives: [makeObjective("KILL")],
    transitions: [],
    phaseRewards: []
  };
}

function makeObjective(type = "KILL", targetId = "minecraft:zombie", requiredCount = 1, displayText = tr("Complete Objective")) {
  return {
    type,
    targetId,
    requiredCount,
    displayText,
    hidden: false,
    optional: false,
    extraData: {}
  };
}

function makeDialogue(localName) {
  const id = namespaced(localName);
  return {
    id,
    defaultNpc: tr("Villager"),
    startNodeId: "root",
    nodes: [makeDialogueNode("root")]
  };
}

function makeDialogueNode(nodeId) {
  return {
    nodeId,
    text: tr("Hello, need any help?"),
    conditionalTexts: [],
    choices: [makeChoice()]
  };
}

function makeChoice() {
  return {
    choiceId: "close",
    text: tr("Leave"),
    nextNodeId: "",
    conditions: [],
    actions: [defaultAction("close")]
  };
}

function makeNpc(localName) {
  return {
    localId: localName,
    entityType: "minecraft:villager",
    cancelVanillaInteract: true,
    dialogueDistance: 8,
    shouldLookAtPlayer: true,
    shouldStopMoving: true,
    bindings: [makeBinding()]
  };
}

function makeBinding() {
  return {
    bindingId: namespaced("farmer_dialogue"),
    dialogueId: state.data.dialogues[0]?.id || namespaced("farmer_dialogue"),
    dialogueIdFromNbt: "",
    condition: { condition: "chronicle_engine:villager_profession", profession: "farmer" },
    priority: 90
  };
}

function makeShop(localName) {
  const shopId = namespaced(localName);
  return {
    shopId,
    displayName: tr("New Shop"),
    description: tr("Sells some items."),
    openCondition: defaultCondition("chronicle_engine:always"),
    categories: [makeCategory("general")],
    entries: [makeShopEntry()]
  };
}

function makeCategory(id) {
  return { categoryId: id, displayName: tr("General"), sortOrder: 0, formatting: "WHITE" };
}

function makeShopEntry() {
  return {
    entryId: "bread",
    displayName: tr("Bread"),
    costs: [defaultReward("item", "minecraft:emerald", 1)],
    rewards: [defaultReward("item", "minecraft:bread", 3)],
    category: "general",
    visibleCondition: defaultCondition("chronicle_engine:always"),
    sortOrder: 0
  };
}

function makeWallet(itemId) {
  return {
    currencyId: itemId,
    itemId,
    itemIds: [itemId],
    displayName: itemId === "minecraft:emerald" ? tr("Emerald") : tr("Currency"),
    sortOrder: 0
  };
}

function defaultCondition(type) {
  if (type === "chronicle_engine:has_flag") return { condition: type, flag: "" };
  if (type === "chronicle_engine:quest_accepted" || type === "chronicle_engine:quest_not_started") return { condition: type, questId: state.data.quests[0]?.id || "" };
  if (type === "chronicle_engine:quest_phase") return { condition: type, questId: state.data.quests[0]?.id || "", phaseId: "" };
  if (type === "chronicle_engine:entity_name") return { condition: type, namePattern: "" };
  if (type === "chronicle_engine:villager_profession") return { condition: type, profession: "farmer" };
  return { condition: "chronicle_engine:always" };
}

function defaultReward(type, itemId = "minecraft:emerald", count = 1) {
  if (type === "enchanted_book") return { type, enchantmentId: "minecraft:sharpness", level: 1 };
  if (type === "potion") return { type, itemId: "minecraft:potion", potionId: "minecraft:water", count: 1 };
  if (type === "command" || type === "run_command") return { type, command: "say hello {player}" };
  return { type, itemId, count, nbt: "" };
}

function defaultAction(type) {
  if (type === "start_quest") return { type, questId: state.data.quests[0]?.id || "" };
  if (type === "notify_interact") return { type, targetId: namespaced("talk") };
  if (type === "set_flag") return { type, flag: "" };
  if (type === "open_shop" || type === "open_trade") return { type, shopId: state.data.shops[0]?.shopId || "" };
  if (type === "item" || type === "give_item") return defaultReward(type, "minecraft:bread", 1);
  if (type === "command" || type === "run_command") return { type, command: "say hello {player}" };
  return { type: "close" };
}

function loadExample() {
  state.settings.namespace = "simple_farmer_story";
  state.settings.packName = "Simple Farmer Story";
  state.settings.defaultCategory = "main";
  const quest = makeQuestFromTemplate("kill");
  quest.id = "simple_farmer_story:kill_zombie";
  quest.category = "main";
  quest.displayName = "Farmer's Zombie Problem";
  quest.description = "Help the farmer by killing one zombie.";
  quest.initialPhaseId = "hunt_zombie";
  quest.phases[0].phaseId = "hunt_zombie";
  quest.phases[0].displayName = "Kill a zombie";
  quest.phases[0].description = "Kill one zombie anywhere.";
  quest.flagsToSetOnComplete = ["simple_farmer_story:zombie_helped"];

  const dialogue = makeDialogue("farmer_dialogue");
  dialogue.id = "simple_farmer_story:farmer_dialogue";
  dialogue.defaultNpc = "Farmer";
  dialogue.nodes[0].text = "The fields are unsafe at night. Can you deal with a zombie for me?";
  dialogue.nodes[0].choices = [
    {
      choiceId: "accept_quest",
      text: "I will help.",
      nextNodeId: "",
      conditions: [{ condition: "chronicle_engine:quest_not_started", questId: quest.id }],
      actions: [{ type: "start_quest", questId: quest.id }, { type: "close" }]
    },
    {
      choiceId: "open_shop",
      text: "Show me your food.",
      nextNodeId: "",
      conditions: [],
      actions: [{ type: "open_shop", shopId: "simple_farmer_story:farmer_food_shop" }]
    },
    {
      choiceId: "leave",
      text: "Goodbye.",
      nextNodeId: "",
      conditions: [],
      actions: [{ type: "close" }]
    }
  ];

  const npc = makeNpc("farmer");
  npc.bindings[0].bindingId = "simple_farmer_story:farmer_profession";
  npc.bindings[0].dialogueId = dialogue.id;
  npc.bindings[0].condition = { condition: "chronicle_engine:villager_profession", profession: "farmer" };

  const shop = makeShop("farmer_food_shop");
  shop.shopId = "simple_farmer_story:farmer_food_shop";
  shop.displayName = "Farmer Food";
  shop.categories[0] = { categoryId: "food", displayName: "Food", sortOrder: 0, formatting: "GREEN" };
  shop.entries = [
    { entryId: "bread", displayName: "Bread", costs: [defaultReward("item", "minecraft:emerald", 1)], rewards: [defaultReward("item", "minecraft:bread", 3)], category: "food", visibleCondition: defaultCondition("chronicle_engine:always"), sortOrder: 0 },
    { entryId: "apple", displayName: "Apple", costs: [defaultReward("item", "minecraft:emerald", 1)], rewards: [defaultReward("item", "minecraft:apple", 4)], category: "food", visibleCondition: defaultCondition("chronicle_engine:always"), sortOrder: 1 }
  ];

  const wallet = makeWallet("minecraft:emerald");
  wallet.currencyId = "minecraft:emerald";
  wallet.displayName = "Emerald";

  state.data = {
    quests: [quest],
    dialogues: [dialogue],
    npcs: [npc],
    shops: [shop],
    wallets: [wallet]
  };
  state.selected = {
    quests: quest.id,
    dialogues: dialogue.id,
    npcs: npc.localId,
    shops: shop.shopId,
    wallets: wallet.currencyId
  };
  refreshDerivedHints();
}

async function pickProject() {
  if (!window.showDirectoryPicker) {
    showMessages(["当前浏览器不支持目录写入。请使用新版 Edge / Chrome 打开此 HTML。"], "warn");
    return;
  }
  try {
    state.projectHandle = await window.showDirectoryPicker({ mode: "readwrite" });
    state.projectName = state.projectHandle.name;
    await scanProject();
    renderAll();
    showMessages([`已选择项目：${state.projectName}`], "ok");
  } catch (error) {
    if (error.name !== "AbortError") showMessages([`选择项目失败：${error.message}`], "error");
  }
}

async function scanProject() {
  if (!state.projectHandle) return;
  const mods = await scanMods();
  const ids = await scanKubeJsIds();
  state.hints.mods = mods;
  state.hints.items = unique([...COMMON_ITEMS, ...ids.items]);
  state.hints.entities = unique([...COMMON_ENTITIES, ...ids.entities]);
  refreshDerivedHints();
}

async function scanMods() {
  const modsDir = await getOptionalDir(state.projectHandle, ["mods"]);
  if (!modsDir) return [];
  const mods = [];
  for await (const [name, handle] of modsDir.entries()) {
    if (handle.kind !== "file" || !name.toLowerCase().endsWith(".jar")) continue;
    const inferred = inferNamespaceFromJar(name);
    if (inferred) mods.push(inferred);
  }
  return unique(mods).sort((a, b) => a.localeCompare(b));
}

async function scanKubeJsIds() {
  const kube = await getOptionalDir(state.projectHandle, ["kubejs"]);
  const result = { items: [], entities: [] };
  if (!kube) return result;
  const files = await listFiles(kube, [".js", ".json"], 120);
  for (const fileHandle of files) {
    const file = await fileHandle.getFile();
    if (file.size > 600000) continue;
    const text = await file.text();
    const idRegex = /["']([a-z0-9_.-]+:[a-z0-9_./-]+)["']/gi;
    for (const match of text.matchAll(idRegex)) {
      const id = match[1];
      if (id.includes("entity") || id.includes("mob")) result.entities.push(id);
      result.items.push(id);
    }
  }
  return result;
}

async function importConfigPack() {
  if (!state.projectHandle) {
    showMessages(["请先选择整合包项目文件夹。"], "warn");
    return;
  }
  const root = await getOptionalDir(state.projectHandle, ["config", "chronicle_engine", "chronicle_pack"]);
  if (!root) {
    showMessages(["没有找到 config/chronicle_engine/chronicle_pack。你可以先创建内容再写入。"], "warn");
    return;
  }
  const loaded = await readChronicleConfig(root);
  if (!loaded.count) {
    showMessages(["找到了 CE:RF 配置目录，但里面没有可读取的 JSON。"], "warn");
    return;
  }
  state.data = loaded.data;
  state.selected = {
    quests: state.data.quests[0]?.id || "",
    dialogues: state.data.dialogues[0]?.id || "",
    npcs: state.data.npcs[0]?.localId || "",
    shops: state.data.shops[0]?.shopId || "",
    wallets: state.data.wallets[0]?.currencyId || ""
  };
  refreshDerivedHints();
  renderAll();
  showMessages([`已读取 ${loaded.count} 个 CE:RF JSON 文件。`], "ok");
}

async function readChronicleConfig(root) {
  const data = { quests: [], dialogues: [], npcs: [], shops: [], wallets: [] };
  let count = 0;
  count += await readJsonDir(root, "quests", (json) => data.quests.push(parseQuest(json)));
  count += await readJsonDir(root, "dialogues", (json) => data.dialogues.push(parseDialogue(json)));
  count += await readJsonDir(root, "npc", (json, name) => data.npcs.push(parseNpc(json, name)));
  count += await readJsonDir(root, "trades", (json) => data.shops.push(parseShop(json)));
  count += await readJsonDir(root, "shops", (json) => data.shops.push(parseShop(json)));
  count += await readJsonDir(root, "wallet", (json) => data.wallets.push(parseWallet(json)));
  return { data, count };
}

async function readJsonDir(root, dirName, consumer) {
  const dir = await getOptionalDir(root, [dirName]);
  if (!dir) return 0;
  let count = 0;
  for await (const [name, handle] of dir.entries()) {
    if (handle.kind !== "file" || !name.endsWith(".json")) continue;
    try {
      const json = JSON.parse(await (await handle.getFile()).text());
      consumer(json, name.replace(/\.json$/i, ""));
      count++;
    } catch (error) {
      console.warn("Failed to import", dirName, name, error);
    }
  }
  return count;
}

function parseQuest(json) {
  return {
    id: json.id || namespaced("quest"),
    category: json.category || "",
    displayName: plainText(json.displayName),
    description: plainText(json.description),
    sortOrder: Number(json.sortOrder || 0),
    repeatable: Boolean(json.repeatable),
    teamSync: Boolean(json.teamSync),
    mode: json.mode || "PROGRESSION",
    initialPhaseId: json.initialPhaseId || "",
    unlockConditions: json.unlockConditions || [],
    flagsToSetOnAccept: json.flagsToSetOnAccept || [],
    flagsToSetOnComplete: json.flagsToSetOnComplete || [],
    completionRewards: json.completionRewards || [],
    phases: (json.phases || []).map((phase) => ({
      phaseId: phase.phaseId || "",
      displayName: plainText(phase.displayName),
      description: plainText(phase.description),
      story: plainText(phase.story),
      flagsToSetOnEnter: phase.flagsToSetOnEnter || [],
      flagsToSetOnComplete: phase.flagsToSetOnComplete || [],
      objectives: (phase.objectives || []).map((objective) => ({
        type: objective.type || "KILL",
        targetId: objective.targetId || "",
        requiredCount: Number(objective.requiredCount || 1),
        displayText: plainText(objective.displayText),
        hidden: Boolean(objective.hidden),
        optional: Boolean(objective.optional),
        extraData: objective.extraData || {}
      })),
      transitions: phase.transitions || [],
      phaseRewards: phase.phaseRewards || []
    }))
  };
}

function parseDialogue(json) {
  return {
    id: json.id || namespaced("dialogue"),
    defaultNpc: plainText(json.defaultNpc),
    startNodeId: json.startNodeId || "root",
    nodes: (json.nodes || []).map((node) => ({
      nodeId: node.nodeId || "root",
      text: plainText(node.text),
      conditionalTexts: node.conditionalTexts || [],
      choices: (node.choices || []).map((choice) => ({
        choiceId: choice.choiceId || "",
        text: plainText(choice.text),
        nextNodeId: choice.nextNodeId || "",
        conditions: choice.conditions || [],
        actions: choice.actions || []
      }))
    }))
  };
}

function parseNpc(json, localId) {
  return {
    localId,
    entityType: json.entityType || "",
    bindings: json.bindings || [],
    cancelVanillaInteract: json.cancelVanillaInteract !== false,
    dialogueDistance: Number(json.dialogueDistance || 8),
    shouldLookAtPlayer: json.shouldLookAtPlayer !== false,
    shouldStopMoving: json.shouldStopMoving !== false
  };
}

function parseShop(json) {
  return {
    shopId: json.shopId || namespaced("shop"),
    displayName: plainText(json.displayName),
    description: plainText(json.description),
    openCondition: json.openCondition || defaultCondition("chronicle_engine:always"),
    categories: (json.categories || []).map((category) => ({
      categoryId: category.categoryId || "",
      displayName: plainText(category.displayName),
      sortOrder: Number(category.sortOrder || 0),
      formatting: category.formatting || "WHITE"
    })),
    entries: Object.values(json.entries || {}).map((entry) => ({
      entryId: entry.entryId || "",
      displayName: plainText(entry.displayName),
      costs: entry.costs || [],
      rewards: entry.rewards || [],
      category: entry.category || "",
      visibleCondition: entry.visibleCondition || defaultCondition("chronicle_engine:always"),
      sortOrder: Number(entry.sortOrder || 0)
    }))
  };
}

function parseWallet(json) {
  return {
    currencyId: json.currencyId || json.itemId || "",
    itemId: json.itemId || "",
    itemIds: json.itemIds || json.items || [json.itemId].filter(Boolean),
    displayName: plainText(json.displayName),
    sortOrder: Number(json.sortOrder || 0)
  };
}

function buildFiles() {
  const files = [];
  const mode = state.settings.exportMode;
  const prefix = mode === "config" ? "" : `data/${state.settings.namespace}/chronicle/`;
  if (mode === "datapack") {
    files.push(["pack.mcmeta", JSON.stringify({
      pack: {
        pack_format: 15,
        description: state.settings.packName || "Chronicle Engine datapack"
      }
    }, null, 2)]);
  }
  for (const quest of state.data.quests) files.push([`${prefix}quests/${fileNameFromId(quest.id)}.json`, pretty(buildQuestJson(quest))]);
  for (const dialogue of state.data.dialogues) files.push([`${prefix}dialogues/${fileNameFromId(dialogue.id)}.json`, pretty(buildDialogueJson(dialogue))]);
  for (const npc of state.data.npcs) files.push([`${prefix}npc/${safeFileName(npc.localId)}.json`, pretty(buildNpcJson(npc))]);
  for (const shop of state.data.shops) files.push([`${prefix}trades/${fileNameFromId(shop.shopId)}.json`, pretty(buildShopJson(shop))]);
  for (const wallet of state.data.wallets) files.push([`${prefix}wallet/${fileNameFromId(wallet.currencyId)}.json`, pretty(buildWalletJson(wallet))]);
  return files;
}

function buildQuestJson(quest) {
  return compactObject({
    id: quest.id,
    category: quest.category,
    displayName: textValue(quest.displayName),
    description: textValue(quest.description),
    sortOrder: Number(quest.sortOrder || 0),
    repeatable: Boolean(quest.repeatable),
    teamSync: Boolean(quest.teamSync),
    mode: quest.mode || "PROGRESSION",
    initialPhaseId: quest.initialPhaseId || quest.phases[0]?.phaseId || "",
    unlockConditions: quest.unlockConditions,
    flagsToSetOnAccept: quest.flagsToSetOnAccept,
    flagsToSetOnComplete: quest.flagsToSetOnComplete,
    completionRewards: cleanRewards(quest.completionRewards),
    phases: quest.phases.map((phase) => compactObject({
      phaseId: phase.phaseId,
      displayName: textValue(phase.displayName),
      description: textValue(phase.description),
      story: textValue(phase.story),
      flagsToSetOnEnter: phase.flagsToSetOnEnter,
      flagsToSetOnComplete: phase.flagsToSetOnComplete,
      objectives: phase.objectives.map((objective) => compactObject({
        type: objective.type,
        targetId: objective.targetId,
        requiredCount: Number(objective.requiredCount || 1),
        displayText: textValue(objective.displayText),
        hidden: Boolean(objective.hidden),
        optional: Boolean(objective.optional),
        extraData: compactObject(objective.extraData || {})
      })),
      transitions: phase.transitions,
      phaseRewards: cleanRewards(phase.phaseRewards)
    }))
  });
}

function buildDialogueJson(dialogue) {
  return compactObject({
    id: dialogue.id,
    defaultNpc: textValue(dialogue.defaultNpc),
    startNodeId: dialogue.startNodeId || dialogue.nodes[0]?.nodeId || "root",
    nodes: dialogue.nodes.map((node) => compactObject({
      nodeId: node.nodeId,
      text: textValue(node.text),
      conditionalTexts: node.conditionalTexts || [],
      choices: node.choices.map((choice) => compactObject({
        choiceId: choice.choiceId,
        text: textValue(choice.text),
        nextNodeId: choice.nextNodeId,
        conditions: choice.conditions,
        actions: cleanRewards(choice.actions)
      }))
    }))
  });
}

function buildNpcJson(npc) {
  return compactObject({
    entityType: npc.entityType,
    bindings: npc.bindings.map((binding) => compactObject({
      bindingId: binding.bindingId,
      dialogueId: binding.dialogueId,
      dialogueIdFromNbt: binding.dialogueIdFromNbt,
      condition: compactObject(binding.condition || {}),
      priority: Number(binding.priority || 0)
    })),
    cancelVanillaInteract: Boolean(npc.cancelVanillaInteract),
    dialogueDistance: Number(npc.dialogueDistance || 8),
    shouldLookAtPlayer: Boolean(npc.shouldLookAtPlayer),
    shouldStopMoving: Boolean(npc.shouldStopMoving)
  });
}

function buildShopJson(shop) {
  const entries = {};
  for (const entry of shop.entries) {
    entries[entry.entryId] = compactObject({
      entryId: entry.entryId,
      displayName: textValue(entry.displayName),
      costs: cleanRewards(entry.costs),
      rewards: cleanRewards(entry.rewards),
      category: entry.category,
      visibleCondition: compactObject(entry.visibleCondition || {}),
      sortOrder: Number(entry.sortOrder || 0)
    });
  }
  return compactObject({
    shopId: shop.shopId,
    displayName: textValue(shop.displayName),
    description: textValue(shop.description),
    openCondition: compactObject(shop.openCondition || {}),
    categories: shop.categories.map((category) => compactObject({
      categoryId: category.categoryId,
      displayName: textValue(category.displayName),
      sortOrder: Number(category.sortOrder || 0),
      formatting: category.formatting || "WHITE"
    })),
    entries
  });
}

function buildWalletJson(wallet) {
  return compactObject({
    currencyId: wallet.currencyId,
    itemId: wallet.itemId || wallet.itemIds[0] || "",
    itemIds: unique(wallet.itemIds || []).filter(Boolean),
    displayName: textValue(wallet.displayName),
    sortOrder: Number(wallet.sortOrder || 0)
  });
}

function cleanRewards(rewards) {
  return (rewards || []).map((reward) => compactObject({ ...reward }));
}

async function saveAll() {
  const validation = validateData();
  showMessages(validation);
  if (validation.some((message) => message.startsWith("错误"))) return;
  if (!state.projectHandle) {
    showMessages(["请先选择项目文件夹。没有目录权限时，可在导出页复制 JSON 预览。"], "warn");
    return;
  }
  try {
    const files = buildFiles();
    const base = state.settings.exportMode === "config"
      ? await ensureDir(state.projectHandle, ["config", "chronicle_engine", "chronicle_pack"])
      : await ensureDir(state.projectHandle, ["generated_chronicle_datapack", safeFileName(state.settings.namespace)]);
    for (const [path, content] of files) {
      await writeFile(base, path, content);
    }
    showMessages([`已写入 ${files.length} 个文件。`], "ok");
  } catch (error) {
    showMessages([`写入失败：${error.message}`], "error");
  }
}

async function writeFile(rootHandle, path, content) {
  const parts = path.split("/");
  const fileName = parts.pop();
  const dir = await ensureDir(rootHandle, parts);
  const file = await dir.getFileHandle(fileName, { create: true });
  const writable = await file.createWritable();
  await writable.write(content);
  await writable.close();
}

async function ensureDir(root, parts) {
  let handle = root;
  for (const part of parts.filter(Boolean)) {
    handle = await handle.getDirectoryHandle(part, { create: true });
  }
  return handle;
}

async function getOptionalDir(root, parts) {
  let handle = root;
  try {
    for (const part of parts) handle = await handle.getDirectoryHandle(part);
    return handle;
  } catch {
    return null;
  }
}

async function listFiles(root, extensions, limit) {
  const result = [];
  async function walk(dir) {
    for await (const [, handle] of dir.entries()) {
      if (result.length >= limit) return;
      if (handle.kind === "directory") await walk(handle);
      if (handle.kind === "file" && extensions.some((extension) => handle.name.toLowerCase().endsWith(extension))) result.push(handle);
    }
  }
  await walk(root);
  return result;
}

function validateData() {
  const messages = [];
  for (const quest of state.data.quests) {
    if (!isNamespacedId(quest.id)) messages.push(`错误：任务 ID 不合法：${quest.id}`);
    if (!quest.phases.length) messages.push(`错误：任务 ${quest.id} 没有阶段。`);
    if (!quest.phases.some((phase) => phase.phaseId === quest.initialPhaseId)) messages.push(`错误：任务 ${quest.id} 的初始阶段不存在。`);
    for (const phase of quest.phases) {
      if (!phase.phaseId) messages.push(`错误：任务 ${quest.id} 有阶段缺少 ID。`);
      for (const objective of phase.objectives) {
        if (!objective.type || !objective.targetId) messages.push(`错误：任务 ${quest.id} 的目标缺少类型或目标 ID。`);
      }
    }
  }
  for (const dialogue of state.data.dialogues) {
    if (!isNamespacedId(dialogue.id)) messages.push(`错误：对话 ID 不合法：${dialogue.id}`);
    if (!dialogue.nodes.some((node) => node.nodeId === dialogue.startNodeId)) messages.push(`错误：对话 ${dialogue.id} 的起始节点不存在。`);
  }
  for (const npc of state.data.npcs) {
    if (!npc.localId) messages.push("错误：有 NPC 绑定缺少文件名。");
    if (!npc.entityType) messages.push(`错误：NPC ${npc.localId} 缺少实体类型。`);
  }
  for (const shop of state.data.shops) {
    if (!isNamespacedId(shop.shopId)) messages.push(`错误：商店 ID 不合法：${shop.shopId}`);
    for (const entry of shop.entries) {
      if (!entry.entryId) messages.push(`错误：商店 ${shop.shopId} 有商品缺少 ID。`);
      if (!entry.costs.length) messages.push(`警告：商品 ${entry.entryId} 没有花费。`);
      if (!entry.rewards.length) messages.push(`警告：商品 ${entry.entryId} 没有获得物品。`);
    }
  }
  for (const wallet of state.data.wallets) {
    if (!wallet.itemId && !wallet.itemIds.length) messages.push(`错误：钱包 ${wallet.currencyId} 没有绑定物品。`);
  }
  if (!messages.length) messages.push("检查通过：没有发现阻止写入的问题。");
  return messages;
}

function showMessages(messages, forcedType = "") {
  const root = $("#validationMessages");
  if (!root) return;
  root.innerHTML = messages.map((message) => {
    const type = forcedType || (message.startsWith("错误") ? "error" : message.startsWith("警告") ? "warn" : "ok");
    return `<div class="message ${type}">${escapeHtml(message)}</div>`;
  }).join("");
  switchView("export");
}

function renderPreviewList() {
  const files = buildFiles();
  $("#previewSelect").innerHTML = files.map(([path]) => `<option value="${escapeAttr(path)}">${escapeHtml(path)}</option>`).join("");
}

function renderPreview() {
  const files = buildFiles();
  const selected = $("#previewSelect").value || files[0]?.[0];
  const file = files.find(([path]) => path === selected) || files[0];
  $("#jsonPreview").value = file ? file[1] : "";
}

function downloadFileIndex() {
  const lines = buildFiles().map(([path, content]) => `# ${path}\n${content}`).join("\n\n");
  const blob = new Blob([lines], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${safeFileName(state.settings.namespace)}_chronicle_files.txt`;
  a.click();
  URL.revokeObjectURL(url);
}

function refreshDerivedHints() {
  state.hints.quests = unique(state.data.quests.map((quest) => quest.id).filter(Boolean));
  state.hints.dialogues = unique(state.data.dialogues.map((dialogue) => dialogue.id).filter(Boolean));
  state.hints.shops = unique(state.data.shops.map((shop) => shop.shopId).filter(Boolean));
  const itemIds = [];
  const entityIds = [];
  const flags = [];
  for (const quest of state.data.quests) {
    flags.push(...quest.flagsToSetOnAccept, ...quest.flagsToSetOnComplete);
    for (const phase of quest.phases) {
      flags.push(...phase.flagsToSetOnEnter, ...phase.flagsToSetOnComplete);
      for (const objective of phase.objectives) {
        if (objective.type === "COLLECT" || objective.type === "OFFER") itemIds.push(objective.targetId);
        else entityIds.push(objective.targetId);
      }
      collectRewardIds(phase.phaseRewards, itemIds);
    }
    collectRewardIds(quest.completionRewards, itemIds);
  }
  for (const shop of state.data.shops) {
    for (const entry of shop.entries) {
      collectRewardIds(entry.costs, itemIds);
      collectRewardIds(entry.rewards, itemIds);
    }
  }
  for (const wallet of state.data.wallets) itemIds.push(wallet.itemId, ...wallet.itemIds);
  state.hints.items = unique([...COMMON_ITEMS, ...state.hints.items, ...itemIds].filter(Boolean));
  state.hints.entities = unique([...COMMON_ENTITIES, ...state.hints.entities, ...entityIds].filter(Boolean));
  state.hints.flags = unique([...state.hints.flags, ...flags].filter(Boolean));
}

function collectRewardIds(rewards, target) {
  for (const reward of rewards || []) {
    if (reward.itemId) target.push(reward.itemId);
  }
}

function getAnyPath(path) {
  const [scope, ...rest] = path.split(".");
  return getPath(scopeObject(scope), rest.join("."));
}

function setAnyPath(path, value) {
  const [scope, ...rest] = path.split(".");
  setPath(scopeObject(scope), rest.join("."), value);
}

function removeAnyPath(path) {
  const parts = path.split(".");
  const index = Number(parts.pop());
  const parent = getAnyPath(parts.join("."));
  if (Array.isArray(parent)) parent.splice(index, 1);
}

function scopeObject(scope) {
  if (scope === "quest") return findById(state.data.quests, "id", state.selected.quests);
  if (scope === "dialogue") return findById(state.data.dialogues, "id", state.selected.dialogues);
  if (scope === "npc") return findById(state.data.npcs, "localId", state.selected.npcs);
  if (scope === "shop") return findById(state.data.shops, "shopId", state.selected.shops);
  return null;
}

function getPath(object, path) {
  if (!object || !path) return object;
  return path.split(".").reduce((current, key) => current?.[Number.isInteger(Number(key)) && key.trim() !== "" ? Number(key) : key], object);
}

function setPath(object, path, value) {
  const parts = path.split(".");
  let current = object;
  for (let i = 0; i < parts.length - 1; i++) {
    const key = pathKey(parts[i]);
    if (current[key] == null) current[key] = {};
    current = current[key];
  }
  current[pathKey(parts.at(-1))] = value;
}

function pathKey(key) {
  return Number.isInteger(Number(key)) && key.trim() !== "" ? Number(key) : key;
}

function textValue(value) {
  return { mode: "literal", value: value || "" };
}

function plainText(value) {
  if (value == null) return "";
  if (typeof value === "string") return value;
  return value.value || "";
}

function compactObject(object) {
  const result = Array.isArray(object) ? [] : {};
  for (const [key, value] of Object.entries(object)) {
    if (value === "" || value == null) continue;
    if (Array.isArray(value)) {
      if (value.length) result[key] = value;
      continue;
    }
    if (typeof value === "object") {
      const compacted = compactObject(value);
      if (Object.keys(compacted).length) result[key] = compacted;
      continue;
    }
    result[key] = value;
  }
  return result;
}

function pretty(json) {
  return JSON.stringify(json, null, 2) + "\n";
}

function findById(rows, key, id) {
  return rows.find((row) => row[key] === id) || null;
}

function namespaced(local) {
  return `${state.settings.namespace}:${safePath(local)}`;
}

function fileNameFromId(id) {
  return safeFileName((id || "").split(":").pop() || "file");
}

function safeFileName(value) {
  return safePath(value).replace(/\//g, "_") || "file";
}

function safePath(value) {
  return String(value || "")
    .trim()
    .toLowerCase()
    .replace(/^[a-z0-9_.-]+:/, "")
    .replace(/[^a-z0-9_./-]+/g, "_")
    .replace(/^_+|_+$/g, "");
}

function normalizeNamespace(value) {
  return String(value || "my_story")
    .toLowerCase()
    .replace(/[^a-z0-9_.-]+/g, "_")
    .replace(/^_+|_+$/g, "") || "my_story";
}

function isNamespacedId(value) {
  return /^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(value || "");
}

function inferNamespaceFromJar(name) {
  const clean = name
    .replace(/\.jar$/i, "")
    .replace(/\[[^\]]+\]/g, "")
    .replace(/(?:forge|fabric|mc|minecraft)?[-_ ]?1\.\d+(?:\.\d+)?/ig, "")
    .replace(/[-_ ]?\d+(?:\.\d+)+(?:[-_+a-z0-9.]*)?$/i, "")
    .replace(/[^a-zA-Z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .toLowerCase();
  return clean && clean.length <= 48 ? clean : "";
}

function splitLines(value) {
  return unique(String(value || "").split(/\r?\n|,/).map((line) => line.trim()).filter(Boolean));
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

function uniqueLocalId(prefix, existing) {
  let index = 1;
  let id = prefix;
  while (existing.includes(id)) id = `${prefix}_${index++}`;
  return id;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function escapeAttr(value) {
  return escapeHtml(value).replace(/"/g, "&quot;");
}

function objectiveTypeOptions() {
  return [
    ["KILL", "击杀生物"],
    ["COLLECT", "背包持有物品"],
    ["OFFER", "交付物品"],
    ["INTERACT", "与 NPC/目标交互"],
    ["ADVANCEMENT", "完成成就/进度"]
  ];
}

function conditionOptions(allowVillager) {
  const base = [
    ["chronicle_engine:always", "始终满足"],
    ["chronicle_engine:has_flag", "拥有旗标"],
    ["chronicle_engine:quest_not_started", "任务未开始"],
    ["chronicle_engine:quest_accepted", "任务进行中"],
    ["chronicle_engine:quest_phase", "任务处于阶段"],
    ["chronicle_engine:entity_name", "实体名称匹配"]
  ];
  if (allowVillager) base.push(["chronicle_engine:villager_profession", "村民职业"]);
  return base;
}

function rewardOptions() {
  return [
    ["item", "物品"],
    ["enchanted_book", "附魔书"],
    ["potion", "药水"],
    ["command", "执行命令"]
  ];
}

function actionOptions() {
  return [
    ["close", "关闭对话"],
    ["start_quest", "接取任务"],
    ["notify_interact", "推进交互目标"],
    ["set_flag", "设置旗标"],
    ["open_shop", "打开商店"],
    ["item", "给予物品"],
    ["command", "执行命令"]
  ];
}

function colorOptions() {
  return ["WHITE", "GRAY", "GREEN", "AQUA", "BLUE", "LIGHT_PURPLE", "GOLD", "YELLOW", "RED"].map((value) => [value, value]);
}

function rewardLabel(type) {
  return Object.fromEntries(rewardOptions())[type] || type;
}

function actionLabel(type) {
  return Object.fromEntries(actionOptions())[type] || type;
}

function conditionLabel(type, allowVillager = false) {
  return Object.fromEntries(conditionOptions(allowVillager))[type] || type;
}

function rewardSummary(reward) {
  const type = reward.type || "item";
  if (type === "item" || type === "give_item") {
    return `${reward.itemId || "未设置物品"} x${reward.count ?? 1}`;
  }
  if (type === "enchanted_book") {
    return `${reward.enchantmentId || "未设置附魔"} ${reward.level ?? 1} 级`;
  }
  if (type === "potion") {
    return `${reward.potionId || "minecraft:water"} x${reward.count ?? 1}`;
  }
  if (type === "command" || type === "run_command") {
    return reward.command || "未设置命令";
  }
  return rewardLabel(type);
}

function actionSummary(action) {
  const type = action.type || "close";
  if (type === "start_quest") return action.questId || "未设置任务";
  if (type === "notify_interact") return action.targetId || "未设置交互目标";
  if (type === "set_flag") return action.flag || action.flagName || "未设置旗标";
  if (type === "open_shop" || type === "open_trade") return action.shopId || "未设置商店";
  if (type === "item" || type === "give_item") return rewardSummary(action);
  if (type === "command" || type === "run_command") return action.command || "未设置命令";
  return "关闭当前对话";
}

document.addEventListener("DOMContentLoaded", init);
