package org.matsim.run;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scoring.ScoringFunctionPenalisingCongestedTimeFactory;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

public class RunBicycleCopenhagen {

	//public final int numberOfThreads = 20;
	public final static int numberOfThreads = 4;

	//public final static String outputBaseDir = "/work1/s103232/ABMTRANS2019/"; //With final /
	//public final static String outputBaseDir = "./output/ABMTRANS2019/"; //With final / 
	public final static String outputBaseDir = "C:/Users/madsp/git/fastOrForcedToFollowMaven/output/Copenhagen/";

	//public final static String inputBaseDir = "./input/";  //With final /
	//public final static String inputBaseDir = "/zhome/81/e/64390/MATSim/ABMTRANS2019/input/"; //With final /
	public final static String inputBaseDir = "C:/Users/madsp/git/fastOrForcedToFollowMaven/input/";	

	public static void main(String[] args) throws IOException{
		boolean congestion = true;
		String size = "small";
		int lastIteration = 1;
		boolean oneLane = false;
		if(args.length > 0){
			size = args[0];
			if(size.contains("NoCongestion")){
				congestion = false;
			}
			if(args.length > 1){
				lastIteration = Integer.valueOf(args[1]);
			}
			if(size.contains("OneLane")){
				oneLane = true;
			}
		}
		System.out.println("Running " + size);

		Config config = RunMatsim.createConfigFromExampleName("berlin");
		config.controler().setOutputDirectory(outputBaseDir + size);



		if(size.substring(0,4).equals("full")){
			size = "full";
		} else if(size.substring(0,5).equals("small")){
			size = "small";
		}
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(lastIteration);
		if(!congestion){
			config.controler().setLastIteration(0);
		}
		final List<String> networkModes = Arrays.asList( new String[]{TransportMode.bike} );
		config.qsim().setMainModes( networkModes );
		config.plansCalcRoute().setNetworkModes(networkModes);
		config.controler().setWritePlansInterval(config.controler().getLastIteration()+1);
		config.controler().setWriteEventsInterval(config.controler().getLastIteration()+1);
		config.controler().setCreateGraphs(true);
		config.linkStats().setWriteLinkStatsInterval(-1);
		config.counts().setWriteCountsInterval(-1);
		config.controler().setDumpDataAtEnd(true);

		config.global().setNumberOfThreads(numberOfThreads);
		config.qsim().setNumberOfThreads(numberOfThreads);
		config.parallelEventHandling().setNumberOfThreads(numberOfThreads);
		
		config.global().setCoordinateSystem("EPSG:32632");   ///EPSG:32632 is WGS84 UTM32N

		{
			Map<String, String> defaultParams = config.planCalcScore().getActivityParams("work").getParams();
			for(String actType : Arrays.asList("other","missing","shopping")){
				ActivityParams ap = new ActivityParams(actType);
				for(String key : defaultParams.keySet()){
					if(!key.equals("activityType")){
						ap.addParam(key, defaultParams.get(key));
					}
				}
				config.planCalcScore().addActivityParams(ap);
			}
			config.planCalcScore().getActivityParams("edu").setActivityType("school");
		}
		for(ActivityParams ap : config.planCalcScore().getActivityParams()){
			ap.setClosingTime(30*3600);
			ap.setOpeningTime(-1.);
			ap.setMinimalDuration(-1);
			ap.setEarliestEndTime(-0.5);
			ap.setLatestStartTime(29.5*3600);

		}

		for(String mode : Arrays.asList(TransportMode.bike, TransportMode.access_walk, TransportMode.egress_walk)){
			ModeParams modeParams = config.planCalcScore().getModes().get(mode);
			modeParams.setMarginalUtilityOfTraveling(-60.);
			//The distance coefficient is used as coefficient for congestion time... ... Not proud of it myself.
			modeParams.setMarginalUtilityOfDistance(-30./3600);
		}



		config.strategy().clearStrategySettings();
		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName(DefaultStrategy.ReRoute);
		reRoute.setWeight(0.2);
		reRoute.setDisableAfter((int) Math.round(config.controler().getLastIteration() * 0.8));
		//StrategySettings bestScore = new StrategySettings();
		//bestScore.setStrategyName(DefaultSelector.BestScore);
		//bestScore.setWeight(0.001);
		StrategySettings logit = new StrategySettings();
		logit.setStrategyName(DefaultSelector.SelectExpBeta);
		logit.setWeight(0.8);
		logit.setDisableAfter((int) Math.round(config.controler().getLastIteration() * 1.1));

		config.strategy().addStrategySettings(reRoute);
		//config.strategy().addStrategySettings(bestScore);
		config.strategy().addStrategySettings(logit);




		if(oneLane){
			config.network().setInputFile(
					inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly_1Lane.xml.gz");
		} else {
			config.network().setInputFile(
					inputBaseDir + "MATSimCopenhagenNetwork_BicyclesOnly.xml.gz");
		}
		config.plans().setInputFile(inputBaseDir + "BicyclePlans_CPH_" + size + ".xml.gz");

		//Possible changes to config
		FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		fffConfig.setLMax(60.);

		Scenario scenario = RunMatsim.addCyclistAttributes(config);
		removeSouthWesternPart(scenario.getNetwork());
		RunMatsim.reducePopulationToN(500, scenario.getPopulation());


		Controler controler;
		if(congestion){
			// controler = RunMatsim.createControler(scenario);
			controler = RunMatsim.createControlerWithRoW(scenario);	
		} else {
			controler = RunMatsim.createControlerWithoutCongestion(scenario);
		}
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.bindScoringFunctionFactory().to( ScoringFunctionPenalisingCongestedTimeFactory.class ) ;
			}
		} );

		try {			
			controler.run();
		} catch ( Exception ee ) {
			ee.printStackTrace();
		}
