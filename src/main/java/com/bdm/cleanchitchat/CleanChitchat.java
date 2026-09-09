/*
 * Clean Chit Chat
 * Copyright (C) 2026 Bendemen Studios
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.bdm.cleanchitchat;

import net.neoforged.fml.common.Mod;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod(CleanChitchat.MODID)
public final class CleanChitchat {
    public static final String MODID = "cleanchitchat";
    public static final String VERSION = "1.1.0";

    private static boolean enabled = true;
    private static boolean removeBrackets = true;
    private static boolean separation = true;
    private static final Path CONFIG = Path.of("config", "cleanchitchat.properties");

    public CleanChitchat() {
        loadConfig();
        registerServerListenersReflectively();
    }

    private static void registerServerListenersReflectively() {
        try {
            Class<?> neoForge = Class.forName("net.neoforged.neoforge.common.NeoForge");
            Object eventBus = neoForge.getField("EVENT_BUS").get(null);
            registerTypedListener(eventBus, Class.forName("net.neoforged.neoforge.event.ServerChatEvent"), CleanChitchat::handleChat);
            registerTypedListener(eventBus, Class.forName("net.neoforged.neoforge.event.RegisterCommandsEvent"), CleanChitchat::handleCommandRegistration);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Clean Chit Chat could not hook NeoForge events", e);
        }
    }

    private static void registerTypedListener(Object eventBus, Class<?> eventClass, Consumer<Object> consumer) throws ReflectiveOperationException {
        for (Method method : eventBus.getClass().getMethods()) {
            if (!method.getName().equals("addListener") || method.getParameterCount() != 2) continue;
            if (!method.getParameterTypes()[0].equals(Class.class)) continue;
            if (!Consumer.class.isAssignableFrom(method.getParameterTypes()[1])) continue;
            method.invoke(eventBus, eventClass, consumer);
            return;
        }
        throw new NoSuchMethodException("IEventBus.addListener(Class, Consumer)");
    }

    private static void loadConfig() {
        Properties p = new Properties();
        enabled = true;
        removeBrackets = true;
        separation = true;
        try {
            Files.createDirectories(CONFIG.getParent());
            if (Files.exists(CONFIG)) {
                try (InputStream in = Files.newInputStream(CONFIG)) {
                    p.load(in);
                }
            }
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            removeBrackets = Boolean.parseBoolean(p.getProperty("remove_angle_brackets", "true"));
            separation = Boolean.parseBoolean(p.getProperty("separation", "true"));
            saveConfig();
        } catch (Exception ignored) {
            saveConfig();
        }
    }

    private static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("remove_angle_brackets", Boolean.toString(removeBrackets));
        p.setProperty("separation", Boolean.toString(separation));
        try {
            Files.createDirectories(CONFIG.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG)) {
                p.store(out, "Clean Chit Chat configuration");
            }
        } catch (Exception ignored) {}
    }

    private static void handleCommandRegistration(Object event) {
        try {
            Object dispatcher = invoke(event, "getDispatcher");
            Object root = literal("chitchat");

            then(root, executable("reload", () -> { loadConfig(); return "Clean Chit Chat config reloaded."; }));
            then(root, executable("enable", () -> { enabled = true; saveConfig(); return "Clean Chit Chat enabled."; }));
            then(root, executable("disable", () -> { enabled = false; saveConfig(); return "Clean Chit Chat disabled."; }));

            Object bracket = literal("bracket");
            then(bracket, executable("on", () -> { removeBrackets = false; saveConfig(); return "Chat brackets enabled."; }));
            then(bracket, executable("off", () -> { removeBrackets = true; saveConfig(); return "Chat brackets removed."; }));
            then(root, bracket);

            Object separationRoot = literal("separation");
            then(separationRoot, executable("on", () -> { separation = true; saveConfig(); return "Chat separation enabled."; }));
            then(separationRoot, executable("off", () -> { separation = false; saveConfig(); return "Chat separation disabled."; }));
            then(root, separationRoot);

            invokeCompatible(dispatcher, "register", root);
        } catch (Exception e) {
            System.err.println("[Clean Chit Chat] Command registration failed: " + e);
        }
    }

    private static Object executable(String name, CommandAction action) throws ReflectiveOperationException {
        Object builder = literal(name);
        Class<?> commandClass = Class.forName("com.mojang.brigadier.Command");
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "toString" -> "CleanChitChatCommand[" + name + "]";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> args != null && args.length > 0 && proxy == args[0];
            case "run" -> executeAction(args != null && args.length > 0 ? args[0] : null, action);
            default -> null;
        };
        Object command = Proxy.newProxyInstance(commandClass.getClassLoader(), new Class<?>[]{commandClass}, handler);
        invokeCompatible(builder, "executes", command);
        return builder;
    }

    private static int executeAction(Object context, CommandAction action) throws Exception {
        Object source = invoke(context, "getSource");
        Method permission = findMethod(source.getClass(), "hasPermission", int.class);
        if (permission != null && !((Boolean) permission.invoke(source, 2))) {
            sendSourceMessage(source, "You do not have permission to use /chitchat.", true);
            return 0;
        }
        sendSourceMessage(source, action.run(), false);
        return 1;
    }

    private static void sendSourceMessage(Object source, String text, boolean failure) throws Exception {
        Object component = literalComponent("[Clean Chit Chat] " + text);
        String methodName = failure ? "sendFailure" : "sendSuccess";
        for (Method m : source.getClass().getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (failure && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(component)) {
                m.invoke(source, component);
                return;
            }
            if (!failure && m.getParameterCount() == 2) {
                Class<?> p0 = m.getParameterTypes()[0];
                if (p0.isAssignableFrom(Supplier.class) || p0.isAssignableFrom(component.getClass())) {
                    Supplier<Object> supplier = () -> component;
                    m.invoke(source, supplier, false);
                    return;
                }
            }
        }
    }

    private static Object literalComponent(String text) throws Exception {
        Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
        return componentClass.getMethod("literal", String.class).invoke(null, text);
    }

    private static Object literal(String name) throws ReflectiveOperationException {
        Class<?> commands = Class.forName("net.minecraft.commands.Commands");
        return commands.getMethod("literal", String.class).invoke(null, name);
    }

    private static void then(Object parent, Object child) throws ReflectiveOperationException {
        invokeCompatible(parent, "then", child);
    }

    private static Object invoke(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        try { return type.getMethod(name, params); } catch (NoSuchMethodException e) { return null; }
    }

    private static Object invokeCompatible(Object target, String methodName, Object argument) throws ReflectiveOperationException {
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(methodName) || m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0].isAssignableFrom(argument.getClass())) return m.invoke(target, argument);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + methodName);
    }

    private static void handleChat(Object event) {
        if (!enabled) return;
        try {
            if (!removeBrackets) return;

            Object player = invoke(event, "getPlayer");
            String raw = (String) invoke(event, "getRawText");
            invokeOneArg(event, "setCanceled", Boolean.TRUE);

            Object clean = copyComponent(invoke(player, "getDisplayName"));

            if (separation) {
                clean = appendStyled(clean, " | ", "DARK_GRAY");
            } else {
                clean = appendStyled(clean, " ", "WHITE");
            }
            clean = appendStyled(clean, raw, "GRAY");

            Object server = invoke(player, "getServer");
            if (server == null) return;
            Object playerList = invoke(server, "getPlayerList");
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method broadcast = playerList.getClass().getMethod("broadcastSystemMessage", componentClass, boolean.class);
            broadcast.invoke(playerList, clean, false);
        } catch (Exception e) {
            System.err.println("[Clean Chit Chat] Chat handling failed: " + e);
        }
    }

    private static Object copyComponent(Object component) throws Exception {
        return component.getClass().getMethod("copy").invoke(component);
    }

    private static Object appendStyled(Object component, String text, String formattingName) throws Exception {
        Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
        Object part = componentClass.getMethod("literal", String.class).invoke(null, text);
        Class<?> styleClass = Class.forName("net.minecraft.network.chat.Style");
        Object style = styleClass.getField("EMPTY").get(null);
        Class<?> chatFormatting = Class.forName("net.minecraft.ChatFormatting");
        Object formatting = Enum.valueOf((Class<Enum>) chatFormatting.asSubclass(Enum.class), formattingName);

        style = applyFormatting(styleClass, style, formatting);
        invokeCompatible(part, "setStyle", style);
        invokeCompatible(component, "append", part);
        return component;
    }

    private static Object applyFormatting(Class<?> styleClass, Object style, Object formatting) throws Exception {
        for (Method method : styleClass.getMethods()) {
            if (!method.getName().equals("withColor") || method.getParameterCount() != 1) continue;
            if (method.getParameterTypes()[0].isAssignableFrom(formatting.getClass())) return method.invoke(style, formatting);
        }
        for (Method method : styleClass.getMethods()) {
            if (!method.getName().equals("applyFormat") || method.getParameterCount() != 1) continue;
            if (method.getParameterTypes()[0].isAssignableFrom(formatting.getClass())) return method.invoke(style, formatting);
        }
        return style;
    }

    private static Object invokeOneArg(Object target, String methodName, Object argument) throws ReflectiveOperationException {
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(methodName) || m.getParameterCount() != 1) continue;
            Class<?> parameter = wrap(m.getParameterTypes()[0]);
            Class<?> arg = wrap(argument.getClass());
            if (parameter.isAssignableFrom(arg)) return m.invoke(target, argument);
        }
        throw new NoSuchMethodException(methodName);
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == boolean.class) return Boolean.class;
        if (c == byte.class) return Byte.class;
        if (c == short.class) return Short.class;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == char.class) return Character.class;
        return c;
    }

    @FunctionalInterface
    private interface CommandAction { String run() throws Exception; }
}
