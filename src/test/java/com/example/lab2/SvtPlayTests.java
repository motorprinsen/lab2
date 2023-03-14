package com.example.lab2;

import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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
        var headingClass = "guide__title";
        var linkXpath = "//a[@href='https://kontakt.svt.se/guide/tillganglighet']";

        // Navigate to the availability site
        driver.findElement(By.xpath(linkXpath)).click();

        var heading = driver.findElement(By.className(headingClass));
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
    void verifyThatCookieSettingsAreRespected() {

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

        // Navigate to the Setting page
        var settingsLinkXpath = "//a[@class='sc-5b00349a-0 hwpvwu sc-87f10045-4 imzlFR' and @href='/installningar']";
        driver.findElement(By.xpath(settingsLinkXpath)).click();

        // Open the cookie consent dialog
        var cookieButtonClass = ".sc-5b00349a-2.hLpVUw";
        driver.findElement(By.cssSelector(cookieButtonClass)).click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("play_cookie_consent_ad_storage")));
        // Toggle the ad storage consent switch
        driver.findElement(By.name("play_cookie_consent_ad_storage")).click();

        //var actions =  new Actions(driver);
        //actions.moveToElement(driver.findElement(By.id("play_cookie_consent_ad_storage"))).click().perform();

        // Grab the cookie again
        driver.manage().getCookieNamed("cookie-consent-1").toJson();

        var expectedAdStorageConsent = false;
        var adStorageConsentAfterToggle = false;

        try {
            adStorageConsentAfterToggle = (boolean) new org.json.JSONObject(cookie.get("value").toString()).get("ad_storage");
        } catch (JSONException e) {
            // Invalid json in the cookie. We handle it in the assertion.
        }

        Assertions.assertEquals(expectedAdStorageConsent, adStorageConsentAfterToggle);
    }
}
