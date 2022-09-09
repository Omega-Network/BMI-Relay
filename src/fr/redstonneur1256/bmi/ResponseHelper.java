package fr.redstonneur1256.bmi;

import java.util.Map;

public class ResponseHelper {

    public static final String STATUS = "status";
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String ERROR_MESSAGE = "message";

    public static Object makeError(String message) {
        return Map.of(STATUS, ERROR, ERROR_MESSAGE, message);
    }

}
