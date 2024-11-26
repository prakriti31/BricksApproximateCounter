package main.java.com.example.bricksapproxcounter;

import boundary.EventDrivenFLX.BO_InitClient;
import boundary.EventDrivenFLX.BO_InitServer;
import boundary.EventDrivenFLX.SynchronousSocketBoundaryObjectFeature;
import bricks.MultiReqCausalCohortFeaturePackage;
import main.BricksStrategy;
import main.SyncCoordinator;

public class TwoPhaseCohortMain2 {

	public static void main(String[] args) {

//		InetSocketAddress serverhostAddress = new InetSocketAddress("localhost", 11114);
//		InetSocketAddress clienthostAddress = new InetSocketAddress("localhost", 11113);
//		TcpChannelFeature coordinator = new TcpChannelFeature(serverhostAddress, clienthostAddress, null);
////		TwoPhaseCohortFeature cohort = new TwoPhaseCohortFeature(new WrapperSender(coordinator), null);
////		ForwardCohortFeaturePackage cohort = new ForwardCohortFeaturePackage(new WrapperSender(coordinator), null, new WrapperSender(coordinator));
//		MultiRequestsCohortFeaturePackage cohort = new MultiRequestsCohortFeaturePackage(new WrapperSender(coordinator), null);
//
//		coordinator.handler = new AcceptHandler(cohort, new EventConverter());
//
//		coordinator.sendEvent(new InitServer());
//		coordinator.sendEvent(new InitClient());
		
		String COORD_REMOTE = "localhost";

	    int coordinatorPort = 11113;
        int port = 11114;
        
        int cohort1Port = 11117;
        int port2 = 11118;
		    
	    SynchronousSocketBoundaryObjectFeature coordinator = new SynchronousSocketBoundaryObjectFeature(port, coordinatorPort, null, new BricksStrategy(), COORD_REMOTE);
	    SynchronousSocketBoundaryObjectFeature cohort1 = new SynchronousSocketBoundaryObjectFeature(port2, cohort1Port, null, new BricksStrategy(), COORD_REMOTE);

//		TwoPhaseCohortFeature cohort = new TwoPhaseCohortFeature(new WrapperSender(coordinator), null);
//		ForwardCohortFeaturePackage cohort = new ForwardCohortFeaturePackage(new WrapperSender(coordinator), null, new WrapperSender(coordinator));
//		WrapperSender coordinatorWrapper = new WrapperSender(coordinator);
//		VersioningForwardCohortFeaturePackage cohort = new VersioningForwardCohortFeaturePackage(coordinatorWrapper, null,coordinatorWrapper,null,100);
//		MultiReqNoTPCForwardCohortFeaturePackage cohort = new MultiReqNoTPCForwardCohortFeaturePackage(coordinatorWrapper, null, coordinatorWrapper);

        SyncCoordinator syncCoordinator = new SyncCoordinator(coordinator);

//		MultiRequestsCohortFeaturePackage cohort = new MultiRequestsCohortFeaturePackage(syncCoordinator, null);
//		MultiReqNoTPCForwardCohortFeaturePackage cohort = new MultiReqNoTPCForwardCohortFeaturePackage(syncCoordinator, null, null, null, null);
		MultiReqCausalCohortFeaturePackage cohort = new MultiReqCausalCohortFeaturePackage(syncCoordinator, null, null, null, null, null, null);

		coordinator.controlObject = cohort;

        coordinator.sendEvent(new BO_InitServer());
        coordinator.sendEvent(new BO_InitClient());
        
        cohort1.controlObject = cohort;
        cohort1.sendEvent(new BO_InitServer());
        cohort1.sendEvent(new BO_InitClient());
	}

}
