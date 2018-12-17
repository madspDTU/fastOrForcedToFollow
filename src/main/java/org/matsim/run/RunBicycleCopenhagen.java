package org.matsim.run;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.scoring.ScoringFunctionPenalisingCongestedTimeFactory;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

public class RunBicycleCopenhagen {

	public final int numberOfThreads = 20;
	//public final static String outputBaseDir = "/work1/s103232/ABMTRANS2019/"; //With final /
	public final static String outputBaseDir = "./output/ABMTRANS2019/"; //With final / 
	
	//public final static String inputBaseDir = "/zhome/81/e/64390/MATSim/ABMTRANS2019/input/"; //With final /
	public final static String inputBaseDir = "./input/"; //With final /
	
	
	
	public static void main(String[] args) throws IOException{
		boolean congestion = true;
		String size = "small";
		int lastIteration = 50;
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
		config.travelTimeCalculator().setAnalyzedModes( TransportMode.bike);
		config.controler().setWritePlansInterval(config.controler().getLastIteration()+1);
		config.controler().setWriteEventsInterval(config.controler().getLastIteration()+1);
		config.controler().setCreateGraphs(true);
		config.linkStats().setWriteLinkStatsInterval(-1);
		config.counts().setWriteCountsInterval(-1);
		config.controler().setDumpDataAtEnd(true);

		config.global().setNumberOfThreads(20);
		config.qsim().setNumberOfThreads(20);
		config.parallelEventHandling().setNumberOfThreads(20);

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

	}
}
