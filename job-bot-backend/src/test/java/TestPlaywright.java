import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestPlaywright {
    public static void main(String[] args) throws Exception {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false)
            );
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            );
            Page page = context.newPage();
            page.navigate("https://www.naukri.com/java-developer-jobs-in-bangalore?experience=1-3");
            page.waitForTimeout(5000);
            String html = page.content();
            Files.writeString(Paths.get("naukri_dom.txt"), html);
            System.out.println("HTML saved to naukri_dom.txt (Length: " + html.length() + ")");
            browser.close();
        }
    }
}
