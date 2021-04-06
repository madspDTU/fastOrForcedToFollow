package org.matsim.core.mobsim.qsim.qnetsimengine;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;

public class DefaultMadsQNetworkFactory extends AbstractMadsQNetworkFactory {

	private static final Logger log = Logger.getLogger( DefaultMadsQNetworkFactory.class ) ;
	
	@Inject DefaultMadsQNetworkFactory(EventsManager events, Scenario scenario) {
		super(events, scenario);
	}	
	
	@Override
	public QLinkI createNetsimLink(final Link link, final QNodeI toQueueNode) {
		if ( link.getAllowedModes().contains( TransportMode.bike ) ) {
			Gbl.assertIf( link.getAllowedModes().size()==1 ); // not possible with multi-modal links! kai, oct'18
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder( context, netsimEngine );
			linkBuilder.setLaneFactory( new QCycleLaneWithSublinks.Builder(context, fffConfig));
			return linkBuilder.build( link, toQueueNode );
		} else {
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder( context, netsimEngine );
			linkBuilder.setLaneFactory(new QueueWithBuffer.Builder( context ));
			return linkBuilder.build( link, toQueueNode );
		}

	}

	@Override
	public QNodeI createNetsimNode(final Node node) {
		QNodeImplWithCounter.Builder builder = new QNodeImplWithCounter.Builder( netsimEngine, context ) ;
		return  builder.build(node);
	}
}
