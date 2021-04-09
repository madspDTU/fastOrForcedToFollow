package fastOrForcedToFollow;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.costcalculators.FFFTravelDisutilityFactory;
import org.matsim.core.events.ParallelEventsManagerImplWithPooledHandlers;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultFFFQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.FFFQNetworkFactoryWithQFFFNodes;
import org.matsim.core.mobsim.qsim.qnetsimengine.FFFQNetworkFactoryWithoutCongestion;
import org.matsim.core.mobsim.qsim.qnetsimengine.FFFQVehicleFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.replanning.strategies.FFFPlanSelectorProvider;
import org.matsim.core.replanning.strategies.FFFReRouteProvider;
import org.matsim.core.router.DesiredSpeedBicycleDijkstraFactory;
import org.matsim.core.router.DesiredSpeedBicycleFastAStarLandmarksFactory;
import org.matsim.core.router.DesiredSpeedBicycleFastDijkstraFactory;
import org.matsim.core.router.FFFLinkToLinkRouting;
import org.matsim.core.router.NetworkRoutingProviderWithCleaning;
import org.matsim.core.router.SingleModeInvertedNetworksCache;
import org.matsim.core.router.SingleModeInvertedTravelTimesCache;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FFFSingleModeTravelTimeCalculatorProvider;
import org.matsim.core.trafficmonitoring.FFFTravelTimeCalculator;
import org.matsim.core.trafficmonitoring.FFFTravelTimeCalculator.FFFObservedLinkToLinkTravelTimes;
import org.matsim.run.RunBicycleCopenhagen;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;
import fastOrForcedToFollow.configgroups.FFFNodeConfigGroup;
import fastOrForcedToFollow.scoring.FFFScoringFunctionFactory;

public class FFFUtils {

	/**
	 * Only adds attributes for cylists lacking attributes. 
	 * 
	 * @param scenario
	 * @return
	 */

	public static void addCyclistAttributes(Scenario scenario){
		final Config config = scenario.getConfig();
		final long RANDOM_SEED = config.global().getRandomSeed();
		final FFFConfigGroup fffConfig = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		addCyclistAttributes(scenario, fffConfig, RANDOM_SEED);
	}
	
	
	private static void addCyclistAttributes(Scenario scenario, FFFConfigGroup fffConfig, long seed){

		final Random speedRandom = new Random(seed);
		final Random headwayRandom = new Random(seed + 341);
		final Random headwayRandom2 = new Random(seed + 732341);

		for(int i = 0; i <200; i++){
			speedRandom.nextDouble();
			headwayRandom.nextDouble();
			headwayRandom2.nextDouble();
		}

		final Population population= scenario.getPopulation() ;

		final List<String> attributeTypes = Arrays.asList(FFFConfigGroup.DESIRED_SPEED,
				FFFConfigGroup.HEADWAY_DISTANCE_INTERCEPT, FFFConfigGroup.HEADWAY_DISTANCE_SLOPE,
				FFFConfigGroup.BICYCLE_LENGTH);
		for ( Person person : population.getPersons().values() ) {
			if(!person.getAttributes().getAsMap().keySet().containsAll(attributeTypes)){
				double v_0 = 0;
				while(v_0 < fffConfig.getMinimumAllowedDesiredSpeed() ||
						v_0 > fffConfig.getMaximumAllowedDesiredSpeed() ){
					v_0 = uniformToJohnson(speedRandom.nextDouble(), fffConfig);
				}
				double z_beta;
				while(true){
					z_beta = headwayRandom.nextDouble();
					double p = Math.pow(4*z_beta*(1-z_beta), fffConfig.getBeta_alpha()- 1);
					double u = headwayRandom2.nextDouble();
					if(u < p){
						break;
					}
				}
				double theta_0 = fffConfig.getTheta_0() + (2*z_beta-1) * fffConfig.getZeta_0();
				double theta_1 = fffConfig.getTheta_1() + (2*z_beta-1) * fffConfig.getZeta_1();

				person.getAttributes().putAttribute(FFFConfigGroup.DESIRED_SPEED, v_0);
				person.getAttributes().putAttribute(FFFConfigGroup.HEADWAY_DISTANCE_INTERCEPT, theta_0);
				person.getAttributes().putAttribute(FFFConfigGroup.HEADWAY_DISTANCE_SLOPE, theta_1);
				person.getAttributes().putAttribute(FFFConfigGroup.BICYCLE_LENGTH, fffConfig.getLambda_c());
			}
		}

		VehicleType type = scenario.getVehicles().getFactory().createVehicleType(Id.create( TransportMode.bike, VehicleType.class  ) ) ;
		scenario.getVehicles().addVehicleType( type );

	}
	
	
	/**
	 * Converts a uniform number to a appropriately Johnson S_u distributed number.
	 * 
	 * @param u The uniform number to be transformed
	 * 
	 * @return A number from the appropriate Johnson S_u distribution.
	 */
	
