import java.util.*;

/**
 * Air Traffic Scheduler - Main scheduling system
 * Manages flights across multiple runways using advanced data structures
 */
public class AirTrafficScheduler {
    // Core data structures
    private PairingHeap pendingFlights;           // Max-heap of pending flights by priority
    private HashMap<Integer, Flight> activeFlights; // All active flights (by flightID)
    private HashMap<Integer, ArrayList<Flight>> airlineIndex; // Flights by airline
    private CompletionHeap timetable;             // Binary min-heap of scheduled flights by (ETA, flightID)
    
    // System state  
    private int currentTime;
    private int nextRunwayID;
    private List<Runway> allRunways; // All runways in system
    
    /**
     * Constructor
     */
    public AirTrafficScheduler() {
        this.pendingFlights = new PairingHeap();
        this.activeFlights = new HashMap<>();
        this.airlineIndex = new HashMap<>();
        this.timetable = new CompletionHeap(100); // Initial capacity
        this.currentTime = 0;
        this.nextRunwayID = 1;
        this.allRunways = new ArrayList<>();
    }
    
    /**
     * 1. Initialize - Create runway system
     */
    public String initialize(int numRunways) {
        if (numRunways <= 0) {
            return "Invalid input. Please provide a valid number of runways.";
        }
        
        // Create runways with ID starting from 1
        for (int i = 0; i < numRunways; i++) {
            Runway runway = new Runway(nextRunwayID++, 0);
            allRunways.add(runway);
        }
        
        return numRunways + " Runways are now available";
    }
    
    /**
     * 2. SubmitFlight - Add new flight to system
     */
    public List<String> submitFlight(int flightID, int airlineID, int currentTime, 
                                      int priority, int duration) {
        List<String> output = new ArrayList<>();
        
        // Check for duplicate
        if (activeFlights.containsKey(flightID)) {
            output.add("Duplicate FlightID");
            return output;
        }
        
        // Advance time and settle
        output.addAll(advanceTime(currentTime));
        
        // Create new flight
        Flight flight = new Flight(flightID, airlineID, currentTime, priority, duration);
        
        // Add to active flights
        activeFlights.put(flightID, flight);
        
        // Add to airline index
        airlineIndex.computeIfAbsent(airlineID, k -> new ArrayList<>()).add(flight);
        
        // Capture old ETAs
        Map<Integer, Integer> oldETAs = captureETAs();
        
        // Reschedule all unsatisfied flights
        scheduleAll();
        
        // Output
        output.add(String.format("Flight %d scheduled - ETA: %d", flightID, flight.ETA));
        output.addAll(generateETAUpdates(oldETAs));
        
        return output;
    }
    
    /**
     * 3. CancelFlight - Remove a flight that hasn't started
     */
    public List<String> cancelFlight(int flightID, int currentTime) {
        List<String> output = new ArrayList<>();
        
        // Phase 1 & 2: Advance time and settle (includes reschedule)
        output.addAll(advanceTime(currentTime));
        
        // Lookup flight
        Flight flight = activeFlights.get(flightID);
        
        if (flight == null) {
            output.add(String.format("Flight %d does not exist", flightID));
            return output;
        }
        
        // Check if already departed or completed
        if (flight.state == FlightState.IN_PROGRESS || flight.state == FlightState.COMPLETED) {
            output.add(String.format("Cannot cancel. Flight %d has already departed", flightID));
            return output;
        }
        
        // Capture ETAs before operation
        Map<Integer, Integer> oldETAs = captureETAs();
        
        // Perform operation: Remove from all structures
        removeFlight(flight);
        
        // Phase 2 again: Reschedule after operation changed unsatisfied flights
        scheduleAll();
        
        output.add(String.format("Flight %d has been canceled", flightID));
        output.addAll(generateETAUpdates(oldETAs));
        
        return output;
    }
    
    /**
     * 4. Reprioritize - Change flight's priority and reschedule
     */
    public List<String> reprioritize(int flightID, int currentTime, int newPriority) {
        List<String> output = new ArrayList<>();
        
        // Advance time and settle
        output.addAll(advanceTime(currentTime));
        
        // Lookup flight
        Flight flight = activeFlights.get(flightID);
        
        if (flight == null) {
            output.add(String.format("Flight %d not found", flightID));
            return output;
        }
        
        // Check if already departed
        if (flight.state == FlightState.IN_PROGRESS || flight.state == FlightState.COMPLETED) {
            output.add(String.format("Cannot reprioritize. Flight %d has already departed", flightID));
            return output;
        }
        
        Map<Integer, Integer> oldETAs = captureETAs();
        
        // Update flight's priority
        flight.priority = newPriority;
        
        // Reschedule all unsatisfied flights
        scheduleAll();
        
        output.add(String.format("Priority of Flight %d has been updated to %d", 
                                 flightID, newPriority));
        output.addAll(generateETAUpdates(oldETAs));
        
        return output;
    }
    
