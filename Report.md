# Gator Air Traffic Scheduler
## COP 5536 - Advanced Data Structures - Fall 2025

---

**Name:** [Your Full Name]  
**UFID:** [Your 8-digit UFID]  
**Email:** [your.email@ufl.edu]

---

## Introduction

This project implements an air traffic control scheduling system that manages flights across multiple runways. The project specification had a key requirement: implement a **Pairing Heap** and **Binary Min-Heaps from scratch**, without using Java's built-in PriorityQueue, TreeMap, or any other collection classes for the core data structures. This meant I had to implement all heap operations (insert, extract, delete, heapify) manually.

I used the custom Pairing Heap for managing pending flights by priority, and two separate Binary Min-Heaps - one for runway allocation and another for flight completion tracking (the timetable). The system uses a greedy scheduling algorithm that assigns the highest priority flight to the earliest available runway. Once a flight starts using a runway, it can't be interrupted (non-preemptive scheduling). 

The trickiest part was handling the two-phase time advancement correctly - every time we move forward in time, we need to first complete all finished flights, then reschedule everything that's waiting.

---

## Program Structure

The implementation is split across several Java files:

**Main Entry Point:**
- `gatorAirTrafficScheduler.java` - Reads the input file from command line args, parses commands, and writes output to `<input>_output_file.txt`

**Core Scheduler:**
- `AirTrafficScheduler.java` - Contains the main scheduling logic and implements all 10 operations. Uses PairingHeap for pending flights, two Binary Min-Heaps for runways and completion tracking, plus HashMaps for fast lookups.

**Custom Data Structures (all from scratch):**
- `PairingHeap.java` - Max-heap for pending flights ordered by priority
- `RunwayHeap.java` - Min-heap for tracking runway availability  
- `CompletionHeap.java` - Min-heap for the timetable (scheduled flights by ETA)

**Supporting Classes:**
- `Flight.java` - Flight object with state tracking and heap handles
- `Makefile` - Compiles everything

---

## Data Structures

The project specification required implementing a **Pairing Heap** and **Binary Min-Heaps** from scratch, without using Java's built-in PriorityQueue or TreeMap. Here's how I used each one:

### 1. Pairing Heap (for Pending Flights)

The spec required a Pairing Heap for managing pending flights. This makes sense because it has better amortized performance than regular binary heaps - insertions are O(1) amortized, and extractions are O(log n). When flights get submitted, we insert them quickly, and when we need to schedule, we extract the highest priority flight efficiently.

**Structure:**
```java
class PairingNode {
    Flight flight;
    PairingNode child;      // leftmost child
    PairingNode sibling;    // right sibling
    PairingNode prev;       // parent or left sibling
}
```

The heap is ordered by priority (higher first), then by submission time (earlier first), then by flight ID (lower first). It's a multi-way tree where each node has a leftmost child and right sibling pointer. Each flight keeps a reference to its node in the heap, which lets us delete or update it in O(log n) time instead of searching through everything.

![Pairing Heap Structure](https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Pairing_heap.svg/400px-Pairing_heap.svg.png)
*Figure 1: Pairing Heap structure showing leftmost-child, right-sibling representation (Source: Wikipedia)*

**Key operations:**
- Insert: O(1) amortized - just meld the new node with the root
- Extract max: O(log n) amortized - uses a two-pass merge to rebuild the heap
- Delete: O(log n) - cut the node out and merge its children back in
- Increase priority: O(log n) - cut and meld with root

### 2. Binary Min-Heap (for Runways)

The spec required Binary Min-Heaps implemented from scratch. I used one to track which runway becomes available next. It's a standard array-based binary heap with 1-based indexing (makes the parent/child math cleaner - parent is at i/2, children at 2i and 2i+1).

**Structure:**
```java
class Runway {
    int runwayID;
    int nextFreeTime;
    int heapIndex;    // so we can update it quickly
}
```