	public static double uniformToJohnson(final double u, FFFConfigGroup fffConfig){
		return fffConfig.getJohnsonLambda() * Math.sinh( (qNorm(u) - fffConfig.getJohnsonGamma()) / fffConfig.getJohnsonDelta()) + fffConfig.getJohnsonXsi();
	}
	
	
	
	
	/**
	 * Method for computing the inverse cumulative distribution function of the standard normal distribution.
	 * Taken directly from https://stackedboxes.org/2017/05/01/acklams-normal-quantile-function/
	 * 
	 * @param u The quantile whose corresponding value, z, is to be found.
	 * 
	 * @return The value that would yield u when inserted into the
	 * cumulative distribution function of the standard normal distribution 
	 */
	private static double qNorm(final double u){
		double a1 = -3.969683028665376e+01;
		double a2 =  2.209460984245205e+02;
		double a3 = -2.759285104469687e+02;
		double a4 =  1.383577518672690e+02;
		double a5 = -3.066479806614716e+01;
		double a6 =  2.506628277459239e+00;

		double b1 = -5.447609879822406e+01;
		double b2 =  1.615858368580409e+02;
		double b3 = -1.556989798598866e+02;
		double b4 =  6.680131188771972e+01;
		double b5 = -1.328068155288572e+01;

		double c1 = -7.784894002430293e-03;
		double c2 = -3.223964580411365e-01;
		double c3 = -2.400758277161838e+00;
		double c4 = -2.549732539343734e+00;
		double c5 =  4.374664141464968e+00;
		double c6 =  2.938163982698783e+00;

		double d1 =  7.784695709041462e-03;
		double d2 =  3.224671290700398e-01;
		double d3 =  2.445134137142996e+00;
		double d4 =  3.754408661907416e+00;

		double p_low  = 0.02425;
		double p_high = 1 - p_low;

		if(u < p_low){
			double q = Math.sqrt(-2*Math.log(u));
			return (((((c1*q+c2)*q+c3)*q+c4)*q+c5)*q+c6) /
					((((d1*q+d2)*q+d3)*q+d4)*q+1);
		}

		if(u > p_high){
			double q = Math.sqrt(-2*Math.log(1-u));
			return -(((((c1*q+c2)*q+c3)*q+c4)*q+c5)*q+c6) /
					((((d1*q+d2)*q+d3)*q+d4)*q+1);
		}

		double q = u - 0.5;
		double r = q * q;
		return (((((a1*r+a2)*r+a3)*r+a4)*r+a5)*r+a6)*q /
				(((((b1*r+b2)*r+b3)*r+b4)*r+b5)*r+1);

	}


	
	public static Controler prepareControlerForFFF(Controler controler){

		// Changing the network factory and vehicle factory - these are method-dependent
		// With this method, the default setup with bicycle congestion but not RoW is used. 
		controler.addOverridingQSimModule(new AbstractQSimModule() {

			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( DefaultFFFQNetworkFactory.class );
				this.bind( QVehicleFactory.class ).to( FFFQVehicleFactory.class ) ;
			}

		});

		// Adding routing and additional stuff to controler
		addGeneralFFFStuffToControler(controler);

