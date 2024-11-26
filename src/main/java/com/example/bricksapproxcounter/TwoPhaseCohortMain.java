package main.java.com.example.bricksapproxcounter;

import java.util.Scanner;

import api.base.APIInit;
import api.zookeeper.Create;
import api.zookeeper.GetData;
import api.zookeeper.SetData;
import api.zookeeper.dynamicconsistencymodel.ConsistencyModel;
import api.zookeeper.dynamicconsistencymodel.DynamicCModelEDomain;
import api.zookeeper.dynamicconsistencymodel.Modify;
import boundary.MultiCohorts;
import boundary.EventDrivenFLX.BO_InitClient;
import boundary.EventDrivenFLX.BO_InitServer;
import boundary.EventDrivenFLX.SynchronousSocketBoundaryObjectFeature;
import bricks.MultipleConsistencyCohortWithAPIFeaturePackage;
import main.BricksStrategy;
import main.SyncCoordinator;
import two_phase.cohort.DTwoPhaseCohortDomain;
import two_phase.coordinator.DTwoPhaseCoordinatorDomain;
import versioning.DVersioningDomain;

public class TwoPhaseCohortMain {

	public static void main(String[] args) {

		// Initialization of coordinator
	    String COORD_REMOTE = "localhost";

	    int coordinatorPort = 11111;
        int port = 11112;

		// Defines the coordinator's address (localhost) and ports for communication.
		// Initializes a synchronous socket communication channel for the coordinator using the BricksStrategy for message handling.
	    SynchronousSocketBoundaryObjectFeature coordinator = new SynchronousSocketBoundaryObjectFeature(port, coordinatorPort, null, new BricksStrategy(), COORD_REMOTE);

		// Initialization of Cohorts

        int port2 = 11117;
        int cohort2Port = 11118;
        
        int port3 = 11119;
        int cohort3Port = 11120;

		// Defines ports for 2 cohorts
		SynchronousSocketBoundaryObjectFeature cohortchannel1 = new SynchronousSocketBoundaryObjectFeature(port2, cohort2Port, null, new BricksStrategy(), COORD_REMOTE);
		SynchronousSocketBoundaryObjectFeature cohortchannel2 = new SynchronousSocketBoundaryObjectFeature(port3, cohort3Port, null, new BricksStrategy(), COORD_REMOTE);

		// Initializes communication channels for the cohorts
		MultiCohorts multiCohorts = new MultiCohorts();
		multiCohorts.add(cohortchannel1);
		multiCohorts.add(cohortchannel2);
		multiCohorts.add(coordinator);

//		Aggregates all cohorts and the coordinator into a MultiCohorts object for collective communication.
//
//		SyncCoordinator

        SyncCoordinator syncCoordinator = new SyncCoordinator(coordinator);

//		Synchronizes the coordinator's activities with the cohorts.
//
//		MultipleConsistencyCohort

		MultipleConsistencyCohortWithAPIFeaturePackage cohort = new MultipleConsistencyCohortWithAPIFeaturePackage(syncCoordinator, null, syncCoordinator, null, syncCoordinator, multiCohorts, null,null,null);

//		Configures a MultipleConsistencyCohort that handles consistency protocols, API features, and communication synchronization.
//		Configures two-phase commit (committers) and versioning (apiFeature).
//				Sets a counter value for dynamic consistency.
//		Sends an initialization event (APIInit).
		DTwoPhaseCoordinatorDomain.committer = cohort;
		DTwoPhaseCohortDomain.committer = cohort;
		DVersioningDomain.apiFeature = cohort;
		DynamicCModelEDomain.control = cohort;
		DynamicCModelEDomain.counter = 1000000000;
		cohort.sendEvent(new APIInit());

		// Server and client initialization
        coordinator.controlObject = cohort;
        coordinator.sendEvent(new BO_InitServer());
        coordinator.sendEvent(new BO_InitClient());

		// Sets the cohort as the coordinator's control object and initializes it as both server and client.
		cohortchannel1.controlObject = cohort;
		cohortchannel2.controlObject = cohort;
        cohortchannel1.sendEvent(new BO_InitServer());
		cohortchannel2.sendEvent(new BO_InitServer());

//		Configures the cohorts similarly as server instances.
		System.out.println("Press ENTER to connect other cohorts");
		new Scanner(System.in).nextLine();
		cohortchannel1.sendEvent(new BO_InitClient());
		cohortchannel2.sendEvent(new BO_InitClient());

		//test api
		// Tests data creation, retrieval, and modification across various consistency models.
		apitest(cohort);
		
		//test multiple messages
		//be careful, after create 6 variable, you need press another enter to setData
//		mutipleRequestsTest(cohort);
	}

