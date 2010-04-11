package org.jsizzle.examples.airline.impl;

import org.junit.Test;


public class AirlineTest
{
    private final AirlineImpl airline = new AirlineImpl();
    
    @Test
    public void testCreateAirline()
    {
        airline.specAirline().checkInvariant();
    }
    
    @Test
    public void testAddFlight()
    {
        airline.specAddFlight("London", "Rome", 10).checkInvariant();
        airline.specAddFlight("London", "Rome", 10).checkInvariant();
        airline.specAddFlight("London", "Paris", 10).checkInvariant();
    }
    
    @Test
    public void testHasRoute()
    {
        airline.addFlight("London", "Rome", 10);
        airline.specHasRoute("London", "Rome").checkInvariant();
        airline.specHasRoute("London", "Paris").checkInvariant();
    }
}
