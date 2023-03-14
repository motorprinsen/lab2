package com.example.lab2;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class SvtPlayTests {

    private static WebDriver driver;

    @BeforeAll
    static void setup() {
        var options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        driver = new ChromeDriver(options);
    }

    @BeforeEach
    void navigate() {
        driver.get("https://iths.se");
    }

    @AfterAll
    static void teardown() {
        driver.quit();
    }

    @Test
    void checkWebsiteTitle() {
        var title = driver.getTitle();
        Assertions.assertEquals("IT-Högskolan – Här startar din IT-karriär!", title);
    }

    @Test
    void checkWebsiteHeading() {
        var xpath = "//*[@id=\"frontpage\"]/div/div[1]/div/div/div[1]/h1";
        var h1 = driver.findElement(By.xpath(xpath));
        Assertions.assertEquals("Här startar din IT-karriär!", h1.getText());
    }

}