	// Similar to apitest, but emphasizes causal consistency.
	private static void casualapitest(MultipleConsistencyCohortWithAPIFeaturePackage cohort) {
		System.out.println("Press ENTER to create variable");
		new Scanner(System.in).nextLine();
		Create create = new Create();
		create.setPath("x");
		create.setData("x_test_data1");
		create.setConsistencyModel(ConsistencyModel.CAUSAL.toString());
		
		cohort.sendEvent(create);
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		GetData getData = new GetData();
		getData.setPath("x");
		cohort.sendEvent(getData);
		
		
		System.out.println("Press ENTER to SetData");
		new Scanner(System.in).nextLine();
		for (int i = 2; i < 30; i++) {
			SetData setData = new SetData();
			setData.setPath("x");
			setData.setData("x_test_data" + i);
			
			cohort.sendEvent(setData);
			System.out.println("sending message: "+ "x_test_data" + i);
		}
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		cohort.sendEvent(getData);
	}

	// Creates multiple variables under different consistency models and updates them concurrently.
	
	private static void lwsrapitest(MultipleConsistencyCohortWithAPIFeaturePackage cohort) {
		System.out.println("Press ENTER to create variable");
		new Scanner(System.in).nextLine();
		Create create = new Create();
		create.setPath("x");
		create.setData("x_test_data1");
		create.setConsistencyModel(ConsistencyModel.LWSR.toString());
		
		cohort.sendEvent(create);
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		GetData getData = new GetData();
		getData.setPath("x");
		cohort.sendEvent(getData);

		System.out.println("Press ENTER to SetData");
		new Scanner(System.in).nextLine();
		for (int i = 2; i < 30; i++) {
			SetData setData = new SetData();
			setData.setPath("x");
			setData.setData("x_test_data" + i);
			
			cohort.sendEvent(setData);
		}
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		cohort.sendEvent(getData);
	}
	
	private static void apitest(MultipleConsistencyCohortWithAPIFeaturePackage cohort) {
		System.out.println("Press ENTER to create variable");
		new Scanner(System.in).nextLine();
		Create create = new Create();
		create.setPath("x");
		create.setData("x_test_data1");
		create.setConsistencyModel(ConsistencyModel.LINEARIZABLE.toString());
		create.setVkNumber(1);//it is required
		
		cohort.sendEvent(create);
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		GetData getData = new GetData();
		getData.setPath("x");
		cohort.sendEvent(getData);
		
		System.out.println("Try getdata: " + cohort.getData("x"));
		
		System.out.println("Press ENTER to SetData");
		new Scanner(System.in).nextLine();
		SetData setData = new SetData();
		setData.setPath("x");
		setData.setData("x_test_data2");
		
		cohort.sendEvent(setData);
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		cohort.sendEvent(getData);
		
		System.out.println("Press ENTER to Modify From LIN to SEQ");
		new Scanner(System.in).nextLine();
		Modify modify = new Modify();
		modify.setPath("x");
		modify.setConsistencyModel(ConsistencyModel.SEQUENTIAL.toString());
		cohort.sendEvent(modify);

		System.out.println("Press ENTER to SetData");
		new Scanner(System.in).nextLine();
		setData.setData("x_test_data3");
		cohort.sendEvent(setData);
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		cohort.sendEvent(getData);
		
		System.out.println("Press ENTER to Modify From SEQ to LWSR");
		new Scanner(System.in).nextLine();
		modify.setConsistencyModel(ConsistencyModel.LWSR.toString());
		cohort.sendEvent(modify);
		
		System.out.println("Press ENTER to SetData");
		new Scanner(System.in).nextLine();
		setData.setData("x_test_data4");
		cohort.sendEvent(setData);
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		cohort.sendEvent(getData);
		
		System.out.println("Press ENTER to Modify From SEQ to V100");
		new Scanner(System.in).nextLine();
		modify.setConsistencyModel(ConsistencyModel.VERSION.toString());
		modify.setVkNumber(100);
		cohort.sendEvent(modify);

		System.out.println("Press ENTER to SetData");
		new Scanner(System.in).nextLine();
		setData.setData("x_test_data5");
		cohort.sendEvent(setData);
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		cohort.sendEvent(getData);
		
		System.out.println("Press ENTER to Modify From V100 to LIN");
		new Scanner(System.in).nextLine();
		modify.setConsistencyModel(ConsistencyModel.LINEARIZABLE.toString());
		modify.setVkNumber(1);//this is required
		cohort.sendEvent(modify);
		
		System.out.println("Press ENTER to SetData");
		new Scanner(System.in).nextLine();
		setData.setData("x_test_data6");		
		cohort.sendEvent(setData);
		
		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		cohort.sendEvent(getData);
		
		System.out.println("Press ENTER to Modify From LIN to Casual");
		new Scanner(System.in).nextLine();
		modify.setConsistencyModel(ConsistencyModel.CAUSAL.toString());
		cohort.sendEvent(modify);
		
		System.out.println("Press ENTER to SetData");
		new Scanner(System.in).nextLine();
		setData.setData("x_test_data7");
		cohort.sendEvent(setData);

		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		cohort.sendEvent(getData);
		
		System.out.println("Press ENTER to Modify From Casual to SO");
		new Scanner(System.in).nextLine();
		modify.setConsistencyModel(ConsistencyModel.SO.toString());
		cohort.sendEvent(modify);
		
		System.out.println("Press ENTER to SetData");
		new Scanner(System.in).nextLine();
		setData.setData("x_test_data8");
		cohort.sendEvent(setData);

		System.out.println("Press ENTER to GetData");
		new Scanner(System.in).nextLine();
		cohort.sendEvent(getData);
	}
	
