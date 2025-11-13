# Gator Air Traffic Scheduler
## COP 5536 - Advanced Data Structures - Fall 2025

---

**Name:** [Your Full Name]  
**UFID:** [Your 8-digit UFID]  
**Email:** [your.email@ufl.edu]

---

## Introduction

This project implements an air traffic control scheduling system that manages flights across multiple runways. The key challenge was implementing a **Pairing Heap** and **Binary Min-Heaps from scratch** without using Java's built-in PriorityQueue or TreeMap.

I used the custom Pairing Heap for managing pending flights by priority, and two Binary Min-Heaps - one for runway allocation and another for flight completion tracking. The system uses a greedy scheduling algorithm and non-preemptive scheduling (once a flight starts, it can't be interrupted).

The trickiest part was handling the two-phase time advancement correctly - completing all finished flights first, then rescheduling everything that's waiting.

---

## Program Structure

**Main Entry Point:**
- `gatorAirTrafficScheduler.java` - Reads input file, parses commands, writes output

**Core Scheduler:**
- `AirTrafficScheduler.java` - Main scheduling logic, implements all 10 operations

**Custom Data Structures (all from scratch):**
- `PairingHeap.java` - Max-heap for pending flights  
- `RunwayHeap.java` - Min-heap for runway management  
- `CompletionHeap.java` - Min-heap for timetable

**Supporting:**
- `Flight.java` - Flight object with state tracking and heap handles
- `Makefile` - Compiles everything

---

## Data Structures

### 1. Pairing Heap (Pending Flights)

The spec required a Pairing Heap for managing pending flights. It has better amortized performance than binary heaps - O(1) insertions and O(log n) extractions.

**Structure:**
```java
class PairingNode {
    Flight flight;
    PairingNode child;      // leftmost child
    PairingNode sibling;    // right sibling
    PairingNode prev;       // parent or left sibling
}
```

It's ordered by (priority DESC, submitTime ASC, flightID ASC). Each flight stores a reference to its node for O(log n) updates.

**Key operations:** Insert O(1), Extract max O(log n), Delete O(log n), Increase priority O(log n)

### 2. Binary Min-Heap (Runways)

Tracks which runway becomes available next. Array-based with 1-indexing where parent is at i/2, children at 2i and 2i+1.

**Structure:**
```java
class Runway {
    int runwayID;
    int nextFreeTime;
    int heapIndex;    // for O(log n) updates
}
```

Ordered by (nextFreeTime ASC, runwayID ASC).

### 3. Binary Min-Heap (Completion/Timetable)

The spec required implementing this from scratch instead of using TreeMap. Manages all scheduled flights ordered by ETA.

**Structure:**
```java
class CompletionHeap {
    Flight[] heap;
    int size;
}
```

Ordered by (ETA ASC, flightID ASC). Each flight stores `completionHeapIndex` for O(log n) deletion.

### 4. Hash Tables & Index

- **ActiveFlights:** HashMap<Integer, Flight> for O(1) lookup by flightID
- **AirlineIndex:** HashMap<Integer, ArrayList<Flight>> for efficient GroundHold operation

---

## Flight Lifecycle

Flights go through four states: **PENDING → SCHEDULED → IN_PROGRESS → COMPLETED**

- **PENDING:** No runway assigned, stored in PairingHeap
- **SCHEDULED:** Has runway/time slot but hasn't started, stored in CompletionHeap  
- **IN_PROGRESS:** Using runway, cannot be canceled (non-preemptive)
- **COMPLETED:** Landed, removed from all structures

Flights can only be canceled or reprioritized when PENDING or SCHEDULED (not started).

---

## Algorithms

### Greedy Scheduling

```
While pending flights exist:
1. Extract highest priority flight (from PairingHeap)
2. Extract earliest available runway (from RunwayHeap)
3. Assign: startTime = max(currentTime, runway.nextFreeTime)
4. Set ETA = startTime + duration
5. Update runway.nextFreeTime = ETA
6. Insert flight into CompletionHeap
```

**Time complexity:** O(m log n + m log r) where m = flights to schedule, n = total flights, r = runways

### Two-Phase Time Advancement

Required before any operation with a currentTime parameter:

**Phase 1:** Extract and complete all flights with ETA ≤ currentTime  
**Promotion:** Mark scheduled flights with startTime ≤ currentTime as IN_PROGRESS  
**Phase 2:** Reschedule all unsatisfied flights (PENDING + SCHEDULED-not-started)

This ensures consistency - completed flights free resources before rescheduling.

---

## Operations Summary

**Initialize(numRunways)** - Creates runway system

**SubmitFlight(...)** - Adds new flight, checks duplicates, schedules if possible

**CancelFlight(...)** - Removes flight if not started, reschedules remaining

**Reprioritize(...)** - Changes flight priority, triggers rescheduling

**AddRunways(...)** - Adds capacity, reschedules to use new runways

**GroundHold(airlineLow, airlineHigh, ...)** - Removes flights from airline range

**PrintActive()** - Shows all active flights sorted by ID

**PrintSchedule(t1, t2)** - Shows scheduled flights with ETA in [t1, t2]

**Tick(t)** - Advances time, completes flights, reschedules

**Quit()** - Terminates program

---

## Function Prototypes

### gatorAirTrafficScheduler.java
```java
public class gatorAirTrafficScheduler {
    public static void main(String[] args)
}
```

### AirTrafficScheduler.java
```java
public class AirTrafficScheduler {
    // Data structures
    private PairingHeap pendingFlights;
    private HashMap<Integer, Flight> activeFlights;
    private HashMap<Integer, ArrayList<Flight>> airlineIndex;
    private CompletionHeap timetable;
    private int currentTime;
    private int nextRunwayID;
    private List<Runway> allRunways;
    
    // Public API (10 operations)
    public String initialize(int numRunways)
    public List<String> submitFlight(int flightID, int airlineID, 
                                      int currentTime, int priority, int duration)
    public List<String> cancelFlight(int flightID, int currentTime)
    public List<String> reprioritize(int flightID, int currentTime, int newPriority)
    public List<String> addRunways(int count, int currentTime)
    public List<String> groundHold(int airlineLow, int airlineHigh, int currentTime)
    public List<String> printActive()
    public List<String> printSchedule(int t1, int t2)
    public List<String> tick(int t)
    public String quit()
    
    // Helper methods
    private List<String> advanceTime(int newTime)
    private List<String> scheduleAll()
    private void removeFlight(Flight flight)
    private String generateETAUpdates(Map<Integer, Integer> oldETAs)
}
```

### Flight.java
```java
enum FlightState {
    PENDING, SCHEDULED, IN_PROGRESS, COMPLETED
}

public class Flight {
    int flightID;
    int airlineID;
    int priority;
    int duration;
    int submitTime;
    int startTime;
    int ETA;
    int runwayID;
    FlightState state;
    PairingNode heapNode;           // handle for Pairing Heap
    int completionHeapIndex;        // handle for Completion Heap
    
    public Flight(int flightID, int airlineID, int submitTime, 
                  int priority, int duration)
}
```

### PairingHeap.java
```java
class PairingNode {
    Flight flight;
    PairingNode child;
    PairingNode sibling;
    PairingNode prev;
}

public class PairingHeap {
    private PairingNode root;
    
    public void insert(Flight flight)
    public Flight extractMax()
    public void increaseKey(PairingNode node)
    public void delete(PairingNode node)
    public boolean isEmpty()
    
    private PairingNode meld(PairingNode a, PairingNode b)
    private PairingNode mergePairs(PairingNode node)
}
```

### RunwayHeap.java
```java
class Runway {
    int runwayID;
    int nextFreeTime;
    int heapIndex;
}

public class RunwayHeap {
    private Runway[] heap;
    private int size;
    private int capacity;
    
    public void insert(Runway runway)
    public Runway extractMin()
    public Runway findMin()
    public void updateRunway(Runway runway)
    
    private void heapifyUp(int index)
    private void heapifyDown(int index)
    private void swap(int i, int j)
}
```

### CompletionHeap.java
```java
public class CompletionHeap {
    private Flight[] heap;
    private int size;
    private int capacity;
    
    public void insert(Flight flight)
    public Flight extractMin()
    public Flight findMin()
    public List<Flight> extractAllUpTo(int time)
    public void delete(Flight flight)
    public List<Flight> getFlightsInRange(int t1, int t2)
    
    private void heapifyUp(int index)
    private void heapifyDown(int index)
    private void swap(int i, int j)
}
```

---

## Time Complexity Analysis

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| Initialize | O(r) | Create r runways |
| SubmitFlight | O(m log n + m log r) | Time advance + schedule |
| CancelFlight | O(m log n + m log r) | Time advance + reschedule |
| Reprioritize | O(m log n + m log r) | Time advance + heap update + reschedule |
| AddRunways | O(m log n + m log r) | Time advance + reschedule |
| GroundHold | O(k log n + m log n) | Remove k flights + reschedule |
| PrintActive | O(n log n) | Sort all active flights |
| PrintSchedule | O(n log n) | Filter and sort |
| Tick | O(k log n + m log n) | Complete k flights + reschedule m |

---

## Key Implementation Decisions

**Handle-Based Updates:** Each Flight stores its position in heaps (`heapNode`, `completionHeapIndex`) for O(log n) delete/update instead of O(n) search.

**Why Pairing Heap?** Better amortized insert performance (O(1) vs O(log n)) for our workload with frequent insertions.

**Why Custom CompletionHeap?** Spec required binary min-heap from scratch. More efficient for our primary use case (extracting completions) despite O(n) range queries.

---

## Challenges

1. **Two-phase time advancement** - Understanding that Phase 1 and 2 must run BEFORE every operation, not just Tick
2. **Pairing heap deletion** - The `prev` pointer can point to either parent or left sibling depending on position
3. **Handle maintenance** - Keeping heap handles correct after every swap and restructure
4. **Tie-breaking** - Getting priority comparison exactly right: (priority, submitTime, flightID)

The trickiest bug was including newly submitted flights in "Updated ETAs" output. Fixed by tracking old ETAs before rescheduling.

---

## Testing

Tested with all three provided test cases - all passed with exact output match.

**Edge cases handled:**
- Duplicate flight IDs
- Cancel/reprioritize in-progress flights
- Invalid inputs (count ≤ 0, high < low)
- Time advancement consistency

---

## Conclusion

This project forced me to deeply understand heap mechanics by implementing Pairing Heap and Binary Min-Heaps from scratch without any built-in libraries. The Pairing Heap's multi-way tree with two-pass merge was particularly complex but the O(1) insert performance made it worthwhile.

The two-phase time advancement pattern was new to me but makes sense for maintaining consistency in a scheduling system.

All test cases pass with exact output matching. The code is well-commented and ready for submission.

---

**Total Lines of Code:** ~2,400  
**Files:** 6 Java source files + Makefile

**Requirements Met:**
- ✅ Pairing Heap implemented from scratch
- ✅ Binary Min-Heaps implemented from scratch  
- ✅ All 10 operations working correctly
- ✅ Greedy scheduling with O(log n) operations
- ✅ Non-preemptive scheduling enforced
- ✅ All test cases passing
