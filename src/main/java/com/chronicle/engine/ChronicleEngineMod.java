package com.chronicle.engine;

import com.chronicle.engine.client.ChronicleEngineClient;
import com.chronicle.engine.data.ChronicleEngineReloadListener;
import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(ChronicleEngine.MOD_ID)
public class ChronicleEngineMod {
    public ChronicleEngineMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ChronicleEngineConfig.COMMON_SPEC);
        ChronicleEngineRegistry.register(FMLJavaModLoadingContext.get().getModEventBus());
        ChronicleEngineNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ChronicleEngineCommonEvents.class);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ChronicleEngineClient::init);
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ChronicleEngineReloadListener());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ChronicleEngineCommands.register(event.getDispatcher());
    }
}

