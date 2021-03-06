package ru.integrations.check.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import ru.integrations.check.config.AllureConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class AllureUtils {
    protected static final AllureConfig config = new AllureConfig();

    public static String sendReportToAllure(Integer projectId) throws Exception {
        if (config.ALLURE_ENABLE) {
            log.info("Allure enable!");
            if (projectId > 0) {
                config.ALLURE_PROJECT_ID = projectId;
                log.info("ProjectId: {}", projectId);
            }
            ProcessBuilder process = new ProcessBuilder();
            StringBuilder command = getCommand();

            if (SystemUtils.IS_OS_WINDOWS) {
                log.info("Use Windows");
                process.command("cmd.exe", "/c", command.toString());
            } else if (SystemUtils.IS_OS_LINUX) {
                log.info("Use Linux(Bash)");
                process.command(config.ALLURE_LINUX_CMD, "-c", command.toString());
            }
            StringBuilder builder = new StringBuilder();
            try {
                Process processProgress = process.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(processProgress.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                int exitCode = processProgress.waitFor();
                log.info("Exit with code: {} with reason: {}", exitCode, builder);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            String launchId = findAndExtractLaunchId(builder.toString());
            if (launchId == null) {
                throw new Exception("Error send to Allure");
            }
            return launchId;
        }
        return "";
    }

    private static String findAndExtractLaunchId(String output) {
        Pattern findLaunch = Pattern.compile("Launch [\\[0-9]*]");
        Pattern findDigital = Pattern.compile("\\d+");
        Matcher launch = findLaunch.matcher(output);
        if (launch.find()) {
            Matcher digit = findDigital.matcher(launch.group());
            if (digit.find()) {
                return digit.group();
            }
        }
        return null;
    }

    private static StringBuilder getCommand() {
        StringBuilder command = new StringBuilder();
        if (!config.ALLURE_COMMAND_FOLDER.isEmpty()){
            command.append(config.ALLURE_COMMAND_FOLDER);
        }
        command.append("allurectl").append(" ").append("upload").append(" ").append(config.ALLURE_FOLDER).append(" ");
        command.append("--token").append(" ").append(config.ALLURE_TOKEN).append(" ");
        command.append("--endpoint").append(" ").append(config.ALLURE_ENDPOINT).append(" ");
        command.append("--project-id").append(" ").append(config.ALLURE_PROJECT_ID).append(" ");
        command.append("--launch-name").append(" ").append(config.ALLURE_LAUNCH_NAME).append(" ");
        command.append("--job-run-uid").append(" ").append(config.ALLURE_JOB_RUN_UID).append(" ");
        command.append("--ci-type").append(" ").append(config.ALLURE_CI_TYPE);
        return command;
    }
}
