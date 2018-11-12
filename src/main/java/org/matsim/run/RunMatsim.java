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
package org.matsim.run;

//import fastOrForcedToFollow.ToolBox;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
//import org.matsim.core.mobsim.qsim.qnetsimengine.MadsPopulationAgentSource;
//import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQNetworkFactory;
//import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import fastOrForcedToFollow.BicycleVehicle;

import java.net.URL;
import java.util.*;

/**
 * @author nagel
 *
 */
public class RunMatsim {
	public static final String DESIRED_SPEED = "v_0";
	public static final String HEADWAY_DISTANCE_PREFERENCE = "z_c";
	public static final long RANDOM_SEED = 5355633;

	public static void main(String[] args) {

	//	Config config = ConfigUtils.loadConfig( configUrl ) ;
	
		Config config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" ) ;

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists );

		config.controler().setLastIteration( 10 );
		config.qsim().setEndTime( 12.*3600. );

		PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike) ;
		config.planCalcScore().addModeParams( params );

		final List<String> networkModes = Arrays.asList( new String[]{TransportMode.car, TransportMode.bike} );
		config.qsim().setMainModes( networkModes );
		config.plansCalcRoute().removeModeRoutingParams( TransportMode.bike );
		config.plansCalcRoute().setNetworkModes( networkModes );
		config.travelTimeCalculator().setAnalyzedModes( TransportMode.car + "," + TransportMode.bike);
		// Mads -> Kai: The "setAnalyzedModes" was missing before.. //sideNote: Takes a String as input, but should be List<String>?;
		
		
		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		Random speedRandom = new Random(RANDOM_SEED);
		Random headwayRandom = new Random(RANDOM_SEED + 341);
		for(int i = 0; i <200; i++){
			speedRandom.nextDouble();
			headwayRandom.nextDouble();
		}

		Population population= scenario.getPopulation() ;
		for ( Person person : population.getPersons().values() ) {
			if ( true ) {  // Forcing all legs in scenario to be made by bicycle...
			//double v_0 = ToolBox.uniformToJohnson(speedRandom.nextDouble());
				double v_0 = 5;
				double z_c = headwayRandom.nextDouble(); 
				person.getAttributes().putAttribute(DESIRED_SPEED, v_0);
				person.getAttributes().putAttribute(HEADWAY_DISTANCE_PREFERENCE, z_c);

				for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
					if ( pe instanceof Leg ) {
						( (Leg) pe ).setMode( TransportMode.bike );
						( (Leg) pe ).setRoute( null  );  // invalidate route since it will be on a different network
					}
				}
				
				// Change location of activities to be on the bicycle link
				for ( PlanElement pe : person.getSelectedPlan().getPlanElements()){
					if( pe instanceof Activity){
						Activity act =  (Activity) pe;
						act.setLinkId(Id.createLinkId(act.getLinkId().toString() + "_" + TransportMode.bike));
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
			bikeLink.setNumberOfLanes( 2 ); //Ideally, this should be done by creating a
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


		NetworkUtils.writeNetwork( scenario.getNetwork(), "net.xml" );
		PopulationUtils.writePopulation( scenario.getPopulation(), "pop.xml" );

		// ---

		Controler controler = new Controler( scenario ) ;

		
		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind( QNetworkFactory.class ).to( MadsQNetworkFactory.class ) ;  //mads: does not run (for me) with this enabled.
				this.bind( AgentSource.class).to( MadsPopulationAgentSource.class).asEagerSingleton();  //mads: Will run with this enabled.
//				this.bind( QVehicleFactory.class ).to( QCycleAsVehicle.Factory.class ) ;
			}
		});
		
		
		
		// ---


		controler.run();

	}
}





