package org.jsizzle.examples.airline;


public interface Airline
{
    boolean hasRoute(String from, String to);
    int addFlight(String from, String to, int capacity);
}
