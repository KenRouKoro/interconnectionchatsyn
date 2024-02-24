package com.foxapplication.mc.interconnectionchatsyn.fabric;

import com.foxapplication.embed.hutool.core.thread.ThreadUtil;
import com.foxapplication.embed.hutool.log.Log;
import com.foxapplication.embed.hutool.log.LogFactory;
import com.foxapplication.mc.interconnection.fabric.util.NBTSendUtil;
import com.foxapplication.mc.interconnectionchatsyn.common.InterconnectionChatSynCommon;
import lombok.Getter;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class InterconnectionChatSynFabric implements DedicatedServerModInitializer {
    private static final Log log = LogFactory.get();
    @Getter
    private static MinecraftServer server = null;
    private static final ExecutorService executor = ThreadUtil.newSingleExecutor();
    private static CommandSourceStack sourceStack = null;
    @Override
    public void onInitializeServer() {
        InterconnectionChatSynCommon.Init();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            InterconnectionChatSynFabric.server = server;
            sourceStack = server.createCommandSourceStack();
        });
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (InterconnectionChatSynCommon.getCONFIG().isEnable()){
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putString("message", message.signedContent());
                compoundTag.putString("displayName", Component.Serializer.toJson(Objects.requireNonNull(sender.getDisplayName())));
                compoundTag.putUUID("UUID",sender.getUUID());

                NBTSendUtil.sendNBTAny("ALL","InterconnectionChatData",compoundTag);
            }
        });
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
}
