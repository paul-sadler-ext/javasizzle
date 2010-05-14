package org.jsizzle.examples.airline.impl;

import static com.google.common.base.Functions.compose;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Sets.newHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcurry.AsFunction;
import org.jsizzle.example.airline.AirlineSpec;
import org.jsizzle.examples.airline.Airline;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public class AirlineImpl implements Airline
{
    private final List<Flight> flights = new ArrayList<Flight>();

    private static class Flight
    {
        final String from;
        final String to;
        final int capacity;

        public Flight(String from, String to, int capacity)
        {
            this.from = from;
            this.to = to;
            this.capacity = capacity;
        }
    }

    @Override
    public int addFlight(String from, String to, int capacity)
    {
        flights.add(new Flight(from, to, capacity));
        return flights.size() - 1;
    }

    @Override
    public boolean hasRoute(final String from, final String to)
    {
        return any(flights, new Predicate<Flight>()
        {
            @Override
            public boolean apply(Flight flight)
            {
                return flight.from.equals(from) && flight.to.equals(to);
            }
        });
    }
    
    final AirlineSpec.AddFlight specAddFlight(String from, String to, int capacity)
    {
        return new AirlineSpec.AddFlight(specAirline(),
                                         new AirlineSpec.FlightId(addFlight(from, to, capacity)),
                                         specFlight(new Flight(from, to, capacity)),
                                         specAirline());
    }
    
    final AirlineSpec.HasRoute specHasRoute(String from, String to)
    {
        return new AirlineSpec.HasRoute(specAirline(), specRoute(from, to), hasRoute(from, to));
    }

    final AirlineSpec specAirline()
    {
        final Set<AirlineSpec.Route> routesSpec = newHashSet(transform(flights, compose(AirlineSpec.Flight.getRoute, specFlight)));
        final Map<AirlineSpec.FlightId, AirlineSpec.Flight> flightsSpec =
            uniqueIndex(transform(flights, specFlight), new Function<AirlineSpec.Flight, AirlineSpec.FlightId>()
        {
            int index = 0;

            @Override
            public AirlineSpec.FlightId apply(AirlineSpec.Flight from)
            {
                return new AirlineSpec.FlightId(index++);
            }
        });
        final Map<AirlineSpec.Customer, Set<AirlineSpec.FlightId>> bookingsSpec = Collections.emptyMap();
        return new AirlineSpec(routesSpec, flightsSpec, bookingsSpec);
    }
    
    @AsFunction
    static final AirlineSpec.Flight specFlight(Flight flight)
    {
        return new AirlineSpec.Flight(specRoute(flight.from, flight.to), flight.capacity);
    }
    
    static final AirlineSpec.Route specRoute(String from, String to)
    {
        return new AirlineSpec.Route(new AirlineSpec.Location(from),
                                     new AirlineSpec.Location(to));
    }
}