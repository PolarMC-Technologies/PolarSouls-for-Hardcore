/*
RevivePlus by Cera and Jakeccz
Copyright (C) 2026 Commune

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RevivePlus.  If not, see <https://www.gnu.org/licenses/>
 */

package org.ssoggy.ssoggysouls.hrm.dlc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.ssoggy.ssoggysouls.hrm.dlc.enums.SOCIALENUM;
import org.ssoggy.ssoggysouls.hrm.dlc.enums.TRUSTENUM;
import org.ssoggy.ssoggysouls.hrm.dlc.enums.COMMANDOUTPUTENUM;
import org.ssoggy.ssoggysouls.hrm.dlc.util.RPCommandOutput;
import org.ssoggy.ssoggysouls.hrm.dlc.util.RPSocial;
import org.ssoggy.ssoggysouls.hrm.dlc.util.RPUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.ssoggy.ssoggysouls.util.MessageUtil;

import java.util.*;

public class SocialCommand implements CommandExecutor, TabCompleter {
    // ⚡ Bolt: Cache enum string mappings to avoid redundant O(N) array allocations and .toLowerCase() calls on every tab complete
    private static final List<String> TRUST_ACTIONS = TRUSTENUM.VALUES.stream().map(x -> x.name().toLowerCase(Locale.ROOT)).toList();

    public void executeFail(CommandSender cmdSender, RPCommandOutput cmdOutput) {
        cmdOutput.success = COMMANDOUTPUTENUM.FALSE;
        cmdOutput.message = "Please use <click:suggest_command:'/trust '><hover:show_text:'<gray>Click to auto-fill this command</gray>'><gray>/trust \\<action\\> [player]</gray></hover></click>";
        cmdSender.sendRichMessage(cmdOutput.toString());
    }

    public void executeFail(CommandSender cmdSender, RPCommandOutput cmdOutput, String msg) {
        cmdOutput.success = COMMANDOUTPUTENUM.FALSE;
        cmdOutput.message = msg;
        cmdSender.sendRichMessage(cmdOutput.toString());
    }

    @SuppressWarnings("java:S3516") // onCommand always returns true by design (Bukkit CommandExecutor convention)
    @Override
    public boolean onCommand(@NotNull CommandSender cmdSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        boolean failArgs = args.length < 2;
        RPCommandOutput output = new RPCommandOutput();
        if (failArgs && !(args.length == 1 && args[0].toLowerCase(Locale.ROOT).equals("info"))) {
            executeFail(cmdSender, output);
            return true;
        }

        Entity player = cmdSender instanceof Entity entity ? entity : null;
        OfflinePlayer targetPlayer = failArgs ? null : cmdSender.getServer().getOfflinePlayer(args[1].toLowerCase(Locale.ROOT).trim());
        if (!(player instanceof Player)) {
            cmdSender.sendMessage(MessageUtil.get("command-only-players"));
            return true;
        }
        UUID playerUUID = player.getUniqueId();
        if (targetPlayer == null) return true;
        UUID targetPlayerUUID = targetPlayer.getUniqueId();
        RPUtil.addUsernameToCache(playerUUID);

        RPUtil.addUsernameToCache(targetPlayerUUID);
        RPSocial social = new RPSocial(playerUUID);
        output.success = COMMANDOUTPUTENUM.TRUE;

        SocialContext ctx = new SocialContext(cmdSender, output, social, player, targetPlayer, playerUUID, targetPlayerUUID);

        if (tryRelationship(ctx, args)) {
            social.saveChanges();
        } else {
            showTrustList(output, social, targetPlayerUUID);
        }

        cmdSender.sendRichMessage(output.toString());
        return true;
    }

    /**
     * Bundles all contextual state needed by social action handlers to avoid excessive method parameters.
     */
    private record SocialContext(
            CommandSender sender,
            RPCommandOutput output,
            RPSocial social,
            Entity player,
            OfflinePlayer targetPlayer,
            UUID playerUUID,
            UUID targetPlayerUUID
    ) {}

    /**
     * Attempts to execute the trust action (block/revoke/grant).
     * Returns true if a relationship action was performed, false if should show trust list instead.
     */
    private boolean tryRelationship(SocialContext ctx, String[] args) {
        try {
            TRUSTENUM action = TRUSTENUM.valueOf(args[0].toUpperCase(Locale.ROOT).trim());
            if (action == TRUSTENUM.INFO) {
                return false; // Show trust list
            }

            RPSocial targetSocial = new RPSocial(ctx.targetPlayerUUID);
            SOCIALENUM currentRelation = ctx.social.getRelationTo(ctx.targetPlayerUUID);
            SOCIALENUM theirRelation = targetSocial.getRelationTo(ctx.playerUUID);

            boolean changed = false;
            switch (action) {
                case TRUSTENUM.BLOCK:
                    changed = handleBlock(ctx, targetSocial, currentRelation, theirRelation);
                    break;
                case TRUSTENUM.REVOKE:
                    changed = handleRevoke(ctx.output, ctx.social, ctx.targetPlayer, ctx.targetPlayerUUID, currentRelation);
                    break;
                case TRUSTENUM.GRANT:
                    changed = handleGrant(ctx, targetSocial, currentRelation, theirRelation);
                    break;
                default:
                    break;
            }
            if (changed) {
                ctx.social.saveChanges();
            }
            return true;
        } catch (IllegalArgumentException ignore) {
            return false;
        }
    }

