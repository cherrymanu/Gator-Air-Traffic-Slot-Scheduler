# COP 5536 Fall 2025
## Programming Project Report

**Name:** Charishma Manupati  
**UFID:** 35457549  
**UF Email:** cmanupati@ufl.edu

---

## Problem Statement

The goal was to implement an air traffic control scheduling system that manages flights across multiple runways. The project requirement was to implement Pairing Heap and Binary Min-Heaps from scratch without using Java's built-in PriorityQueue or TreeMap.

The system must handle 10 operations: Initialize, SubmitFlight, CancelFlight, Reprioritize, AddRunways, GroundHold, PrintActive, PrintSchedule, Tick, and Quit. Flights follow a lifecycle (PENDING → SCHEDULED → IN_PROGRESS → COMPLETED) with non-preemptive scheduling.

## Data Structures

### Pairing Heap
The Pairing Heap is a multi-way tree implementation used for the pending flights priority queue. It has better amortized performance than binary heaps with O(1) insert and O(log n) extractMax operations. The heap orders flights by (priority DESC, submitTime ASC, flightID ASC).

### Binary Min-Heap (Runway Heap)
A binary min-heap implemented using arrays to track runway availability. Runways are ordered by (nextFreeTime ASC, runwayID ASC). Uses 1-based indexing where parent is at i/2, left child at 2i, and right child at 2i+1.

### Binary Min-Heap (Completion Heap)
Another binary min-heap for the timetable that tracks scheduled flights by completion time. Flights are ordered by (ETA ASC, flightID ASC). This structure enables efficient extraction of completed flights during time advancement.

### Hash Tables
HashMap<Integer, Flight> for O(1) flight lookups by flightID, and HashMap<Integer, ArrayList<Flight>> for airline indexing to support efficient GroundHold operations.

The idea is to have a Flight object store handles to its positions in heaps. Each Flight stores heapNode (position in Pairing Heap) and completionHeapIndex (position in Completion Heap) for O(log n) updates instead of O(n) searching.

## Operations

### Initialize
Creates the runway system with the specified number of runways. Each runway gets a unique ID starting from 1 and initial nextFreeTime of 0. Takes O(r) time where r is the number of runways.

### SubmitFlight
Adds a new flight to the system. First checks for duplicate flightIDs. Advances time to currentTime, creates the flight with submitTime recorded, adds it to all data structures (PairingHeap, activeFlights, airlineIndex), and attempts to schedule it using the greedy algorithm.

### CancelFlight
Removes a flight that hasn't started yet. If the flight is IN_PROGRESS or COMPLETED, cancellation is rejected. Otherwise, removes it from all structures and reschedules remaining flights. Takes O(log n) for deletion plus O(m log n) for rescheduling.

### Reprioritize
Changes a flight's priority if it hasn't started. Updates the priority in the Pairing Heap (delete old node, insert with new priority) and reschedules all unsatisfied flights. This can move the flight earlier or later in the schedule.

### AddRunways
Adds new runways to increase system capacity. New runways are immediately available (nextFreeTime = currentTime) and the system reschedules flights to utilize the additional capacity.

### GroundHold
Removes unsatisfied flights from airlines in the specified range [airlineLow, airlineHigh]. Only affects PENDING or SCHEDULED-not-started flights. IN_PROGRESS flights continue (non-preemptive). Uses the airline index for efficient lookup.

### PrintActive
Shows all active flights (pending, scheduled, or in-progress) sorted by flightID. For pending flights without assignments, prints -1 for runway, startTime, and ETA.

### PrintSchedule
Shows scheduled flights with ETAs in the range [t1, t2]. Only includes SCHEDULED flights that haven't started yet. Filters out pending and in-progress flights.

### Tick
Advances system time to t. Completes all flights with ETA ≤ t, marks starting flights as IN_PROGRESS, and reschedules unsatisfied flights. This implements the two-phase time advancement.

### Quit
Terminates the program.

## Scheduling Algorithm

