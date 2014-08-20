/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.excel;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class RosterError {

    private CellCoordinate coordinate;
    private String key;
    private String message;
    private String invalidValue;

    public RosterError(CellCoordinate coordinate, String key, String message, String invalidValue) {
        this.coordinate = coordinate;
        this.key = key;
        this.message = message;
        this.invalidValue = invalidValue;
    }

    public RosterError(CellCoordinate coordinate, String key, String message) {
        this(coordinate, key, message, null);
    }

    public CellCoordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(CellCoordinate coordinate) {
        this.coordinate = coordinate;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getInvalidValue() {
        return invalidValue;
    }

    @Override
    public String toString() {
        return "RosterError{" + "coordinate=" + coordinate + ", key=" + key + ", message=" + message +
                ", invalidValue=" + invalidValue + '}';
    }

}
