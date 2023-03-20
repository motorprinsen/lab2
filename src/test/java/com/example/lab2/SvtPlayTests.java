package com.example.lab2;

import net.bytebuddy.asm.Advice;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.util.Assert;

import java.io.IOException;
import java.text.Collator;
import java.text.DateFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static java.util.List.of;

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

        // Accept the default settings
        var modal = driver.findElement(By.xpath(modalXpath));
        var consentButton = modal.findElement(By.xpath(".//button[text() = 'Acceptera alla']"));
        consentButton.click();

        // The modal takes a couple of seconds to close after accepting
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(modalXpath)));
    }

    @AfterAll
    static void teardown() {
        driver.quit();
    }

    @BeforeEach
    void navigate() {
        driver.get("https://www.svtplay.se/");
    }

    @Test
    void isTitleCorrect() {
        // Grab the title and assert
        var actualTitle = driver.getTitle();
        var expectedTitle = "SVT Play";

        Assertions.assertEquals(expectedTitle, actualTitle);
    }

    @Test
    void isLogoVisible() {
        //var logoXpath = "//*[name()='svg']/*[name()='title' and text()='SVT Play logotyp']/..";
        var logo = driver.findElement(By.tagName("svg"));

        Assertions.assertTrue(logo.isDisplayed());
    }

    @Test
    void areMainLinksOnStartPageCorrect() {
        // Actual values are Pascal cased but then upper-cased through CSS
        var expectedStartText = "Start".toUpperCase();
        var startTextXpath = "//li[@type='start']/a";
        var startText = driver.findElement(By.xpath(startTextXpath));

        Assertions.assertEquals(expectedStartText, startText.getText());

        var expectedProgramsText = "Program".toUpperCase();
        var expectedProgramsXpath = "//li[@type='programs']/a";
        var programsText = driver.findElement(By.xpath(expectedProgramsXpath));

        Assertions.assertEquals(expectedProgramsText, programsText.getText());

        var expectedChannelsText = "Kanaler".toUpperCase();
        var expectedChannelsXpath = "//li[@type='channels']/a";
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
        // Navigate to the Programs page
        var programsXpath = "//li[@type='programs']/a";
        driver.findElement(By.xpath(programsXpath)).click();

        var expectedNoOfCategories = 18;
        var categoriesXpath = "//article";

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(categoriesXpath)));
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

        // TDOD: Implicit wait
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
        // Get hold of the LocalStorage implementation
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

        // Navigate to the Settings page.
        driver.findElement(By.linkText("Inställningar".toUpperCase())).click();

        // Toggle the autoplay switch
        var autoplaySwitchXpath = "//label[@for='play_autoplay-settings-switch']";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(autoplaySwitchXpath)));
        driver.findElement(By.xpath(autoplaySwitchXpath)).click();

        var expectedAutoplayEnabled = false;
        var autoplayEnabledAfterToggle = false;

        // Grab the autoplay setting from LocalStorage again
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
        // Navigate to the Settings page.
        driver.findElement(By.linkText("Inställningar".toUpperCase())).click();

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

    // Program->Syntolkat->Öppna första träffen->Kolla att texten "Finns även utan tolkning" visas
    @Test
    void verifyThatVisualDescriptionIsActiveIfSelected() {
        driver.findElement(By.linkText("PROGRAM")).click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Syntolkat")));
        driver.findElement(By.linkText("Syntolkat")).click();

        var firstShowXpath = "//main/descendant::article[1]";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath((firstShowXpath))));
        driver.findElement(By.xpath(firstShowXpath)).click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.linkText("utan tolkning")));
        var noVisualDescription = driver.findElement(By.linkText("utan tolkning"));
        Assertions.assertTrue(noVisualDescription.isDisplayed());
    }

    @Test
    void searchingWithoutTermShouldShouldShowNoResultsPage() {
        var searchInputXpath = "//button[@type='submit']";
        driver.findElement(By.xpath(searchInputXpath)).click();

        var expectedResultText = "Inga sökträffar.";
        var actualResultText = driver.findElement(By.className("p.sc-da02705a-0.giCVDd")).getText();

        Assertions.assertEquals(expectedResultText, actualResultText);
    }


    @Test
    void agendaShouldBeFirstMatchWhenSearchingForAgenda() {
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

    // 1st VG-test kvar

    @Test
    void verifyThatProgramListingIsInAlphabeticalOrder() {
        // Navigate to the "Program" page
        driver.findElement(By.linkText("PROGRAM")).click();

        var programsXpath = "//li[@data-rt='alphabetic-list-item']/a";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(programsXpath)));
        var programs = driver.findElements(By.xpath(programsXpath));

        var lastProgram = "";
        for (var program : programs) {
            var expectedResult = -1; // Actually, -1 or lower

            var programTitle = program.getText();
            var actualResult = lastProgram.compareTo(programTitle);
            var comparison = actualResult <= expectedResult;

            var swedishCollator = Collator.getInstance(Locale.forLanguageTag("SV-SE"));
            swedishCollator.setStrength(Collator.PRIMARY);
            var collateResult = swedishCollator.compare(lastProgram, programTitle);

            if (!(collateResult <= expectedResult)) {
                System.out.println(collateResult + " :: " + lastProgram + ":: " + programTitle);
                //System.out.println(collateResult);
            }
            Assertions.assertTrue(collateResult <= expectedResult);
            //System.out.println(actualResult <= expectedResult);

            lastProgram = programTitle;
            //System.out.println(lastProgram);
        }
    }

    @Test
    void verifyThatSearchFormIsHiddenInResponsivePortraitMode() {
        // We need this to restore the screen size later
        var originalSize = driver.manage().window().getSize();

        // 600px is the magic width. From there (and narrower) the search input gets
        // hidden and replaced by a link (to reveal the input)
        driver.manage().window().setSize(new Dimension(600, 900));

        // The input should be there, but not visible
        var searchInput = driver.findElement(By.name("q"));
        Assertions.assertFalse(searchInput.isDisplayed());

        // Restore the old size
        driver.manage().window().setSize(originalSize);
    }

    @Test
    void verifyThatShowsInMyListAreRememberedWhenNavigating() {
        // Navigate to the "Program" page
        driver.findElement(By.linkText("PROGRAM")).click();

        // Navigate to the first available show
        var firstShowXpath = "(//li[@data-rt='alphabetic-list-item']/a)[1]";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(firstShowXpath)));
        driver.findElement(By.xpath(firstShowXpath)).click();

        // Find the "Add to my list" button and add the current show
        var addToListXpath = "//button[@data-rt='my-list-btn']";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(addToListXpath)));
        driver.findElement(By.xpath(addToListXpath)).click();

        // Go back to the listing and select another show
        driver.navigate().back();

        var secondShowXpath = "(//li[@data-rt='alphabetic-list-item']/a)[2]";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(secondShowXpath)));
        driver.findElement(By.xpath(secondShowXpath)).click();

        // Navigate back to the first show and verify that it is still
        // added in my list
        driver.navigate().back();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(firstShowXpath)));
        driver.findElement(By.xpath(firstShowXpath)).click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(addToListXpath)));
        var addButton = driver.findElement(By.xpath(addToListXpath));

        Assertions.assertEquals("Ta bort från Min lista".toUpperCase(), addButton.getText());
    }

    @Test
    void noBrokenImageLinks() {
        // Grab all images from the main page
        var images = driver.findElements(By.tagName("img"));

        for (var image : images) {
            {
                // It is probably not the best of practises to create a new
                // instance for each request. It is a big no-no in Dotnet.
                var client = HttpClientBuilder.create().build();

                // Get the actual source image
                var request = new HttpGet(image.getAttribute("src"));
                CloseableHttpResponse response = null;
                try {
                    response = client.execute(request);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                var expectedResponseCode = 200;
                Assertions.assertEquals(expectedResponseCode, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    void verifyThatAllImagesHaveAlternateTexts() {
        // These are the images that lack an alt text at the time of writing (2023-03-20)
        var exceptions = List.of(
                "https://www.svtstatic.se/play/play7/_next/static/images/nyhetsbrev-f560c8ed3341a6e3b32f96d6fd5c6249.jpg",
                "https://www.svtstatic.se/play/play7/_next/static/images/sprakplay-d66bb69136fe39fd5a68f9a52de5fba7.jpg",
                "https://www.svtstatic.se/play/play7/_next/static/images/barnplay-0005eee3a97c31b3b746ba9dfd86d5d2.jpg",
                "https://www.svtstatic.se/play/play7/_next/static/images/atv-kollage-f9a40ce443f7ba0806ec3d225ff6bcd8.jpg"
        );

        // Get all images
        var images = driver.findElements(By.tagName("img"));

        for (var image : images) {
            // If the alt attribute is missing or empty, and the current image
            // is not in the known exception list, the test will fail
            var altText = image.getAttribute("alt");
            var src = image.getAttribute("src");

            if ((altText == null || altText.isEmpty()) && !exceptions.contains(src)) {
                Assertions.fail("Image " + src + " does not have an alt text");
            }
        }
    }

    @Test
    void verifyThatTodayIsDefaultOnChannelsPage() {
        // Navigate to the "Channels" page
        driver.findElement(By.linkText("Kanaler".toUpperCase())).click();

        // Find the displayed date
        var todayXpath = "//span[@data-rt='navigation-date-current']/h2";
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(todayXpath)));
        var today = driver.findElement(By.xpath(todayXpath));

        // Construct a localized version of the expected result
        var localizedSymbols = new DateFormatSymbols(Locale.forLanguageTag("SV-SE"));
        var localDate = LocalDate.now();
        var dayOfMonth = localDate.getDayOfMonth();
        var month = localizedSymbols.getShortMonths()[localDate.getMonthValue() - 1].substring(0, 3);
        var expectedTodayText = "Idag " + dayOfMonth + " " + month;

        Assertions.assertEquals(expectedTodayText.toUpperCase(), today.getText());
    }
}