    /**
     * 5. AddRunways - Add more runways to the system
     */
    public List<String> addRunways(int count, int currentTime) {
        List<String> output = new ArrayList<>();
        
        if (count <= 0) {
            output.add("Invalid input. Please provide a valid number of runways.");
            return output;
        }
        
        // Advance time and settle
        output.addAll(advanceTime(currentTime));
        
        Map<Integer, Integer> oldETAs = captureETAs();
        
        // Add new runways
        for (int i = 0; i < count; i++) {
            Runway runway = new Runway(nextRunwayID++, currentTime);
            allRunways.add(runway);
        }
        
        // Reschedule unsatisfied flights
        scheduleAll();
        
        output.add(String.format("Additional %d Runways are now available", count));
        output.addAll(generateETAUpdates(oldETAs));
        
        return output;
    }
    
    /**
     * 6. GroundHold - Block flights from specific airlines
     */
    public List<String> groundHold(int airlineLow, int airlineHigh, int currentTime) {
        List<String> output = new ArrayList<>();
        
        if (airlineHigh < airlineLow) {
            output.add("Invalid input. Please provide a valid airline range.");
            return output;
        }
        
        // Advance time and settle
        output.addAll(advanceTime(currentTime));
        
        Map<Integer, Integer> oldETAs = captureETAs();
        
        // Remove unsatisfied flights in airline range
        List<Flight> toRemove = new ArrayList<>();
        for (int airlineID = airlineLow; airlineID <= airlineHigh; airlineID++) {
            ArrayList<Flight> flights = airlineIndex.get(airlineID);
            if (flights != null) {
                for (Flight flight : new ArrayList<>(flights)) {
                    if (flight.state == FlightState.PENDING || 
                        (flight.state == FlightState.SCHEDULED && flight.startTime > currentTime)) {
                        toRemove.add(flight);
                    }
                }
            }
        }
        
        for (Flight flight : toRemove) {
            removeFlight(flight);
        }
        
        // Reschedule remaining flights
        scheduleAll();
        
        output.add(String.format("Flights of the airlines in the range [%d, %d] have been grounded",
                                 airlineLow, airlineHigh));
        output.addAll(generateETAUpdates(oldETAs));
        
        return output;
    }
    
    /**
     * 7. PrintActive - Show all active flights
     */
    public List<String> printActive() {
        if (activeFlights.isEmpty()) {
            return Arrays.asList("No active flights");
        }
        
        // Sort by flightID
        List<Flight> flights = new ArrayList<>(activeFlights.values());
        flights.sort(Comparator.comparingInt(f -> f.flightID));
        
        List<String> output = new ArrayList<>();
        for (Flight flight : flights) {
            output.add(flight.toString());
        }
        
        return output;
    }
    
    /**
     * 8. PrintSchedule - Show scheduled flights in time range [t1, t2]
     */
    public List<String> printSchedule(int t1, int t2) {
        // Get flights from completion heap with ETA in [t1, t2]
        List<Flight> scheduled = timetable.getFlightsInRange(t1, t2, currentTime);
        
        if (scheduled.isEmpty()) {
            return Arrays.asList("There are no flights in that time period");
        }
        
        // Already sorted by (ETA, flightID) from TreeMap
        List<String> output = new ArrayList<>();
        for (Flight flight : scheduled) {
            output.add(String.format("[%d]", flight.flightID));
        }
        
        return output;
    }
    
    /**
     * 9. Tick - Advance time and land flights
     * This just calls advanceTime which does Phase 1, Phase 2, and prints Updated ETAs
     */
    public List<String> tick(int t) {
        // advanceTime already does everything Tick needs:
        // - Phase 1: Settle completions (land flights with ETA <= t)
        // - Promotion: Mark SCHEDULED → IN_PROGRESS
        // - Phase 2: Reschedule unsatisfied flights
        // - Print Updated ETAs if any changed
        return advanceTime(t);
    }
    
    /**
     * Advance system time to t, settle completions, and reschedule
     * Implements the two-phase update process
     */
    private List<String> advanceTime(int t) {
        if (t < currentTime) {
            return new ArrayList<>();
        }
        
        List<String> output = new ArrayList<>();
        
        // Phase 1: Settle completions - extract all flights with ETA <= t from heap
        List<Flight> landed = timetable.extractAllUpTo(t);
        
        // Mark as completed and remove from active structures
        for (Flight flight : landed) {
            if (flight.state != FlightState.COMPLETED) {
                flight.state = FlightState.COMPLETED;
                removeFlight(flight);
            }
            output.add(String.format("Flight %d has landed at time %d", flight.flightID, flight.ETA));
        }
        
        // Update current time
        currentTime = t;
        
        // Promotion Step (between phases): Mark scheduled flights as in-progress
        for (Flight flight : activeFlights.values()) {
            if (flight.state == FlightState.SCHEDULED && flight.startTime <= currentTime) {
                flight.state = FlightState.IN_PROGRESS;
            }
        }
        
        // Capture ETAs before Phase 2
        Map<Integer, Integer> oldETAs = captureETAs();
        
        // Phase 2: Reschedule unsatisfied flights from currentTime
        scheduleAll();
        
        // Print Updated ETAs if any changed during Phase 2
        output.addAll(generateETAUpdates(oldETAs));
        
        return output;
    }
    
