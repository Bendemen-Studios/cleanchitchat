package com.bdm.cleanchitchat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import static net.minecraft.commands.Commands.literal;

@Mod(CleanChitchat.MOD_ID)
public final class CleanChitchat {
    public static final String MOD_ID = "cleanchitchat";

    private static boolean enabled = true;
    private static boolean removeBrackets = true;

    public CleanChitchat(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onChat);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = literal("chitchat");
        root.then(literal("enable").executes(src -> {
            enabled = true;
            src.getSource().sendSuccess(() -> Component.literal("Clean Chit Chat enabled."), false);
            return Command.SINGLE_SUCCESS;
        }));
        root.then(literal("disable").executes(src -> {
            enabled = false;
            src.getSource().sendSuccess(() -> Component.literal("Clean Chit Chat disabled."), false);
            return Command.SINGLE_SUCCESS;
        }));
        root.then(literal("reload").executes(src -> {
            src.getSource().sendSuccess(() -> Component.literal("Clean Chit Chat reloaded."), false);
            return Command.SINGLE_SUCCESS;
        }));

        LiteralArgumentBuilder<CommandSourceStack> bracket = literal("bracket");
        bracket.then(literal("on").executes(src -> {
            removeBrackets = false;
            src.getSource().sendSuccess(() -> Component.literal("Chat brackets enabled."), false);
            return Command.SINGLE_SUCCESS;
        }));
        bracket.then(literal("off").executes(src -> {
            removeBrackets = true;
            src.getSource().sendSuccess(() -> Component.literal("Chat brackets removed."), false);
            return Command.SINGLE_SUCCESS;
        }));
        root.then(bracket);

        event.getDispatcher().register(root);
    }

    private void onChat(ServerChatEvent event) {
        if (!enabled || !removeBrackets) {
            return;
        }

        // Let the normal decorated chat component pass through unchanged.
        // For the vanilla formatter we only need to remove the outer < > from
        // the formatted name portion. The event message itself is the
        // decorated message before the vanilla chat wrapper is applied.
        ServerPlayer player = event.getPlayer();
        Component message = event.getMessage();

        // FTB Ranks decorates the player's display name. We replace the chat
        // component with that decorated name + separator + raw message.
        Component clean = Component.empty()
                .append(player.getDisplayName())
                .append(Component.literal(" | "))
                .append(Component.literal(event.getRawText()));

        event.setMessage(clean);
    }
}