		return controler;
	}

	public static void addGeneralFFFStuffToControler(Controler controler){

		final Scenario scenario = controler.getScenario();

		Collection<String> networkModes = scenario.getConfig().plansCalcRoute().getNetworkModes();

		FFFNodeConfigGroup fffNodeConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), FFFNodeConfigGroup.class);


		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {

				// New scoring function (taking desired speeds into account)
				this.bindScoringFunctionFactory().to( FFFScoringFunctionFactory.class ) ;

				// Replacing the least cost path calculator accordingly.
				switch(scenario.getConfig().controler().getRoutingAlgorithmType()) {
				case FastAStarLandmarks: 
					this.bind(LeastCostPathCalculatorFactory.class).to(
							DesiredSpeedBicycleFastAStarLandmarksFactory.class );
					break;
				case FastDijkstra: 
					this.bind(LeastCostPathCalculatorFactory.class).to(
							DesiredSpeedBicycleFastDijkstraFactory.class);
					break;
				default:
					this.bind(LeastCostPathCalculatorFactory.class).to(
							DesiredSpeedBicycleDijkstraFactory.class);
					break;
				}

				// New plan selection/removal strategies. Using a bounded model instead of the traditional approach a fixed capacity		
				addPlanStrategyBinding(FFFPlanSelectorProvider.getName()).toProvider(FFFPlanSelectorProvider.class);
				addPlanStrategyBinding(FFFReRouteProvider.getName()).toProvider(FFFReRouteProvider.class);
		

				// An improved (for large-scale FFF-scenarios) parallel events manager - also counting stuck events
				bindEventsManager().to(ParallelEventsManagerImplWithPooledHandlers.class).asEagerSingleton();

				// Changing traveldisutility factory.
				// When linktolink routing is enabled, using another pessimistic linkToLinkRouting 
				// otherwise providing additional cleaning (necessary for FFF simulation)
				for(String mode : networkModes){
					addTravelDisutilityFactoryBinding(mode).toInstance( new FFFTravelDisutilityFactory( mode, getConfig() ) );
					if(getConfig().controler().isLinkToLinkRoutingEnabled()){
						this.addRoutingModuleBinding(mode).toProvider(new FFFLinkToLinkRouting(mode));
					} else {
						this.addRoutingModuleBinding(mode).toProvider(new NetworkRoutingProviderWithCleaning(mode));
					}
				}


				// Adjusting the traveltimecalculators, so that they can become pessimistic and can work without using linkLeaveEvents. 
				{
					// bind the TravelTimeCalculator, which is the observer and aggregator:
					//	bind(FFFTravelTimeCalculator.class).in(Singleton.class);
					// bind the TravelTime objects.  In this case, this just passes on the same information from TravelTimeCalculator to each individual mode:
					if (getConfig().travelTimeCalculator().isCalculateLinkTravelTimes()) {
						//					for (String mode : CollectionUtils.stringToSet(getConfig().travelTimeCalculator().getAnalyzedModesAsString() )) {
						for ( String mode : getConfig().plansCalcRoute().getNetworkModes() ) {
							bind(FFFTravelTimeCalculator.class).annotatedWith(Names.named(mode)).toProvider(
									new FFFSingleModeTravelTimeCalculatorProvider(fffNodeConfig)).in(Singleton.class);

							addTravelTimeBinding(mode).toProvider(new Provider<TravelTime>() {
								@Inject Injector injector;
								@Override public TravelTime get() {
									return injector.getInstance( Key.get( FFFTravelTimeCalculator.class, Names.named( mode ) ) ).getLinkTravelTimes();
								}
							}).in( Singleton.class );

						}
					}
					if (getConfig().travelTimeCalculator().isCalculateLinkToLinkTravelTimes()) {
						bind(SingleModeInvertedNetworksCache.class).asEagerSingleton();
						bind(SingleModeInvertedTravelTimesCache.class).asEagerSingleton();
						bind(FFFTravelTimeCalculator.class).toProvider(
								new FFFSingleModeTravelTimeCalculatorProvider(fffNodeConfig)).in(Singleton.class);
						bind(LinkToLinkTravelTime.class).toProvider(FFFObservedLinkToLinkTravelTimes.class);

					}
				}
			}
		} );
	}


	public static void prepareControlerForFFFWithoutCongestion(Controler controler){


		// Changing the network factory and vehicle factory - these are method-dependent
		// With this method, the setup without bicycle congestion is used. 
		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( FFFQNetworkFactoryWithoutCongestion.class );
				this.bind( QVehicleFactory.class ).to( FFFQVehicleFactory.class ) ;
			}

		});

		addGeneralFFFStuffToControler(controler);
	}

	public static void prepareControlerForFFFWithRoW(Controler controler){
		
		// Changing the network factory and vehicle factory - these are method-dependent
		// With this method, the setup with bicycle congestion and explicit modelling of right-of-way is used. 

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( FFFQNetworkFactoryWithQFFFNodes.class );
				this.bind( QVehicleFactory.class ).to( FFFQVehicleFactory.class ) ;
			}

		});

		addGeneralFFFStuffToControler(controler);
	}

	
	
	public static void addRoWModuleToConfig(Config config){
		FFFNodeConfigGroup fffNodeConfig = ConfigUtils.addOrGetModule(config, FFFNodeConfigGroup.class);
		fffNodeConfig.setBicycleDelay(RunBicycleCopenhagen.timeOutTime);
		fffNodeConfig.setCarDelay(RunBicycleCopenhagen.timeOutTime);
		fffNodeConfig.setSmallRoadLeftBufferCapacity(RunBicycleCopenhagen.leftCapacity);
	}
	
	



	public static void prepareScenarioForFFF(Scenario scenario){

		final Config config= scenario.getConfig();
		final int lanesPerLink = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class).getDefaultNumberOfLanesPerLink();
		
		final long RANDOM_SEED = config.global().getRandomSeed();

		final int L = scenario.getNetwork().getLinks().size();
		final BigInteger aux = BigInteger.valueOf((long) Math.ceil(L / 4.));
		final int linkStepSize =  Integer.parseInt(String.valueOf(aux.nextProbablePrime()));

		final Random speedRandom = new Random(RANDOM_SEED);
		final Random headwayRandom = new Random(RANDOM_SEED + 341);
		final Random modeRandom = new Random(RANDOM_SEED + 513);

		for(int i = 0; i <200; i++){
			speedRandom.nextDouble();
			headwayRandom.nextDouble();
			modeRandom.nextDouble();
		}

		final Population population= scenario.getPopulation() ;

		int linkInt = 0;
		for ( Person person : population.getPersons().values() ) {
			for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
				if ( pe instanceof Leg ) {
					( (Leg) pe ).setMode( TransportMode.bike );
					( (Leg) pe ).setRoute( null  );  // invalidate route since it will be on a different network
				}
			}

			// Create activities on the bicycle links, ensuring that all links except the first and last
			// are different (except if only having two activities).
			int N = (person.getSelectedPlan().getPlanElements().size() +1) / 2;
			int n =1;
			int firstLinkInt = linkInt;

			for ( PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if( pe instanceof Activity){
					Activity act =  (Activity) pe;
					if(n < N  || N == 2){
						act.setLinkId(Id.createLinkId(((linkInt % L) +1) + "_" + TransportMode.bike));
					} else{
						act.setLinkId(Id.createLinkId((firstLinkInt % L) +1 + "_" + TransportMode.bike));
					}
					linkInt+=linkStepSize;
					n++;

				}
			}

		}

		// Add bicycle attributes if not already present.
		FFFUtils.addCyclistAttributes(scenario);


		// adjust network to bicycle stuff:
		Network network = scenario.getNetwork() ;
		final NetworkFactory nf = network.getFactory();
		LinkedList<Link> bikeLinks = new LinkedList<Link>();
		for(Link link : network.getLinks().values()){
			{
				Set<String> set = new HashSet<>();
				for(String allowedMode : link.getAllowedModes()){
					if(!allowedMode.equals(TransportMode.bike)){
						set.add(allowedMode);
					}
				}
				link.setAllowedModes( set );
			}



			final Id<Link> id = Id.createLinkId(  link.getId().toString() + "_bike" ) ;
			final Node fromNode = link.getFromNode() ;
			final Node toNode = link.getToNode() ;
			final Link bikeLink = nf.createLink( id, fromNode, toNode );

			bikeLink.setLength( link.getLength() );
			bikeLink.setFreespeed( Double.MAX_VALUE);  //This is controlled by the desired speed of the individual.
			bikeLink.setCapacity( Double.MAX_VALUE); //The FFF-framework does not use such value.
			bikeLink.setNumberOfLanes( lanesPerLink ); //Ideally, this should be done by creating a
			// custom attribute to the link: width. This value is only a fallback value. It will be overwritten later on
			{
				Set<String> set = new HashSet<>();
				set.add( TransportMode.bike );
				bikeLink.setAllowedModes( set );
			}

			bikeLinks.add(bikeLink);
		}
		for(Link link : bikeLinks){
			network.addLink(link);
		}

		{
			VehicleType type = scenario.getVehicles().getFactory().createVehicleType(Id.create( TransportMode.car, VehicleType.class  ) );
			if(scenario.getVehicles().getVehicleTypes().containsKey(type.getId())) {
				scenario.getVehicles().addVehicleType( type );
			}
		}
		{
			VehicleType type = scenario.getVehicles().getFactory().createVehicleType(Id.create( TransportMode.truck, VehicleType.class  ) );
			type.setPcuEquivalents(2);
			type.setMaximumVelocity(80/3.6);
			if(scenario.getVehicles().getVehicleTypes().containsKey(type.getId())) {
				scenario.getVehicles().addVehicleType( type );
			}
		}
		{
			VehicleType type = scenario.getVehicles().getFactory().createVehicleType( Id.create( TransportMode.bike, VehicleType.class ) );
			if(scenario.getVehicles().getVehicleTypes().containsKey(type.getId())) {
				scenario.getVehicles().addVehicleType( type );
			}
		}

	}




	public static void reducePopulationToN(int n, Population population){
		LinkedList<Person> personsToRemove = new LinkedList<Person>();
		for(Person person : population.getPersons().values()){
			if(n >0){
				n--;
			} else {
				personsToRemove.add(person);
			}
		}
		for(Person person : personsToRemove){
			population.removePerson(person.getId());
		}
	}
	
	public static void removeSouthWesternPart(Network network) {
		// Based on
		// http://www.ytechie.com/2009/08/determine-if-a-point-is-contained-within-a-polygon/
		FFFNodeConfigGroup nodeConfig = new FFFNodeConfigGroup();
		HashMap<String, Integer> map = nodeConfig.getRoadTypeToValueMap();

		Coord[] vertices = getVertices();
		Coord[] southernVertices = getSouthernVertices();

		int linksBefore = network.getLinks().size();
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();
		for (Node node : network.getNodes().values()) {
			if (!isCoordInsidePolygon(node.getCoord(), vertices)) {
				if(isCoordInsidePolygon(node.getCoord(), southernVertices)) {
					nodesToBeRemoved.add(node);
				} else {
					int largestRoadValue = Integer.MIN_VALUE;
					for(Collection<? extends Link> col : Arrays.asList(node.getInLinks().values(), node.getOutLinks().values())) {
						for(Link link : col) {
							int roadValue = map.get(link.getAttributes().getAttribute("type"));
							if(roadValue > largestRoadValue) {
								largestRoadValue = roadValue;
							}
						}
					}
					if(largestRoadValue < map.get("secondary") ) {
						nodesToBeRemoved.add(node);
					}
				}
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
				if (v[i].getX() + (c.getY() - v[i].getY()) / 
						(v[j].getY() - v[i].getY()) * (v[j].getX() - v[i].getX()) < c.getX()) {
					oddNodes = !oddNodes;
				}
			}
			j = i;
		}
		return oddNodes;
	}

	private static Coord[] getVertices() {
		LinkedList<Coord> coords = new LinkedList<Coord>();
		coords.addLast(new Coord(673977.7833, 6172099.281)); // 
		coords.addLast(new Coord(679281.4926, 6189542,191)); // 
		coords.addLast(new Coord(675045.9795, 6206992.6616)); // 
		coords.addLast(new Coord(703658.9441, 6228283.6447)); // 
		coords.addLast(new Coord(728969.9982, 6216640.5598)); // 
		coords.addLast(new Coord(738845.9346, 6137938.4159)); // 
		coords.addLast(new Coord(699130.2605, 6135696.2925)); // 
		coords.addLast(new Coord(686615.3337, 6142709.614)); // 

		Coord[] output = new Coord[coords.size()];
		for (int i = 0; i < output.length; i++) {
			output[i] = coords.pollFirst();
		}
		return output;
	}

	private static Coord[] getSouthernVertices() {
		LinkedList<Coord> coords = new LinkedList<Coord>();
		// All sites from https://epsg.io/map#srs=32632
		coords.addLast(new Coord(621986.78, 6111239.17)); // Lohals
		coords.addLast(new Coord(573186.35, 6022149.54)); // Kiel
		coords.addLast(new Coord(782969.26, 6027815.34)); // Garz
		coords.addLast(new Coord(777294.89, 6142058.83)); // Smygehamn
		coords.addLast(new Coord(697776.79, 6094234.48)); // Tærø

		Coord[] output = new Coord[coords.size()];
		for (int i = 0; i < output.length; i++) {
			output[i] = coords.pollFirst();
		}
		return output;
	}

	
}
