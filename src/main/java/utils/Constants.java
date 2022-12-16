package utils;

public final class Constants {
    public static final String[] EW_HTTP_METHODS = new String[]{"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE"};
    public static final String EW_USER_AGENT = "EdgeWorkers IntelliJ Plugin";
    public static final String CONVERTED_FILE_NAME = "convertedCpuProfile";
    public static final int SPEEDSCOPE_NUMBER_OF_FILES = 16;
    public static String JAVA_TMP_URL = createJavaTmpURL();

    private Constants() {
    }

    private static String createJavaTmpURL() {
        // Remove any forward slashes with backslashes since this is a file URL
        String tempUrl = System.getProperty("java.io.tmpdir").replace('\\', '/');
        if (!tempUrl.endsWith("/")) {
            return tempUrl + "/";
        } else {
            return tempUrl;
        }
    }
}
