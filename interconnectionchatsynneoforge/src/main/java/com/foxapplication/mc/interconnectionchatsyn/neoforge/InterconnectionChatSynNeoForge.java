package com.foxapplication.mc.interconnectionchatsyn.neoforge;

import com.foxapplication.embed.hutool.core.thread.ThreadUtil;
import com.foxapplication.embed.hutool.log.Log;
import com.foxapplication.embed.hutool.log.LogFactory;
import com.foxapplication.mc.interconnectionchatsyn.common.InterconnectionChatSynCommon;
import com.foxapplication.mc.interconnectionneo.forge.util.NBTSendUtil;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Mod(InterconnectionChatSynNeoForge.MODID)
public class InterconnectionChatSynNeoForge {
    public static final String MODID = "interconnectionchatsynneoforge";
    private static final Log log = LogFactory.get();
    @Getter
    private static MinecraftServer server = null;
    private static final ExecutorService executor = ThreadUtil.newSingleExecutor();
    private static CommandSourceStack sourceStack = null;
    public InterconnectionChatSynNeoForge(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        InterconnectionChatSynCommon.Init();
        InterconnectionChatSynCommon.addHandler(message -> {
            executor.execute(() -> {
                if (server==null)return;
                if (InterconnectionChatSynCommon.getCONFIG().isEnable()){
                    Tag data;
                    try {
                        data = NBTSendUtil.parseNBT(message.getMessage());
                    } catch (IOException e) {
                        log.error("解析NBT异常",e);
                        return;
                    }

                    CompoundTag compoundTag;
                    if (data.getType()==CompoundTag.TYPE){
                        compoundTag = (CompoundTag)data;
                    }else{
                        log.error("转换NBT异常，不是目标类型");
                        return;
                    }
                    PlayerChatMessage playerChatMessage ;
                    Component component;


                    try {
                        component = Component.literal(compoundTag.getString("message"));
                    }catch (Exception e){
                        log.error("解析Component异常",e);
                        return;
                    }

                    UUID uuid = compoundTag.getUUID("UUID");

                    SignedMessageBody signedMessageBody = SignedMessageBody.unsigned(compoundTag.getString("message"));
                    SignedMessageLink signedMessageLink = SignedMessageLink.unsigned(uuid);
                    MessageSignature messageSignature = new MessageSignature(new byte[256]);
                    playerChatMessage = new PlayerChatMessage(signedMessageLink, messageSignature, signedMessageBody, component, FilterMask.PASS_THROUGH);

                    if (InterconnectionChatSynCommon.getCONFIG().isPlayerWhitelist()){
                        if (InterconnectionChatSynCommon.getCONFIG().isPlayerWhitelistUseReverse()){
                            if (InterconnectionChatSynCommon.getCONFIG().getPlayerWhitelistList().contains(uuid.toString())){
                                return;
                            }
                        }else{
                            if (!InterconnectionChatSynCommon.getCONFIG().getPlayerWhitelistList().contains(uuid.toString())){
                                return;
                            }
                        }
                    }

                    ChatType.Bound bound = ChatType.bind(ChatType.CHAT, Objects.requireNonNull(server.getLevel(Level.OVERWORLD)).registryAccess(), Component.Serializer.fromJson(compoundTag.getString("displayName")));
                    server.execute(() -> {
                        server.getPlayerList().broadcastChatMessage(playerChatMessage, sourceStack ,bound);
                    });
                }
            });
        });
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        sourceStack = server.createCommandSourceStack();
    }
    @SubscribeEvent
    public void onChatMessage(ServerChatEvent event){
        if (InterconnectionChatSynCommon.getCONFIG().isEnable()){
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putString("message", event.getMessage().getString());
            compoundTag.putString("displayName", Component.Serializer.toJson(Objects.requireNonNull(event.getPlayer().getDisplayName())));
            compoundTag.putUUID("UUID",event.getPlayer().getUUID());

            NBTSendUtil.sendNBTAny("ALL","InterconnectionChatData",compoundTag);
        }
    }

}
