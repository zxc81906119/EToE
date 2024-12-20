package e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

public class EToE {
    static {
        WebDriverManager.getInstance(DriverManagerType.CHROME).setup();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EToE.class);

    public static final String EMPTY_STRING = "";
    public static final char WHITE_SPACE_CHAR = ' ';
    public static final char DASH_CHAR = '-';
    public static final char DOUBLE_QUOTES = '"';
    public static final char APOSTROPHE = '\'';
    public static final char ESCAPE_CHARACTER = '\\';

    public interface CommandInterface {
        void executeCommand(WebDriver webDriver, Map<String, String> cmdOptions);
    }

    public enum Command implements CommandInterface {
        GO_PAGE("goPage") {
            public void executeCommand(WebDriver webDriver, Map<String, String> cmdOptions) {
                String pOption = cmdOptions.get("p");
                if (pOption == null || pOption.isEmpty()) {
                    throw new RuntimeException("沒有提供頁面資訊");
                }
                webDriver.get(pOption);
            }
        },
        REFRESH("refresh") {
            public void executeCommand(WebDriver webDriver, Map<String, String> cmdOptions) {
                webDriver.navigate().refresh();
            }
        },
        CLICK("click") {
            public void executeCommand(WebDriver webDriver, Map<String, String> cmdOptions) {
                String bOption = cmdOptions.get("b");
                if (bOption == null || bOption.isEmpty()) {
                    throw new RuntimeException("沒有提供欄位搜尋根據");
                }
                String sOption = cmdOptions.get("s");
                if (sOption == null || sOption.isEmpty()) {
                    throw new RuntimeException("沒有提供欄位搜尋名稱");
                }
                List<WebElement> webElements = findElementsOnPage(webDriver, bOption, sOption);
                if (webElements != null && !webElements.isEmpty()) {
                    for (WebElement webElement : webElements) {
                        clickByJs(webDriver, webElement);
                    }
                }
            }
        },
        WAIT_PAGE("wait page") {
            public void executeCommand(WebDriver webDriver, Map<String, String> cmdOptions) {
                String pOption = cmdOptions.get("p");
                if (pOption == null || pOption.isEmpty()) {
                    throw new RuntimeException("沒有提供頁面資訊");
                }
                if (cmdOptions.containsKey("r")) {
                    waitTargetPageReady(webDriver, pOption);
                } else if (cmdOptions.containsKey("l")) {
                    waitLeaveThisPage(webDriver, pOption);
                } else {
                    waitUtilGoTargetPage(webDriver, pOption);
                }
            }
        },

        // 根據html或jsp內容去找而非畫面上
        WAIT_ELEMENT("wait element") {
            public void executeCommand(WebDriver webDriver, Map<String, String> cmdOptions) {
                String bOption = cmdOptions.get("b");
                if (bOption == null || bOption.isEmpty()) {
                    throw new RuntimeException("沒有提供欄位搜尋根據");
                }
                String sOption = cmdOptions.get("s");
                if (sOption == null || sOption.isEmpty()) {
                    throw new RuntimeException("沒有提供欄位搜尋名稱");
                }
                String cOption = cmdOptions.get("c");
                if (cOption == null || cOption.isEmpty()) {
                    throw new RuntimeException("沒有提供欄位等待條件");
                }
                By byCondition = getByCondition(bOption, sOption);
                switch (cOption) {
                    case "clickable":
                        new WebDriverWait(webDriver, ofSeconds(30), ofMillis(500)).until(ExpectedConditions.elementToBeClickable(byCondition));
                        break;
                    case "visible":
                        new WebDriverWait(webDriver, ofSeconds(30), ofMillis(500))
                                .until(ExpectedConditions.visibilityOfElementLocated(byCondition));
                        break;
                    case "exist":
                        new WebDriverWait(webDriver, ofSeconds(30), ofMillis(500))
                                .until(ExpectedConditions.presenceOfElementLocated(byCondition));
                        break;
                    default:
                        break;
                }
            }
        },
        SET_FIELD("set field") {
            public void executeCommand(WebDriver webDriver, Map<String, String> cmdOptions) {
                String bOption = cmdOptions.get("b");
                if (bOption == null || bOption.isEmpty()) {
                    throw new RuntimeException("沒有提供欄位搜尋根據");
                }
                String sOption = cmdOptions.get("s");
                if (sOption == null || sOption.isEmpty()) {
                    throw new RuntimeException("沒有提供欄位搜尋名稱");
                }
                String vOption = cmdOptions.get("v");
                List<WebElement> webElements = findElementsOnPage(webDriver, bOption, sOption);
                setValue(webDriver, webElements, vOption);
            }
        },
        SLEEP("sleep") {
            @Override
            public void executeCommand(WebDriver webDriver, Map<String, String> cmdOptions) {
                String timeMillis = cmdOptions.get("t");
                long sleepTime;
                if (timeMillis == null || timeMillis.isEmpty()) {
                    sleepTime = 1000;
                } else {
                    sleepTime = Long.parseLong(timeMillis);
                }
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    LOGGER.error("sleep 發生錯誤", e);
                }
            }

        };

