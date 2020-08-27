package com.example.aveg;

public final class CommonData {
    // activities request codes
    public final static int REQUEST_CODE_CONFIG = 1;

    // configuration info: names and default values
    public final static String CONFIG_IP_ADDRESS = "ipAddress";
    public final static String DEFAULT_IP_ADDRESS = "192.168.56.22";

    public final static String CONFIG_SAMPLE_TIME = "sampleTime";
    public final static int DEFAULT_SAMPLE_TIME = 500;

    // error codes
    public final static int ERROR_TIME_STAMP = -1;
    public final static int ERROR_NAN_DATA = -2;
    public final static int ERROR_RESPONSE = -3;

    // IoT server data
    public final static String FILE_NAME = "AndroidTasks/weatherCondition.json";
    public final static String FILE_NAME2 = "jQuery/webApp/rpyValue.json";
    public final static String FILE_NAME3 = "SH_5/joystick.json";

}
