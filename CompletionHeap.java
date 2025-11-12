/**
 * Binary Min-Heap Implementation for Flight Completions (Timetable)
 * Tracks scheduled flights sorted by completion time (ETA)
 * 
 * Key Operations:
 * - insert: O(log n)
 * - extractMin: O(log n)
 * - findMin: O(1)
 * - extractAllUpTo(t): O(k log n) where k is number of completions
 */
public class CompletionHeap {
    private Flight[] heap;
    private int size;
    private int capacity;
    
    /**
     * Constructor
     */
    public CompletionHeap(int capacity) {
        this.capacity = capacity;
        this.heap = new Flight[capacity + 1]; // 1-indexed for easier parent/child calculation
        this.size = 0;
    }
    
    /**
     * Check if heap is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Get size of heap
     */
    public int size() {
        return size;
    }
    
    /**
     * Insert a flight into the heap
     */
    public void insert(Flight flight) {
        if (flight.ETA <= 0) {
            return; // Don't insert pending/invalid flights
        }
        
        if (size >= capacity) {
            // Expand capacity
            capacity = capacity * 2;
            Flight[] newHeap = new Flight[capacity + 1];
            System.arraycopy(heap, 0, newHeap, 0, size + 1);
            heap = newHeap;
        }
        
        size++;
        heap[size] = flight;
        flight.completionHeapIndex = size;
        heapifyUp(size);
    }
    
    /**
     * Get the flight with earliest ETA (without removing)
     */
    public Flight findMin() {
        if (size == 0) {
            return null;
        }
        return heap[1];
    }
    
    /**
     * Extract and return the flight with earliest ETA
     */
    public Flight extractMin() {
        if (size == 0) {
            return null;
        }
        
        Flight min = heap[1];
        heap[1] = heap[size];
        if (heap[1] != null) {
            heap[1].completionHeapIndex = 1;
        }
        heap[size] = null;
        size--;
        
        if (size > 0) {
            heapifyDown(1);
        }
        
        min.completionHeapIndex = -1;
        return min;
    }
    
    /**
     * Remove a specific flight from the heap
     * Used when a flight is cancelled or grounded
     */
    public void delete(Flight flight) {
        if (flight.completionHeapIndex <= 0 || flight.completionHeapIndex > size) {
            return; // Not in heap
        }
        
        int index = flight.completionHeapIndex;
        
        // Replace with last element
        heap[index] = heap[size];
        if (heap[index] != null) {
            heap[index].completionHeapIndex = index;
        }
        heap[size] = null;
        size--;
        
        flight.completionHeapIndex = -1;
        
        // Restore heap property
        if (size > 0 && index <= size) {
            heapifyUp(index);
            heapifyDown(index);
        }
    }
    
    /**
     * Extract all flights with ETA <= t
     * Returns list sorted by (ETA, flightID)
     */
    public java.util.List<Flight> extractAllUpTo(int t) {
        java.util.List<Flight> completed = new java.util.ArrayList<>();
        
        // Extract all flights with ETA <= t
        while (!isEmpty() && heap[1].ETA <= t) {
            completed.add(extractMin());
        }
        
        // Sort by (ETA, flightID) as required by spec
        completed.sort((a, b) -> {
            if (a.ETA != b.ETA) return Integer.compare(a.ETA, b.ETA);
            return Integer.compare(a.flightID, b.flightID);
        });
        
        return completed;
    }
    
    /**
     * Get all flights with ETA in range [t1, t2] without removing
     * Used for PrintSchedule operation
     */
    public java.util.List<Flight> getFlightsInRange(int t1, int t2, int currentTime) {
        java.util.List<Flight> result = new java.util.ArrayList<>();
        
        // Scan entire heap (can't avoid O(n) for range query)
        for (int i = 1; i <= size; i++) {
            Flight flight = heap[i];
            if (flight.ETA >= t1 && flight.ETA <= t2) {
                // Only include scheduled flights that haven't started
                if (flight.state == FlightState.SCHEDULED && flight.startTime > currentTime) {
                    result.add(flight);
                }
            }
        }
        
        // Sort by (ETA, flightID)
        result.sort((a, b) -> {
            if (a.ETA != b.ETA) return Integer.compare(a.ETA, b.ETA);
            return Integer.compare(a.flightID, b.flightID);
        });
        
        return result;
    }
    
    /**
     * Heapify up from position i
     */
    private void heapifyUp(int i) {
        while (i > 1) {
            int parent = i / 2;
            
            if (compare(heap[i], heap[parent]) < 0) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }
    
    /**
     * Heapify down from position i
     */
    private void heapifyDown(int i) {
        while (2 * i <= size) {
            int left = 2 * i;
            int right = 2 * i + 1;
            int smallest = i;
            
            if (left <= size && compare(heap[left], heap[smallest]) < 0) {
                smallest = left;
            }
            
            if (right <= size && compare(heap[right], heap[smallest]) < 0) {
                smallest = right;
            }
            
            if (smallest != i) {
                swap(i, smallest);
                i = smallest;
            } else {
                break;
            }
        }
    }
    
    /**
     * Swap two elements in the heap
     */
    private void swap(int i, int j) {
        Flight temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
        
        heap[i].completionHeapIndex = i;
        heap[j].completionHeapIndex = j;
    }
    
    /**
     * Compare two flights
     * Returns negative if a < b, 0 if equal, positive if a > b
     * Comparison: first by ETA, then by flightID
     */
    private int compare(Flight a, Flight b) {
        if (a.ETA != b.ETA) {
            return Integer.compare(a.ETA, b.ETA);
        }
        return Integer.compare(a.flightID, b.flightID);
    }
    
    /**
     * Clear the heap
     */
    public void clear() {
        for (int i = 1; i <= size; i++) {
            if (heap[i] != null) {
                heap[i].completionHeapIndex = -1;
            }
            heap[i] = null;
        }
        size = 0;
    }
    
    /**
     * Rebuild entire heap (used after major changes)
     */
    public void buildHeap() {
        for (int i = size / 2; i >= 1; i--) {
            heapifyDown(i);
        }
    }
}

