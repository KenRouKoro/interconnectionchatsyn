package com.foxapplication.mc.interconnectionchatsyn.paper;

import com.foxapplication.embed.hutool.core.thread.ThreadUtil;
import com.foxapplication.embed.hutool.log.Log;
import com.foxapplication.embed.hutool.log.LogFactory;
import com.foxapplication.mc.interconnection.paper.util.NBTSendUtil;
import com.foxapplication.mc.interconnectionchatsyn.common.InterconnectionChatSynCommon;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public final class InterconnectionChatSynPaper extends JavaPlugin implements Listener {

    private static final Log log = LogFactory.get();
    private static MinecraftServer server = null;
    private static final ExecutorService executor = ThreadUtil.newSingleExecutor();
    private static CommandSourceStack sourceStack = null;

    private static CraftServer craftServer = null;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        // Plugin startup logic
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerStart(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.RELOAD) return;

        if (getServer() instanceof CraftServer craftServer){
            InterconnectionChatSynPaper.craftServer = craftServer;
            server = craftServer.getServer();
            sourceStack = server.createCommandSourceStack();
        }else {
            log.error("服务器实例获取失败。");
            return;
        };
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChatEvent(AsyncChatEvent event){
        if (InterconnectionChatSynCommon.getCONFIG().isEnable()){

            if (event.getPlayer() instanceof CraftPlayer player){

                CompoundTag compoundTag = new CompoundTag();

                compoundTag.putString("message", PlainTextComponentSerializer.plainText().serialize(event.message()));

                compoundTag.putString("displayName", JSONComponentSerializer.json().serialize((Objects.requireNonNull(event.getPlayer().displayName()))));
                compoundTag.putUUID("UUID",player.getHandle().getUUID());

                NBTSendUtil.sendNBTAny("ALL","InterconnectionChatData",compoundTag);

            }else {

            }
        }
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public MinecraftServer getMinecraftServer() {
        return server;
    }
}
