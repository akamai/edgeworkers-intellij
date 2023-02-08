package utils;

public final class Constants {
    public static final String CPU_PROFILING = "CPU_PROFILING";
    public static final String MEM_PROFILING = "MEM_PROFILING";
    public static final String[] EW_HTTP_METHODS = new String[]{"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE"};
    public static final String EW_DEFAULT_SAMPLING_SIZE = "5";
    public static final String EW_SAMPLING_HEADER = "x-ew-code-profile-cpu-sampling-interval";
    public static final String EW_MEM_PROFILING_HEADER = "x-ew-code-profile-memory";
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
