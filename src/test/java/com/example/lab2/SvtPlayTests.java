package com.example.lab2;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;

public class SvtPlayTests {

    private static WebDriver driver;

    @BeforeAll
    static void setup() {
        var options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*", "--incognito");
        driver = new ChromeDriver(options);

        driver.get("https://www.svtplay.se/");
        acceptCookieConsentModal();
    }

    static void acceptCookieConsentModal() {

        var modalXpath = "//div[@data-rt='cookie-consent-modal']";

        // The modal takes a couple of seconds to show up
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(modalXpath)));

        var modal = driver.findElement(By.xpath(modalXpath));
        var consentButton = modal.findElement(By.xpath(".//button[text() = 'Acceptera alla']"));
        consentButton.click();

        // The modal takes a couple of seconds to close after accepting
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(modalXpath)));
    }

    @BeforeEach
    void navigate() {
        driver.get("https://www.svtplay.se/");
    }

    @AfterAll
    static void teardown() {
        driver.quit();
    }

    @Test
    void isTitleCorrect() {
        var title = driver.getTitle();
        var expectedTitle = "SVT Play";

        Assertions.assertEquals(expectedTitle, title);
    }

    @Test
    void isLogoVisible() {
        var logoXpath = "//*[name()='svg']/*[name()='title' and text()='SVT Play logotyp']";
        var logo = driver.findElement(By.xpath(logoXpath));

        Assertions.assertTrue(logo.isDisplayed());
    }

    @Test
    void areMainLinksOnStartPageCorrect() {
        // Actual values are Pascal cased but then upper-cased using CSS
        var expectedStartText = "START";
        var startTextXpath = "//li[@type='start']/a";

        var expectedProgramsText = "PROGRAM";
        var expectedProgramsXpath = "//li[@type='programs']/a";

        var expectedChannelsText = "KANALER";
        var expectedChannelsXpath = "//li[@type='channels']/a";

        var startText = driver.findElement(By.xpath(startTextXpath));
        Assertions.assertEquals(expectedStartText, startText.getText());

        var programsText = driver.findElement(By.xpath(expectedProgramsXpath));
        Assertions.assertEquals(expectedProgramsText, programsText.getText());

        var channelsText = driver.findElement(By.xpath(expectedChannelsXpath));
        Assertions.assertEquals(expectedChannelsText, channelsText.getText());

    }

    @Test
    void checkAvailabilityLink() {
        var expectedLinkText = "Tillgänglighet i SVT Play";
        var linkXpath = "//a[@href='https://kontakt.svt.se/guide/tillganglighet']";

        var link = driver.findElement(By.xpath(linkXpath));

        Assertions.assertTrue(link.isDisplayed());

        // Selenium cannot CSS select using compound class names,
        // so we work around it by concatenating the names
        var linkSpan = link.findElement(By.cssSelector(".sc-343fed33-3.dmRxHt"));
        Assertions.assertEquals(expectedLinkText, linkSpan.getText());
    }

    @Test
    void verifyHeadingInAvailabilitySite() {
        var expectedText = "Så arbetar SVT med tillgänglighet";
        var linkXpath = "//a[@href='https://kontakt.svt.se/guide/tillganglighet']";

        // Navigate to the availability site
        driver.findElement(By.xpath(linkXpath)).click();

        var heading = driver.findElement(By.tagName("h1"));
        Assertions.assertEquals(expectedText, heading.getText());
    }

    @Test
    void checkNumberOfCategoriesOnProgramsPage() {
        var programsXpath = "//li[@type='programs']/a";

        // Navigate to the Programs page
        driver.findElement(By.xpath(programsXpath)).click();

        var expectedNoOfCategories = 18;
        var categoriesXpath = "//article";

        // NOTE: Had to add an implicit wait since the categories/articles seem to
        //       take some time to load...
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

        var categories = driver.findElements(By.xpath(categoriesXpath));

        Assertions.assertEquals(expectedNoOfCategories, categories.size());
    }

    @Test
    void verifyThatCookieSettingIsRespected() {

        // Grab the cookie consent cookie (Inception...)
        var cookie = driver.manage().getCookieNamed("cookie-consent-1").toJson();

        // We accepted all cookies in the setup
        var expectedInitialAdStorageConsent = true;
        var adStorageConsent = false;

        try {
            adStorageConsent = (boolean) new org.json.JSONObject(cookie.get("value").toString()).get("ad_storage");
        } catch (JSONException e) {
            // Invalid json in the cookie. We handle it in the assertion.
        }

        Assertions.assertEquals(expectedInitialAdStorageConsent, adStorageConsent);

        // Navigate to the Settings page
        var settingsLinkXpath = "//a[@class='sc-5b00349a-0 hwpvwu sc-87f10045-4 imzlFR' and @href='/installningar']";
        driver.findElement(By.xpath(settingsLinkXpath)).click();

        // Open the cookie consent dialog
        var cookieButtonClass = ".sc-5b00349a-2.hLpVUw";
        driver.findElement(By.cssSelector(cookieButtonClass)).click();

        // Wait for the element to show up
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("play_cookie_consent_ad_storage")));

        // Toggle the ad storage consent switch
        var consentSwitchXpath = "//label[@for='play_cookie_consent_ad_storage']";
        driver.findElement(By.xpath(consentSwitchXpath)).click();

        // Save the new cookie preferences
        driver.findElement(By.cssSelector(".sc-5b00349a-2.fuGbXH.sc-4f221cd2-9.hEiUxP")).click();

        // The modal takes a couple of seconds to close after accepting
        var modalXpath = "//div[@data-rt='cookie-consent-modal']";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(modalXpath)));

        // Grab the cookie again
        cookie = driver.manage().getCookieNamed("cookie-consent-1").toJson();

        var expectedAdStorageConsent = false;
        var adStorageConsentAfterToggle = false;

        try {
            adStorageConsentAfterToggle = (boolean) new org.json.JSONObject(cookie.get("value").toString()).get("ad_storage");
        } catch (JSONException e) {
            // Invalid json in the cookie. We handle it in the assertion.
            adStorageConsentAfterToggle = true;
        }

        Assertions.assertEquals(expectedAdStorageConsent, adStorageConsentAfterToggle);
    }

    @Test
    void verifyThatAutoplaySettingIsRespected() {
        // Get hold of LocalStorage
        var webStorage = (WebStorage) new Augmenter().augment(driver);
        var localStorage = webStorage.getLocalStorage();

        var expectedInitialAutoplayEnabled = true;
        var autoplayEnabled = false;

        // Grab the autoplay setting from LocalStorage
        try {
            autoplayEnabled = (boolean) new org.json.JSONObject(localStorage.getItem("redux")).getJSONObject("settings").get("autoplay");
        } catch (JSONException e) {
            // Invalid json in the entry. We handle it in the assertion.
        }

        Assertions.assertEquals(expectedInitialAutoplayEnabled, autoplayEnabled);

        // Navigate to the Settings page
        var settingsLinkXpath = "//a[@class='sc-5b00349a-0 hwpvwu sc-87f10045-4 imzlFR' and @href='/installningar']";
        driver.findElement(By.xpath(settingsLinkXpath)).click();

        // Toggle the autoplay switch
        var autoplaySwitchXpath = "//label[@for='play_autoplay-settings-switch']";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(autoplaySwitchXpath)));
        driver.findElement(By.xpath(autoplaySwitchXpath)).click();

        var expectedAutoplayEnabled = false;
        var autoplayEnabledAfterToggle = false;

        try {
            autoplayEnabledAfterToggle = (boolean) new org.json.JSONObject(localStorage.getItem("redux")).getJSONObject("settings").get("autoplay");
        } catch (JSONException e) {
            // Invalid json in the entry. We handle it in the assertion.
            autoplayEnabledAfterToggle = true;
        }

        Assertions.assertEquals(expectedAutoplayEnabled, autoplayEnabledAfterToggle);
    }

    @Test
    void verifyThatChildProtectionSettingsAreRespected() {
        // Start by activating child protection on the Settings page
        var settingsLinkXpath = "//a[@class='sc-5b00349a-0 hwpvwu sc-87f10045-4 imzlFR' and @href='/installningar']";
        driver.findElement(By.xpath(settingsLinkXpath)).click();

        // Toggle the child protection switch
        var childProtectionSwitchXpath = "//label[@data-rt='child-protection-switch']";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(childProtectionSwitchXpath)));
        driver.findElement(By.xpath(childProtectionSwitchXpath)).click();

        var input = driver.findElement(By.id("play_settings-parental-control-input"));
        input.sendKeys("111");

        var button = driver.findElement(By.xpath("//button[@data-rt='child-protection-password-activate']"));
        Assertions.assertEquals(false, button.isEnabled());

        input.sendKeys("1");
        Assertions.assertEquals(true, button.isEnabled());

        button.click();

        //var searchForm = driver.findElement(By.xpath("//form[@data-rt='autocomplete-search-form']"));
        var searchText = driver.findElement(By.id("search"));
        searchText.sendKeys("detektiven från beledweyne");
        searchText.submit();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//main/section/div/ul/li[1]/article/a")));
        var seriesLink = driver.findElement(By.xpath("//main/section/div/ul/li[1]/article/a"));
        seriesLink.click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@data-rt='top-area-play-button']")));
        var playFirstEpisode = driver.findElement(By.xpath("//a[@data-rt='top-area-play-button']"));
        playFirstEpisode.click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@role='alertdialog']/h2")));
        var alert = driver.findElement(By.xpath("//div[@role='alertdialog']/h2"));

        var expectedWarningText = "Detta program är olämpligt för barn";
        Assertions.assertEquals(expectedWarningText, alert.getText());
    }

    // 2st G-test kvar

    // Program->Syntolkat->Öppna första träffen->Kolla att texten "Finns även utan tolkning" visas

    // Kolla att alla img har alt-text
    @Test
    void verifyThatAllImagesHaveAlternateTexts() {
        var images = driver.findElements(By.tagName("img"));

        for (var image : images)
        {
            var altText = image.getAttribute("alt");
            if (altText == null || altText.isEmpty() || altText.trim().isEmpty()) {
                Assertions.assertEquals(true, false);
            }
        }
    }

    // Kolla att inga trasiga bildlänkar finns
    @Test
    void noBrokenImageLinks() {
        var images = driver.findElements(By.tagName("img"));

        for (var image : images)
        {
            {
                var client = HttpClientBuilder.create().build();
                var request = new HttpGet(image.getAttribute("src"));
                CloseableHttpResponse response = null;
                try {
                    response = client.execute(request);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                /* For valid images, the HttpStatus will be 200 */
                if (response.getStatusLine().getStatusCode() != 200)
                {
                    Assertions.assertEquals(true, false);
                }
            }
        }

    }

    @Test
    void agendaShouldBeFirstHitWhenSearchingForAgenda() {
        var searchForm = driver.findElement(By.name("q"));
        searchForm.sendKeys("agenda");
        searchForm.submit();

        var firstSearchHitXpath = "//ul/li[@data-rt='search-result-item'][1]";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(firstSearchHitXpath)));
        var firstSearchElement = driver.findElement(By.xpath(firstSearchHitXpath));
        var programTitle = firstSearchElement.findElement(By.tagName("h2"));

        Assertions.assertEquals("Agenda", programTitle.getText());
    }

    @Test
    void verifySeasonLengthAndNameOfS2E5ofPistvakt() {
        var searchForm = driver.findElement(By.name("q"));
        searchForm.sendKeys("pistvakt");
        searchForm.submit();

        var firstSearchHitXpath = "//ul/li[@data-rt='search-result-item'][1]";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(firstSearchHitXpath)));
        driver.findElement(By.xpath(firstSearchHitXpath)).click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.linkText("SÄSONG 2")));
        driver.findElement(By.linkText("SÄSONG 2")).click();

        var expectedEpisodesInS2 = 6;
        var S2ListXpath = "//section[@data-helix-type='list'][2]/div/article";
        var actualEpisodesInS2 = driver.findElements(By.xpath(S2ListXpath)).size();

        Assertions.assertEquals(expectedEpisodesInS2, actualEpisodesInS2);

        var expectedS2E5Name = "5. Personalfestan";
        var S2E5Xpath = "//section[@data-helix-type='list'][2]/div/article[5]/div[2]/h3/a";
        var actualS2E5Name = driver.findElement(By.xpath(S2E5Xpath)).getText();

        Assertions.assertEquals(expectedS2E5Name, actualS2E5Name);
    }

    // 5st VG-test kvar
}