	private static void mutipleRequestsTest(MultipleConsistencyCohortWithAPIFeaturePackage cohort) {
		System.out.println("Press ENTER to create six variables");
		new Scanner(System.in).nextLine();
		Create create = new Create();
		create.setPath("x");
		create.setData("x_test_data0");
		create.setConsistencyModel(ConsistencyModel.LINEARIZABLE.toString());
		create.setVkNumber(1);//it is required
		cohort.sendEvent(create);
		
		Create create2 = new Create();
		create2.setPath("y");
		create2.setData("y_test_data0");
		create2.setConsistencyModel(ConsistencyModel.VERSION.toString());
		create2.setVkNumber(100);
		cohort.sendEvent(create2);
		
		Create create3 = new Create();
		create3.setPath("z");
		create3.setData("z_test_data0");
		create3.setConsistencyModel(ConsistencyModel.LWSR.toString());
		cohort.sendEvent(create3);
		
		Create create4 = new Create();
		create4.setPath("a");
		create4.setData("a_test_data0");
		create4.setConsistencyModel(ConsistencyModel.SEQUENTIAL.toString());
		cohort.sendEvent(create4);
		
		Create create5 = new Create();
		create5.setPath("b");
		create5.setData("b_test_data0");
		create5.setConsistencyModel(ConsistencyModel.CAUSAL.toString());
		cohort.sendEvent(create5);
		
		Create create6 = new Create();
		create6.setPath("c");
		create6.setData("c_test_data0");
		create6.setConsistencyModel(ConsistencyModel.SO.toString());
		cohort.sendEvent(create6);
		
		System.out.println("Press ENTER to setData for this five variables");
		new Scanner(System.in).nextLine();
		for (int i = 1; i < 30; i++) {
			SetData setData = new SetData();
			setData.setPath("x");
			setData.setData("x_test_data" + i);
			cohort.sendEvent(setData);
			
			SetData setData2 = new SetData();
			setData2.setPath("y");
			setData2.setData("y_test_data" + i);
			cohort.sendEvent(setData2);
			
			SetData setData3 = new SetData();
			setData3.setPath("z");
			setData3.setData("z_test_data" + i);
			cohort.sendEvent(setData3);
			
			SetData setData4 = new SetData();
			setData4.setPath("a");
			setData4.setData("a_test_data" + i);
			cohort.sendEvent(setData4);
			
			SetData setData5 = new SetData();
			setData5.setPath("b");
			setData5.setData("b_test_data" + i);
			cohort.sendEvent(setData5);
			
			SetData setData6 = new SetData();
			setData6.setPath("c");
			setData6.setData("c_test_data" + i);
			cohort.sendEvent(setData6);
		}
	}




