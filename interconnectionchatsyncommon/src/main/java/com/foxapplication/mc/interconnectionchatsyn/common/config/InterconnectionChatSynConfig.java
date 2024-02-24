package com.foxapplication.mc.interconnectionchatsyn.common.config;

import com.foxapplication.mc.core.config.interfaces.FieldAnnotation;
import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class InterconnectionChatSynConfig {
    @FieldAnnotation(value = "是否启用聊天消息同步",name = "聊天消息同步功能开关")
    boolean enable = true;
    @FieldAnnotation(value = "是否启用节点白名单",name = "节点白名单功能开关")
    boolean nodeWhitelist = false;
    @FieldAnnotation(value = "节点白名单使用黑名单模式",name = "节点黑名单功能开关")
    boolean nodeWhitelistUseReverse = true;
    @FieldAnnotation(value = "节点白名单列表",name = "节点白名单列表")
    List<String> nodeWhitelistList = new CopyOnWriteArrayList<String>();
    @FieldAnnotation(value = "是否启用玩家白名单",name = "玩家白名单功能开关")
    boolean playerWhitelist = false;
    @FieldAnnotation(value = "玩家白名单使用黑名单模式",name = "玩家黑名单功能开关")
    boolean playerWhitelistUseReverse = true;
    @FieldAnnotation(value = "玩家白名单列表，使用的是***UUID***匹配",name = "玩家白名单列表")
    List<String> playerWhitelistList = new CopyOnWriteArrayList<String>();
}
