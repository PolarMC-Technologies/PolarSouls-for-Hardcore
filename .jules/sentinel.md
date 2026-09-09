## 2026-05-06 - [Fix OutOfMemoryError reading admin log]
**Vulnerability:** `Files.readAllLines()` was used to read the admin log file, which can grow arbitrarily large and cause an OOM error.
**Learning:** Admin log files should always be read using a streaming approach if only the most recent N lines are needed.
**Prevention:** Use a `Stream` via `Files.lines()` combined with a `Deque` to store only the last N lines.
## 2024-05-07 - [Prevent potential SQL Injection in schema generation]
**Vulnerability:** In database managers (`MySQLManager`, `SQLiteManager`), dynamic DDL methods like `createMetadataTableIfNeeded` directly concatenated string arguments (`metaTable`) into `CREATE TABLE` commands. Even though the table name was internally hardcoded, lack of validation poses an SQL injection risk if the method's signature or usage expands.
**Learning:** Defensive programming requires validating all dynamic identifiers used in non-parameterizable SQL statements (DDL).
**Prevention:** Apply the existing `isValidIdentifier` (whitelisting regex `^\w+$`) check to table and column name parameters before they are concatenated into SQL queries.

## 2025-05-15 - Prevent Log Forging / CRLF Injection in Custom Loggers
**Vulnerability:** Unsanitized user inputs (e.g., `sender`, `action` parameters) were passed directly to `PrintWriter.println()` and `Logger.info()` in `AdminLogger.java` across all module implementations (`paper`, `fabric`, `forge`, `neoforge`).
**Learning:** This exposes the application to Log Forging (CRLF Injection), where a malicious user could supply inputs containing newline (`\n`) and carriage return (`\r`) characters to insert fake log entries, obscuring legitimate audit trails or tricking log parsing systems.
**Prevention:** Always implement an input sanitization step—such as replacing `\n` and `\r` with an underscore `_`—before writing user-controlled strings to application or system logs.
## 2026-05-28 - [Prevent DoS via unbounded array allocation in network payload]
**Vulnerability:** In `ServerTransferUtil` across `fabric`, `forge`, and `neoforge` modules, `new byte[buf.readableBytes()]` was being called when decoding a custom network payload. A malicious client could send an extremely large payload length, causing the server to allocate a massive byte array and crash with an OutOfMemoryError.
**Learning:** `buf.readableBytes()` is user-controlled input when decoding packets and must be explicitly validated against a sane maximum limit before being used for memory allocation.
**Prevention:** Always enforce a strict maximum length (e.g., 1024 bytes) when reading variable-length data structures from network buffers before allocating memory.
## 2026-06-10 - [Prevent sensitive file path exposure in error messages]
**Vulnerability:** In `CommandRegistration.java` across `fabric`, `forge`, and `neoforge` modules, if an `IOException` occurred when reading the admin log file, the exception message (which includes the absolute file path) was concatenated and sent directly to the command sender. This exposes internal server directory structures to users.
**Learning:** Sending raw exception messages (like `e.getMessage()`) to user-facing outputs can leak sensitive information such as absolute file paths, database schemas, or infrastructure details.
**Prevention:** Catch the exception, log the full exception details safely to the server console (using `LOGGER.error`), and send only a generic error message (e.g., "Error reading admin log. Check console for details.") back to the user.
## 2024-03-20 - [Hardcoded Database Password Exposure]
**Vulnerability:** MySQL configuration defaults with empty or default passwords in code/documentation.
**Learning:** Default configurations can lead to insecure deployments if users don't change them.
**Prevention:** Ensure configurations force explicit password setup or use environment variables, and avoid hardcoding fallback passwords.
## 2026-06-25 - [Prevent potential sensitive information leakage in logs]
**Vulnerability:** In `LeaveLimboCommand.java`, `RPConfig.java`, and `UpdateChecker.java`, exception messages (`e.getMessage()`) were directly appended to error logs using `logger.severe()` or `logger.warning()`.
**Learning:** Appending `e.getMessage()` manually instead of passing the entire Exception object can leak sensitive information to the logs without providing a full stack trace, reducing debuggability and posing a potential security risk.
**Prevention:** Always use `logger.log(Level.SEVERE, "context message", exception)` or equivalent to properly log the context message and the full stack trace securely.
## 2026-07-28 - [Prevent MiniMessage XSS via OfflinePlayer]
**Vulnerability:** `Bukkit.getOfflinePlayer(String)` returns an `OfflinePlayer` that exactly matches the provided string argument if the user does not exist. Using its `.getName()` method directly inside MiniMessage chat outputs allows MiniMessage injection/XSS spoofing.
**Learning:** Data returned from seemingly safe Bukkit APIs like `OfflinePlayer.getName()` might still contain unmodified user input if the player does not exist.
**Prevention:** Always escape the result of `OfflinePlayer.getName()` using `MiniMessage.miniMessage().escapeTags()` before concatenating it into a MiniMessage component string.
## 2026-07-28 - [Prevent SonarCloud Failures when Copying]
**Vulnerability:** Not a direct vulnerability, but a workflow issue.
**Learning:** When trying to update or fix legacy error markers across platforms, make sure to add `.copy()` before styling `Component` instances.
**Prevention:** Apply `.copy()` before styling shared `Component` instances like `MessageUtil.get(...)` to avoid SonarCloud failures and compilation errors.
## 2026-07-28 - [Prevent SonarCloud Failures with Duplication]
**Vulnerability:** Code Smells (Duplication) in newly created cross-platform command syntax logic.
**Learning:** Repetitive component styling chains trigger "Duplication on New Code" Quality Gate failures in SonarCloud. Additionally, variables initialized with properties from configuration files can cause NullPointerExceptions if the configuration file is null.
**Prevention:** Extract complex chained style or component builders into dedicated static helper methods to prevent duplication. Add explicit null checks before using configuration objects or chaining method calls.
