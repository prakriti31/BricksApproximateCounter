package main.java.com.example.bricksapproxcounter;

import java.util.Scanner;

import api.base.APIInit;
import api.zookeeper.dynamicconsistencymodel.DynamicCModelEDomain;
import boundary.MultiCohorts;
import boundary.EventDrivenFLX.BO_InitClient;
import boundary.EventDrivenFLX.BO_InitServer;
import boundary.EventDrivenFLX.SynchronousSocketBoundaryObjectFeature;
import bricks.MultipleConsistencyCoordinatorWithAPIFeaturePackage;
import main.BricksStrategy;
import two_phase.cohort.DTwoPhaseCohortDomain;
import two_phase.coordinator.DTwoPhaseCoordinatorDomain;
import versioning.DVersioningDomain;

public class TwoPhaseMultiCohortsMain {

	public static void main(String[] args) {
		
		String COHORT1_REMOTE = "localhost";
	    String COHORT2_REMOTE = "localhost";
	    String COHORT3_REMOTE = "localhost";
	    
	    int port1 = 11111;
        int cohort1Port = 11112;
        
        int port2 = 11113;
        int cohort2Port = 11114;
        
        int port3 = 11115;
        int cohort3Port = 11116;
	    
		SynchronousSocketBoundaryObjectFeature cohortchannel1 = new SynchronousSocketBoundaryObjectFeature(port1, cohort1Port, null, new BricksStrategy(), COHORT1_REMOTE);
		SynchronousSocketBoundaryObjectFeature cohortchannel2 = new SynchronousSocketBoundaryObjectFeature(port2, cohort2Port, null, new BricksStrategy(), COHORT2_REMOTE);
		SynchronousSocketBoundaryObjectFeature cohortchannel3 = new SynchronousSocketBoundaryObjectFeature(port3, cohort3Port, null, new BricksStrategy(), COHORT3_REMOTE);
		MultiCohorts cohort = new MultiCohorts();
		cohort.add(cohortchannel1);
		cohort.add(cohortchannel2);
		cohort.add(cohortchannel3);
		
		MultipleConsistencyCoordinatorWithAPIFeaturePackage coordinator = new MultipleConsistencyCoordinatorWithAPIFeaturePackage(null, null, cohort, null, null, null, null, null);
		DVersioningDomain.coordinator = coordinator;
		DVersioningDomain.apiFeature = coordinator;
		DTwoPhaseCoordinatorDomain.committer = coordinator;
		DTwoPhaseCohortDomain.committer = coordinator;
		DynamicCModelEDomain.control = coordinator;
		
		cohortchannel1.controlObject = coordinator;
		cohortchannel2.controlObject = coordinator;
		cohortchannel3.controlObject = coordinator;

		coordinator.sendEvent(new APIInit());

		cohortchannel1.sendEvent(new BO_InitServer());
		cohortchannel2.sendEvent(new BO_InitServer());
		cohortchannel3.sendEvent(new BO_InitServer());

		System.out.println("Press ENTER to connect cohorts");
		new Scanner(System.in).nextLine();

		cohortchannel1.sendEvent(new BO_InitClient());
		cohortchannel2.sendEvent(new BO_InitClient());
		cohortchannel3.sendEvent(new BO_InitClient());
	
	}

}
