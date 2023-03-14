package com.example.lab2;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SvtPlayTests {

    private static WebDriver driver;
    private static final String expectedTitle = "SVT Play";

    @BeforeAll
    static void setup() {
        var options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*", "--incognito");
        driver = new ChromeDriver(options);

        driver.get("https://www.svtplay.se/");
        getPassedTheCookieConsentModal();
    }

    static void getPassedTheCookieConsentModal() {

        var modalXpath = "//div[@data-rt='cookie-consent-modal']";
        var wait = new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(modalXpath)));

        var modal = driver.findElement(By.xpath(modalXpath));
        var consentButton = modal.findElement(By.xpath(".//button[text() = 'Acceptera alla']"));
        consentButton.click();
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

        Assertions.assertEquals(expectedTitle, title);
    }

    @Test
    void isLogoVisible() {
        var logoXpath = "//*[name()='svg']/*[name()='title' and text()='SVT Play logotyp']";
        var logo = driver.findElement(By.xpath(logoXpath));

        Assertions.assertTrue(logo.isDisplayed());
    }

    @Test
    void checkWebsiteHeading() {
        var xpath = "//*[@id=\"frontpage\"]/div/div[1]/div/div/div[1]/h1";
        var h1 = driver.findElement(By.xpath(xpath));
        Assertions.assertEquals("Här startar din IT-karriär!", h1.getText());
    }

}
