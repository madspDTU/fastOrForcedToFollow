package org.matsim.run;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.FDEventsHandler;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

public class CreateFDs {

	public static String travelModes = TransportMode.bike;
	public static double endTime = 8*3600;
	public final static double timeStepSize = 300;
	public static HashMap<Integer, Integer[]> validCombinations = new HashMap<Integer, Integer[]>();
	public static int seed=1;
	public static int divideFactor = 2;
	private static double correctionFactor = 1.0;
	private static final int maxSeed = 10;
	private static final double baseLMax = 45.;

	private static DecimalFormat numberFormat = new DecimalFormat("#.000");
	private static boolean useBatesDistributionForSpeed = false;
	private static boolean useBatesDistributionForHeadway = false;



	public static void main(String[] args) throws IOException{


		for(int df : new int[]{1,2,3,4,5,8,10,20,25,50,100}){
			divideFactor = df;
			for(double cf : new double[]{1.00}){ 
				correctionFactor = cf;
				if(divideFactor==1){
					correctionFactor = 1.000;
				}
				for(int i = 1; i <= maxSeed; i++){
					seed = i;

					final Config config = ConfigUtils.createConfig();
					config.qsim().setMainModes(Arrays.asList(travelModes));
					config.qsim().setEndTime(endTime);


					final int numberOfLanes = 2;
					final int numberOfPersons = 25000;
					config.controler().setOutputDirectory("./output/FD" + numberOfPersons + "_" + numberOfLanes + "_" + divideFactor +  "_" + 
							numberFormat.format(correctionFactor).replace(',', 'd') +  "_" + seed);
					config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
					config.controler().setLastIteration(0);

					PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike) ;
					config.planCalcScore().addModeParams( params );
					final List<String> networkModes = Arrays.asList( new String[]{TransportMode.bike} );
					config.qsim().setMainModes( networkModes );
					config.plansCalcRoute().removeModeRoutingParams( TransportMode.bike );
					config.plansCalcRoute().setNetworkModes( networkModes );
					config.plansCalcRoute().setInsertingAccessEgressWalk(true);
					config.planCalcScore().getOrCreateModeParams(TransportMode.access_walk);
					config.planCalcScore().getOrCreateModeParams(TransportMode.egress_walk);
					ActivityParams paramsHome = new PlanCalcScoreConfigGroup.ActivityParams("home");
					paramsHome.setTypicalDuration(1);
					ActivityParams paramsWork = new PlanCalcScoreConfigGroup.ActivityParams("work");
					paramsWork.setTypicalDuration(1);
					config.planCalcScore().addActivityParams(paramsHome);
					config.planCalcScore().addActivityParams(paramsWork);


					config.travelTimeCalculator().setAnalyzedModes(TransportMode.bike);

					config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );


					//Possible changes to config
					FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
					fffConfig.setLMax(baseLMax * divideFactor);


					config.global().setRandomSeed(seed);


					// ---





					Scenario scenario = ScenarioUtils.loadScenario(config);
					createNetwork(scenario, numberOfLanes);
					createPopulation(scenario, numberOfPersons);

					{
						VehicleType type = new VehicleTypeImpl( Id.create( TransportMode.bike, VehicleType.class  ) ) ;
						scenario.getVehicles().addVehicleType( type );
					}


					org.matsim.core.controler.Controler controler = RunMatsim.createControler(scenario);

					controler.run();

					FDEventsHandler eventsHandler = new FDEventsHandler();
					eventsHandler.readNetwork(config.controler().getOutputDirectory() + "/output_network.xml.gz");


					EventsManager eventsManager = EventsUtils.createEventsManager();
					eventsManager.addHandler(eventsHandler);

					MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
					eventsReader.readFile(config.controler().getOutputDirectory() + "/output_events.xml.gz");
					eventsHandler.writeFlowSpeeds(config.controler().getOutputDirectory() + "/speedFlows.csv");

				}

				if(divideFactor == 1){
					break;
				}
			}
		}
	}


	private static void createPopulation(Scenario scenario, int numberOfPersons){

		Random populationRandom = new Random(scenario.getConfig().global().getRandomSeed());
		for(int i = 0; i<200; i++){
			populationRandom.nextDouble();
		}

		PopulationFactory pf = scenario.getPopulation().getFactory();
		final FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), FFFConfigGroup.class);
		
		
		//Surprisingly, this seems to do the job
		
		//fffConfig.setJohnsonGamma(fffConfig.getJohnsonGamma() * correctionFactor);
		//fffConfig.setJohnsonDelta(fffConfig.getJohnsonDelta() * (1+(correctionFactor-1)*2/3));
			

		fffConfig.setCorrectionFactor(Math.pow(divideFactor,0.017));
					
		
		

		int inverseSD = 5;

		for(int i = 1; i <= numberOfPersons/divideFactor; i++){
			int fromInt = 1;
			Link fromLink = scenario.getNetwork().getLinks().get(Id.createLinkId(fromInt + "_" + TransportMode.bike));
			int toInt = 4;

			Link toLink = scenario.getNetwork().getLinks().get(Id.createLinkId(toInt + "_" + TransportMode.bike));
			double startTime = populationRandom.nextGaussian()*(endTime-3600)/inverseSD + (endTime-3600)/2;
			if(startTime < 0){
				startTime = 0;
			} else if(startTime > endTime - 3600){
				startTime = endTime - 3600;
			}

			Person person = pf.createPerson(Id.createPersonId(i));
			Plan plan = PopulationUtils.createPlan(person);
			Activity startAct = PopulationUtils.createActivityFromLinkId("work", fromLink.getId());
			startAct.setEndTime(startTime);
			startAct.setCoord(fromLink.getCoord());

			Leg leg = PopulationUtils.createLeg(TransportMode.bike);
			leg.setRoute(null);

			Activity endAct = PopulationUtils.createActivityFromLinkId("home", toLink.getId());
			endAct.setEndTime(endTime);
			endAct.setCoord(toLink.getCoord());

			plan.addActivity(startAct);
			plan.addLeg(leg);
			plan.addActivity(endAct);
			person.addPlan(plan);

			//Really, the desired speeds shouldn't be based on random numbers from the uniform distribution, but from Bates Distribution (https://en.wikipedia.org/wiki/Bates_distribution).
			//There seems to be no closed-form way of doing this. Instead, the draws can be done internally.

			double u = 0;
			double v_0;
			if(useBatesDistributionForSpeed ){
				for(int j = 0; j < divideFactor; j++){
					u += 1/RunMatsim.uniformToJohnson(populationRandom.nextDouble(), fffConfig);
				}
				u /= divideFactor;
				v_0 = 1/u;
				System.out.println(v_0);
			} else {
				u = populationRandom.nextDouble();
				v_0 = RunMatsim.uniformToJohnson(u, fffConfig);
			}

			v_0 = Math.max(v_0, fffConfig.getMinimumAllowedDesiredSpeed());

			//The z_c's can be done directly by using a standard deviation that is sqrt(divideFactor) smaller.
			double z_c =  populationRandom.nextGaussian();
			if(useBatesDistributionForHeadway){
				z_c /= Math.sqrt(divideFactor);
			}
			double theta_0 = fffConfig.getTheta_0() + z_c * fffConfig.getZeta_0();
			double theta_1 = fffConfig.getTheta_1() + z_c * fffConfig.getZeta_1();
			person.getAttributes().putAttribute(RunMatsim.DESIRED_SPEED, v_0);
			person.getAttributes().putAttribute(RunMatsim.HEADWAY_DISTANCE_INTERCEPT, theta_0 * divideFactor);
			person.getAttributes().putAttribute(RunMatsim.HEADWAY_DISTANCE_SLOPE, theta_1 * divideFactor);
			person.getAttributes().putAttribute(RunMatsim.BICYCLE_LENGTH, fffConfig.getLambda_c() * divideFactor );

			scenario.getPopulation().addPerson(person);
		}
	}



	private static void createNetwork(Scenario scenario, int numberOfLanes){



		Network network = scenario.getNetwork();

		Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord(2, 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId(3), new Coord(4, 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId(4), new Coord(6, 0));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId(5), new Coord(8, 0));

		NetworkUtils.createAndAddLink(network,Id.createLinkId(1 + "_" + TransportMode.bike), node1, node2, 200, Double.MAX_VALUE, Double.MAX_VALUE, (double) numberOfLanes + 2 );
		NetworkUtils.createAndAddLink(network,Id.createLinkId(2 + "_" + TransportMode.bike), node2, node3, 1000, Double.MAX_VALUE, Double.MAX_VALUE, (double) numberOfLanes );
		NetworkUtils.createAndAddLink(network,Id.createLinkId(3 + "_" + TransportMode.bike), node3, node4, 1000, Double.MAX_VALUE, Double.MAX_VALUE, (double) numberOfLanes );
		NetworkUtils.createAndAddLink(network,Id.createLinkId(4 + "_" + TransportMode.bike), node4, node5, 200, Double.MAX_VALUE, Double.MAX_VALUE, (double) numberOfLanes + 2);

		Set<String> allowedModes = new HashSet<>();
		allowedModes.addAll(Arrays.asList(TransportMode.bike));

		for(Link l:network.getLinks().values()){
			l.setAllowedModes(allowedModes);
		}
	}


}
