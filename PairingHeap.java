/**
 * Pairing Heap (Max-Heap) Implementation
 * Used for managing pending flights by priority
 * 
 * Key Operations:
 * - insert: O(1)
 * - extractMax: O(log n) amortized
 * - increaseKey: O(log n) amortized
 * - delete: O(log n) amortized
 */
public class PairingHeap {
    private PairingNode root;
    private int size;
    
    public PairingHeap() {
        this.root = null;
        this.size = 0;
    }
    
    /**
     * Check if heap is empty
     */
    public boolean isEmpty() {
        return root == null;
    }
    
    /**
     * Get size of heap
     */
    public int size() {
        return size;
    }
    
    /**
     * Insert a flight into the heap
     * Returns the node handle for future updates
     */
    public PairingNode insert(Flight flight) {
        PairingNode newNode = new PairingNode(flight);
        flight.heapNode = newNode;
        root = meld(root, newNode);
        size++;
        return newNode;
    }
    
    /**
     * Get the maximum priority flight without removing
     */
    public Flight findMax() {
        if (root == null) {
            return null;
        }
        return root.flight;
    }
    
    /**
     * Extract and return the maximum priority flight
     */
    public Flight extractMax() {
        if (root == null) {
            return null;
        }
        
        Flight maxFlight = root.flight;
        root = mergePairs(root.child);
        size--;
        
        if (maxFlight.heapNode != null) {
            maxFlight.heapNode = null;
        }
        
        return maxFlight;
    }
    
    /**
     * Increase the priority of a flight
     * Used when reprioritizing with higher priority
     */
    public void increaseKey(PairingNode node, int newPriority) {
        if (node == null || node.flight == null) {
            return;
        }
        
        node.flight.priority = newPriority;
        
        // If it's the root, we're done
        if (node == root) {
            return;
        }
        
        // Cut the node from its current position
        cutNode(node);
        
        // Meld with root
        root = meld(root, node);
    }
    
    /**
     * Delete a node from the heap
     * Used when canceling flights or when priority decreases
     */
    public void delete(PairingNode node) {
        if (node == null) {
            return;
        }
        
        // If it's the root, just extract
        if (node == root) {
            extractMax();
            return;
        }
        
        // Cut the node from its position
        cutNode(node);
        
        // Merge its children back into the heap
        if (node.child != null) {
            PairingNode mergedChildren = mergePairs(node.child);
            root = meld(root, mergedChildren);
        }
        
        size--;
        node.flight.heapNode = null;
    }
    
    /**
     * Meld (merge) two heaps
     * The one with higher priority becomes the root
     */
    private PairingNode meld(PairingNode a, PairingNode b) {
        if (a == null) return b;
        if (b == null) return a;
        
        // Determine which node should be parent
        // Compare: (priority, -submitTime, -flightID)
        boolean aIsGreater = false;
        
        // Compare priorities (max-heap: higher priority wins)
        if (a.flight.priority > b.flight.priority) {
            aIsGreater = true;
        } else if (a.flight.priority == b.flight.priority) {
            // Tie-breaker 1: earlier submitTime wins
            if (a.flight.submitTime < b.flight.submitTime) {
                aIsGreater = true;
            } else if (a.flight.submitTime == b.flight.submitTime) {
                // Tie-breaker 2: lower flightID wins
                if (a.flight.flightID < b.flight.flightID) {
                    aIsGreater = true;
                }
            }
        }
        
        if (aIsGreater) {
            // a becomes parent of b
            b.sibling = a.child;
            if (a.child != null) {
                a.child.prev = b;
            }
            a.child = b;
            b.prev = a;
            return a;
        } else {
            // b becomes parent of a
            a.sibling = b.child;
            if (b.child != null) {
                b.child.prev = a;
            }
            b.child = a;
            a.prev = b;
            return b;
        }
    }
    
    /**
     * Merge pairs of siblings (two-pass algorithm)
     */
    private PairingNode mergePairs(PairingNode first) {
        if (first == null || first.sibling == null) {
            if (first != null) {
                first.prev = null;
            }
            return first;
        }
        
        // First pass: merge pairs from left to right
        PairingNode next = first.sibling;
        PairingNode remaining = next.sibling;
        
        first.sibling = null;
        first.prev = null;
        next.sibling = null;
        next.prev = null;
        
        PairingNode merged = meld(first, next);
        PairingNode rest = mergePairs(remaining);
        
        // Second pass: merge results from right to left
        return meld(merged, rest);
    }
    
    /**
     * Cut a node from its parent/siblings
     */
    private void cutNode(PairingNode node) {
        if (node.prev != null) {
            if (node.prev.child == node) {
                // node is the leftmost child
                node.prev.child = node.sibling;
            } else {
                // node is a sibling
                node.prev.sibling = node.sibling;
            }
            
            if (node.sibling != null) {
                node.sibling.prev = node.prev;
            }
        }
        
        node.prev = null;
        node.sibling = null;
    }
    
    /**
     * Clear the entire heap
     */
    public void clear() {
        root = null;
        size = 0;
    }
}

/**
 * Node class for Pairing Heap
 */
class PairingNode {
    Flight flight;
    PairingNode child;    // Leftmost child
    PairingNode sibling;  // Right sibling
    PairingNode prev;     // Parent or left sibling
    
    public PairingNode(Flight flight) {
        this.flight = flight;
        this.child = null;
        this.sibling = null;
        this.prev = null;
    }
}