/*

		if(oneLane){
			size += "OneLane";
		}
		if(!congestion){
			size += "NoCongestion";
		}
		ConstructSpeedFlowsFromCopenhagen.main(new String[]{size, "-1"}); //PostProcessing final iteration
		if(lastIteration != 0){
			ConstructSpeedFlowsFromCopenhagen.main(new String[]{size, "0"});	//PostProcessing first iteration
		}
		*/

	}


	

	private static void removeDuplicateLinks(Network network){

		for(String mode : Arrays.asList(TransportMode.car, TransportMode.bike)){
			System.out.print("Starting to remove duplicate links network...");
			LinkedList<Link> linksToBeRemoved = new LinkedList<Link>();
			for(Node node : network.getNodes().values()){
				HashMap<Node, Link> outNodes = new HashMap<Node, Link>();
				for(Link link : node.getOutLinks().values()){
					if(link.getAllowedModes().contains(mode)){
						if(!outNodes.containsKey(link.getToNode())){
							outNodes.put(link.getToNode(),link);
						} else {
							if(link.getNumberOfLanes() > outNodes.get(link.getToNode()).getNumberOfLanes()){
								linksToBeRemoved.add(outNodes.get(link.getToNode()));
								outNodes.put(link.getToNode(), link);
							} else {
								linksToBeRemoved.add(link);
								if(mode.equals(TransportMode.car)){
									System.out.println("Car");
								}
							}
						}
					}
				}
			}
			int counter = 0;
			for(Link link : linksToBeRemoved){
				network.removeLink(link.getId());
				counter++;
			}
			System.out.println(counter + " duplicate " + mode + " links removed from the network");
		}
	}



	private static void removeSouthWesternPart(Network network) {
		// Based on http://www.ytechie.com/2009/08/determine-if-a-point-is-contained-within-a-polygon/  
		Coord[] vertices = getVertices();
		int linksBefore = network.getLinks().size();
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();
		for(Node node : network.getNodes().values()){
			if(!isCoordInsidePolygon(node.getCoord(), vertices)){
				nodesToBeRemoved.add(node);
			}
		}
		for(Node node : nodesToBeRemoved){
			network.removeNode(node.getId());
		}
		System.out.println(nodesToBeRemoved.size() + " nodes and " + (linksBefore - network.getLinks().size())
				+ " links removed from South-Western part...");
	}

	private static boolean isCoordInsidePolygon(Coord c, Coord[] v){
		int j  = v.length -1;
		boolean oddNodes = false;
		for(int i = 0; i< v.length; i++){
			if((v[i].getY() < c.getY() && v[j].getY() >= c.getY()) ||
					v[j].getY() < c.getY() && v[i].getY() >= c.getY() ){
				if(v[i].getX() + (c.getY() - v[i].getY()) / (v[j].getY() - v[i].getY()) * (v[j].getX() - v[i].getX()) < c.getX() ){
					oddNodes = !oddNodes;
				}
			}
			j = i;
		}
		return oddNodes;
	}



	private static Coord[] getVertices() {
		LinkedList<Coord> coords = new LinkedList<Coord>();
		coords.addLast(new Coord(705928.346681,	6125917.168974)); // Vemmetofte Strand
		coords.addLast(new Coord(680313.490601,	6147920.287373)); // Adamshøj
		coords.addLast(new Coord(672214.258594,	6172492.227007)); // Vipperød
		coords.addLast(new Coord(675429.669649,	6259160.285312)); // Mellem Anholt og Tisvilde
		coords.addLast(new Coord(728256.015736,	6216505.994517)); // Mellem Helsingør og Helsingborg
		coords.addLast(new Coord(740100.750946,	6118474.660517)); // Mellem Møn og Trelleborg
		
		Coord[] output = new Coord[coords.size()];
		for(int i = 0; i < output.length; i++){
			output[i] = coords.pollFirst();
		}
		return output;
	}


	
}
