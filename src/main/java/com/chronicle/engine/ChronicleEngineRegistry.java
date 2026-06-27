package com.chronicle.engine;

import com.chronicle.engine.item.ChronicleEngineWalletItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ChronicleEngineRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ChronicleEngine.MOD_ID);

    public static final RegistryObject<Item> WALLET = ITEMS.register("wallet",
            () -> new ChronicleEngineWalletItem(new Item.Properties().stacksTo(1)));

    private ChronicleEngineRegistry() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

