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

import org.hsqldb.lib.Iterator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsPopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import fastOrForcedToFollow.ToolBox;

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

/**
 * @author nagel
 *
 */
public class RunMatsim {
	public static final String DESIRED_SPEED = "v_0";
	public static final String HEADWAY_DISTANCE_PREFERENCE = "z_c";

	public static void main(String[] args) {

		final URL configUrl = IOUtils.newUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );

		Config config = ConfigUtils.loadConfig( configUrl ) ;

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists );

		config.controler().setLastIteration( 2 );

		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		Random speedRandom = new Random();
		Random headwayRandom = new Random();
		for(int i = 0; i <200; i++){
			speedRandom.nextDouble();
			headwayRandom.nextDouble();
		}

		Population population= scenario.getPopulation() ;
		for ( Person person : population.getPersons().values() ) {
			if ( true ) {  // Forcing all legs in scenario to be made by bicycle...
				double v_0 = ToolBox.uniformToJohnson(speedRandom.nextDouble());
				double z_c = headwayRandom.nextDouble(); 
				person.getAttributes().putAttribute(DESIRED_SPEED, v_0);
				person.getAttributes().putAttribute(HEADWAY_DISTANCE_PREFERENCE, z_c);

				for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
					if ( pe instanceof Leg ) {
						( (Leg) pe ).setMode( TransportMode.bike );
						( (Leg) pe ).setRoute( null  );  // invalidate route since it will be on a different network
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
			bikeLink.setFreespeed( 10. );
			bikeLink.setCapacity( 200000. ); // this is in PCE; should be 3000 _bicycles_ per efficient lane; efficient lane is about 1.5 m.
			bikeLink.setNumberOfLanes( 2 );
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


		NetworkUtils.writeNetwork( scenario.getNetwork(), "./output/net.xml" );
		PopulationUtils.writePopulation( scenario.getPopulation(), "./output/pop.xml" );

		System.exit(-1) ;

		// ---

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new AbstractModule(  ) {
			@Override public void install() {
				this.bind( QNetworkFactory.class ).to( MadsQNetworkFactory.class ) ;
			}
		} );
		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind( AgentSource.class).to( MadsPopulationAgentSource.class).asEagerSingleton();
			}
		});


		// ---

		controler.run();

	}
}





