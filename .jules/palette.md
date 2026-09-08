## 2024-05-07 - Accessible Navigation Active States
**Learning:** Using a visual class like `is-active` is insufficient for screen readers to understand which page is currently being viewed in a navigation menu. They require an explicit semantic attribute to indicate the current context.
**Action:** Always pair visual active classes with `aria-current="page"` on the active navigation link to provide full context to assistive technologies.
## 2024-05-08 - Skip-to-Content Link Visibility
**Learning:** A "skip-to-content" link was present in the HTML but lacked styling, meaning it wasn't visible when focused via keyboard navigation. This defeats the purpose of the skip link for sighted keyboard users.
**Action:** When adding or maintaining a skip-to-content link, ensure it has specific `:focus` styles that make it visible and easily readable when receiving keyboard focus.
## 2026-05-10 - [Interactive CLI Help Menus]
**Learning:** Admin help menus in legacy command systems (like Bukkit/Paper) are often static text, forcing users to manually type complex subcommands.
**Action:** Upgrade static text help menus to use Rich Components with `ClickEvent.suggestCommand()` and `HoverEvent.showText()` to reduce friction and typos for command-line users.
## 2026-05-10 - [Interactive CLI Usage Error Catching]\n**Learning:** Implementing interactive components (like clickable auto-fill suggestions) on error handling messages transforms static frustration points into helpful recovery paths.\n**Action:** Use `CommandUtil.sendInteractiveUsage()` when catching command formatting errors in Bukkit to allow users to quickly fix their typos with a single click.
## 2026-05-15 - [Interactive CLI Error Messages]
**Learning:** When users mistype a command (like missing an argument in /revive), the default static error message forces them to re-type the whole thing. By using Kyori Adventure Rich Components (via CommandUtil.sendInteractiveUsage) for the usage error message, they can just click the error text to auto-fill the command in their chat bar.
**Action:** Update static command error messages to use sendInteractiveUsage so that mistakes are easily correctable.
## 2026-05-15 - [Interactive CLI Argument Error Catching]
**Learning:** Static argument validation error messages (like "Invalid number" or "Out of bounds") force users to completely re-type the command. By replacing `sendMessage` with `CommandUtil.sendInteractiveUsage()` for these errors, users can simply click the error message to auto-fill the command and correct their mistake, significantly reducing friction.
**Action:** When catching `NumberFormatException` or validating numeric bounds in command arguments, use `sendInteractiveUsage()` with a pre-filled suggestion (e.g. `/psetlives <player> `) to create a helpful recovery path instead of a static frustration point.
## 2026-05-15 - [Interactive CLI Number Parsing Errors]
**Learning:** Number parsing errors (e.g., NumberFormatException) in legacy command systems often default to static "Invalid number" messages. By refactoring methods like `parseIntOrError` to accept a base `suggestCmd`, these static errors can be upgraded to use `CommandUtil.sendInteractiveUsage`, allowing users to quickly correct invalid numeric inputs via a clickable chat component.
**Action:** Always provide the base command context when catching input format errors to build actionable auto-fill error suggestions.

