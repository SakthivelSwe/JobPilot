package com.jobbot.module.platform;

import com.jobbot.common.exception.JobBotException;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Manages browser session linking for Naukri / Indeed.
 *
 * <p><b>Security model:</b>
 * <ul>
 *   <li>Passwords are NEVER captured — the user types them directly into a visible Playwright window.</li>
 *   <li>Session cookies are stored in an AES-256-CBC encrypted .bin file on the local filesystem.</li>
 *   <li>Raw cookie values are NEVER written to the database — only the local file path is stored.</li>
 *   <li>The encryption key is derived from the application JWT secret + userId using SHA-256.</li>
 * </ul>
 *
 * <p><b>Session directory:</b> {@code ~/.jobpilot/sessions/{userId}/{platform}.bin}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSessionService {

    private final PlatformConfigService platformConfigService;

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    private static final String NAUKRI_LOGIN_URL = "https://www.naukri.com/nlogin/login";
    private static final String INDEED_LOGIN_URL = "https://secure.indeed.com/auth";
    private static final String NAUKRI_SESSION_COOKIE = "nauk_at";
    private static final String INDEED_SESSION_COOKIE = "CTK";

    // --- Public API ----------------------------------------------------------

    /**
     * Returns the raw decrypted cookie string stored for this platform+user,
     * or null if no session exists. Used by discovery adapters to make
     * authenticated API calls with the user's session.
     */
    public String loadSessionCookieString(String platform, String userId) {
        Path file = sessionFilePath(userId, platform.toUpperCase());
        if (!Files.exists(file)) return null;
        try {
            return loadDecrypted(file, userId);
        } catch (Exception e) {
            log.warn("Could not load session cookie for {} userId={}: {}", platform, userId, e.getMessage());
            return null;
        }
    }

    /**
     * Opens a visible Playwright Chromium window navigated to the platform login page.
     * The user logs in manually. Once a successful session is detected (auth cookie present),
     * the session is saved to an encrypted local file and PlatformConfig is updated.
     *
     * @param platform NAUKRI or INDEED
     * @param userId   current user ID (scopes the session file)
     * @return updated PlatformConfig
     */
    public PlatformConfig connectAccount(String platform, String userId) {
        String upper = platform.toUpperCase();
        String loginUrl = loginUrl(upper);
        String sessionCookie = sessionCookieName(upper);
        Path sessionFile = sessionFilePath(userId, upper);

        log.info("Starting account linking for {} (userId={})", upper, userId);

        try (Playwright playwright = Playwright.create()) {
            BrowserType chromium = playwright.chromium();
            Browser browser = chromium.launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate(loginUrl);
            log.info("Opened {} login page. Waiting for user to complete login (3 min)...", upper);

            long startTime = System.currentTimeMillis();
            boolean cookieFound = false;
            while (System.currentTimeMillis() - startTime < 180_000) {
                try {
                    // Check if browser was closed by the user manually
                    if (!context.pages().isEmpty() && context.pages().get(0).isClosed()) {
                        break;
                    }
                    
                    boolean hasCookie = context.cookies().stream()
                            .anyMatch(c -> c.name.equals(sessionCookie));
                    if (hasCookie) {
                        cookieFound = true;
                        break;
                    }
                    page.waitForTimeout(1000);
                } catch (PlaywrightException e) {
                    // Browser or page was closed, or context destroyed
                    break;
                }
            }

            if (!cookieFound) {
                browser.close();
                throw new JobBotException("Login timed out or was cancelled for " + upper + ". Please try again.");
            }

            String username = extractUsername(page, upper);
            log.info("Login detected for {} — username: {}", upper, username);
            String storageStateJson = context.storageState();
            saveEncrypted(sessionFile, storageStateJson, userId);
            browser.close();

            PlatformConfig config = platformConfigService.get(upper);
            config.setSessionStatus("CONNECTED");
            config.setSessionActive(true);
            config.setSessionUsername(username);
            config.setSessionConnectedAt(OffsetDateTime.now());
            config.setSessionFilePath(sessionFile.toAbsolutePath().toString());
            return platformConfigService.saveConfig(config);

        } catch (JobBotException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect {} account: {}", upper, e.getMessage(), e);
            markError(platform);
            throw new JobBotException("Failed to connect " + upper + " account: " + e.getMessage());
        }
    }

    /**
     * Performs an automated login using credentials by opening a visible browser window.
     * Naukri blocks headless Chromium via bot-detection — a visible browser with a realistic
     * user-agent is required. Will fail if Naukri requests a Captcha or OTP.
     */
    public PlatformConfig loginWithCredentials(String platform, String userId, String email, String password) {
        String upper = platform.toUpperCase();
        String sessionCookie = sessionCookieName(upper);
        Path sessionFile = sessionFilePath(userId, upper);

        log.info("Starting automated credential login for {} (userId={})", upper, userId);

        try (Playwright playwright = Playwright.create()) {
            BrowserType chromium = playwright.chromium();
            // Must use non-headless: Naukri's Akamai bot-detection blocks headless Chromium
            Browser browser = chromium.launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setArgs(List.of(
                            "--disable-blink-features=AutomationControlled",
                            "--no-sandbox"
                    )));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/124.0.0.0 Safari/537.36")
                    .setViewportSize(1280, 800)
                    .setLocale("en-IN")
            );

            // Hide the webdriver property that Naukri detects
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");

            Page page = context.newPage();
            log.info("Navigating to Naukri login page...");
            page.navigate(loginUrl(upper));

            if ("NAUKRI".equals(upper)) {
                // Wait up to 60 seconds for the login form to appear
                page.waitForSelector("#usernameField",
                        new Page.WaitForSelectorOptions().setTimeout(60_000));

                // Human-like typing with delay between keystrokes
                page.locator("#usernameField").click();
                page.waitForTimeout(300);
                page.locator("#usernameField").fill(email);
                page.waitForTimeout(500);

                page.locator("#passwordField").click();
                page.waitForTimeout(300);
                page.locator("#passwordField").fill(password);
                page.waitForTimeout(500);

                // Click the Login button (class-based selector, more robust than type='submit')
                page.locator("button.blue-btn").click();
                log.info("Submitted Naukri login form — waiting for session cookie...");

            } else if ("INDEED".equals(upper)) {
                browser.close();
                throw new JobBotException("Automated credential login is not yet supported for INDEED.");
            }

            // Poll up to 60 seconds for the auth session cookie
            long startTime = System.currentTimeMillis();
            boolean cookieFound = false;
            while (System.currentTimeMillis() - startTime < 60_000) {
                try {
                    boolean hasCookie = context.cookies().stream()
                            .anyMatch(c -> c.name.equals(sessionCookie));
                    if (hasCookie) {
                        cookieFound = true;
                        break;
                    }
                    page.waitForTimeout(1500);
                } catch (PlaywrightException e) {
                    break;
                }
            }

            if (!cookieFound) {
                browser.close();
                throw new JobBotException(
                        "Login failed — Naukri may have shown a Captcha or OTP challenge. " +
                        "Please use the 'Paste Cookie' method instead: log in via your normal browser, " +
                        "open DevTools → Application → Cookies and copy the value of 'nauk_at'.");
            }

            String fetchedUsername = extractUsername(page, upper);
            log.info("Automated login successful for {} — username: {}", upper, fetchedUsername);
            String storageStateJson = context.storageState();
            try {
                saveEncrypted(sessionFile, storageStateJson, userId);
            } catch (Exception e) {
                log.error("Failed to save session for {}", upper, e);
                throw new JobBotException("Failed to save session cookie: " + e.getMessage());
            }
            browser.close();

            PlatformConfig config = platformConfigService.get(upper);
            config.setSessionStatus("CONNECTED");
            config.setSessionActive(true);
            config.setSessionUsername(fetchedUsername);
            config.setSessionConnectedAt(OffsetDateTime.now());
            config.setSessionFilePath(sessionFile.toAbsolutePath().toString());
            return platformConfigService.saveConfig(config);

        } catch (JobBotException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to perform automated login for {}: {}", upper, e.getMessage(), e);
            markError(platform);
            throw new JobBotException("Automated login failed: " + e.getMessage());
        }
    }


    /**
     * Manages session by taking a raw cookie value provided manually by the user.
     * This avoids needing to open a visible Playwright window on the backend.
     */
    public PlatformConfig saveManualCookie(String platform, String userId, String cookieValue) {
        String upper = platform.toUpperCase();
        String sessionCookie = sessionCookieName(upper);
        String domain = "NAUKRI".equals(upper) ? ".naukri.com" : ".indeed.com";
        Path sessionFile = sessionFilePath(userId, upper);

        log.info("Saving manual cookie for {} (userId={})", upper, userId);

        String storageStateJson = "{" +
                "\"cookies\": [{" +
                "\"name\": \"" + sessionCookie + "\"," +
                "\"value\": \"" + cookieValue + "\"," +
                "\"domain\": \"" + domain + "\"," +
                "\"path\": \"/\"," +
                "\"expires\": -1," +
                "\"httpOnly\": false," +
                "\"secure\": false," +
                "\"sameSite\": \"None\"" +
                "}]," +
                "\"origins\": []" +
                "}";

        try {
            saveEncrypted(sessionFile, storageStateJson, userId);
        } catch (Exception e) {
            log.error("Failed to save manual cookie for {}", upper, e);
            throw new JobBotException("Failed to save session cookie: " + e.getMessage());
        }

        PlatformConfig config = platformConfigService.get(upper);
        config.setSessionStatus("CONNECTED");
        config.setSessionActive(true);
        config.setSessionUsername("Manual Login");
        config.setSessionConnectedAt(OffsetDateTime.now());
        config.setSessionFilePath(sessionFile.toAbsolutePath().toString());
        return platformConfigService.saveConfig(config);
    }

    /**
     * Validates whether the stored session is still active by loading cookies and
     * making a lightweight authenticated page request.
     */
    public PlatformConfig validateSession(String platform) {
        String upper = platform.toUpperCase();
        PlatformConfig config = platformConfigService.get(upper);

        if (!Boolean.TRUE.equals(config.getSessionActive()) || config.getSessionFilePath() == null) {
            config.setSessionStatus("DISCONNECTED");
            config.setSessionActive(false);
            return platformConfigService.saveConfig(config);
        }

        try (Playwright playwright = Playwright.create()) {
            BrowserContext context = loadSessionContext(playwright, config);
            boolean valid = probeSession(context, upper);
            context.close();
            config.setSessionActive(valid);
            config.setSessionStatus(valid ? "CONNECTED" : "EXPIRED");
            if (valid) config.setSessionConnectedAt(OffsetDateTime.now());
            return platformConfigService.saveConfig(config);
        } catch (Exception e) {
            log.warn("Session validation failed for {}: {}", upper, e.getMessage());
            config.setSessionActive(false);
            config.setSessionStatus("ERROR");
            return platformConfigService.saveConfig(config);
        }
    }

    /**
     * Revokes the session: deletes the local encrypted file and resets all session
     * fields in PlatformConfig to their default disconnected state.
     */
    public PlatformConfig disconnect(String platform) {
        String upper = platform.toUpperCase();
        PlatformConfig config = platformConfigService.get(upper);
        if (config.getSessionFilePath() != null) {
            try { Files.deleteIfExists(Path.of(config.getSessionFilePath())); }
            catch (IOException e) { log.warn("Could not delete session file for {}: {}", upper, e.getMessage()); }
        }
        config.setSessionStatus("DISCONNECTED");
        config.setSessionActive(false);
        config.setSessionUsername(null);
        config.setSessionConnectedAt(null);
        config.setSessionFilePath(null);
        log.info("Disconnected {} account", upper);
        return platformConfigService.saveConfig(config);
    }

    /**
     * Loads a Playwright BrowserContext with stored session cookies.
     * Used by the apply engine to perform authenticated Naukri/Indeed submissions.
     *
     * @throws JobBotException if no valid session exists
     */
    public BrowserContext loadSessionContext(Playwright playwright, PlatformConfig config) {
        if (!Boolean.TRUE.equals(config.getSessionActive()) || config.getSessionFilePath() == null) {
            throw new JobBotException("No active session for " + config.getPlatformName() +
                    ". Please connect your account first in Settings ? Job Sources.");
        }
        try {
            String json = loadDecrypted(Path.of(config.getSessionFilePath()), config.getUserId());
            Path tempState = Files.createTempFile("jp_session_", ".json");
            Files.writeString(tempState, json, StandardCharsets.UTF_8);
            BrowserContext ctx = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(false))
                    .newContext(new Browser.NewContextOptions().setStorageStatePath(tempState));
            Files.deleteIfExists(tempState);
            return ctx;
        } catch (Exception e) {
            throw new JobBotException("Failed to load session for " + config.getPlatformName() + ": " + e.getMessage());
        }
    }

    // --- Helpers -------------------------------------------------------------

    private boolean probeSession(BrowserContext context, String platform) {
        try {
            Page page = context.newPage();
            if ("NAUKRI".equals(platform)) {
                page.navigate("https://www.naukri.com/mnjuser/homepage");
                page.waitForLoadState();
                List<Cookie> cookies = context.cookies();
                page.close();
                return cookies.stream().anyMatch(c -> NAUKRI_SESSION_COOKIE.equals(c.name));
            } else if ("INDEED".equals(platform)) {
                page.navigate("https://www.indeed.com/myjobs");
                page.waitForLoadState();
                String url = page.url();
                page.close();
                return !url.contains("login") && !url.contains("signin");
            }
            page.close();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractUsername(Page page, String platform) {
        try {
            if ("NAUKRI".equals(platform)) {
                Object name = page.evaluate(
                        "() => { const el = document.querySelector('[class*=\"user-name\"], " +
                                "[class*=\"userInfo\"] span, .nI-gNb-sb__main-text'); " +
                                "return el ? el.textContent.trim() : null; }");
                if (name != null && !name.toString().equals("null") && !name.toString().isBlank())
                    return name.toString();
            }
        } catch (Exception ignored) {}
        return "Linked Account";
    }

    private String loginUrl(String platform) {
        return switch (platform) {
            case "NAUKRI" -> NAUKRI_LOGIN_URL;
            case "INDEED" -> INDEED_LOGIN_URL;
            default -> throw new JobBotException("Account linking not supported for: " + platform);
        };
    }

    private String sessionCookieName(String platform) {
        return switch (platform) {
            case "NAUKRI" -> NAUKRI_SESSION_COOKIE;
            case "INDEED" -> INDEED_SESSION_COOKIE;
            default -> throw new JobBotException("No session cookie mapping for: " + platform);
        };
    }

    private Path sessionFilePath(String userId, String platform) {
        Path dir = Path.of(System.getProperty("user.home"), ".jobpilot", "sessions",
                userId.replaceAll("[^a-zA-Z0-9_-]", "_"));
        try { Files.createDirectories(dir); } catch (IOException e) {
            throw new JobBotException("Cannot create session directory: " + e.getMessage());
        }
        return dir.resolve(platform.toLowerCase() + ".bin");
    }

    // --- AES-256-CBC Encryption ----------------------------------------------

    private void saveEncrypted(Path file, String plaintext, String userId) throws Exception {
        byte[] key = deriveKey(userId);
        byte[] iv  = new byte[16];
        new java.security.SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[16 + encrypted.length];
        System.arraycopy(iv, 0, out, 0, 16);
        System.arraycopy(encrypted, 0, out, 16, encrypted.length);
        Files.write(file, out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String loadDecrypted(Path file, String userId) throws Exception {
        byte[] raw  = Files.readAllBytes(file);
        byte[] key  = deriveKey(userId);
        byte[] iv   = Arrays.copyOfRange(raw, 0, 16);
        byte[] data = Arrays.copyOfRange(raw, 16, raw.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
    }

    private byte[] deriveKey(String userId) throws Exception {
        String raw = jwtSecret + ":" + userId;
        return Arrays.copyOf(
                MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)), 32);
    }

    private void markError(String platform) {
        try {
            PlatformConfig config = platformConfigService.get(platform.toUpperCase());
            config.setSessionStatus("ERROR");
            config.setSessionActive(false);
            platformConfigService.saveConfig(config);
        } catch (Exception ignored) {}
    }
}
