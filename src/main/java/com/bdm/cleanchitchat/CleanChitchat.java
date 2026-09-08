package com.bdm.cleanchitchat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Clean Chit Chat
 * Copyright (C) 2026 Bendemen Studios
 *
 * GNU General Public License v3.0 only.
 */
@Mod(CleanChitchat.MODID)
public final class CleanChitchat {
    public static final String MODID = "cleanchitchat";
    public static final String VERSION = "1.0.0";

    private static boolean enabled = true;
    private static boolean removeBrackets = true;

    private static final Path CONFIG =
            Path.of("config", "cleanchitchat.properties");

    public CleanChitchat() {
        loadConfig();
        registerServerListenersReflectively();
    }

    private static void registerServerListenersReflectively() {
        try {
            Class<?> neoForge = Class.forName("net.neoforged.neoforge.common.NeoForge");
            Object eventBus = neoForge.getField("EVENT_BUS").get(null);

            registerTypedListener(
                    eventBus,
                    Class.forName("net.neoforged.neoforge.event.ServerChatEvent"),
                    CleanChitchat::handleChat
            );

            registerTypedListener(
                    eventBus,
                    Class.forName("net.neoforged.neoforge.event.RegisterCommandsEvent"),
                    CleanChitchat::handleCommandRegistration
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Clean Chit Chat could not hook NeoForge events", e
            );
        }
    }

    private static void registerTypedListener(
            Object bus,
            Class<?> eventType,
            Consumer<Object> listener
    ) throws ReflectiveOperationException {
        for (Method method : bus.getClass().getMethods()) {
            if (!method.getName().equals("addListener")
                    || method.getParameterCount() != 2) {
                continue;
            }

            Class<?>[] types = method.getParameterTypes();
            if (!types[0].isAssignableFrom(Class.class)
                    || !types[1].isAssignableFrom(Consumer.class)) {
                continue;
            }

            method.invoke(bus, eventType, listener);
            return;
        }

        throw new NoSuchMethodException("IEventBus.addListener(Class, Consumer)");
    }

    private static void loadConfig() {
        Properties properties = new Properties();

        try {
            Files.createDirectories(CONFIG.getParent());

            if (Files.exists(CONFIG)) {
                try (InputStream input = Files.newInputStream(CONFIG)) {
                    properties.load(input);
                }
            }

            enabled = Boolean.parseBoolean(
                    properties.getProperty("enabled", "true")
            );
            removeBrackets = Boolean.parseBoolean(
                    properties.getProperty("remove_angle_brackets", "true")
            );

            saveConfig();
        } catch (Exception ignored) {
            enabled = true;
            removeBrackets = true;
        }
    }

    private static void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty(
                "remove_angle_brackets",
                Boolean.toString(removeBrackets)
        );

