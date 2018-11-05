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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.DefaultActivityTypes;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.qnetsimengine.MadsQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author nagel
 *
 */
public class RunMatsim {

	public static void main(String[] args) {
//		Gbl.assertIf(args.length >=1 && args[0]!="" );
//		run(ConfigUtils.loadConfig(args[0]));
		// makes some sense to not modify the config here but in the run method to help  with regression testing.
		
		run( ConfigUtils.createConfig() ) ;
		
	}
	
	static void run(Config config) {
		
		// possibly modify config here
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		{
			Network network = scenario.getNetwork();
			final NetworkFactory factory = network.getFactory();
			;
			
			final Id<Node> id = Id.createNodeId( "abc" );
			final Coord coord = new Coord( 0., 0. );
			final Node node = factory.createNode( id, coord );
//		final Node node = NetworkUtils.createNode( id, coord );
			network.addNode( node );

//		final Link link ;
//		network.addLink( link );
		}
		{
			Population population = scenario.getPopulation();
			final PopulationFactory pf = population.getFactory();
			
			final Id<Person> id = Id.createPersonId( "abc" ) ;
			final Person person = pf.createPerson( id );
			population.addPerson( person );
			
			final Plan plan  = pf.createPlan() ;
			person.addPlan( plan );
			
			final String actType = DefaultActivityTypes.work ;
			final Coord coord = CoordUtils.createCoord( 0. , 1. );
			final Activity act = pf.createActivityFromCoord( actType, coord ) ;
			plan.addActivity( act );
			
			final String mode = TransportMode.car ;
			final Leg leg = pf.createLeg( mode ) ;
			plan.addLeg( leg );
			
		}
		
		// possibly modify scenario here
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		// possibly modify controler here
		
		controler.addOverridingModule( new AbstractModule() {
			@Override public void install() {
				this.bind( QNetworkFactory.class ).to( MadsQNetworkFactory.class ) ;
				// (yyyy  unstable API!  kai, oct'18)
			}
		} );
		
//		controler.addOverridingQSimModule( new AbstractQSimModule() {
//			@Override protected void configureQSim() {
//				throw new RuntimeException( "not implemented" );
//			}
//		} );
		
		// ---
		
		controler.run();
	}
	
}
