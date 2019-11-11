package org.matsim.run;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.NetworkRoutingProviderWithCleaning;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;
import fastOrForcedToFollow.configgroups.FFFNodeConfigGroup;
import fastOrForcedToFollow.configgroups.FFFScoringConfigGroup;
import fastOrForcedToFollow.scoring.FFFModeUtilityParameters;
import fastOrForcedToFollow.scoring.FFFScoringFactory;

public class RunBicycleCopenhagen {

	public static int numberOfGlobalThreads = 3;
	public static Collection<String> networkModes = null;
	public static final double capFactor = 0.1;

	//public static String outputBaseDir = "/work1/s103232/ABMTRANS2019/"; // With
	public static String outputBaseDir = "C:/Users/madsp/DTA/Output/"; // With

	// final
	// /
	// public static String outputBaseDir = "./output/ABMTRANS2019/"; //With
	// final /
	// public static String outputBaseDir =
	// "C:/Users/madsp/git/fastOrForcedToFollowMaven/output/Copenhagen/";

	//public final static String inputBaseDir = "/zhome/81/e/64390/MATSim/ABMTRANS2019/input/"; // With
	//public final static String inputResumeBaseDir = "/work1/s103232/ABMTRANS2019/input/"; // With
	public final static String inputBaseDir = "C:/Users/madsp/DTA/Input/"; // With
	public final static String inputResumeBaseDir = "C:/Users/madsp/DTA/ResumableInput/"; // With

	
	// final
	// /

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
		boolean tenPercentCars = false;
		boolean carOnly = false;
		boolean berlin = false;
		boolean runMatsim = true;
		boolean resumeNonRoW = false;
		double qSimEndTime = 100 * 3600;

		List<String> analysedModes = Arrays.asList(TransportMode.bike);

		if (args.length > 0) {
			scenarioType = args[0];
		}
		if (args.length > 1) {
			lastIteration = Integer.valueOf(args[1]);
		}
		if (args.length > 2) {
			analysedModes = Arrays.asList(args[2].split(","));
		}
		if (args.length > 3) {
			runMatsim = Boolean.parseBoolean(args[3]);
		}
		if (args.length > 4) {
			numberOfGlobalThreads = Integer.parseInt(args[4]);
		}

		if (scenarioType.contains("NoCongestion")) {
			congestion = false;
		}
		if (scenarioType.contains("Berlin")) {
			berlin = true;
		}