        try {
            Files.createDirectories(CONFIG.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG)) {
                properties.store(output, "Clean Chit Chat configuration");
            }
        } catch (Exception ignored) {
            // Never prevent the server from loading because of a config write error.
        }
    }

    private static void handleCommandRegistration(Object event) {
        try {
            Object dispatcher = invoke(event, "getDispatcher");
            Object root = literal("chitchat");

            then(root, executable("reload", () -> {
                loadConfig();
                return "Clean Chit Chat config reloaded.";
            }));

            then(root, executable("enable", () -> {
                enabled = true;
                saveConfig();
                return "Clean Chit Chat enabled.";
            }));

            then(root, executable("disable", () -> {
                enabled = false;
                saveConfig();
                return "Clean Chit Chat disabled.";
            }));

            Object bracket = literal("bracket");

            then(bracket, executable("on", () -> {
                removeBrackets = false;
                saveConfig();
                return "Chat brackets enabled.";
            }));

            then(bracket, executable("off", () -> {
                removeBrackets = true;
                saveConfig();
                return "Chat brackets removed.";
            }));

            then(root, bracket);
            invokeCompatible(dispatcher, "register", root);
        } catch (Exception error) {
            System.err.println(
                    "[Clean Chit Chat] Command registration failed: " + error
            );
        }
    }

    private static Object executable(
            String commandName,
            CommandAction action
    ) throws ReflectiveOperationException {
        Object builder = literal(commandName);
        Class<?> commandInterface = Class.forName("com.mojang.brigadier.Command");

        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "toString" -> "/" + commandName;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null ? null : args[0]);
            case "run" -> executeAction(
                    args == null || args.length == 0 ? null : args[0], action
            );
            default -> null;
        };

        Object command = Proxy.newProxyInstance(
                commandInterface.getClassLoader(),
                new Class<?>[]{commandInterface},
                handler
        );

        invokeCompatible(builder, "executes", command);
        return builder;
    }

    private static int executeAction(Object context, CommandAction action) throws Exception {
        Object source = invoke(context, "getSource");
        Method hasPermission = source.getClass().getMethod("hasPermission", int.class);

        boolean allowed = (Boolean) hasPermission.invoke(source, 2);
        if (!allowed) {
            sendSourceMessage(
                    source,
                    "You do not have permission to use /chitchat.",
                    true
            );
            return 0;
        }

        sendSourceMessage(source, action.run(), false);
        return 1;
    }

    private static void sendSourceMessage(
            Object source,
            String message,
            boolean failure
    ) throws Exception {
        Component component = Component.literal(message);
        String methodName = failure ? "sendFailure" : "sendSuccess";

        for (Method method : source.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            if (failure
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(Component.class)) {
                method.invoke(source, component);
                return;
            }

            if (!failure
                    && method.getParameterCount() == 2
                    && method.getParameterTypes()[0].isAssignableFrom(Supplier.class)) {
                Supplier<Component> supplier = () -> component;
                method.invoke(source, supplier, false);
                return;
            }
        }

        if (failure) {
            throw new NoSuchMethodException("sendFailure");
        }
    }

    private static Object literal(String name) throws ReflectiveOperationException {
        Class<?> commands = Class.forName("net.minecraft.commands.Commands");
        Method method = commands.getMethod("literal", String.class);
        return method.invoke(null, name);
    }

    private static void then(Object parent, Object child)
            throws ReflectiveOperationException {
        invokeCompatible(parent, "then", child);
    }

    private static Object invoke(Object target, String method)
            throws ReflectiveOperationException {
        Method found = target.getClass().getMethod(method);
        return found.invoke(target);
    }

    private static Object invokeCompatible(
            Object target,
            String name,
            Object argument
    ) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name)
                    || method.getParameterCount() != 1) {
                continue;
            }

            Class<?> parameterType = method.getParameterTypes()[0];
            if (parameterType.isAssignableFrom(argument.getClass())) {
                return method.invoke(target, argument);
            }
        }

        throw new NoSuchMethodException(
                target.getClass().getName()
                        + "." + name
                        + "(" + argument.getClass().getName() + ")"
        );
    }

    private static void handleChat(Object event) {
        if (!enabled || !removeBrackets) {
            return;
        }

        try {
            ServerPlayer player = (ServerPlayer) invoke(event, "getPlayer");
            String rawText = (String) invoke(event, "getRawText");

            invokeOneArg(event, "setCanceled", Boolean.TRUE);

            Component clean = player.getDisplayName()
                    .copy()
                    .append(Component.literal(" | "))
                    .append(Component.literal(rawText));

            var server = player.getServer();
            if (server == null) {
                return;
            }

            Object playerList = server.getPlayerList();
            Method broadcast = playerList.getClass().getMethod(
                    "broadcastSystemMessage",
                    Component.class,
                    boolean.class
            );

            broadcast.invoke(playerList, clean, false);
        } catch (Exception error) {
            System.err.println(
                    "[Clean Chit Chat] Chat handling failed: " + error
            );
        }
    }

    private static Object invokeOneArg(
            Object target,
            String method,
            Object argument
    ) throws ReflectiveOperationException {
        for (Method candidate : target.getClass().getMethods()) {
            if (!candidate.getName().equals(method)
                    || candidate.getParameterCount() != 1) {
                continue;
            }

            Class<?> parameter = wrap(candidate.getParameterTypes()[0]);
            Class<?> argumentType = wrap(argument.getClass());
            if (parameter.isAssignableFrom(argumentType)) {
                return candidate.invoke(target, argument);
            }
        }

        throw new NoSuchMethodException(method);
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }

    @FunctionalInterface
    private interface CommandAction {
        String run() throws Exception;
    }
}
