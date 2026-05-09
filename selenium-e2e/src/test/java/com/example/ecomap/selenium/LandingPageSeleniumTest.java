package com.example.ecomap.selenium;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Smoke UI test against a running Next.js app ({@code BASE_URL}) using a remote Chrome
 * ({@code SELENIUM_REMOTE_URL}, e.g. {@code selenium/standalone-chrome} from Docker Compose).
 */
class LandingPageSeleniumTest {

    private WebDriver driver;

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? def : v;
    }

    @BeforeEach
    void setUp() throws Exception {
        String grid = env("SELENIUM_REMOTE_URL", "http://127.0.0.1:4444");
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1280,900");
        driver = new RemoteWebDriver(URI.create(grid).toURL(), options);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void landingShowsEcoMapBranding() {
        String base = env("BASE_URL", "http://127.0.0.1:3000").replaceAll("/$", "");
        driver.get(base + "/");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(d -> d.getPageSource().contains("EcoMap Invest"));
    }
}
