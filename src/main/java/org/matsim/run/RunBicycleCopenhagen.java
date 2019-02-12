package org.matsim.run;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionPenalisingCongestedTimeFactory;

public class RunBicycleCopenhagen {

	public final static int numberOfThreads = 20;
	public final static int numberOfQSimThreads = 1;

	//public static String outputBaseDir = "/work1/s103232/ABMTRANS2019/"; //With final /
	//public static String outputBaseDir = "./output/ABMTRANS2019/"; //With final / 
	public static String outputBaseDir = "C:/Users/madsp/git/fastOrForcedToFollowMaven/output/Copenhagen/";

	//public final static String inputBaseDir = "./input/";  //With final /
	//public final static String inputBaseDir = "/zhome/81/e/64390/MATSim/ABMTRANS2019/input/"; //With final /
	public final static String inputBaseDir = "C:/Users/madsp/git/fastOrForcedToFollowMaven/input/";	

	public static void main(String[] args) throws IOException{
		boolean congestion = true;
		String scenarioType = "small";
		int lastIteration = 200;
		boolean oneLane = false;
		boolean roW = false;
		if(args.length > 0){
			scenarioType = args[0];
			if(scenarioType.contains("NoCongestion")){
				congestion = false;
			}
			if(args.length > 1){
				lastIteration = Integer.valueOf(args[1]);
			}
			if(scenarioType.contains("OneLane")){
				oneLane = true;
			}
			if(scenarioType.contains("RoW")){
				roW = true;
				outputBaseDir += "withNodeModelling/";
			}
		}
		
		System.out.println("Running " + scenarioType);

		Config config = RunMatsim.createConfigFromExampleName("berlin");
		config.controler().setOutputDirectory(outputBaseDir + scenarioType);



		if(scenarioType.substring(0,4).equals("full")){
			scenarioType = "full";
		} else if(scenarioType.substring(0,5).equals("small")){
			scenarioType = "small";
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
		config.qsim().setNumberOfThreads(numberOfQSimThreads);
		config.qsim().setUsingThreadpool(false);
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
			ap.setClosingTime(98*3600);
			ap.setOpeningTime(-1.);
			ap.setMinimalDuration(-1);
			ap.setEarliestEndTime(-0.5);
			ap.setLatestStartTime(99*3600);

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
		config.plans().setInputFile(inputBaseDir + "BicyclePlans_CPH_" + scenarioType + ".xml.gz");

		//Possible changes to config
		FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		// fffConfig.setLMax(Double.MAX_VALUE); // To disable sublinks (faster computation, but lower realism)

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		RunMatsim.cleanBicycleNetwork(scenario.getNetwork());
		removeSouthWesternPart(scenario.getNetwork());
		scenario = RunMatsim.addCyclistAttributes(config, scenario);
		//RunMatsim.reducePopulationToN(0, scenario.getPopulation());


		Controler controler;
		if(congestion){
				controler = RunMatsim.createControler(scenario);
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
		//	System.out.println(isCoordInsidePolygon(new Coord(671092.33, 6177550.04), vertices));
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
		// All sites from https://epsg.io/map#srs=32632
		coords.addLast(new Coord(705928.346681,	6125917.168974)); // Vemmetofte Strand
		coords.addLast(new Coord(680313.490601,	6147920.287373)); // Adamshøj
		coords.addLast(new Coord(669263.733097, 6172981.752523)); // Hellestrup
		coords.addLast(new Coord(666336.561495,	6384536.656468)); // Vrångö
		coords.addLast(new Coord(732555.631618,	6201557.084542)); // Bäckviken
		coords.addLast(new Coord(748457.912623,	6146312.994732)); // Ljunghusen
		coords.addLast(new Coord(702262.206001, 6111994.192165)); // Bønsvig 

		Coord[] output = new Coord[coords.size()];
		for(int i = 0; i < output.length; i++){
			output[i] = coords.pollFirst();
		}
		return output;
	}



}
