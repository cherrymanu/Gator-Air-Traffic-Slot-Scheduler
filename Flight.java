/**
 * Flight class - Represents a flight in the air traffic control system
 * Stores all flight information and current state
 */
public class Flight {
    // Flight attributes
    int flightID;
    int airlineID;
    int submitTime;
    int priority;
    int duration;
    
    // Scheduling information
    int startTime;
    int ETA;
    int runwayID;
    
    // Flight states: PENDING, SCHEDULED, IN_PROGRESS, COMPLETED
    FlightState state;
    
    // Handles for heap structures (for efficient updates)
    PairingNode heapNode;           // Handle in pairing heap (pending flights)
    int completionHeapIndex;        // Index in completion heap (timetable)
    
    /**
     * Constructor for new flight submission
     */
    public Flight(int flightID, int airlineID, int submitTime, int priority, int duration) {
        this.flightID = flightID;
        this.airlineID = airlineID;
        this.submitTime = submitTime;
        this.priority = priority;
        this.duration = duration;
        
        // Initialize as pending with no schedule
        this.startTime = -1;
        this.ETA = -1;
        this.runwayID = -1;
        this.state = FlightState.PENDING;
        this.heapNode = null;
        this.completionHeapIndex = -1;
    }
    
    /**
     * Check if flight has started
     */
    public boolean hasStarted(int currentTime) {
        return state == FlightState.IN_PROGRESS || 
               (state == FlightState.SCHEDULED && startTime <= currentTime);
    }
    
    /**
     * Check if flight has completed
     */
    public boolean hasCompleted() {
        return state == FlightState.COMPLETED;
    }
    
    /**
     * Check if flight is unsatisfied (needs scheduling)
     */
    public boolean isUnsatisfied(int currentTime) {
        return state == FlightState.PENDING || 
               (state == FlightState.SCHEDULED && startTime > currentTime);
    }
    
    @Override
    public String toString() {
        return String.format("[flight%d, airline%d, runway%d, start%d, ETA%d]",
                           flightID, airlineID, runwayID, startTime, ETA);
    }
}

/**
 * Enum for flight states
 */
enum FlightState {
    PENDING,        // Not yet scheduled
    SCHEDULED,      // Scheduled but not started
    IN_PROGRESS,    // Currently in progress (non-preemptive)
    COMPLETED       // Landed
}



