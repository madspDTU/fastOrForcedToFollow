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

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * @author nagel
 *
 */
public class RunMatsim {
	public static final String DESIRED_SPEED="desiredSpeed" ;

	public static void main(String[] args) {
		
		final URL configUrl = IOUtils.newUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );
		
		Config config = ConfigUtils.loadConfig( configUrl ) ;
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists );

		config.controler().setLastIteration( 2 );

		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Population population= scenario.getPopulation() ;
		for ( Person person : population.getPersons().values() ) {
			if ( person.getId()==Id.createPersonId( 1 ) ) {
				person.getAttributes().putAttribute( DESIRED_SPEED, 13.0  ) ;
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
		for ( Link link : network.getLinks().values() ) {
//			if ( link.getAllowedModes().contains( TransportMode.bike ) && link.getAllowedModes().contains(  TransportMode.car ) ) {
			if ( true ) { // add bicycle links everywhere

				// make a car only link:  // yy what about the other modes?
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

				network.addLink( bikeLink );
			}
		}
		
		NetworkUtils.writeNetwork( scenario.getNetwork(), "net.xml" );
		PopulationUtils.writePopulation( scenario.getPopulation(), "pop.xml" );
		
//		System.exit(-1) ;
		
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





