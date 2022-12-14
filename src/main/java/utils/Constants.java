package utils;

public final class Constants {
    public static final String[] EW_HTTP_METHODS = new String[]{"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE"};
    public static final String EW_DEFAULT_SAMPLING_SIZE = "5";
    public static final String EW_SAMPLING_HEADER = "x-ew-code-profile-cpu-sampling-interval";
    public static final String EW_USER_AGENT = "EdgeWorkers IntelliJ Plugin";
    public static final String CONVERTED_FILE_NAME = "convertedCpuProfile";
    public static final int SPEEDSCOPE_NUMBER_OF_FILES = 16;

    private Constants() {
    }
}
