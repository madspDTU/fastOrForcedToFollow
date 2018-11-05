package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import javax.inject.Inject;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

class RunMatsimForClass {
	
	public static void main( String [] args ) {
		
		final URL context = ExamplesUtils.getTestScenarioURL( "equil" );;
		final URL configFileName = IOUtils.newUrl( context, "config.xml" );;
		Config config = ConfigUtils.loadConfig( configFileName );
		
		
		config.controler().setOverwriteFileSetting( deleteDirectoryIfExists );
		
		config.controler().setLastIteration( 2 );
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Network network = scenario.getNetwork() ;
		final NetworkFactory nf = network.getFactory();;
		for ( Link link : network.getLinks().values() ) {
			if ( link.getAllowedModes().contains( TransportMode.bike ) && link.getAllowedModes().contains(  TransportMode.car ) ) {

				// make a car only link:  // yy what about the other modes?
				{
					Set<String> set = new HashSet<>();
					set.add( TransportMode.car );
					link.setAllowedModes( set );
				}
				
				final Id<Link> id = Id.createLinkId(  link.getId().toString() + "_bike" ) ;
				final Node fromNode = link.getFromNode() ;
				final Node toNode = link.getToNode() ;
				final Link bikeLink = nf.createLink( id, fromNode, toNode );

				bikeLink.setLength( link.getLength() );
				bikeLink.setFreespeed( 10. );
				bikeLink.setCapacity( 2000. ); // this is in PCE; should be 3000 _bicycles_ per efficient lane; efficient lane is about 1.5 m.
				bikeLink.setNumberOfLanes( 3 );
				{
					Set<String> set = new HashSet<>();
					set.add( TransportMode.bike );
					bikeLink.setAllowedModes( set );
				}

				network.addLink( bikeLink );
			}
		}
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule( new AbstractModule() {
			@Override public void install() {
				this.bind( AgentFactory.class ).to( MyAgentFactory.class );
				final TravelDisutilityFactory abc = new MyTDF() ;
				this.addTravelDisutilityFactoryBinding(TransportMode.bike).toInstance( abc ) ;
			}
		} ) ;
		
		controler.addOverridingQSimModule( new AbstractQSimModule() {
			@Override protected void configureQSim() {
				this.bind( AgentFactory.class ).to( MyAgentFactory.class );
			}
		} );
		
		// ---
		
		controler.run();
		
	}
	
	private static class MyAgentFactory implements AgentFactory {
		
		@Inject private Scenario scenario;
		
		@Override public MobsimAgent createMobsimAgentFromPerson( final Person p ) {
			Scenario sc = scenario ;
		}
	}
}
