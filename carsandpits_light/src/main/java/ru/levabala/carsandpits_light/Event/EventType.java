package ru.levabala.carsandpits_light.Event;

/**
 * Created by levabala on 28.04.2017.
 */

public enum EventType {
    EVENT("Event"),
    PIT_EVENT("PitEvent");

    private String stringValue;
    EventType(String toString) {
        stringValue = toString;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
