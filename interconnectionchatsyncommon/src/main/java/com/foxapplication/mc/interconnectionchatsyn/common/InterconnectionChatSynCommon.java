package com.foxapplication.mc.interconnectionchatsyn.common;

import com.foxapplication.embed.hutool.log.Log;
import com.foxapplication.embed.hutool.log.LogFactory;
import com.foxapplication.mc.core.config.LocalFoxConfig;
import com.foxapplication.mc.core.config.webconfig.WebConfig;
import com.foxapplication.mc.interaction.base.data.BaseMessage;
import com.foxapplication.mc.interconnection.common.util.MessageUtil;
import com.foxapplication.mc.interconnectionchatsyn.common.config.InterconnectionChatSynConfig;
import lombok.Getter;

import java.util.concurrent.CopyOnWriteArrayList;

public class InterconnectionChatSynCommon {
    private static final Log log = LogFactory.get();
    private static final CopyOnWriteArrayList<Handler> handlers = new CopyOnWriteArrayList<>();
    @Getter
    private static InterconnectionChatSynConfig CONFIG;
    @Getter
    private static LocalFoxConfig localFoxConfig;



    public static void Init(){
        log.info("InterconnectionChatSyn Init");

        localFoxConfig = new LocalFoxConfig(InterconnectionChatSynConfig.class);
        CONFIG = (InterconnectionChatSynConfig) localFoxConfig.getBeanFoxConfig().getBean();
        WebConfig.addConfig(localFoxConfig.getBeanFoxConfig());

        MessageUtil.addListener("InterconnectionChatData",message->{
            if (CONFIG.isNodeWhitelist()){
                if (CONFIG.isNodeWhitelistUseReverse()){
                    if (CONFIG.getNodeWhitelistList().contains(message.getForm())){
                        return;
                    }
                }else{
                    if (!CONFIG.getNodeWhitelistList().contains(message.getForm())){
                        return;
                    }
                }
            }
            handlers.forEach(handler->handler.handleMessage(message));
        });
    }

    public static void addHandler(Handler handler){
        handlers.add(handler);
    }

    public interface Handler{
        void handleMessage(BaseMessage message);
    }
}
