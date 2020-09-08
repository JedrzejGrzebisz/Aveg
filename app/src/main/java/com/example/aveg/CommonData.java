package com.example.aveg;

public final class CommonData {
    // activities request codes
    public final static int REQUEST_CODE_CONFIG = 1;

    // configuration info: names and default values
    public final static String CONFIG_IP_ADDRESS = "ipAddress";
    public final static String DEFAULT_IP_ADDRESS = "192.168.56.22";

    public final static String CONFIG_SAMPLE_TIME = "sampleTime";
    public final static int DEFAULT_SAMPLE_TIME = 500;

    public final static String CONFIG_RPY_UNIT = "rpyUnit";
    public final static String DEFAULT_RPY_UNIT = "rad";

    public final static String CONFIG_TEMPERATURE_UNIT = "temperatureUnit";
    public final static String DEFAULT_TEMPERATURE_UNIT = "C";

    public final static String CONFIG_PRESSURE_UNIT = "pressureUnit";
    public final static String DEFAULT_PRESSURE_UNIT = "hPa";

    public final static String CONFIG_HUMIDITY_UNIT = "humidityUnit";
    public final static String DEFAULT_HUMIDITY_UNIT = "0_1";

    // error codes
    public final static int ERROR_TIME_STAMP = -1;
    public final static int ERROR_NAN_DATA = -2;
    public final static int ERROR_RESPONSE = -3;

    // IoT server data
    public final static String WEATHER_FILE_NAME = "Project/weatherCondition.json";
    public final static String RPY_RAD_FILE_NAME = "Project/rpyValueRad.json";
    public final static String RPY_DEG_FILE_NAME = "Project/rpyValueDeg.json";
    public final static String JOYSTICK_FILE_NAME = "Project/joystick.json";
    public final static String SINGLE_LED_FILE_NAME = "Project/singleLedColor.php";
    public final static String TEXT_LED_FILE_NAME = "Project/textLedColor.php";
    
}