## 2026-05-18 - [Interactive CLI Coordinate Copying]
**Learning:** Players frequently need to share or navigate to coordinates displayed in chat (like death locations in obituaries), but manually transcribing them into waypoints or chat is tedious and error-prone.
**Action:** When displaying coordinates in chat via MiniMessage, wrap them in `<click:copy_to_clipboard:'X Y Z'>` and a `<hover>` prompt to allow instant, frictionless copying.
## 2026-05-29 - [Interactive CLI Coordinate Copying]
**Learning:** Players frequently need to navigate to locations displayed in chat (like death locations in death messages), but manually transcribing coordinates is tedious and error-prone.
**Action:** When displaying coordinates in chat via MiniMessage, wrap them in `<click:copy_to_clipboard:'X Y Z'>` and a `<hover>` prompt to allow instant, frictionless copying for players.
## 2026-05-30 - [Interactive CLI Coordinate Copying]
**Learning:** Players frequently need to navigate to locations displayed in chat (like death locations in death messages), but manually transcribing coordinates is tedious and error-prone.
**Action:** When displaying coordinates in chat via MiniMessage, wrap them in `<click:copy_to_clipboard:'X Y Z'>` and a `<hover>` prompt to allow instant, frictionless copying for players.
## 2026-06-02 - [Configurable Console Error Messages]
**Learning:** Hardcoding standard API error strings (like "Only players can use this command") breaks consistency for server administrators running commands via console.\n**Action:** When migrating hardcoded console-only rejection messages, always use MessageUtil.get("command-only-players") to allow localization.
## 2026-06-03 - [Interactive CLI Number Parsing Errors with RPCommandOutput]
**Learning:** In the `RPConfigCommand` class, number parsing errors (e.g., `NumberFormatException` when parsing timer values) fall back to static error messages. Since this command uses Kyori Adventure MiniMessage implicitly by resolving custom UI strings via `RPCommandOutput`, we can't use the standard `CommandUtil.sendInteractiveUsage` method, which assumes Legacy (`&`) codes and Bukkit CommandSender execution immediately.
**Action:** Replace static error assignments in `RPCommandOutput.message` directly with MiniMessage-compatible `<click:suggest_command:...>` and `<hover:show_text:...>` components so that players can interactively recover from invalid timer setups without re-typing.
## 2026-06-03 - [Interactive CLI Time Formatting Errors]
**Learning:** Static time format parsing errors force users to completely re-type complex commands (like `/psadmin grace set <player> <time>`). By replacing `sendMessage` with `CommandUtil.sendInteractiveUsage()` for these errors, users can click the error message to auto-fill the command with the context pre-filled, significantly reducing friction.
**Action:** When validating time format arguments and catching invalid inputs, use `sendInteractiveUsage()` with a pre-filled suggestion (e.g. `/psadmin grace set <player> `) to create a helpful recovery path instead of a static frustration point.
## 2026-06-11 - [Interactive CLI MiniMessage Errors]
**Learning:** Legacy CLI systems often fail silently when commands are executed from the console but meant only for players. Hardcoded strings for error messages (like `cmdSender.sendMessage("This command can only be run by a player.")`) break consistency across localized servers. Furthermore, when users misconfigure arguments, standard static usage errors force complete retyping of the command, increasing friction.
**Action:** When commands are restricted to players, always fallback to localized strings like `MessageUtil.get("command-only-players")`. For static usage errors (like `/trust <action> [player]`), leverage Kyori Adventure MiniMessage components (`<click:suggest_command:'...'><hover:show_text:'...'>`) to provide clickable auto-fill recovery paths.
## 2024-06-14 - [Interactive CLI Base Command Errors]
**Learning:** When users execute base commands with complex sub-argument trees (like `/revivalconfig`) without any arguments, standard static usage errors force them to completely retype the command. In Brigadier, the `.executes()` block on the literal node itself is the perfect place to inject interactive recovery.
**Action:** Replace static `sendResult(..., fail("..."))` calls in base command `.executes()` blocks with native interactive components (`MutableComponent.append()`) containing `.withClickEvent(SUGGEST_COMMAND)` to instantly provide users with a pre-filled chat bar.
## 2024-05-10 - [Interactive CLI Log Copying]
**Learning:** Displaying administrative logs or records in chat via the Paper module as legacy colored text makes it difficult for users to copy them.
**Action:** When displaying administrative logs or records in chat, convert legacy colored text into a Kyori Adventure `Component` with a `clickEvent` (`copyToClipboard`) and a `hoverEvent` to allow frictionless copying of log entries without manual highlighting.
## 2026-06-21 - [Interactive CLI UUID Copying]
**Learning:** Displaying player UUIDs in chat as static text in administrative commands makes copying them difficult and error-prone.
**Action:** Replace static UUID text with Kyori Adventure Components (`copyToClipboard` and `showText`) to allow server administrators to click and copy the UUID instantly.
## 2024-06-22 - [Interactive CLI UUID Copying]
**Learning:** Displaying player UUIDs in chat as legacy colored text makes copying difficult for admins.
**Action:** Convert legacy colored text into a Kyori Adventure Component with a clickEvent (copyToClipboard) and hoverEvent.
## 2024-06-25 - [Interactive CLI Security Recovery]
**Learning:** Security error messages that provide resolution steps (like permission nodes or commands) as plain text force users to manually type them out, causing friction.
**Action:** Replace plain text resolution steps in security block messages with interactive Kyori components (e.g., `suggestCommand` for `/deop`, `copyToClipboard` for permission nodes) to allow quick, single-click recovery.
## 2026-06-25 - [Interactive CLI Base Command Errors]
**Learning:** When users execute base commands with sub-argument trees (like `/revive`) without any arguments, standard static usage errors force them to completely retype the command. In Brigadier, the `.executes()` block on the literal node itself is the perfect place to inject interactive recovery.
**Action:** Replace static `sendFailure(...)` calls in base command `.executes()` blocks with native interactive components containing `SUGGEST_COMMAND` click events to instantly provide users with a pre-filled chat bar.
## 2026-06-25 - [Interactive CLI Number Parsing Errors with RPCommandOutput]
**Learning:** In the `RPConfigCommand` class, legacy error messages used static `>>...<<` markers which were not interactive. By swapping these out for a helper method that escapes `<` and `>` input and uses `<click:suggest_command:'...'><hover:show_text:'...'>...`, players can now click errors to have the command auto-filled in their chat bar, preventing them from needing to type it all out again.
**Action:** Replace unescaped static error markers with Kyori Adventure MiniMessage clickable recovery paths when catching syntax or config-related argument failures.
## 2026-06-25 - [Interactive CLI Number Parsing Errors with RPCommandOutput] (Fabric CI Fix)
**Learning:** When trying to update the legacy error markers to use Kyori Adventure MiniMessage components and applying hover/click styles in the Fabric implementation, calling `.styled(...)` directly on the interface `net.minecraft.text.Text` causes compilation errors because the modern Fabric API treats `Text` as immutable. You must call `.copy()` first to get a `MutableText`.
**Action:** When porting chat interactivity or applying `.styled(...)` to an existing `Text` object in Fabric, always insert `.copy()` first.
## 2026-07-10 - [Interactive CLI Name Linking]
**Learning:** Players often want to take follow-up actions (like checking status) on users listed in chat output (like obituaries). By making usernames clickable with a suggest_command, we reduce the friction of typing out another command manually.
**Action:** When displaying lists of players or events involving players in chat, wrap the usernames in a clickable component that suggests a logical follow-up command (e.g., /pstatus).
## 2026-07-15 - [Interactive CLI Name Linking in Trust List]
**Learning:** Players checking their trust list often want to check the status of trusted or blocked players using `/pstatus`.
**Action:** When displaying the trust list in `/trust info`, wrap the usernames in a clickable component that suggests a logical follow-up command (e.g., `/pstatus <player>`).
