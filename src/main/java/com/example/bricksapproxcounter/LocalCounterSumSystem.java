package main.java.com.example.bricksapproxcounter;

import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import api.zookeeper.Create;
import api.zookeeper.GetData;
import api.zookeeper.SetData;
import boundary.MultiCohorts;
import boundary.EventDrivenFLX.SynchronousSocketBoundaryObjectFeature;
import main.BricksStrategy;

public class LocalCounterSumSystem {

    private static final ConcurrentHashMap<String, String> responseMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {

        Scanner scanner = new Scanner(System.in);

        // Input: Threshold for local counters
        System.out.println("Enter the threshold for local counters:");
        int threshold = scanner.nextInt();

        // Initialize Bricks components and counters
        MultiCohorts multiCohorts = initializeBricks();
        initializeCounters(multiCohorts);

        long startTime = System.nanoTime(); // Start timing

        // Simulate cohort operations
        while (true) {
            // Increment local counters for each cohort
            incrementCounter("local_counter1", multiCohorts);
            incrementCounter("local_counter2", multiCohorts);
            incrementCounter("local_counter3", multiCohorts);

            // Check thresholds for each local counter and reset if necessary
            checkAndResetCounter("local_counter1", threshold, multiCohorts);
            checkAndResetCounter("local_counter2", threshold, multiCohorts);
            checkAndResetCounter("local_counter3", threshold, multiCohorts);

            // Calculate the sum of all local counters
            int totalSum = getCounterValue("local_counter1", multiCohorts)
                    + getCounterValue("local_counter2", multiCohorts)
                    + getCounterValue("local_counter3", multiCohorts);

            System.out.println("Current total sum of local counters: " + totalSum);

            // Stop condition (for demonstration purposes)
            if (totalSum >= 1000) { // Example: Stop when total sum reaches 1000
                break;
            }
        }

        long endTime = System.nanoTime(); // End timing

        // Calculate time taken
        long timeTakenNs = endTime - startTime;
        double timeTakenSeconds = timeTakenNs / 1_000_000_000.0; // Convert to seconds
        double timeTakenMs = timeTakenNs / 1_000_000.0;          // Convert to milliseconds

        // Output results
        System.out.println("Counting completed!");
        System.out.println("Time taken: " + timeTakenSeconds + " seconds");
        System.out.println("Time taken: " + timeTakenMs + " milliseconds");

        scanner.close();
    }

    private static MultiCohorts initializeBricks() {
        MultiCohorts multiCohorts = new MultiCohorts();

        String COORD_REMOTE = "localhost";

        int port1 = 11111, cohort1Port = 11112;
        int port2 = 11113, cohort2Port = 11114;
        int port3 = 11115, cohort3Port = 11116;

        SynchronousSocketBoundaryObjectFeature cohortchannel1 =
                new SynchronousSocketBoundaryObjectFeature(port1, cohort1Port, null, new BricksStrategy(), COORD_REMOTE);
        SynchronousSocketBoundaryObjectFeature cohortchannel2 =
                new SynchronousSocketBoundaryObjectFeature(port2, cohort2Port, null, new BricksStrategy(), COORD_REMOTE);
        SynchronousSocketBoundaryObjectFeature cohortchannel3 =
                new SynchronousSocketBoundaryObjectFeature(port3, cohort3Port, null, new BricksStrategy(), COORD_REMOTE);

        multiCohorts.add(cohortchannel1);
        multiCohorts.add(cohortchannel2);
        multiCohorts.add(cohortchannel3);

        return multiCohorts;
    }

    private static void initializeCounters(MultiCohorts cohort) {
        createCounter("local_counter1", "0", cohort);
        createCounter("local_counter2", "0", cohort);
        createCounter("local_counter3", "0", cohort);
    }

    private static void createCounter(String path, String initialValue, MultiCohorts cohort) {
        Create create = new Create();
        create.setPath(path);
        create.setData(initialValue);
        create.setConsistencyModel("LINEARIZABLE");
        cohort.sendEvent(create);
    }

    private static void incrementCounter(String path, MultiCohorts cohort) throws InterruptedException {
        int currentValue = getCounterValue(path, cohort);

        SetData setData = new SetData();
        setData.setPath(path);
        setData.setData(String.valueOf(currentValue + 1));

        cohort.sendEvent(setData);
    }

    private static int getCounterValue(String path, MultiCohorts cohort) throws InterruptedException {
        // Create and send a GetData event
        GetData getData = new GetData();
        getData.setPath(path);
        cohort.sendEvent(getData);

        // Wait for and retrieve the response
        String response = waitForResponse(path);

        // Parse and return the counter value
        return Integer.parseInt(response);
    }

    private static String waitForResponse(String path) throws InterruptedException {
        int maxRetries = 10; // Maximum retries for waiting
        int retryIntervalMs = 100; // Interval between retries in milliseconds

        for (int i = 0; i < maxRetries; i++) {
            if (responseMap.containsKey(path)) {
                return responseMap.remove(path); // Retrieve and remove the response from the map
            }
            TimeUnit.MILLISECONDS.sleep(retryIntervalMs); // Wait before retrying
        }

        throw new RuntimeException("Failed to retrieve data for path: " + path);
    }

    private static void checkAndResetCounter(String path, int threshold, MultiCohorts cohort) throws InterruptedException {
        int currentValue = getCounterValue(path, cohort);

        if (currentValue >= threshold) {
            SetData resetLocal = new SetData();
            resetLocal.setPath(path);
            resetLocal.setData("0");
            cohort.sendEvent(resetLocal);

            System.out.println(path + " reached the threshold and was reset.");
        }
    }
}