The greedy scheduling algorithm works as follows:
1. Extract highest priority flight from PairingHeap
2. Extract earliest available runway from RunwayHeap
3. Calculate startTime = max(currentTime, runway.nextFreeTime)
4. Assign flight to runway with ETA = startTime + duration
5. Update runway.nextFreeTime = ETA
6. Insert flight into CompletionHeap
7. Repeat until no pending flights remain

Time complexity is O(m log n + m log r) where m = flights to schedule, n = total flights, r = runways.

## Two-Phase Time Advancement

Before any operation with a currentTime parameter, the system must:
- **Phase 1:** Extract and complete all flights with ETA ≤ currentTime
- **Promotion Step:** Mark scheduled flights with startTime ≤ currentTime as IN_PROGRESS (non-preemptive)
- **Phase 2:** Reschedule all unsatisfied flights using the greedy algorithm

This ensures consistency by completing flights and freeing resources before rescheduling.

## Implementation Details

### gatorAirTrafficScheduler.java
The main function:
- Reads input file name from command line arguments
- Establishes input file stream and output file stream to `<input>_output_file.txt`
- Instantiates AirTrafficScheduler
- Reads and parses the input file, calling the respective methods
- Writes output to file

### AirTrafficScheduler.java
This class manages the core scheduling logic and contains all data structures:
- `private PairingHeap pendingFlights` - Pending flights ordered by priority
- `private HashMap<Integer, Flight> activeFlights` - All active flights
- `private HashMap<Integer, ArrayList<Flight>> airlineIndex` - Flights by airline
- `private CompletionHeap timetable` - Scheduled flights by ETA
- `private List<Runway> allRunways` - All runways in the system
- `private int currentTime` - Current system time
- `private int nextRunwayID` - Counter for runway IDs

**Public Methods (10 operations):**
- `String initialize(int numRunways)` - Creates runway system
- `List<String> submitFlight(int flightID, int airlineID, int currentTime, int priority, int duration)` - Adds and schedules new flight
- `List<String> cancelFlight(int flightID, int currentTime)` - Removes flight if not started
- `List<String> reprioritize(int flightID, int currentTime, int newPriority)` - Changes flight priority and reschedules
- `List<String> addRunways(int count, int currentTime)` - Adds runway capacity
- `List<String> groundHold(int airlineLow, int airlineHigh, int currentTime)` - Removes flights from airline range
- `List<String> printActive()` - Shows all active flights
- `List<String> printSchedule(int t1, int t2)` - Shows scheduled flights in time range
- `List<String> tick(int t)` - Advances time and processes completions
- `String quit()` - Terminates program

**Private Helper Methods:**
- `List<String> advanceTime(int newTime)` - Implements two-phase time advancement
- `List<String> scheduleAll()` - Greedy scheduling algorithm
- `void removeFlight(Flight flight)` - Removes flight from all structures
- `String generateETAUpdates(Map<Integer, Integer> oldETAs)` - Generates ETA change output

### Flight.java
Represents a flight with all its attributes:
- `int flightID` - Unique identifier
- `int airlineID` - Airline identifier
- `int priority` - Scheduling priority
- `int duration` - Flight duration
- `int submitTime` - Time when flight was submitted
- `int startTime` - When flight starts using runway (-1 if not scheduled)
- `int ETA` - Estimated time of arrival (-1 if not scheduled)
- `int runwayID` - Assigned runway (-1 if not scheduled)
- `FlightState state` - Current state (PENDING, SCHEDULED, IN_PROGRESS, COMPLETED)
- `PairingNode heapNode` - Handle for position in PairingHeap
- `int completionHeapIndex` - Handle for position in CompletionHeap

Constructor: `Flight(int flightID, int airlineID, int submitTime, int priority, int duration)`

### PairingHeap.java
Implements the Pairing Heap with leftmost-child, right-sibling representation.

**PairingNode structure:**
- `Flight flight` - The flight data
- `PairingNode child` - Leftmost child
- `PairingNode sibling` - Right sibling
- `PairingNode prev` - Parent or left sibling

