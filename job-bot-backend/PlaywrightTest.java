import com.microsoft.playwright.*;
public class PlaywrightTest {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate("https://www.naukri.com/nlogin/login");
            System.out.println("Opened page");
            page.waitForFunction("() => document.cookie.includes('nauk_at')", new Page.WaitForFunctionOptions().setTimeout(30_000));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
