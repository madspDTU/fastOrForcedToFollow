package org.matsim.run;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.FFFPlan;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import fastOrForcedToFollow.FFFUtils;
import fastOrForcedToFollow.configgroups.FFFNodeConfigGroup;
import fastOrForcedToFollow.configgroups.FFFScoringConfigGroup.PlanSelectorType;

public class RunBicycleCopenhagen {

	public static int numberOfGlobalThreads = 20;
	public static Collection<String> networkModes = null;
	public static final double flowCapFactor = 0.2;
	public static final double storageCapFactorWhenUsing10Percent = 0.3;
	public static double stuckTime = 5.;



	public static String outputBaseDir = "/work1/s103232/DTA2020/"; // With
	//public static String outputBaseDir = "C:/Users/madsp/DTA/Output/"; // With
	private static int numberOfThreadsReservedForParallelEventHandling = 2;
	private static double reRoutingProportion = 0.1;
	private static int choiceSetSize = 5;
	public static double qSimEndTime = 30 * 3600.;
	public static double storageCapacityFactor = 1.;
	public static TrafficDynamics trafficDynamics = TrafficDynamics.queue;
	public static LinkDynamics linkDynamics = LinkDynamics.PassingQ;
	private static String intersectionSimplificationSuffix = "";
	public static double timeOutTime = 1.0;
	public static double leftCapacity = 2.0;
	// Only needed if the network has to remain the same across scenarios (cars, bicycles, cars+bicycles).
	// If this is not needed, set to false for faster computations in uni-modal runs. 
	private static boolean enforceSameNetworkAcrossModeScenarios = true;


	public final static String inputBaseDir = "/zhome/81/e/64390/MATSim/DTA2020/input/"; // With
	public final static String inputResumeBaseDir = "/work1/s103232/DTA2020/input/"; // With
	//public final static String inputBaseDir = "C:/Users/madsp/DTA/Input/"; // With
	//public final static String inputResumeBaseDir = "C:/Users/madsp/DTA/ResumableInput/"; // With

	// public final static String inputBaseDir = "./input/"; //With final /
	// public final static String inputBaseDir =
	// "C:/Users/madsp/git/fastOrForcedToFollowMaven/input/";

