package com.example.lab2;

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

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class SvtPlayTests {

    private static WebDriver driver;
    private static int toWait = 5;

    /**
     * Sets up a fresh, new browser for every run.
     * Navigates to the site and accepts the cookie consent dialog.
     */
    @BeforeAll
    static void setup() {
        var options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*", "--incognito");
        driver = new ChromeDriver(options);

        driver.get("https://www.svtplay.se/");
        acceptCookieConsentDialog();
    }

    static void acceptCookieConsentDialog() {
        // The modal takes a couple of seconds to show up
        var modalXpath = "//div[@data-rt='cookie-consent-modal']";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(modalXpath)));

        // Accept the default settings
        var modal = driver.findElement(By.xpath(modalXpath));
        var consentButton = modal.findElement(By.xpath(".//button[text() = 'Acceptera alla']"));
        consentButton.click();

        // The modal takes a couple of seconds to close after accepting
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(modalXpath)));
    }

    /**
     * Shuts down the driver after all tests are done.
     */
    @AfterAll
    static void teardown() {
        driver.quit();
    }

    /**
     * Navigates to the start page before each test.
     */
    @BeforeEach
    void navigate() {
        driver.get("https://www.svtplay.se/");
    }

    /**
     * G1 - Kontrollera att webbplatsens titel stämmer.
     */
    @Test
    void isTitleCorrect() {
        // Grab the title
        var actualTitle = driver.getTitle();
        var expectedTitle = "SVT Play";

        Assertions.assertEquals(expectedTitle, actualTitle, "Incorrect title is shown");
    }

    /**
     * G2 - Kontrollera att webbplatsens logotyp är synlig.
     */
    @Test
    void isLogoVisible() {
        // The logo is the only svg in the site
        var logo = driver.findElement(By.tagName("svg"));

        Assertions.assertTrue(logo.isDisplayed(), "No logo is displayed");
    }

    /**
     * G3 - Kontrollera namnen på de tre länkarna i huvudmenyn “Start, Program, Kanaler”.
     */
    @Test
    void areMainLinksOnStartPageCorrect() {
        // Actual values are Pascal cased but then upper-cased through CSS
        var expectedStartText = "Start".toUpperCase();
        var startTextXpath = "//li[@type='start']/a";
        var startText = driver.findElement(By.xpath(startTextXpath));

        Assertions.assertEquals(expectedStartText, startText.getText(), "Wrong text on 'Start' link");

        var expectedProgramsText = "Program".toUpperCase();
        var expectedProgramsXpath = "//li[@type='programs']/a";
        var programsText = driver.findElement(By.xpath(expectedProgramsXpath));

        Assertions.assertEquals(expectedProgramsText, programsText.getText(), "Wrong text on 'Program' link");

        var expectedChannelsText = "Kanaler".toUpperCase();
        var expectedChannelsXpath = "//li[@type='channels']/a";
        var channelsText = driver.findElement(By.xpath(expectedChannelsXpath));

        Assertions.assertEquals(expectedChannelsText, channelsText.getText(), "Wrong text on 'Kanaler' link");
    }

    /**
     * G4 - Kontrollera att länken för “Tillgänglighet i SVT Play” är synlig och att rätt länktext visas.
     */
    @Test
    void checkAvailabilityLink() {
        var expectedLinkText = "Tillgänglighet i SVT Play";
        var linkXpath = "//a[@href='https://kontakt.svt.se/guide/tillganglighet']";
        var link = driver.findElement(By.xpath(linkXpath));

        Assertions.assertTrue(link.isDisplayed(), "No availability link displayed");

        // Selenium cannot CSS select using compound class names,
        // so we work around it by concatenating the names
        var linkSpan = link.findElement(By.cssSelector(".sc-343fed33-3.dmRxHt"));
        Assertions.assertEquals(expectedLinkText, linkSpan.getText(), "Wrong text on availability link");
    }

    /**
     * G6 - Följ länken Tillgänglighet i SVT Play och kontrollera huvudrubriken.
     */
    @Test
    void verifyHeadingInAvailabilitySite() {
        var expectedText = "Så arbetar SVT med tillgänglighet";
        var linkXpath = "//a[@href='https://kontakt.svt.se/guide/tillganglighet']";

        // Navigate to the availability site
        driver.findElement(By.xpath(linkXpath)).click();

        // Grab the heading
        var heading = driver.findElement(By.tagName("h1"));
        Assertions.assertEquals(expectedText, heading.getText(), "Wrong heading on availability page");
    }

    /**
     * G7 - Använd metoden “click()” för att navigera in till sidan “Program”.
     *      Kontrollera antalet kategorier som listas.
     */
    @Test
    void checkNumberOfCategoriesOnProgramsPage() {
        // Navigate to the Programs page
        var programsXpath = "//li[@type='programs']/a";
        driver.findElement(By.xpath(programsXpath)).click();

        // The number of categories as of 2023-03-20
        var expectedCategories = 18;
        var categoriesXpath = "//article";

        // Get the actual categories
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(categoriesXpath)));
        var actualCategories = driver.findElements(By.xpath(categoriesXpath)).size();

        Assertions.assertEquals(expectedCategories, actualCategories, "Wrong number of categories displayed");
    }

    /**
     * G Extra 1 - First checks the default cookie settings,
     *             then sets a new values, reloads the page
     *             and verifies the new value.
     */
    @Test
    void verifyThatCookieSettingsAreRespected() {
        // Grab the cookie consent cookie
        var cookie = driver.manage().getCookieNamed("cookie-consent-1").toJson();

        // We accepted all cookies in the setup
        var expectedInitialAdStorageConsent = true;
        var adStorageConsent = false;

        try {
            // Json-parse the cookie and get the "ad_storage" value
            adStorageConsent = (boolean) new org.json.JSONObject(cookie.get("value").toString()).get("ad_storage");
        } catch (JSONException e) {
            // Invalid json in the cookie. We handle it in the assertion.
        }

        Assertions.assertEquals(expectedInitialAdStorageConsent, adStorageConsent,
                "Wrong default 'ad storage' consent setting");

        // Navigate to the Settings page
        var settingsLinkXpath = "//a[@class='sc-5b00349a-0 hwpvwu sc-87f10045-4 imzlFR' and @href='/installningar']";
        driver.findElement(By.xpath(settingsLinkXpath)).click();

        // Open the cookie consent dialog
        var cookieButtonClass = ".sc-5b00349a-2.hLpVUw";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(cookieButtonClass)));
        driver.findElement(By.cssSelector(cookieButtonClass)).click();

        // Wait for the element to show up
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("play_cookie_consent_ad_storage")));

        // Toggle the ad storage consent switch
        var consentSwitchXpath = "//label[@for='play_cookie_consent_ad_storage']";
        driver.findElement(By.xpath(consentSwitchXpath)).click();

        // Save the new cookie preferences
        driver.findElement(By.cssSelector(".sc-5b00349a-2.fuGbXH.sc-4f221cd2-9.hEiUxP")).click();

        // The modal takes a couple of seconds to close after accepting
        var modalXpath = "//div[@data-rt='cookie-consent-modal']";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(modalXpath)));

        driver.navigate().refresh();

        // Grab the cookie again.
        cookie = driver.manage().getCookieNamed("cookie-consent-1").toJson();

        var expectedAdStorageConsent = false;
        var adStorageConsentAfterToggle = false;

        try {
            adStorageConsentAfterToggle = (boolean) new org.json.JSONObject(cookie.get("value").toString()).get("ad_storage");
        } catch (JSONException e) {
            // Invalid json in the cookie. We handle it in the assertion.
            adStorageConsentAfterToggle = true;
        }

        Assertions.assertEquals(expectedAdStorageConsent, adStorageConsentAfterToggle,
                "Wrong 'ad storage' consent setting after toggling");
    }

    /**
     * G Extra 2 - Sets a child protection pin code and verifies that a restricted
     *             program doesn't play
     */
    @Test
    void verifyThatChildProtectionSettingsAreRespected() {
        // Navigate to the Settings page.
        driver.findElement(By.linkText("Inställningar".toUpperCase())).click();

        // Toggle the child protection switch
        var childProtectionSwitchXpath = "//label[@data-rt='child-protection-switch']";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(childProtectionSwitchXpath)));
        driver.findElement(By.xpath(childProtectionSwitchXpath)).click();

        // The pin code must be four digits.
        // If we only enter three, the "Activate" button shouldn't be available.
        var input = driver.findElement(By.id("play_settings-parental-control-input"));
        input.sendKeys("111");

        var button = driver.findElement(By.xpath("//button[@data-rt='child-protection-password-activate']"));
        Assertions.assertFalse(button.isEnabled(), "Button is enabled when only three digits have been entered");

        // Set the last digit of the code and verify that the button is enabled
        input.sendKeys("1");
        Assertions.assertTrue(button.isEnabled(), "Button is not enabled although a valid code has been entered");

        // Save the code
        button.click();

        // Search for a known program that has age restrictions.
        var searchText = driver.findElement(By.id("search"));
        searchText.sendKeys("detektiven från beledweyne");
        searchText.submit();

        // Navigate to the program page
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//main/section/div/ul/li[1]/article/a")));
        var seriesLink = driver.findElement(By.xpath("//main/section/div/ul/li[1]/article/a"));
        seriesLink.click();

        // Try to play the first episode
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@data-rt='top-area-play-button']")));
        var playFirstEpisode = driver.findElement(By.xpath("//a[@data-rt='top-area-play-button']"));
        playFirstEpisode.click();

        // The "Unsuitable for children" dialog should appear
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@role='alertdialog']/h2")));
        var alert = driver.findElement(By.xpath("//div[@role='alertdialog']/h2"));

        var expectedWarningText = "Detta program är olämpligt för barn";
        Assertions.assertEquals(expectedWarningText, alert.getText(),
                "Could play age restricted program although a code has been set");
    }

    /**
     * G Extra 3 - Toggles the autoplay setting and verifies that it gets
     *             turned off.
     */
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

        Assertions.assertEquals(expectedInitialAutoplayEnabled, autoplayEnabled, "Wrong default autoplay setting");

        // Navigate to the Settings page.
        driver.findElement(By.linkText("Inställningar".toUpperCase())).click();

        // Toggle the autoplay switch
        var autoplaySwitchXpath = "//label[@for='play_autoplay-settings-switch']";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
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

        Assertions.assertEquals(expectedAutoplayEnabled, autoplayEnabledAfterToggle,
                "Wrong autoplay setting after toggling");
    }

    /**
     * G Extra 4 - Opens the first program with visual aid/description and verifies
     *             that the program is in visual aid/description mode.
     */
    @Test
    void verifyThatVisualAidIsActiveIfSelected() {
        // Navigate to the Programs page
        driver.findElement(By.linkText("Program".toUpperCase())).click();

        // Find and click the "Visual aid" link
        var visualAidLinkText = "Syntolkat";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.linkText(visualAidLinkText)));
        driver.findElement(By.linkText(visualAidLinkText)).click();

        // Find the first available program and navigate to it
        var firstShowXpath = "//main/descendant::article[1]";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath((firstShowXpath))));
        driver.findElement(By.xpath(firstShowXpath)).click();

        // Check that the option to view the program without aid id displayed.
        // This indicates that the current program is in "visual aid" mode.
        var withoutVisualAidLinkText = "utan tolkning";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.linkText(withoutVisualAidLinkText)));
        var noVisualAid = driver.findElement(By.linkText(withoutVisualAidLinkText));

        Assertions.assertTrue(noVisualAid.isDisplayed(),
                "No 'visual aid' available although we selected a program that has it");
    }

    /**
     * G Extra 5 - Searches for an empty string and verifies that
     *             no results are presented.
     */
    @Test
    void searchingWithoutTermShouldShouldShowNoResultsPage() {
        // Click/submit an empty search
        var searchFormXpath = "//button[@type='submit']";
        driver.findElement(By.xpath(searchFormXpath)).click();

        // Verify that no programs were found
        var mainElement = driver.findElement(By.id("play_main-content"));
        var paragraphXpath = "section/div/p[1]";
        var actualResultText = mainElement.findElement(By.xpath(paragraphXpath)).getText();
        var expectedResultText = "Inga sökträffar.";

        Assertions.assertEquals(expectedResultText, actualResultText, "Empty search criteria yielded results");
    }

    /**
     * VG1 - Testa sökformuläret genom att söka efter “Agenda” och kontrollera att
     *        programmet Agenda dyker upp överst i sökresultatet. Sökformuläret ska
     *        lokaliseras med locatorn för name-attributet.
     */
    @Test
    void agendaShouldBeFirstMatchWhenSearchingForAgenda() {
        // Search for "agenda"
        var searchForm = driver.findElement(By.name("q"));
        searchForm.sendKeys("agenda");
        searchForm.submit();

        // Grab the first available program and verify that it is correct
        var firstSearchHitXpath = "//ul/li[@data-rt='search-result-item'][1]";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(firstSearchHitXpath)));
        var firstSearchElement = driver.findElement(By.xpath(firstSearchHitXpath));
        var programTitle = firstSearchElement.findElement(By.tagName("h2"));

        Assertions.assertEquals("Agenda", programTitle.getText(),
                "Wrong program found when searching for 'Agenda'");
    }

    /**
     * VG2 - Använd sökförmuläret för att navigera in till programsidan för programmet
     *        “Pistvakt”. Kontrollera därefter antalet program i säsong 2 av serien. Kontrollera
     *        även namnet på avsnitt 5 i säsong 2.
     */
    @Test
    void verifySeasonLengthAndNameOfS2E5ofPistvakt() {
        // Search for "pistvakt"
        var searchForm = driver.findElement(By.name("q"));
        searchForm.sendKeys("pistvakt");
        searchForm.submit();

        // Navigate to the first available program, which should be "Pistvakt"
        var firstSearchHitXpath = "//ul/li[@data-rt='search-result-item'][1]";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(firstSearchHitXpath)));
        driver.findElement(By.xpath(firstSearchHitXpath)).click();

        // Find and click on the second season
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.linkText("SÄSONG 2")));
        driver.findElement(By.linkText("SÄSONG 2")).click();

        // Verify the number of episodes in the season
        var expectedEpisodesInS2 = 6;
        var S2ListXpath = "//section[@data-helix-type='list'][2]/div/article";
        var actualEpisodesInS2 = driver.findElements(By.xpath(S2ListXpath)).size();

        Assertions.assertEquals(expectedEpisodesInS2, actualEpisodesInS2,
                "Wrong number of episodes in season two of 'Pistvakt'");

        // Verify the name of the 5th episode
        var expectedS2E5Name = "5. Personalfestan";
        var S2E5Xpath = "//section[@data-helix-type='list'][2]/div/article[5]/div[2]/h3/a";
        var actualS2E5Name = driver.findElement(By.xpath(S2E5Xpath)).getText();

        Assertions.assertEquals(expectedS2E5Name, actualS2E5Name,
                "Wrong name of episode 5 in season two of 'Pistvakt'");
    }

    /**
     * VG Extra 1 - Makes the window narrower and verifies that the search input becomes
     *              hidden and replaces by a link.
     *
     */
    @Test
    void verifyThatSearchFormIsHiddenInResponsivePortraitMode() {
        // The input should visible to start with
        var searchForm = driver.findElement(By.name("q"));
        Assertions.assertTrue(searchForm.isDisplayed());

        // We need this to restore the screen size later
        var originalSize = driver.manage().window().getSize();

        // 600px is the magic width. From there (and narrower) the search form gets
        // hidden and replaced by a link (to reveal the form)
        driver.manage().window().setSize(new Dimension(600, 900));

        // The input should be there, but not visible
        searchForm = driver.findElement(By.name("q"));
        Assertions.assertFalse(searchForm.isDisplayed());

        // Restore the old size
        driver.manage().window().setSize(originalSize);
    }

    /**
     * VG Extra 2 - Adds a show to "My list", navigates and then verifies
     *              that the show is still in "My list".
     */
    @Test
    void verifyThatShowsInMyListAreRememberedWhenNavigating() {
        // Navigate to the Programs page
        driver.findElement(By.linkText("Program".toUpperCase())).click();

        // Navigate to the first available program
        var firstProgramXpath = "(//li[@data-rt='alphabetic-list-item']/a)[1]";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(firstProgramXpath)));
        driver.findElement(By.xpath(firstProgramXpath)).click();

        // Find the "Add to my list" button and add the current program
        var addToListXpath = "//button[@data-rt='my-list-btn']";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(addToListXpath)));
        driver.findElement(By.xpath(addToListXpath)).click();

        // Go back to the listing and select another show
        driver.navigate().back();

        var secondProgramXpath = "(//li[@data-rt='alphabetic-list-item']/a)[2]";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(secondProgramXpath)));
        driver.findElement(By.xpath(secondProgramXpath)).click();

        // Navigate back to the listing
        driver.navigate().back();

        // Go to the first program again
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(firstProgramXpath)));
        driver.findElement(By.xpath(firstProgramXpath)).click();

        // Grab the 'Add' button
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(addToListXpath)));
        var addButton = driver.findElement(By.xpath(addToListXpath));

        Assertions.assertEquals("Ta bort från Min lista".toUpperCase(), addButton.getText(),
                "Program was not saved in 'My list'");
    }

    /**
     * VG Extra 3 - Iterate over all the images of the page and fetch them using
     *              their "src" attribute to verify that no broken image links exist.
     *              Also verify that all images have an alternate text attribute.
     *
     *              Please note that the alt text check sometimes fails.
     *              Although SVT claims that they are "accessibility certified" and adhere
     *              the guidelines of WCAG, I have noticed that some images occasionally does not
     *              have an alt text.
     *              And this directly violates section H37 of WCAG (https://www.w3.org/TR/WCAG20-TECHS/H37.html).
     *              I have reported this to SVT, but they haven't responded yet (2023-03-20).
     */
    @Test
    void verifyImages() {
        // These are the images that lack an alt text at the time of writing (2023-03-20).
        // Since they are only for decoration, all guidelines say it is ok.
        var exceptions = List.of(
                "https://www.svtstatic.se/play/play7/_next/static/images/nyhetsbrev-f560c8ed3341a6e3b32f96d6fd5c6249.jpg",
                "https://www.svtstatic.se/play/play7/_next/static/images/sprakplay-d66bb69136fe39fd5a68f9a52de5fba7.jpg",
                "https://www.svtstatic.se/play/play7/_next/static/images/barnplay-0005eee3a97c31b3b746ba9dfd86d5d2.jpg",
                "https://www.svtstatic.se/play/play7/_next/static/images/atv-kollage-f9a40ce443f7ba0806ec3d225ff6bcd8.jpg"
        );

        // Grab all images from the main page
        var images = driver.findElements(By.tagName("img"));

        for (var image : images) {
            {
                // The alt attribute is allowed to be missing or empty for
                // images in the known exceptions list.
                var altText = image.getAttribute("alt");
                var src = image.getAttribute("src");

                if ((altText == null || altText.isEmpty()) && !exceptions.contains(src)) {
                    Assertions.fail("Image " + src + " does not have an alt text");
                }

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
                Assertions.assertEquals(expectedResponseCode, response.getStatusLine().getStatusCode(),
                        "Found a broken image link");
            }
        }
    }

    /**
     * VG Extra 4 - Verify that fewer programs become available when
     *              ticking the "Can be seen abroad" box.
     */
    @Test
    void verifyThatFewerProgramsAreAvailableWhenAbroad() {
        // Navigate to the Programs page
        driver.findElement(By.linkText("Program".toUpperCase())).click();

        // Count the number of programs available
        var programsXpath = "//li[@data-rt='alphabetic-list-item']/a";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(programsXpath)));
        var programsViewableInSweden = driver.findElements(By.xpath(programsXpath)).size();

        // Tick the "Abroad" box
        driver.findElement(By.id("abroad")).click();

        // Count the available programs
        var programsViewableAbroad = driver.findElements(By.xpath(programsXpath)).size();

        Assertions.assertTrue(programsViewableAbroad < programsViewableInSweden,
                "Expected the number of available programs abroad to be fewer than those available in Sweden");
    }

    /**
     * VG Extra 5 - Open the Channels page and verify that today's date is showing.
     */
    @Test
    void verifyThatTodayIsDefaultOnChannelsPage() {
        // Navigate to the Channels page
        driver.findElement(By.linkText("Kanaler".toUpperCase())).click();

        // Find the displayed date
        var todayXpath = "//span[@data-rt='navigation-date-current']/h2";
        new WebDriverWait(driver, Duration.ofSeconds(toWait))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(todayXpath)));
        var today = driver.findElement(By.xpath(todayXpath));

        // Construct a localized version of the expected result
        var localizedSymbols = new DateFormatSymbols(Locale.forLanguageTag("SV-SE"));
        var localDate = LocalDate.now();
        var dayOfMonth = localDate.getDayOfMonth();
        var month = localizedSymbols.getShortMonths()[localDate.getMonthValue() - 1].substring(0, 3);
        var expectedText = "Idag " + dayOfMonth + " " + month;

        Assertions.assertEquals(expectedText.toUpperCase(), today.getText(),
                "Wrong day and month is displayed as 'today'");
    }
}
