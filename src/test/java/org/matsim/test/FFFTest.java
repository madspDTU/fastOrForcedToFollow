/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.test;

import java.math.BigInteger;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator.Result;

import fastOrForcedToFollow.ToolBox;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;


public class FFFTest {

	private static final Logger log = Logger.getLogger( FFFTest.class);
	public static final String DESIRED_SPEED = "v_0";
	public static final String HEADWAY_DISTANCE_PREFERENCE = "z_c";
	public static final long RANDOM_SEED = 5355633;


	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test
	public final void testBerlin() {
		if(true) return;
		int lanesPerLink = 4;    //Works for 1, 2, 3, and 4

		Config config = configCreator("berlin");
		Scenario scenario = scenarioCreator(config, lanesPerLink, false);
		Controler controler = controlerCreator(scenario);
		try {		
			controler.run();
		} catch ( Exception ee ) {
			ee.printStackTrace();
		}

		String newEventsFile = utils.getOutputDirectory() + "/output_events.xml.gz";
		String referenceEventsFile = utils.getInputDirectory() + "/output_events_" + lanesPerLink + ".xml.gz";
		String referencePopulationFile = utils.getInputDirectory() + "/output_plans_" + lanesPerLink + ".xml.gz";

		Config refConfig = ConfigUtils.createConfig();
		Scenario refScenario = ScenarioUtils.createScenario(refConfig);
		PopulationReader pr = new PopulationReader(refScenario);
		pr.readFile(referencePopulationFile);
		boolean booleanPopulation = PopulationUtils.equalPopulation(refScenario.getPopulation(), scenario.getPopulation());

		Result resultEvents = EventsFileComparator.compare(referenceEventsFile, newEventsFile);
		if(booleanPopulation){ log.info("Populations files are semantic equivalent"); 			} else {
			log.warn("Populations files are not semantic equivalent"); 	}

		Assert.assertEquals(resultEvents, Result.FILES_ARE_EQUAL);
		Assert.assertTrue("Different plans file", booleanPopulation);

	}




	@Test
	public final void test() {
		//if(true) return;

		int lanesPerLink = 4;   //Works for 1, 2, 3, and 4

		Config config = configCreator("equil");
		Scenario scenario = scenarioCreator(config, lanesPerLink, true);
		Controler controler = controlerCreator(scenario);
		try {		
			controler.run();
		} catch ( Exception ee ) {
			ee.printStackTrace();
		}

		String newEventsFile = utils.getOutputDirectory() + "/output_events.xml.gz";
		String referenceEventsFile = utils.getInputDirectory() + "/output_events_" + lanesPerLink + ".xml.gz";
		String referencePopulationFile = utils.getInputDirectory() + "/output_plans_" + lanesPerLink + ".xml.gz";

		Config refConfig = ConfigUtils.createConfig();
		Scenario refScenario = ScenarioUtils.createScenario(refConfig);
		PopulationReader pr = new PopulationReader(refScenario);
		pr.readFile(referencePopulationFile);
		boolean booleanPopulation = PopulationUtils.equalPopulation(refScenario.getPopulation(), scenario.getPopulation());

		Result resultEvents = EventsFileComparator.compare(referenceEventsFile, newEventsFile);
		if(booleanPopulation){ log.info("Populations files are semantic equivalent"); 			} else {
			log.warn("Populations files are not semantic equivalent"); 	}

		Assert.assertEquals(resultEvents, Result.FILES_ARE_EQUAL);
		Assert.assertTrue("Different plans file", booleanPopulation);

	}


	private Config configCreator(String exampleName){
		URL url = ExamplesUtils.getTestScenarioURL( exampleName);;
		URL configUrl = IOUtils.newUrl( url, "config.xml" ) ;
		Config config = ConfigUtils.loadConfig( configUrl ) ;

		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 0 );
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);

		config.qsim().setEndTime( 100.*3600. );

		PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike) ;
		config.planCalcScore().addModeParams( params );
		final List<String> networkModes = Arrays.asList( new String[]{TransportMode.car, TransportMode.bike} );
		config.qsim().setMainModes( networkModes );
		config.plansCalcRoute().removeModeRoutingParams( TransportMode.bike );
		config.plansCalcRoute().setNetworkModes( networkModes );
		config.travelTimeCalculator().setAnalyzedModes( TransportMode.car + "," + TransportMode.bike);

		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

		return config;		
	}

	private Scenario scenarioCreator(Config config, int lanesPerLink, boolean useRandomActivityLocations){

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		final int L = scenario.getNetwork().getLinks().size();
		BigInteger aux = BigInteger.valueOf((long) Math.ceil(L / 4.));
		int linkStepSize =  Integer.parseInt(String.valueOf(aux.nextProbablePrime()));

		Random speedRandom = new Random(RANDOM_SEED);
		Random headwayRandom = new Random(RANDOM_SEED + 341);
		for(int i = 0; i <200; i++){
			speedRandom.nextDouble();
			headwayRandom.nextDouble();
		}

		Population population= scenario.getPopulation() ;
		int linkInt = 0;
		for ( Person person : population.getPersons().values() ) {
			if ( true ) {  // Forcing all legs in scenario to be made by bicycle...
				{
					double v_0 = ToolBox.uniformToJohnson(speedRandom.nextDouble());
					v_0 = Math.max(v_0, 2.);
					double z_c = headwayRandom.nextDouble(); 
					person.getAttributes().putAttribute(DESIRED_SPEED, v_0);
					person.getAttributes().putAttribute(HEADWAY_DISTANCE_PREFERENCE, z_c);
				}

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
						if(!useRandomActivityLocations){
							act.setLinkId(Id.createLinkId(act.getLinkId().toString() + "_" + TransportMode.bike));
						} else {
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
			}
		}


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
			// custom attribute to the link: width.
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
			VehicleType type = new VehicleTypeImpl( Id.create( TransportMode.car, VehicleType.class  ) ) ;
			scenario.getVehicles().addVehicleType( type );
		}
		{
			VehicleType type = new VehicleTypeImpl( Id.create( TransportMode.bike, VehicleType.class  ) ) ;
			scenario.getVehicles().addVehicleType( type );
		}

		return scenario;

	}


	public Controler controlerCreator(Scenario scenario){
		Controler controler = new Controler( scenario ) ;


		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( MadsQNetworkFactory.class );
//				this.bind( AgentSource.class).to( MadsPopulationAgentSource.class).asEagerSingleton();
								this.bind( QVehicleFactory.class ).to( QCycleAsVehicle.Factory.class ) ;
			}

		});

		return controler;
	}

}
