/**
 * Binary Min-Heap Implementation for Runways
 * Used to efficiently find the runway with earliest available time
 * 
 * Key Operations:
 * - insert: O(log n)
 * - extractMin: O(log n)
 * - decreaseKey: O(log n)
 * - findMin: O(1)
 */
public class RunwayHeap {
    private Runway[] heap;
    private int size;
    private int capacity;
    
    /**
     * Constructor
     */
    public RunwayHeap(int capacity) {
        this.capacity = capacity;
        this.heap = new Runway[capacity + 1]; // 1-indexed for easier parent/child calculation
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
     * Insert a runway into the heap
     */
    public void insert(Runway runway) {
        if (size >= capacity) {
            // Expand capacity
            capacity = capacity * 2;
            Runway[] newHeap = new Runway[capacity + 1];
            System.arraycopy(heap, 0, newHeap, 0, size + 1);
            heap = newHeap;
        }
        
        size++;
        heap[size] = runway;
        runway.heapIndex = size;
        heapifyUp(size);
    }
    
    /**
     * Get the runway with earliest available time (without removing)
     */
    public Runway findMin() {
        if (size == 0) {
            return null;
        }
        return heap[1];
    }
    
    /**
     * Extract and return the runway with earliest available time
     */
    public Runway extractMin() {
        if (size == 0) {
            return null;
        }
        
        Runway min = heap[1];
        heap[1] = heap[size];
        heap[1].heapIndex = 1;
        heap[size] = null;
        size--;
        
        if (size > 0) {
            heapifyDown(1);
        }
        
        min.heapIndex = -1;
        return min;
    }
    
    /**
     * Update runway's nextFreeTime and restore heap property
     * Called after assigning a flight to a runway
     */
    public void updateRunway(Runway runway, int newNextFreeTime) {
        runway.nextFreeTime = newNextFreeTime;
        
        if (runway.heapIndex > 0 && runway.heapIndex <= size) {
            // Since nextFreeTime typically increases, heapify down
            heapifyDown(runway.heapIndex);
        }
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
        Runway temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
        
        heap[i].heapIndex = i;
        heap[j].heapIndex = j;
    }
    
    /**
     * Compare two runways
     * Returns negative if a < b, 0 if equal, positive if a > b
     * Comparison: first by nextFreeTime, then by runwayID
     */
    private int compare(Runway a, Runway b) {
        if (a.nextFreeTime != b.nextFreeTime) {
            return Integer.compare(a.nextFreeTime, b.nextFreeTime);
        }
        return Integer.compare(a.runwayID, b.runwayID);
    }
    
    /**
     * Rebuild heap from array (used when adding multiple runways)
     */
    public void buildHeap() {
        for (int i = size / 2; i >= 1; i--) {
            heapifyDown(i);
        }
    }
}

/**
 * Runway class - Represents a runway in the system
 */
class Runway {
    int runwayID;
    int nextFreeTime;
    int heapIndex;  // Position in heap (for efficient updates)
    
    public Runway(int runwayID, int nextFreeTime) {
        this.runwayID = runwayID;
        this.nextFreeTime = nextFreeTime;
        this.heapIndex = -1;
    }
    
    @Override
    public String toString() {
        return String.format("Runway %d (free at %d)", runwayID, nextFreeTime);
    }
}