        private final String cmdString;

        Command(String cmdString) {
            this.cmdString = cmdString;
        }

        public String getCmdString() {
            return cmdString;
        }

        // 必須是畫面上可被找到的
        private static List<WebElement> findElementsOnPage(WebDriver webDriver, String byType, String search) {
            List<WebElement> webElements = null;
            By by = getByCondition(byType, search);
            if (by != null) {
                webElements = webDriver.findElements(by);
            }
            return webElements;
        }

        private static By getByCondition(String byType, String search) {
            By by = null;
            if (byType != null && search != null) {
                switch (byType) {
                    case "className":
                        by = By.className(search);
                        break;
                    case "cssSelector":
                        by = By.cssSelector(search);
                        break;
                    case "id":
                        by = By.id(search);
                        break;
                    case "linkText":
                        by = By.linkText(search);
                        break;
                    case "name":
                        by = By.name(search);
                        break;
                    case "partialLinkText":
                        by = By.partialLinkText(search);
                        break;
                    case "tagName":
                        by = By.tagName(search);
                        break;
                    case "xpath":
                        by = By.xpath(search);
                        break;
                    default:
                        break;
                }
            }
            return by;
        }
    }

    public static Command getCommand(String inputCommand) {
        Command cmd = null;
        for (Command importantCmd : Command.values()) {
            final String importantCmdPrefix = importantCmd.getCmdString();
            if (inputCommand.startsWith(importantCmdPrefix)
                    && (cmd == null || importantCmdPrefix.length() > cmd.getCmdString().length())) {
                cmd = importantCmd;
            }
        }
        return cmd;
    }

    public static void processCmd(WebDriver webDriver, String inputCommand) {
        final Command cmd = getCommand(inputCommand);
        if (cmd != null) {
            LOGGER.debug("[processCmd] 對應到的指令:{}", cmd.getCmdString());
            final String newCommand = inputCommand.replaceFirst(cmd.getCmdString(), EMPTY_STRING);
            final Map<String, String> cmdOptions = processCmdDetail(newCommand);
            LOGGER.debug("[processCmd] optionKey和optionValue的對應:{}", cmdOptions);
            cmd.executeCommand(webDriver, cmdOptions);
            LOGGER.debug("[processCmd] 處理完畢");
        } else {
            throw new RuntimeException("[processCmd] 無找到此指令,輸入的完整指令為" + inputCommand);
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        final File eToEResourceRootDir = new File(Objects.requireNonNull(EToE.class.getResource("/e2e")).toURI());
        final File runPropertiesFile = new File(eToEResourceRootDir, "conf/run.properties");
        if (runPropertiesFile.isFile()) {
            final Properties properties = new Properties();
            properties.load(Files.newInputStream(runPropertiesFile.toPath()));
            final String runFileNames = getRunFileNames(properties);
            final String[] runFileNamesSplit = runFileNames.split(",");
            final ExecutorService executorService = Executors.newFixedThreadPool(4);
            try {
                for (String runFileName : runFileNamesSplit) {
                    final File runFile = new File(eToEResourceRootDir, "run/" + runFileName);
                    if (runFile.isFile()) {
                        CompletableFuture.runAsync(() -> {
                            final WebDriver webDriver = new ChromeDriver();
                            try {
                                runFile(webDriver, runFile);
                            } catch (IOException e) {
                                LOGGER.debug("runFile:{} e2e 發生錯誤！", runFile);
                            }
                        }, executorService);
                    }
                }
            } finally {
                executorService.shutdown();
            }
        } else {
            LOGGER.debug("file:{} doesn't exist", runPropertiesFile);
        }

    }

    private static String getRunFileNames(Properties properties) {
        final String runFileNames = properties.getProperty("run.file.names");
        if (runFileNames == null) {
            return "runFile.txt";
        }
        final String runFileNamesTrim = runFileNames.trim();
        if (runFileNamesTrim.isEmpty()) {
            return "runFile.txt";
        }
        return runFileNamesTrim;
    }

    public static void setValue(WebDriver webDriver, List<WebElement> webElements, Object value) {
        if (webElements != null && !webElements.isEmpty()) {
            for (WebElement webElement : webElements) {
                String tagName = webElement.getTagName();
                String stringValue = String.valueOf(value);
                switch (tagName) {
                    case "select":
                        // wait option exist
                        waitElementExist(webDriver, webElement, stringValue);
                        Select select = new Select(webElement);
                        select.selectByValue(String.valueOf(value));
                        break;
                    case "input":
                        String inputType = webElement.getAttribute("type");
                        if ("file".equalsIgnoreCase(inputType)) {
                            if (isElementShow(webDriver, webElement)) {
                                webElement.sendKeys(stringValue);
                            } else {
                                showElement(webDriver, webElement);
                                webElement.sendKeys(stringValue);
                            }
                        } else if ("tel".equalsIgnoreCase(inputType) || "text".equalsIgnoreCase(inputType)
                                || "password".equalsIgnoreCase(inputType)) {
                            webElement.clear();
                            webElement.sendKeys(stringValue);
                        } else if ("radio".equalsIgnoreCase(inputType)) {
                            if (stringValue.equals(webElement.getAttribute("value"))) {
                                clickByJs(webDriver, webElement);
                            }
                        } else if ("checkbox".equalsIgnoreCase(inputType)) {
                            clickByJs(webDriver, webElement);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public static void triggerEvent(WebDriver webDriver, WebElement webElement, String eventName) {
        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        js.executeScript("var event=new Event('" + eventName + "'); element.dispatchEvent(event);", webElement);
    }

    public static void clickByJs(WebDriver webDriver, WebElement webElement) {
        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        js.executeScript("var element=arguments[0]; element.click();", webElement);
    }

    public static void setValueByJs(WebDriver webDriver, WebElement webElement, Object value) {
        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        js.executeScript("var element=arguments[0]; element.value=arguments[1];", webElement, value);
    }

    public static void showElement(WebDriver webDriver, WebElement webElement) {
        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        js.executeScript("var element=arguments[0]; element.style.display='block';", webElement);
    }

    public static void hideElement(WebDriver webDriver, WebElement webElement) {
        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        js.executeScript("var element=arguments[0]; element.style.display='none';", webElement);
    }

    public static boolean isElementShow(WebDriver webDriver, WebElement webElement) {
        JavascriptExecutor js = (JavascriptExecutor) webDriver;
        String display = (String) js.executeScript("var element=arguments[0]; return element.style.display;",
                webElement);
        return !"none".equals(display);
    }

    public static void waitElementExist(WebDriver webDriver, WebElement webElement, String value) {
        new WebDriverWait(webDriver, ofSeconds(30), ofMillis(500)).until((input) -> {
            List<WebElement> webElements = webElement.findElements(By.cssSelector("option[value=\"" + value + "\"]"));
            return webElements != null && !webElements.isEmpty();
        });
    }

    public static void waitLeaveThisPage(WebDriver webDriver, String thisPage) {
        if (thisPage != null) {
            new WebDriverWait(webDriver, ofSeconds(30), ofMillis(500)).until((input) -> !thisPage.equals(input.getCurrentUrl()));
        }
    }

    public static void waitUtilGoTargetPage(WebDriver webDriver, String targetPageUrl) {
        if (targetPageUrl != null) {
            new WebDriverWait(webDriver, ofSeconds(30), ofMillis(500)).until((input) -> targetPageUrl.equals(input.getCurrentUrl()));
        }
    }

    public static void waitPageLoading(WebDriver driver) {
        new WebDriverWait(driver, ofSeconds(30), ofMillis(500)).until((input) -> ((JavascriptExecutor) input)
                .executeScript("return document.readyState").toString().equals("complete"));
    }

    public static void waitTargetPageReady(WebDriver webDriver, String targetPageUrl) {
        waitUtilGoTargetPage(webDriver, targetPageUrl);
        waitPageLoading(webDriver);
    }

    public static Map<String, String> processCmdDetail(String command) {
        boolean hasGoDash = false;
        boolean isAfterOption = false;
        Character doQuotesChar = null;
        Stack<Character> stack = new Stack<>();// 左邊
        Deque<Character> deque = new ArrayDeque<>();// 右邊
        Map<String, String> outCome = new HashMap<>();
        char[] commandCharArray = command.toCharArray();

        for (char thisChar : commandCharArray) {
            switch (thisChar) {
                case WHITE_SPACE_CHAR:
                    if (hasGoDash) {// 已經走過-
                        if (stack.isEmpty()) {// 左邊的堆疊是空的
                            throw new RuntimeException("不支援- ");
                        } else if (!isAfterOption) {// 左邊
                            isAfterOption = true;// 開啟變成右邊
                        } else if (!deque.isEmpty() || doQuotesChar != null) {// 引號模式或右邊雙向佇列不為空
                            deque.offer(thisChar);
                        }
                    }
                    break;
                case DASH_CHAR:
                    if (!hasGoDash) {// 還沒走過-
                        hasGoDash = true;
                    } else if (!isAfterOption) {// 左邊
                        throw new RuntimeException("不支援--,-X-");
                    } else if (doQuotesChar != null) {// 右邊雙向佇列是"或'開頭
                        deque.offer(thisChar);
                    } else if (!deque.isEmpty() && deque.peekLast() != WHITE_SPACE_CHAR) {// 右邊雙向佇列不是空且最後一個不是空白字元
                        throw new RuntimeException("不支援-X YYY-");
                    } else {
                        do {
                            char last = stack.pop();
                            if (!deque.isEmpty()) {
                                StringBuilder stringBuilder = getDequeToStringBuilder(deque);
                                String optionContent = stringBuilder.toString().trim();
                                outCome.put(String.valueOf(last), optionContent);
                            } else {
                                outCome.put(String.valueOf(last), EMPTY_STRING);
                            }
                        } while (!stack.isEmpty());
                        isAfterOption = false;// 設定為左邊
                    }
                    break;
                case DOUBLE_QUOTES:
                case APOSTROPHE:
                    if (hasGoDash) {// 已走過-
                        if (!isAfterOption) {// 左邊
                            throw new RuntimeException("不支援-',-\",-X',-X\"");
                        } else if (deque.isEmpty() && doQuotesChar == null) {// 右邊佇列是空的
                            doQuotesChar = thisChar;// 開啟引號模式
                        } else if (doQuotesChar != null && doQuotesChar == thisChar
                                && isOpenEvenJump(deque.descendingIterator())) {// 結束引號模式
                            do {
                                char last = stack.pop();
                                if (!deque.isEmpty()) {
                                    StringBuilder stringBuilder = getDequeToStringBuilder(deque);
                                    String optionContent = stringBuilder.toString();
                                    outCome.put(String.valueOf(last), optionContent);
                                } else {
                                    outCome.put(String.valueOf(last), EMPTY_STRING);
                                }
                            } while (!stack.isEmpty());
                            hasGoDash = false;// 設定未走過-
                            isAfterOption = false;// 設定為左邊
                            doQuotesChar = null;// 關閉引號模式
                        } else {
                            deque.offer(thisChar);
                        }
                    } else {
                        throw new RuntimeException("不支援 指令和-中間有'or\"");
                    }
                    break;
                case ESCAPE_CHARACTER:
                    if (hasGoDash) {// 已走過-
                        if (!isAfterOption) {// 左邊
                            throw new RuntimeException("沒有支援-safsa\\");
                        } else {
                            deque.offer(thisChar);
                        }
                    } else {
                        throw new RuntimeException("指令有問題");
                    }
                    break;
                default:
                    if (hasGoDash) {// 已走過-
                        if (!isAfterOption) {// 左邊
                            stack.push(thisChar);
                        } else {
                            deque.offer(thisChar);
                        }
                    } else {
                        throw new RuntimeException("指令有問題");
                    }
                    break;
            }
        }
        while (!stack.isEmpty()) {
            char last = stack.pop();
            if (!deque.isEmpty()) {
                if (doQuotesChar != null) {
                    Deque<Character> beforeDeque = new ArrayDeque<>();
                    StringBuilder afterStringBuilder = new StringBuilder();
                    boolean isWhiteSpace = false;
                    boolean isAfter = false;
                    beforeDeque.offer(doQuotesChar);
                    do {
                        char element = deque.poll();
                        if (!isAfter) {
                            if (!isWhiteSpace) {// 前一個不是' '
                                if (element == WHITE_SPACE_CHAR) {
                                    isWhiteSpace = true;
                                } else if (element == DASH_CHAR) {
                                    throw new RuntimeException("指令錯誤");
                                } else {
                                    beforeDeque.offer(element);
                                }
                            } else if (element == DASH_CHAR) {
                                afterStringBuilder.append(element);
                                isAfter = true;
                            } else {
                                beforeDeque.offer(WHITE_SPACE_CHAR);
                                beforeDeque.offer(element);
                                isWhiteSpace = false;
                            }
                        } else {
                            afterStringBuilder.append(element);
                        }
                    } while (!deque.isEmpty());
                    outCome.put(String.valueOf(last), getDequeToStringBuilder(beforeDeque).toString());
                    outCome.putAll(processCmdDetail(afterStringBuilder.toString()));
                } else {
                    StringBuilder stringBuilder = getDequeToStringBuilder(deque);
                    String optionContent = stringBuilder.toString().trim();
                    outCome.put(String.valueOf(last), optionContent);
                }
            } else if (doQuotesChar != null) {
                outCome.put(String.valueOf(last), doQuotesChar.toString());
                doQuotesChar = null;
            } else {
                outCome.put(String.valueOf(last), EMPTY_STRING);
            }
        }
        return outCome;
    }

    public static boolean isOpenEvenJump(Iterator<Character> iterator) {
        boolean isEven = true;
        while (iterator.hasNext() && iterator.next() == ESCAPE_CHARACTER) {
            isEven = !isEven;
        }
        return isEven;
    }

    public static StringBuilder getDequeToStringBuilder(Deque<Character> deque) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean isJump = false;
        while (!deque.isEmpty()) {
            char element = deque.poll();
            if (!isJump) {
                if (element == ESCAPE_CHARACTER) {
                    isJump = true;
                } else {
                    stringBuilder.append(element);
                }
            } else {
                switch (element) {
                    case 't':
                        stringBuilder.append('\t');
                        break;
                    case 'n':
                        stringBuilder.append('\n');
                        break;
                    case 'b':
                        stringBuilder.append('\b');
                        break;
                    case 'f':
                        stringBuilder.append('\f');
                        break;
                    case 'r':
                        stringBuilder.append('\r');
                        break;
                    case ESCAPE_CHARACTER:
                    case DOUBLE_QUOTES:
                    case APOSTROPHE:
                        stringBuilder.append(element);
                        break;
                    default:
                        stringBuilder.append(ESCAPE_CHARACTER).append(element);
                        break;
                }
                isJump = false;
            }
        }
        if (isJump) {
            stringBuilder.append(ESCAPE_CHARACTER);
        }
        return stringBuilder;
    }

    public static void runFile(WebDriver webDriver, File file) throws IOException {
        final String cmd = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        runString(webDriver, cmd);
    }

    public static void runString(WebDriver webDriver, String cmd) {
        final String[] eachCommands = cmd.split("\r?\n");
        for (String eachCmd : eachCommands) {
            final String eachCmdTrim = eachCmd.trim();
            if (!eachCmdTrim.isEmpty() && !eachCmdTrim.startsWith("--")) {
                processCmd(webDriver, eachCmdTrim);
            }
        }
    }

}