    private boolean handleBlock(SocialContext ctx, RPSocial targetSocial,
                             SOCIALENUM currentRelation, SOCIALENUM theirRelation) {
        if (ctx.playerUUID.equals(ctx.targetPlayerUUID)) {
            executeFail(ctx.sender, ctx.output, "Player has you blocked");
            return false;
        }
        boolean changed = false;
        if (currentRelation == SOCIALENUM.BLOCKED) {
            ctx.output.success = COMMANDOUTPUTENUM.INFO;
            ctx.output.message = "You already blocked " + ctx.targetPlayer.getName(); // easter egg: You tryna double-block this dude?
        } else {
            changed |= ctx.social.setRelationTo(ctx.targetPlayerUUID, SOCIALENUM.BLOCKED);
            if (theirRelation.isTrustworthy()) {
                changed |= targetSocial.setRelationTo(ctx.playerUUID, SOCIALENUM.UNTRUSTED);
            } // Unbind both players
            ctx.output.message = "You have blocked " + ctx.targetPlayer.getName();
        }
        return changed;
    }

    private boolean handleRevoke(RPCommandOutput output, RPSocial social, OfflinePlayer targetPlayer,
                              UUID targetPlayerUUID, SOCIALENUM currentRelation) {
        boolean changed = false;
        if (currentRelation == SOCIALENUM.UNTRUSTED) {
            output.success = COMMANDOUTPUTENUM.INFO;
            output.message = "You have no relations with " + targetPlayer.getName();
        } else {
            changed |= social.setRelationTo(targetPlayerUUID, null); // Ensures that you don't get stray "Untrusted" values (saves memory)
            output.message = "You no longer trust " + targetPlayer.getName();
        }
        return changed;
    }

    private boolean handleGrant(SocialContext ctx, RPSocial targetSocial,
                             SOCIALENUM currentRelation, SOCIALENUM theirRelation) {
        if (ctx.playerUUID.equals(ctx.targetPlayerUUID)) {
            executeFail(ctx.sender, ctx.output, "Player has you blocked");
            return false;
        }

        boolean changed = false;
        if (currentRelation.isTrustworthy()) { // Already Trusted
            ctx.output.success = COMMANDOUTPUTENUM.INFO;
            ctx.output.message = "You have already entrusted " + ctx.targetPlayer.getName();
        } else if (theirRelation == SOCIALENUM.BLOCKED) { // They Blocked you
            executeFail(ctx.sender, ctx.output, "Player has you blocked.");
        } else if (theirRelation == SOCIALENUM.TRUSTED) { // Make Players Allies
            changed |= ctx.social.setRelationTo(ctx.targetPlayerUUID, SOCIALENUM.FRIENDS);
            changed |= targetSocial.setRelationTo(ctx.playerUUID, SOCIALENUM.FRIENDS);
            ctx.output.message = "You are now friends with " + ctx.targetPlayer.getName();

            if (ctx.targetPlayer.getPlayer() instanceof Player targetOnline) {
                RPCommandOutput targetMessage = new RPCommandOutput();
                targetMessage.success = COMMANDOUTPUTENUM.TRUE;
                targetMessage.message =  "You are now friends with " + ctx.player.getName();
                targetOnline.sendRichMessage(targetMessage.toString());
            }
        } else { // Trust Player
            changed |= ctx.social.setRelationTo(ctx.targetPlayerUUID, SOCIALENUM.TRUSTED);
            ctx.output.message = "You have now entrusted " + ctx.targetPlayer.getName();
        }
        return changed;
    }

    private void showTrustList(RPCommandOutput output, RPSocial social, UUID targetPlayerUUID) {
        output.success = COMMANDOUTPUTENUM.RAW;
        output.message = "\n<green>--- Trust List ---</green>\n";

        social.getRelationsToAll((k, v) -> k.equals(targetPlayerUUID)).forEach((k, v) -> {
            String username = RPUtil.getUsernameFromCache(k);
            String escapedUsername = username != null ? net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().escapeTags(username) : "Unknown";
            output.message += "- <click:suggest_command:'/pstatus " + escapedUsername + "'><hover:show_text:'<gray>Click to check player status</gray>'>" + escapedUsername + "</hover></click>: " + v + "\n";
            // Future: Make them glow locally when this command is ran
        });
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return switch (args.length) { // This is just preference
            case 0, 1 ->
                    org.ssoggy.ssoggysouls.util.TabCompleteUtil.filterStartsWith(TRUST_ACTIONS, args.length > 0 ? args[0] : "");
            case 2 -> {
                List<String> names = org.ssoggy.ssoggysouls.util.TabCompleteUtil.getOnlinePlayerNames(args[1]);
                if (sender instanceof Player p) {
                    names.remove(p.getName());
                }
                yield names;
            }
            default -> Collections.emptyList(); // Allowing unnecessary suggestions feels unfinished
        };
    }
}
