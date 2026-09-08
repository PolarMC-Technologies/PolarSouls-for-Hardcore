## 2026-05-27 - Interactive Coordinates Port
**Learning:** When porting interactive text components from Bukkit/Paper (MiniMessage) to Fabric/Forge, you must manually construct the click and hover events using `Text.literal` (Fabric) or `Component.literal` (Forge/NeoForge) and style them using the platform's specific styling classes (`Formatting` and `ChatFormatting` respectively). Ensure styles don't bleed into appended text components by explicitly resetting the style (e.g. `.withBold(false)`).
**Action:** When porting chat/text features, check if they use interactive elements like hover/click and reconstruct them using the appropriate platform's text component API. Use explicit style resets on appended components.
## 2026-06-09 - Porting Head Tracking to Fabric/Forge
**Learning:** When porting entity and block tracking logic from NeoForge to Fabric/Forge (specifically tracking dropped items and block locations via `GlobalPos`), we must adapt to loader-specific save formats and registries. In Fabric, `GlobalPos` dimension keys are serialized using `dimension.getValue().toString()` and resolved via `RegistryKey.of(RegistryKeys.WORLD, Identifier)`. In Forge/NeoForge, it uses `dimension.location().toString()` and `ResourceKey.create(Registries.DIMENSION, ResourceLocation)`.
**Action:** When porting NBT serialization/deserialization logic involving custom dimensions or worlds across loaders, check the platform's specific registry identification classes (`Identifier` vs `ResourceLocation`) to ensure correct world reconstruction.
## 2026-06-16 - Porting click-to-copy admin logs
**Learning:** When porting click-to-copy log components from Paper to Fabric/Forge command responses, you must check if the source is a player (`isExecutedByPlayer()` in Fabric, `isPlayer()` in Forge/NeoForge) before attaching click/hover events to the components, while keeping the console output simple.
**Action:** Always provide fallback simple text responses for console execution when returning rich interactive UI elements in command outputs.
## 2026-07-03 - Interactive Security Block Parity
**Learning:** When porting interactive command responses across platforms, explicitly check if the command source is a player (`isExecutedByPlayer()` in Fabric, `isPlayer()` in Forge/NeoForge) before attaching rich text events (e.g. click/hover), providing a simple fallback text for console execution.
**Action:** Always verify command source context for interactive parity to prevent exceptions on headless environments.
## 2026-07-11 - Command Aliases
**Learning:** In Bukkit/Paper, command aliases must be declaratively defined in the `plugin.yml` configuration using the `aliases: [alias_name]` property under the root command definition, rather than duplicating the Java executor registration as is done in Forge, NeoForge, and Fabric environments.
**Action:** When porting command aliases from mod loaders to Paper, directly edit `plugin.yml` instead of modifying Java command registration logic.
## 2026-07-15 - Immutable Component Styling in Forge
**Learning:** When applying styles (`.withStyle()`) to an existing `Component` returned by a utility like `MessageUtil.get()` in Forge and NeoForge, the returned component is immutable. Calling `.withStyle()` directly causes compilation errors because the method requires a `MutableComponent`.
**Action:** When applying styles or adding click/hover events to a retrieved base `Component` in Forge/NeoForge, always wrap it using `Component.empty().append(...)` to get a `MutableComponent` before chaining `.withStyle()`.
