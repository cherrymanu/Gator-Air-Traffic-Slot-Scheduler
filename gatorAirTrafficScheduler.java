import java.io.*;
import java.util.*;

/**
 * Main class for Gator Air Traffic Scheduler
 * Handles command-line input/output and command parsing
 * 
 * Usage: java gatorAirTrafficScheduler input_filename
 */
public class gatorAirTrafficScheduler {
    
    public static void main(String[] args) {
        // Check command-line arguments
        if (args.length != 1) {
            System.err.println("Usage: java gatorAirTrafficScheduler <input_filename>");
            System.exit(1);
        }
        
        String inputFilename = args[0];
        String outputFilename = inputFilename.replace(".txt", "") + "_output_file.txt";
        
        AirTrafficScheduler scheduler = new AirTrafficScheduler();
        
        try {
            // Read input file
            BufferedReader reader = new BufferedReader(new FileReader(inputFilename));
            // Write output file
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename));
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                // Parse and execute command
                List<String> output = parseAndExecute(scheduler, line);
                
                // Write output
                for (String outputLine : output) {
                    writer.write(outputLine);
                    writer.newLine();
                }
                
                // Check for Quit command
                if (line.startsWith("Quit")) {
                    break;
                }
            }
            
            reader.close();
            writer.close();
            
        } catch (FileNotFoundException e) {
            System.err.println("Error: Input file not found - " + inputFilename);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error: IO exception - " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Parse command and execute appropriate operation
     */
    private static List<String> parseAndExecute(AirTrafficScheduler scheduler, String command) {
        try {
            // Remove whitespace and parse command
            command = command.trim();
            
            // Extract command name and parameters
            int openParen = command.indexOf('(');
            int closeParen = command.lastIndexOf(')');
            
            if (openParen == -1 || closeParen == -1) {
                return Arrays.asList("Error: Invalid command format");
            }
            
            String commandName = command.substring(0, openParen).trim();
            String paramsStr = command.substring(openParen + 1, closeParen).trim();
            
            // Parse parameters
            List<Integer> params = new ArrayList<>();
            if (!paramsStr.isEmpty()) {
                String[] paramArray = paramsStr.split(",");
                for (String param : paramArray) {
                    params.add(Integer.parseInt(param.trim()));
                }
            }
            
            // Execute command
            switch (commandName) {
                case "Initialize":
                    return Arrays.asList(scheduler.initialize(params.get(0)));
                    
                case "SubmitFlight":
                    return scheduler.submitFlight(params.get(0), params.get(1), 
                                                  params.get(2), params.get(3), params.get(4));
                    
                case "CancelFlight":
                    return scheduler.cancelFlight(params.get(0), params.get(1));
                    
                case "Reprioritize":
                    return scheduler.reprioritize(params.get(0), params.get(1), params.get(2));
                    
                case "AddRunways":
                    return scheduler.addRunways(params.get(0), params.get(1));
                    
                case "GroundHold":
                    return scheduler.groundHold(params.get(0), params.get(1), params.get(2));
                    
                case "PrintActive":
                    return scheduler.printActive();
                    
                case "PrintSchedule":
                    return scheduler.printSchedule(params.get(0), params.get(1));
                    
                case "Tick":
                    return scheduler.tick(params.get(0));
                    
                case "Quit":
                    return Arrays.asList("Program Terminated!!");
                    
                default:
                    return Arrays.asList("Error: Unknown command - " + commandName);
            }
            
        } catch (Exception e) {
            return Arrays.asList("Error parsing command: " + command + " - " + e.getMessage());
        }
    }
}



