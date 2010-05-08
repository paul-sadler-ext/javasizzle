package org.jsizzle.example.airline;

import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.singleton;

import java.util.Map;
import java.util.Set;

import org.jsizzle.Disjoint;
import org.jsizzle.Include;
import org.jsizzle.Invariant;
import org.jsizzle.Schema;

import com.google.common.base.Predicate;

@Schema
class AirlineSpec
{
    class Location {}
    class FlightId {}
    class Customer {}

    class Route
    {
        Location from, to;

        @Invariant boolean notCircular()
        {
            return !from.equals(to);
        }
    }

    class Flight
    {
        Route route;
        int capacity;
        
        @Invariant boolean capacityLimited()
        {
            return capacity <= 10;
        }
    }

    Set<Route> routes;
    Map<FlightId, Flight> flights;
    Map<Customer, Set<FlightId>> bookings;

    @Invariant boolean allFlightRoutesAreRegistered()
    {
        return all(flights.values(), new Predicate<Flight>()
        {
            public boolean apply(Flight flight)
            {
                return routes.contains(flight.route);
            }
        });
    }
    
    @Invariant boolean allBookingFlightsAreRegistered()
    {
        return all(concat(bookings.values()), new Predicate<FlightId>()
        {
            public boolean apply(FlightId flightId)
            {
                return flights.containsKey(flightId);
            }
        });
    }
    
    @Invariant boolean allFlightsDoNotExceedCapacity()
    {
        return all(flights.keySet(), new Predicate<FlightId>()
        {
            public boolean apply(final FlightId flightId)
            {
                return size(filter(bookings.keySet(), new Predicate<Customer>()
                {
                    public boolean apply(Customer customer)
                    {
                        return bookings.get(customer).contains(flightId);
                    }
                })) <= flights.get(flightId).capacity;
            }
        });
    }

    class HasRoute
    {
        AirlineSpec airline;
        Route route;
        boolean result;

        @Invariant boolean resultIsHasRoute()
        {
            return result == airline.routes.contains(route);
        }
    }

    class AddFlight0
    {
        Flight flight;
        FlightId flightId;
        AirlineSpec airline;
        AirlineSpec after;

        @Invariant boolean flightAdded()
        {
            return after.routes.equals(union(singleton(flight.route), airline.routes))
                    && after.bookings.equals(airline.bookings)
                    && after.flights.size() == airline.flights.size() + 1
                    && after.flights.get(flightId).equals(flight);
        }
    }
    
    class FlightExists
    {
        AirlineSpec airline;
        FlightId flightId;
        
        @Invariant boolean flightExists()
        {
            return airline.flights.containsKey(flightId);
        }
    }
    
    @Disjoint class AddFlight
    {
        @Include FlightExists alreadyExists;
        @Include AddFlight0 addFlight0;
    }
}
