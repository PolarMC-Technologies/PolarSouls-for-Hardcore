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
import org.ssoggy.ssoggysouls.hrm.dlc.enums.GAMEMODESENUM;
import org.ssoggy.ssoggysouls.hrm.dlc.enums.COMMANDOUTPUTENUM;
import org.ssoggy.ssoggysouls.hrm.dlc.util.RPCommandOutput;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
// Note to self, rewrite again later

public class GhostModeCommand implements CommandExecutor, TabCompleter { // Lazy Command Write

    @Override
    public boolean onCommand(@NotNull CommandSender cmdSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        RPCommandOutput result = new RPCommandOutput();
        OfflinePlayer playerArg = Bukkit.getOfflinePlayer(args.length == 0 ? cmdSender.getName() : args[0]);

        if (playerArg.getPlayer() instanceof Player onlinePlayer) {
            result.success = COMMANDOUTPUTENUM.TRUE;
            result.message = "Updated " + (onlinePlayer.getName() != null ? MiniMessage.miniMessage().escapeTags(onlinePlayer.getName()) : "Unknown") + " gamemode to GhostMode!";
            GAMEMODESENUM.setPlayerGameMode(onlinePlayer, GAMEMODESENUM.GHOSTMODE);
        }

        if (result.success == COMMANDOUTPUTENUM.NULL) {
            result.success = COMMANDOUTPUTENUM.FALSE;
            result.message = "Something went wrong, check the console for more details";
        }

        cmdSender.sendRichMessage(result.toString()); // feedback
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            return org.ssoggy.ssoggysouls.util.TabCompleteUtil.getOnlinePlayerNames(args.length > 0 ? args[0] : "");
        }
        return Collections.emptyList();
    }
}