		if (scenarioType.contains("OneLane")) {
			oneLane = true;
		}
		if(scenarioType.contains("Both")){
			carsAreSomehowIncluded = true;
			oneLane = false;
		} else if(scenarioType.contains("Uneven")) {
			tenPercentCars = true;
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
		if (scenarioType.contains("DasAuto")) {
			carOnly = true;
			carsAreSomehowIncluded = true;
		}


		if (scenarioType.contains("QSimEndsAt")) {
			qSimEndTime = Double.valueOf(scenarioType.substring(scenarioType.lastIndexOf("QSimEndsAt") + 10,
					scenarioType.length())) * 3600;
		}





		System.out.println("Running " + scenarioType);

		if (carOnly) {
			networkModes = Arrays.asList(TransportMode.car);
		} else if (carsAreSomehowIncluded) {
			networkModes = Arrays.asList(TransportMode.car, TransportMode.bike);
		} else {
			networkModes = Arrays.asList(TransportMode.bike);
		}

		Config config = RunMatsim.createConfigFromExampleName(networkModes);
		config.controler().setOutputDirectory(outputBaseDir + scenarioType);

		String size = null;
		if (scenarioType.substring(0, 4).equals("full")) {
			size = "full";
		} else if (scenarioType.substring(0, 5).equals("small")) {
			size = "small";
			numberOfGlobalThreads = 1;
		}
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(lastIteration);
		if (!congestion) {
			config.controler().setLastIteration(0);
		}

		config.controler().setWritePlansInterval(config.controler().getLastIteration() + 1);
		config.controler().setWriteEventsInterval(config.controler().getLastIteration() + 1);

		config.global().setNumberOfThreads(numberOfGlobalThreads);
		if(roW && false){
			config.qsim().setNumberOfThreads(1);
			config.parallelEventHandling().setNumberOfThreads(numberOfGlobalThreads - 1);
			if(false){
				config.controler().setLinkToLinkRoutingEnabled(true);
			}
		} else {
			config.qsim().setNumberOfThreads(numberOfGlobalThreads - 1);
			config.parallelEventHandling().setNumberOfThreads(1);
		}

		config.global().setCoordinateSystem("EPSG:32632"); // /EPSG:32632 is
		// WGS84 UTM32N

		config.qsim().setEndTime(qSimEndTime);

		if (roW) {
			RunMatsim.addRoWModuleToConfig(config, tenPercentCars);
		}
		if (tenPercentCars) {
			config.qsim().setFlowCapFactor(capFactor); // This has to be calibrated
			config.qsim().setStorageCapFactor(capFactor); // This has to be calibrated
			config.qsim().setStuckTime(60); // This has to be calibrated
		}

		if (berlin) {
			config.network().setInputFile("/zhome/81/e/64390/MATSim/input/Berlin/simplifiedNetwork.xml.gz");
			// } else if(carOnly){
			// config.network().setInputFile(
			// inputBaseDir + "MATSimCopenhagenNetwork_CarsOnly.xml.gz");
		} else if (carsAreSomehowIncluded) {
			config.network().setInputFile(inputBaseDir + "MATSimCopenhagenNetwork_WithBicycleInfrastructure.xml.gz");
		} else if (oneLane) {
			config.network().setInputFile(inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly_1Lane.xml.gz");
		} else {
			config.network().setInputFile(inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly.xml.gz");
		}

		if (berlin) {
			config.plans().setInputFile("/zhome/81/e/64390/MATSim/input/Berlin/AllPlans_CPH_uneven_Berlin.xml.gz");
		} else if (tenPercentCars) {
			if(carOnly){
				config.plans().setInputFile(inputBaseDir + "CarPopulation2019_Micro_10p.xml.gz");
			} else {
				config.plans().setInputFile(inputBaseDir + "COMPASBicycle100_COMPASSCarMicro10.xml.gz");
			}
			// Must be 100% car or no cars...
		} else if (carOnly){
			if(resumeNonRoW){
				config.plans().setInputFile(inputResumeBaseDir + "ResumablePlans_Cars.xml.gz");
			} else {
				config.plans().setInputFile(inputBaseDir + "CarPopulation2019_Micro_full.xml.gz");
			}
		} else if (carsAreSomehowIncluded) {
			if(resumeNonRoW){
				config.plans().setInputFile(inputResumeBaseDir + "ResumablePlans_Both.xml.gz");
			} else {
				config.plans().setInputFile(inputBaseDir + "COMPASBicycle100_COMPASSCarMicro100.xml.gz");
			}
		} else {
			if(resumeNonRoW && size.equals("full")){
				config.plans().setInputFile(inputResumeBaseDir + "ResumablePlans_Bicycles.xml.gz");
			} else {
				config.plans().setInputFile(inputBaseDir + "BicyclePlans_CPH_" + size + ".xml.gz");
			}
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);
		if (!berlin) {
			RunMatsim.cleanBicycleNetwork(scenario.getNetwork(), config);
		}

		System.out.println(size);
		if (!berlin) {
			removeSouthWesternPart(scenario.getNetwork());
		}

		scenario = RunMatsim.addCyclistAttributes(config, scenario);
		// RunMatsim.reducePopulationToN(0, scenario.getPopulation());
		// if(mixed){
		VehicleType vehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		scenario.getVehicles().addVehicleType(vehicleType);
		// }

		Controler controler;

		if (congestion) {
			if (!roW) {
				controler = RunMatsim.createControler(scenario);
			} else {
				controler = RunMatsim.createControlerWithRoW(scenario);
			}
		} else {
			controler = RunMatsim.createControlerWithoutCongestion(scenario);
		}

		if (runMatsim) {
			try {
				controler.run();
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}

		List<String> ignoredModes = new LinkedList<String>();
		for (String mode : Arrays.asList(TransportMode.car, TransportMode.bike)) {
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
		String outDir = config.controler().getOutputDirectory();
		ConstructSpeedFlowsFromCopenhagen.run(outDir, scenarioType, -1, ignoredModes, analysedModes);
		// PostProcessing final iteration
		if (lastIteration != 0) {
			ConstructSpeedFlowsFromCopenhagen.run(outDir, scenarioType, 0, ignoredModes, analysedModes);
			// PostProcessing first iteration
		}

	}

	private static void removeSouthWesternPart(Network network) {
		// Based on
		// http://www.ytechie.com/2009/08/determine-if-a-point-is-contained-within-a-polygon/
		Coord[] vertices = getVertices();
		int linksBefore = network.getLinks().size();
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();
		for (Node node : network.getNodes().values()) {
			if (!isCoordInsidePolygon(node.getCoord(), vertices)) {
				nodesToBeRemoved.add(node);
			}
		}
		for (Node node : nodesToBeRemoved) {
			network.removeNode(node.getId());
		}
		// System.out.println(isCoordInsidePolygon(new Coord(671092.33,
		// 6177550.04), vertices));
		System.out.println(nodesToBeRemoved.size() + " nodes and " + (linksBefore - network.getLinks().size())
				+ " links removed from South-Western part...");
	}

	private static boolean isCoordInsidePolygon(Coord c, Coord[] v) {
		int j = v.length - 1;
		boolean oddNodes = false;
		for (int i = 0; i < v.length; i++) {
			if ((v[i].getY() < c.getY() && v[j].getY() >= c.getY()) || v[j].getY() < c.getY()
					&& v[i].getY() >= c.getY()) {
				if (v[i].getX() + (c.getY() - v[i].getY()) / (v[j].getY() - v[i].getY()) * (v[j].getX() - v[i].getX()) < c
						.getX()) {
					oddNodes = !oddNodes;
				}
			}
			j = i;
		}
		return oddNodes;
	}

	private static Coord[] getVertices() {
		LinkedList<Coord> coords = new LinkedList<Coord>();
		// All sites from https://epsg.io/map#srs=32632
		coords.addLast(new Coord(705928.346681, 6125917.168974)); // Vemmetofte
		// Strand
		coords.addLast(new Coord(680313.490601, 6147920.287373)); // Adamshøj
		coords.addLast(new Coord(669263.733097, 6172981.752523)); // Hellestrup
		coords.addLast(new Coord(666336.561495, 6384536.656468)); // Vrångö
		coords.addLast(new Coord(732555.631618, 6201557.084542)); // Bäckviken
		coords.addLast(new Coord(748457.912623, 6146312.994732)); // Ljunghusen
		coords.addLast(new Coord(702262.206001, 6111994.192165)); // Bønsvig

		Coord[] output = new Coord[coords.size()];
		for (int i = 0; i < output.length; i++) {
			output[i] = coords.pollFirst();
		}
		return output;
	}

}