    /**
     * Schedule all unsatisfied flights using greedy policy with Pairing Heap
     * This is the main scheduling algorithm
     */
    private void scheduleAll() {
        // Clear pending heap and collect unsatisfied flights
        pendingFlights.clear();
        
        for (Flight flight : activeFlights.values()) {
            boolean isUnsatisfied = (flight.state == FlightState.PENDING ||
                                    (flight.state == FlightState.SCHEDULED && flight.startTime > currentTime));
            
            if (isUnsatisfied) {
                // Remove from completion heap if it was there
                if (flight.completionHeapIndex > 0) {
                    timetable.delete(flight);
                }
                
                // Add to pending heap (will be sorted by priority, submitTime, flightID)
                pendingFlights.insert(flight);
            }
        }
        
        // Build runway min-heap with current availability
        RunwayHeap runwayHeap = new RunwayHeap(allRunways.size());
        
        // Track which runways are in use by in-progress flights
        Map<Integer, Integer> runwayNextFree = new HashMap<>();
        for (Runway runway : allRunways) {
            runwayNextFree.put(runway.runwayID, currentTime);
        }
        
        // Update with in-progress flights
        for (Flight flight : activeFlights.values()) {
            if (flight.state == FlightState.IN_PROGRESS) {
                runwayNextFree.put(flight.runwayID, flight.ETA);
            }
        }
        
        // Insert all runways into heap with their current nextFreeTime
        for (Runway runway : allRunways) {
            int nextFree = runwayNextFree.get(runway.runwayID);
            Runway heapRunway = new Runway(runway.runwayID, nextFree);
            runwayHeap.insert(heapRunway);
        }
        
        // Schedule flights by extracting from pairing heap (highest priority first)
        while (!pendingFlights.isEmpty()) {
            Flight flight = pendingFlights.extractMax();
            
            // Pick earliest free runway using min-heap
            Runway runway = runwayHeap.extractMin();
            
            // Assign flight to runway
            int startTime = Math.max(currentTime, runway.nextFreeTime);
            int eta = startTime + flight.duration;
            
            flight.startTime = startTime;
            flight.ETA = eta;
            flight.runwayID = runway.runwayID;
            flight.state = (startTime <= currentTime) ? FlightState.IN_PROGRESS : FlightState.SCHEDULED;
            
            // Update runway's nextFreeTime and push back into heap
            runway.nextFreeTime = eta;
            runwayHeap.insert(runway);
            
            // Add to completion heap (timetable)
            timetable.insert(flight);
        }
    }
    
    /**
     * Remove flight from all structures
     */
    private void removeFlight(Flight flight) {
        // Remove from active flights
        activeFlights.remove(flight.flightID);
        
        // Remove from airline index
        ArrayList<Flight> airlineFlights = airlineIndex.get(flight.airlineID);
        if (airlineFlights != null) {
            airlineFlights.remove(flight);
        }
        
        // Remove from completion heap (timetable)
        if (flight.completionHeapIndex > 0) {
            timetable.delete(flight);
        }
    }
    
    /**
     * Capture current ETAs for comparison
     */
    private Map<Integer, Integer> captureETAs() {
        Map<Integer, Integer> etas = new HashMap<>();
        for (Flight flight : activeFlights.values()) {
            if (flight.ETA > 0) {
                etas.put(flight.flightID, flight.ETA);
            }
        }
        return etas;
    }
    
    /**
     * Generate ETA update output
     */
    private List<String> generateETAUpdates(Map<Integer, Integer> oldETAs) {
        List<String> output = new ArrayList<>();
        List<String> updates = new ArrayList<>();
        
        for (Flight flight : activeFlights.values()) {
            Integer oldETA = oldETAs.get(flight.flightID);
            // Only include flights that existed before AND whose ETA changed
            if (flight.ETA > 0 && oldETA != null && oldETA != flight.ETA) {
                updates.add(String.format("%d: %d", flight.flightID, flight.ETA));
            }
        }
        
        if (!updates.isEmpty()) {
            updates.sort((a, b) -> {
                int idA = Integer.parseInt(a.split(":")[0]);
                int idB = Integer.parseInt(b.split(":")[0]);
                return Integer.compare(idA, idB);
            });
            output.add("Updated ETAs: [" + String.join(", ", updates) + "]");
        }
        
        return output;
    }
}