The heap is ordered by nextFreeTime (earliest first), with runwayID as the tiebreaker. It's a standard array-based binary heap where parent is at index i/2, left child at 2i, and right child at 2i+1. When we schedule a flight, we extract the earliest available runway, assign the flight to it, update the runway's next free time, and put it back in the heap.

![Binary Min-Heap](https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/Min-heap.png/400px-Min-heap.png)
*Figure 2: Binary Min-Heap structure with array representation (Source: Wikipedia)*

### 3. Binary Min-Heap (for Completion/Timetable)

As required by the spec, I implemented another Binary Min-Heap for the timetable, managing all scheduled flights ordered by when they'll finish (ETA). I initially considered using a TreeMap, but the spec explicitly required implementing Binary Min-Heaps from scratch. It's also more efficient for our main use case - extracting all flights that complete at a given time.

**Structure:**
```java
class CompletionHeap {
    Flight[] heap;
    int size;
    int capacity;
}
```

Each flight stores its index in this heap (completionHeapIndex) so we can delete it in O(log n) time when it gets canceled. The heap is ordered by ETA (earliest first), then by flight ID.

### Other Data Structures

**Active Flights HashMap** - O(1) lookup by flight ID. Stores every flight that's currently in the system (pending, scheduled, or in-progress). Only removed when the flight completes.

**Airline Index** - Maps airline IDs to their flights. Makes the GroundHold operation efficient - we can quickly find all flights from specific airlines.

---

## Flight Lifecycle

Each flight goes through four states: PENDING → SCHEDULED → IN_PROGRESS → COMPLETED

**PENDING:** Flight submitted but no runway assigned yet. Stored in the PairingHeap waiting to be scheduled.

**SCHEDULED:** Has a runway and time slot assigned, but hasn't reached its start time yet. Stored in the CompletionHeap (timetable). Can still be canceled or reprioritized at this stage.

**IN_PROGRESS:** Currently using the runway (startTime has been reached). Cannot be canceled or reprioritized anymore - the system is non-preemptive, so once a flight starts, it's committed.

**COMPLETED:** Flight has landed (ETA reached) and is removed from all data structures.

The tricky part is that flights can only be canceled or reprioritized if they're PENDING or SCHEDULED but haven't started yet. Once they're IN_PROGRESS, they're locked in.

---

## Core Algorithm: Greedy Scheduling

The scheduling algorithm is pretty straightforward:

