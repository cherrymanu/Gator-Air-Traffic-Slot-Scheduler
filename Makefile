# Makefile for Gator Air Traffic Scheduler
# Compiles Java source files and creates executable

# Java compiler
JC = javac
JFLAGS = -g

# Java files to compile
SOURCES = Flight.java \
          PairingHeap.java \
          RunwayHeap.java \
          CompletionHeap.java \
          AirTrafficScheduler.java \
          gatorAirTrafficScheduler.java

# Class files (output)
CLASSES = $(SOURCES:.java=.class)

# Main class
MAIN = gatorAirTrafficScheduler

# Default target
all: $(CLASSES)

# Compile Java files
%.class: %.java
	$(JC) $(JFLAGS) $<

# Run the program (for testing)
run: all
	java $(MAIN) $(FILE)

# Clean compiled files
clean:
	rm -f *.class
	rm -f *_output_file.txt

# Help target
help:
	@echo "Makefile for Gator Air Traffic Scheduler"
	@echo ""
	@echo "Usage:"
	@echo "  make          - Compile all Java files"
	@echo "  make run FILE=<input_file> - Compile and run with input file"
	@echo "  make clean    - Remove all compiled files and output files"
	@echo "  make help     - Show this help message"
	@echo ""
	@echo "Example:"
	@echo "  make"
	@echo "  java gatorAirTrafficScheduler test1.txt"

.PHONY: all run clean help