	// Prakriti Sharma
	/*
	* Define Local Counters: Each node inside Bricks (cohorts) will maintain a local counter that clients update. This allows for lock-free updates.

Threshold Handling:

Set a threshold for the local counters.
When a node’s local counter reaches the threshold, trigger the "breach check."
Maintain a counter for "steps" (how many times the threshold is breached but not acted upon).
If the number of breaches crosses a defined limit, synchronize the global counter.
Update the Global Counter:

The coordinator aggregates updates from the local counters when the "breach threshold" is exceeded.
Use the provided SetData API to update the global counter.
Modify APIs for Counter Management:

Use APIs like Create, GetData, and SetData to manage counters for local and global levels.
Adapt consistency models as required (e.g., linearizable for the global counter).
Communication Between Coordinator and Cohorts:

The coordinator will synchronize local counters periodically or on demand (e.g., when a threshold breach occurs).
* */


	// Increment local counter on client request
//	public void incrementLocalCounter(String cohortName) {
//		// Get current counter value
//		GetData getData = new GetData();
//		getData.setPath(cohortName + "_counter");
//		cohort.sendEvent(getData);
//
//		// Increment locally (simulated with dummy values for illustration)
//		int localCounter = Integer.parseInt(cohort.getData(cohortName + "_counter"));
//		localCounter++;
//
//		// Update the local counter
//		SetData setData = new SetData();
//		setData.setPath(cohortName + "_counter");
//		setData.setData(String.valueOf(localCounter));
//		cohort.sendEvent(setData);
//	}

	// Check and handle threshold breaches
//	public void checkThreshold(String cohortName, int threshold, int breachLimit) {
//		GetData getData = new GetData();
//		getData.setPath(cohortName + "_counter");
//		cohort.sendEvent(getData);
//
//		int localCounter = Integer.parseInt(cohort.getData(cohortName + "_counter"));
//
//		if (localCounter >= threshold) {
//			int breachCount = Integer.parseInt(cohort.getData(cohortName + "_breaches"));
//			breachCount++;
//
//			// Update breach count
//			SetData setBreachCount = new SetData();
//			setBreachCount.setPath(cohortName + "_breaches");
//			setBreachCount.setData(String.valueOf(breachCount));
//			cohort.sendEvent(setBreachCount);
//
//			if (breachCount >= breachLimit) {
//				// Reset local counter and breach count
//				resetLocalCounter(cohortName);
//
//				// Update global counter
//				updateGlobalCounter(localCounter);
//			}
//		}
//	}

	// Reset the local counter
//	public void resetLocalCounter(String cohortName) {
//		SetData resetCounter = new SetData();
//		resetCounter.setPath(cohortName + "_counter");
//		resetCounter.setData("0");
//		cohort.sendEvent(resetCounter);
//
//		SetData resetBreaches = new SetData();
//		resetBreaches.setPath(cohortName + "_breaches");
//		resetBreaches.setData("0");
//		cohort.sendEvent(resetBreaches);
//	}

//	public void updateGlobalCounter(int incrementValue) {
//		// Get global counter value
//		GetData getGlobalCounter = new GetData();
//		getGlobalCounter.setPath("global_counter");
//		cohort.sendEvent(getGlobalCounter);
//
//		int globalCounter = Integer.parseInt(cohort.getData("global_counter"));
//
//		// Update global counter
//		globalCounter += incrementValue;
//
//		SetData setGlobalCounter = new SetData();
//		setGlobalCounter.setPath("global_counter");
//		setGlobalCounter.setData(String.valueOf(globalCounter));
//		cohort.sendEvent(setGlobalCounter);
//	}


	// MAIN METHOD
	/*
	Create createLocalCounter = new Create();
createLocalCounter.setPath("cohort1_counter");
createLocalCounter.setData("0");
createLocalCounter.setConsistencyModel(ConsistencyModel.LINEARIZABLE.toString());
cohort.sendEvent(createLocalCounter);

Create createGlobalCounter = new Create();
createGlobalCounter.setPath("global_counter");
createGlobalCounter.setData("0");
createGlobalCounter.setConsistencyModel(ConsistencyModel.LINEARIZABLE.toString());
cohort.sendEvent(createGlobalCounter);

	* */

	// Periodically check thresholds for cohorts:
	// while (true) {
	//    checkThreshold("cohort1", 100, 5); // Example: threshold = 100, breachLimit = 5
	//    Thread.sleep(1000); // Sleep to avoid constant polling
	//}


}