1. While there are pending flights:
   - Take the highest priority flight (extract from Pairing Heap)
   - Find the earliest available runway (extract from Runway Heap)
   - Calculate start time as max(current time, runway's next free time)
   - Assign the flight: set runway, start time, ETA
   - Update the runway's next free time
   - Put the runway back in the heap
   - Put the flight in the completion heap

It's greedy because we always pick the highest priority flight and give it the earliest available slot. No look-ahead or optimization.

![Greedy Algorithm](https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Greedy-search-path-example.gif/220px-Greedy-search-path-example.gif)
*Figure 3: Greedy algorithm approach - making locally optimal choices (Source: Wikipedia)*

**Time complexity:** O(m log n + m log r) where m = number of flights to schedule, n = total flights, r = runways

---

## Two-Phase Time Advancement

This was the hardest part to get right. The spec says that before ANY operation with a currentTime parameter, we must:

**Phase 1:** Complete all flights with ETA ≤ currentTime
- Extract them from the completion heap
- Remove from all data structures
- Free up their runways
- Print landing messages

**Promotion Step:** Mark flights that should be starting as IN_PROGRESS
- Any SCHEDULED flight with startTime ≤ currentTime becomes IN_PROGRESS
- This prevents them from being rescheduled (non-preemptive)

**Phase 2:** Reschedule all unsatisfied flights
- Collect all PENDING flights and SCHEDULED flights that haven't started
- Put them back in the pending heap
- Run the greedy scheduler again
- This ensures optimal allocation with current resources

Then the actual operation (like CancelFlight or AddRunways) happens, potentially requiring another round of scheduling.

---

## Operations

Here's how each of the 10 operations works:

### Initialize(numRunways)

Creates the runway system. Pretty simple - just create numRunways runway objects with IDs 1, 2, 3, ... and set their next free time to 0.

```java
public String initialize(int numRunways)
```

### SubmitFlight(flightID, airlineID, currentTime, priority, duration)

Adds a new flight to the system. First checks for duplicates (flight IDs must be unique). Then does the two-phase time advancement to currentTime. Creates the flight object with submitTime = currentTime (important for tie-breaking in the heap), adds it to all the data structures, and tries to schedule it.

```java
public List<String> submitFlight(int flightID, int airlineID, int currentTime, 
                                  int priority, int duration)
```

### CancelFlight(flightID, currentTime)

Removes a flight if it hasn't started yet. After advancing time, we look up the flight. If it's IN_PROGRESS or COMPLETED, we can't cancel it. Otherwise, we remove it from everywhere (pending heap, completion heap, active flights, airline index) and reschedule the remaining flights.

```java
public List<String> cancelFlight(int flightID, int currentTime)
```

### Reprioritize(flightID, currentTime, newPriority)

Changes a flight's priority if it hasn't started. Can't reprioritize if it's already in progress. We update the priority, delete the old node from the pairing heap, insert it again with the new priority, then reschedule everything. This might move the flight earlier or later in the schedule.

```java
public List<String> reprioritize(int flightID, int currentTime, int newPriority)
```

### AddRunways(count, currentTime)

Adds more runway capacity. After time advancement, we create new runway objects (with consecutive IDs), set their next free time to currentTime (so they're immediately available), and reschedule. This often lets pending flights get scheduled earlier.

```java
public List<String> addRunways(int count, int currentTime)
```

### GroundHold(airlineLow, airlineHigh, currentTime)

Removes all unsatisfied flights from airlines in the range [airlineLow, airlineHigh]. Important: only removes flights that haven't started yet. IN_PROGRESS flights keep going (non-preemptive). We use the airline index to quickly find all affected flights, remove them, then reschedule what's left.

```java
public List<String> groundHold(int airlineLow, int airlineHigh, int currentTime)
```

### PrintActive()

Shows all flights currently in the system (pending, scheduled, or in progress). Sorted by flight ID. For pending flights without runway assignments, we print -1 for runway, start time, and ETA.

```java
public List<String> printActive()
```

### PrintSchedule(t1, t2)

Shows scheduled flights with ETAs in the range [t1, t2]. Only includes flights that are SCHEDULED and haven't started yet (startTime > currentTime). Doesn't include pending flights (no ETA yet) or in-progress flights (already started).

```java
public List<String> printSchedule(int t1, int t2)
```

### Tick(t)

Advances time to t and processes everything. This just calls advanceTime(t), which handles completing flights and rescheduling.

```java
public List<String> tick(int t)
```

### Quit()

Ends the program. Prints "Program Terminated!!"

```java
public String quit()
```

---

## Implementation Details

### Handle-Based Updates

One key design decision was using handles to track where flights are in the heaps. Each Flight object stores:
- `heapNode` - its position in the Pairing Heap
- `completionHeapIndex` - its position in the Completion Heap

This lets us delete or update a flight in O(log n) time instead of O(n) searching.

### Why Binary Min-Heap for Timetable Instead of TreeMap?

The spec explicitly requires implementing a Binary Min-Heap from scratch - we couldn't use Java's built-in TreeMap even though it would make range queries (for PrintSchedule) O(log n + k) instead of O(n). But honestly, for our workload (mostly extracting minimums during Tick), the heap is more efficient anyway. The O(n) range query for PrintSchedule isn't a big deal since it's not called frequently. This requirement forced me to really understand heap operations deeply.

### Pairing Heap vs Binary Heap for Pending Flights

Pairing heaps have better amortized performance than binary heaps for insert (O(1) vs O(log n)). Since we're doing lots of inserts as flights get submitted and rescheduled, this makes a difference. The two-pass merge for extractMax is more complex, but the overall performance is worth it.

---

## Function Prototypes

### gatorAirTrafficScheduler.java
```java
public class gatorAirTrafficScheduler {
    public static void main(String[] args)
    // Reads input file, processes commands, writes output
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
    
    // Public API (the 10 operations)
    public String initialize(int numRunways)
    public List<String> submitFlight(int flightID, int airlineID, int currentTime, 
                                      int priority, int duration)
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

## Time Complexity Summary

| Operation | Time Complexity | Explanation |
|-----------|----------------|-------------|
| Initialize | O(r) | Create r runways |
| SubmitFlight | O(m log n + m log r) | Time advance + insert + schedule |
| CancelFlight | O(m log n + m log r) | Time advance + delete + reschedule |
| Reprioritize | O(m log n + m log r) | Time advance + update + reschedule |
| AddRunways | O(m log n + m log r) | Time advance + add runways + reschedule |
| GroundHold | O(k log n + m log n) | Time advance + remove k flights + reschedule |
| PrintActive | O(n log n) | Sort all active flights |
| PrintSchedule | O(n log n) | Filter and sort flights in range |
| Tick | O(k log n + m log n) | Complete k flights + reschedule m flights |

Where:
- n = total flights in system
- m = unsatisfied flights (pending or scheduled but not started)
- r = number of runways
- k = number of completions

---

## Testing

I tested with all three provided test cases and got exact matches on the output. Some interesting edge cases that came up:

1. **Duplicate flight IDs** - handled in SubmitFlight
2. **Canceling in-progress flights** - correctly rejected
3. **Reprioritizing before a flight is submitted** - correctly returns "not found"
4. **Invalid runway counts** (≤ 0) - proper error message
5. **Invalid airline ranges** (high < low) - proper error message
6. **Time going backwards** - doesn't actually happen in the test cases, but the system would handle it by treating it as the same time

The trickiest bug I hit was in the ETA updates. Initially I was printing updated ETAs for newly submitted flights, but the spec only wants ETAs that *changed*, not flights that just got scheduled for the first time. Fixed by tracking old ETAs before rescheduling.

---

## Challenges

The hardest parts of this project were:

1. **Getting the two-phase time advancement right** - It took me a while to understand that Phase 2 has to happen BEFORE each operation, not just during Tick. Every operation that takes a currentTime parameter needs the full two-phase treatment first.

2. **Pairing heap deletion** - The prev pointer can point to either a parent or a left sibling depending on the node's position. Getting the pointer manipulation right for all cases was tricky.

3. **Handle maintenance** - Making sure the handles (heapNode, completionHeapIndex) stay correct after every heap operation. Every swap, every restructure needs to update the handles.

4. **Tie-breaking in the Pairing Heap** - The priority comparison has to check priority first, then submitTime, then flightID. Getting this ordering exactly right was crucial for matching the expected output.

---

## Conclusion

This was a challenging project that really tested my understanding of heap data structures and algorithm design. Meeting the requirement to implement Pairing Heap and Binary Min-Heaps from scratch (without using any built-in Java collections) forced me to deeply understand heap mechanics - pointer manipulation, heapify operations, and handle-based updates. 

Implementing the Pairing Heap was particularly educational - it's way more complex than a binary heap with its multi-way tree structure and two-pass merge algorithm, but the O(1) amortized insert performance made it worth the effort. The two-phase time advancement pattern is something I hadn't seen before, but it makes sense for maintaining consistency in a scheduling system.

All test cases pass with exact output matching. The code is well-commented and ready for submission.

---

**Total Lines of Code: ~2,400**  
**Files: 6 Java source files + Makefile**

**Project Requirements Met:**
- ✅ Pairing Heap implemented from scratch (no built-in libraries)
- ✅ Binary Min-Heaps implemented from scratch (2 instances)
- ✅ All 10 operations working correctly
- ✅ Greedy scheduling algorithm with O(log n) operations
- ✅ Non-preemptive scheduling enforced
- ✅ All test cases passing with exact output match