**Public Methods:**
- `void insert(Flight flight)` - Inserts flight into heap in O(1) amortized time
- `Flight extractMax()` - Removes and returns highest priority flight in O(log n) time
- `void increaseKey(PairingNode node)` - Increases priority of a node in O(log n) time
- `void delete(PairingNode node)` - Removes a node in O(log n) time
- `boolean isEmpty()` - Checks if heap is empty

**Private Methods:**
- `PairingNode meld(PairingNode a, PairingNode b)` - Melds two pairing heaps
- `PairingNode mergePairs(PairingNode node)` - Two-pass merge for extract operation

### RunwayHeap.java
Implements array-based binary min-heap for runway management.

**Runway structure:**
- `int runwayID` - Unique identifier
- `int nextFreeTime` - When runway becomes available
- `int heapIndex` - Position in heap array (for O(log n) updates)

**Public Methods:**
- `void insert(Runway runway)` - Adds runway to heap
- `Runway extractMin()` - Removes and returns earliest available runway
- `Runway findMin()` - Returns earliest available runway without removing
- `void updateRunway(Runway runway)` - Updates runway after use

**Private Methods:**
- `void heapifyUp(int index)` - Restores heap property upward
- `void heapifyDown(int index)` - Restores heap property downward
- `void swap(int i, int j)` - Swaps two elements and updates their heapIndex

### CompletionHeap.java
Implements array-based binary min-heap for the timetable.

**Public Methods:**
- `void insert(Flight flight)` - Adds flight to timetable
- `Flight extractMin()` - Removes and returns next completing flight
- `Flight findMin()` - Returns next completing flight without removing
- `List<Flight> extractAllUpTo(int time)` - Extracts all flights with ETA ≤ time (for Tick)
- `void delete(Flight flight)` - Removes flight using its completionHeapIndex handle
- `List<Flight> getFlightsInRange(int t1, int t2)` - Returns flights with ETA in [t1, t2] (for PrintSchedule)

**Private Methods:**
- `void heapifyUp(int index)` - Restores heap property upward
- `void heapifyDown(int index)` - Restores heap property downward  
- `void swap(int i, int j)` - Swaps two flights and updates their completionHeapIndex

## Time Complexity Analysis

| Operation | Time Complexity | Explanation |
|-----------|----------------|-------------|
| Initialize | O(r) | Create r runways |
| SubmitFlight | O(m log n + m log r) | Time advance + insert + schedule |
| CancelFlight | O(m log n + m log r) | Time advance + delete + reschedule |
| Reprioritize | O(m log n + m log r) | Time advance + heap update + reschedule |
| AddRunways | O(m log n + m log r) | Time advance + create runways + reschedule |
| GroundHold | O(k log n + m log n) | Time advance + remove k flights + reschedule |
| PrintActive | O(n log n) | Sort all active flights |
| PrintSchedule | O(n log n) | Filter and sort scheduled flights |
| Tick | O(k log n + m log n) | Complete k flights + reschedule m flights |

Where: n = total flights, m = unsatisfied flights, r = runways, k = completions

## Key Design Decisions

**Handle-Based Updates:** Each Flight stores its position in heaps (heapNode, completionHeapIndex) enabling O(log n) delete/update operations instead of O(n) searching.

**Pairing Heap Choice:** Better amortized insert performance (O(1) vs O(log n) for binary heap) benefits our workload with frequent insertions during submissions and rescheduling.

**Custom Completion Heap:** Spec required Binary Min-Heap from scratch. More efficient for our primary use case (extracting completions during Tick) despite O(n) range queries for PrintSchedule.

**Two-Phase Pattern:** Ensures consistency by always completing flights and freeing resources before any operation executes. Critical for maintaining valid system state.

## Conclusion

The required Gator Air Traffic Scheduler is implemented using Pairing Heap and Binary Min-Heaps (all from scratch) achieving the required time complexities. All 10 operations work correctly with greedy scheduling and non-preemptive execution. All test cases pass with exact output matching.

**Total Implementation:** ~2,400 lines of code across 6 Java source files + Makefile