	public static void main(String[] args) throws IOException {
		boolean congestion = true;
		String scenarioType = "fullRoWUneven_NoHPC";
		int lastIteration = 50;
		boolean oneLane = false;
		boolean roW = false;
		boolean carsAreSomehowIncluded = false;
		boolean carOnly = false;
		boolean runMatsim = true;
		boolean resumeNonRoW = false;


		Set<String> analysedModes = new HashSet<String>(List.of(TransportMode.bike));

		if (args.length > 0) {
			scenarioType = args[0];
		}
		if (args.length > 1) {
			lastIteration = Integer.valueOf(args[1]);
		}
		if (args.length > 2) {
			String[] modes = args[2].split(",");
			if(modes.length>0) {
				analysedModes.clear();
			}
			for(int i = 0; i < modes.length; i++) {
				String mode = modes[i];
				analysedModes.add(modes[i]);
			}
			if(analysedModes.contains(TransportMode.car)) {
				analysedModes.add(TransportMode.truck);
			}
		}
		if (args.length > 3) {
			runMatsim = Boolean.parseBoolean(args[3]);
		}
		if (args.length > 4) {
			numberOfGlobalThreads = Integer.parseInt(args[4]);
		}
		if (args.length > 5) {
			reRoutingProportion  = Double.parseDouble(args[5]);
		}
		if (args.length > 6) {
			if(args[6].contains("b") || args[6].contains("B")) {
				choiceSetSize = Integer.MAX_VALUE;
			} else {
				choiceSetSize   = Integer.parseInt(args[6]);
			}
		}
		PlanSelectorType planSelectorType = PlanSelectorType.BoundedLogit;
		if (args.length > 7) {
			if(args[7].toLowerCase().contains("g")) {
				planSelectorType = PlanSelectorType.GradualBoundedLogit;
			} else if(args[7].toLowerCase().contains("b")) {
				planSelectorType = PlanSelectorType.BestBounded;
			}
		}
		boolean approximateNullLinkToLinkTravelTimes = false;
		if (args.length > 8) {
			if(args[8].toLowerCase().contains("tru") || args[8].toLowerCase().contains("ye")) {
				System.out.println("Approximating link-to-link travel times when none exist");
				approximateNullLinkToLinkTravelTimes = true;
			} else {
				System.out.println("NOT Approximating link-to-link travel times when none exist");
			}
		}
		if (args.length > 9) {
			storageCapacityFactor = Double.parseDouble(args[9]);
		}
		if (args.length > 10) {
			if(args[10].toLowerCase().contains("k") || args[10].toLowerCase().contains("w")) {
				trafficDynamics = TrafficDynamics.kinematicWaves;
			}
		}
		if (args.length > 11) {
			int n = Integer.parseInt(args[11]);
			if(n>0) {
				intersectionSimplificationSuffix = "_SIMPLIFIED_" + n;
			}
		}
		if (args.length > 12) {
			stuckTime = Double.parseDouble(args[12]);
		}
		if (args.length > 13) {
			if(Double.parseDouble(args[13]) >0) {
				timeOutTime = Double.parseDouble(args[13]);
			}
		}
		if (args.length > 14) {
			if(Double.parseDouble(args[14]) > 0) {
				leftCapacity = Double.parseDouble(args[14]);
			}
		}



		if (scenarioType.contains("NoCongestion")) {
			congestion = false;
		}

		if (scenarioType.contains("OneLane")) {
			oneLane = true;
		}
		if(scenarioType.contains("Both")){
			carsAreSomehowIncluded = true;
			oneLane = false;
		} 
		
		if (scenarioType.contains("RoW")) {
			roW = true;
			outputBaseDir += "withNodeModelling/";
		}
		if(scenarioType.contains("Resume")){
			resumeNonRoW = true;
		}
		if (scenarioType.contains("Auto")) {
			carOnly = true;
			carsAreSomehowIncluded = true;
		}


		if (scenarioType.contains("QSimEndsAt")) {
			qSimEndTime = Double.valueOf(scenarioType.substring(scenarioType.lastIndexOf("QSimEndsAt") + 10,
					scenarioType.length())) * 3600;
		}



		System.out.println("Running " + scenarioType);

		if (carOnly) {
			networkModes = Arrays.asList(TransportMode.car, TransportMode.truck);
		} else if (carsAreSomehowIncluded) {
			networkModes = Arrays.asList(TransportMode.car, TransportMode.truck, TransportMode.bike);
		} else {
			networkModes = Arrays.asList(TransportMode.bike);
		}

		Config config = RunMatsimWithFFF.createConfigWithSuitableSettings(networkModes, reRoutingProportion, choiceSetSize, planSelectorType);
		config.controler().setOutputDirectory(outputBaseDir + scenarioType);

		String size = null;
		if (scenarioType.contains("Full")) {
			size = "Full";
		} else if (scenarioType.contains("Small")) {
			size = "Small";
		}
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(lastIteration);
		if (!congestion) {
			config.controler().setLastIteration(0);
		}

		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setWritePlansInterval(lastIteration);
		config.controler().setWritePlansUntilIteration(-1);
		config.controler().setWriteEventsInterval(lastIteration);
		config.controler().setWriteEventsUntilIteration(-1);
		config.controler().setWriteSnapshotsInterval(0);
		config.controler().setWriteTripsInterval(0);

		config.counts().setWriteCountsInterval(-1);
		config.linkStats().setAverageLinkStatsOverIterations(0);
		config.linkStats().setWriteLinkStatsInterval(0);


		config.parallelEventHandling().setSynchronizeOnSimSteps(true); //Is this neede to trigger my own, or will it be ignored???? 'mads may'20.

		config.global().setNumberOfThreads(numberOfGlobalThreads);
		if(args[0].toLowerCase().contains("cheat")) {
			numberOfThreadsReservedForParallelEventHandling = 4;
		} else if(carsAreSomehowIncluded && !carOnly) { // i.e. both
			numberOfThreadsReservedForParallelEventHandling = 4; // normally 4
		} else {
			numberOfThreadsReservedForParallelEventHandling = 3;
		} 
		config.qsim().setNumberOfThreads(numberOfGlobalThreads - numberOfThreadsReservedForParallelEventHandling);
		config.parallelEventHandling().setNumberOfThreads(numberOfThreadsReservedForParallelEventHandling);


		/// A: Old TravelTimeCalculator. QSim: 19, WITH linkTravelTime.  Not synchronized. 4 billion
		/// B: Old TravelTimeCalculator. QSim: 19. WITHOUT linkTravelTIME, and thus WRONG.  Not synchronized. 4 billion
		/// C: New TravelTimeCalculator. QSim: 18 WITHOUT linkTravelTime.  Not synchronized. 4 billion
		/// D: New TravelTimeCalculator. QSim: 16 WITHOUT linkTavelTime. Not synchronized. 550 millon
		/// E: New TravelTimeCalculator. QSim: 16 WITHOUT linkTravelTime. Not Synchronized. 550 million. (Might have been linkleaveomitted)...
		///    V V V   This one!   V V V Was the best. Used for initial submission NSE.
		/// F: New TravelTimeCalculator. QSim: 18 WITHOUT linkTravelTime. Not synchronized. LinkLeaveIn. 0 events. (aiming for fully during)
		///    ^^^ ^   This one!   ^ ^ ^ Was the best. Used for initial submission NSE.
		/// G: New TravelTimeCalculator. QSim: 18 WITHOUT linkTravelTime. Not synchronized. LinkLeaveIn. 5 billion evens. (aiming for fully after)
		/// H: New TravelTimeCalculator. QSim: 16 WITHOUT linkTravelTime. Synchronized. LinkLeaveIn. 42l never read!
		/// I: New TravelTimeCalculator. QSim: 18 WITHOUT linkTravelTime. Synchronized. LinkLeaveIn. 42l never read!
		/// J_died: New TravelTimeCalculator. QSim: 14 WITHOUT linkTravelTime. Synchronized. LinkLeaveIn. 42l never read!
		/// J: New TravelTimeCalculator. QSim: 16 WITHOUT linkTravelTime. My Not synchronized. LinkLeaveIn. 
		/// K: New TravelTimeCalculator. QSim: 18 WITHOUT linkTravelTime. My Not Synchronized. LinkLeaveIn. 
		/// l: New TravelTimeCalculator. QSim: 18 WITHOUT linkTravelTime. My Not synchronized. LinkLeaveOmitted. 
		/// m: New TravelTimeCalculator. QSim: 19 WITHOUT linkTravelTime. My Not synchronized. LinkLeaveOmitted. 
		/// n: New TravelTimeCalculator. QSim: 17 WITHOUT linkTravelTime. My Not synchronized. LinkLeaveOmitted. 
		/// o: New TravelTimeCalcflasulator. QSim: 16 WITHOUT linkTravelTime. My Not synchronized. LinkLeaveOmitted. 





		if(roW){
			config.controler().setLinkToLinkRoutingEnabled(true);
			config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
			config.travelTimeCalculator().setCalculateLinkTravelTimes(false);
			config.travelTimeCalculator().setSeparateModes(false);
		} 

		config.global().setCoordinateSystem("EPSG:32632"); // /EPSG:32632 is
		// WGS84 UTM32N


		config.qsim().setStartTime(0.);
		config.qsim().setEndTime(qSimEndTime);
		config.qsim().setStuckTime(stuckTime);
		config.qsim().setStorageCapFactor(storageCapacityFactor);
		config.qsim().setFlowCapFactor(storageCapacityFactor);
		config.qsim().setTrafficDynamics(trafficDynamics);
		config.qsim().setLinkDynamics(linkDynamics);


		config.travelTimeCalculator().setMaxTime((int) Math.round(qSimEndTime));

		if (roW) {
			FFFUtils.addRoWModuleToConfig(config);
			if(!config.travelTimeCalculator().isCalculateLinkTravelTimes()) {
				System.out.println("Omitting linkLeaveEvents");
				ConfigUtils.addOrGetModule(config, FFFNodeConfigGroup.class).setOmitLinkLeaveEvents(true);
				ConfigUtils.addOrGetModule(config, FFFNodeConfigGroup.class).setApproximateNullLinkToLinkTravelTimes(approximateNullLinkToLinkTravelTimes);				
			}
		}
	
		if (carsAreSomehowIncluded || enforceSameNetworkAcrossModeScenarios ) { // We need to use the same network across scenarios. 
			config.network().setInputFile(inputBaseDir + "MATSimCopenhagenNetwork_WithBicycleInfrastructure" + intersectionSimplificationSuffix + ".xml.gz");
		} else if (oneLane) {
			config.network().setInputFile(inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly_1Lane" + intersectionSimplificationSuffix + ".xml.gz");
		} else {
			config.network().setInputFile(inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly" + intersectionSimplificationSuffix + ".xml.gz");
		}

		if(args[0].toLowerCase().contains("cheat")) {
			config.plans().setInputFile(  "/work1/s103232/DTA2020/withNodeModelling/FullRoWAuto200azFin/output_plans.xml.gz");
			config.network().setInputFile("/work1/s103232/DTA2020/withNodeModelling/FullRoWAuto200azFin/output_network.xml.gz");
			config.controler().setLastIteration(0);
		} else if (carOnly){
			if(resumeNonRoW){
				config.plans().setInputFile(inputResumeBaseDir + "ResumablePlans_Cars_selectedOnly.xml.gz");
			} else {
				config.plans().setInputFile(inputBaseDir + "CarPopulation2020_OTM_full.xml.gz");
			}
		} else if (carsAreSomehowIncluded) {
			if(resumeNonRoW){
				config.plans().setInputFile(inputResumeBaseDir + "ResumablePlans_Both_selectedOnly.xml.gz");
			} else {
				//		config.plans().setInputFile("/zhome/81/e/64390/MATSim/DTA2020/input/AllPlans_CPH_Small.xml");
				config.plans().setInputFile(inputBaseDir + "COMPASBicycle100_COMPASSCarOTM100.xml.gz");
			}
		} else {
			if(resumeNonRoW && size.equals("full")){
				config.plans().setInputFile(inputResumeBaseDir + "ResumablePlans_Bicycles_selectedOnly.xml.gz");
			} else {
				config.plans().setInputFile(inputBaseDir + "BicyclePlans_CPH_" + size + ".xml.gz");
			}
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);
		FFFPlan.convertToFFFPlans(scenario.getPopulation());
		

		System.out.println(size);
		FFFUtils.removeSouthWesternPart(scenario.getNetwork());
		
		FFFUtils.addCyclistAttributes(scenario);
		// RunMatsim.reducePopulationToN(0, scenario.getPopulation());
		// if(mixed){
		VehicleType carVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		carVehicleType.setPcuEquivalents(1.0);
		carVehicleType.setMaximumVelocity(130/3.6);
		scenario.getVehicles().addVehicleType(carVehicleType);
		VehicleType truckVehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create(TransportMode.truck, VehicleType.class));
		truckVehicleType.setPcuEquivalents(2.0);
		truckVehicleType.setMaximumVelocity(80/3.6);
		scenario.getVehicles().addVehicleType(truckVehicleType);

		HashSet<String> carTruckModesSet = new HashSet<String>();
		carTruckModesSet.add(TransportMode.car);
		carTruckModesSet.add(TransportMode.truck);

		for(Link link : scenario.getNetwork().getLinks().values()) {
			if(link.getAllowedModes().contains(TransportMode.car) && !link.getAllowedModes().contains(TransportMode.truck)) {
				link.setAllowedModes(carTruckModesSet);
			}
		}
		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);


		// }

		Controler controler = new Controler(scenario);

		if (congestion) {
			if (!roW) {
				FFFUtils.prepareControlerForFFF(controler);
			} else {
				FFFUtils.prepareControlerForFFFWithRoW(controler);
			}
		} else {
			FFFUtils.prepareControlerForFFFWithoutCongestion(controler);
		}


		if (runMatsim) {
			try {
				System.gc();
				controler.run();
				System.gc();
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}

		List<String> ignoredModes = new LinkedList<String>();
		for (String mode : Arrays.asList(TransportMode.car, TransportMode.bike, TransportMode.truck)) {
			if (!analysedModes.contains(mode)) {
				ignoredModes.add(mode);
			}
		}
		System.out.print("\nIgnored modes are: ");
		for (String s : ignoredModes) {
			System.out.print(s + ", ");
		}
		System.out.print("\nAnalysed modes are: ");
		for (String s : analysedModes) {
			System.out.print(s + ", ");
		}
		System.out.println();
		ConstructSpeedFlowsFromCopenhagen.run(scenario, scenarioType, -1, ignoredModes, analysedModes, runMatsim);
		// PostProcessing final iteration
		//		if (lastIteration != 0) {
		//			System.gc();
		//			ConstructSpeedFlowsFromCopenhagen.run(outDir, scenarioType, 0, ignoredModes, analysedModes);
		//			// PostProcessing first iteration
		//		}
		System.out.println("Postprocessing finished!");
		System.exit(-1);

	}

}
