package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Sublink;
import fastOrForcedToFollow.configgroups.FFFConfigGroup;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;

import java.util.PriorityQueue;

class QCycleLaneWithoutCongestion extends QCycleLane{
	private static final Logger log = Logger.getLogger( QCycleLaneWithoutCongestion.class ) ;

	private QCycleLaneWithoutCongestion( Sublink[] fffLinkArray, AbstractQLink qLinkImpl, NetsimEngineContext context){
		super(fffLinkArray, qLinkImpl, context);
	}

	static final class Builder implements LaneFactory {
		private final NetsimEngineContext context;
		private final FFFConfigGroup fffConfig;
		Builder(NetsimEngineContext context, FFFConfigGroup fffConfig){
			this.context = context;
			this.fffConfig = fffConfig;
		}
		
		@Override
		public QLaneI createLane(AbstractQLink qLinkImpl) {
			Link link = qLinkImpl.getLink();
			Sublink[] sublinkArray = Sublink.createLinkArrayFromNumberOfPseudoLanes( link.getId().toString() + "_" + TransportMode.bike, 
					1, link.getLength(), fffConfig.getLMax() );
			return new QCycleLaneWithoutCongestion(sublinkArray, qLinkImpl, context );
		}
	}
	
	@Override public boolean isAcceptingFromUpstream() { 
		return true;
	}

	protected void doFFFStuff(Cyclist cyclist, Sublink fffLink) {
		double vTilde = cyclist.getDesiredSpeed();
		cyclist.setSpeed(vTilde);
		double tStart = cyclist.getTEarliestExit() ;
		final double tEarliestExit = tStart + qLinkImpl.getLink().getLength() / vTilde;
		cyclist.setTEarliestExit( tEarliestExit );
	}



	@Override public boolean doSimStep() {
		QCycle qCyc;
		Sublink fffLink = fffLinkArray[0];
		PriorityQueue<QCycle> q = fffLink.getQ();

		while((qCyc = q.peek()) != null){

			double tEarliestExit = qCyc.getEarliestLinkExitTime();
			if( tEarliestExit > context.getSimTimer().getTimeOfDay()){
				break;
			}

			q.remove();
			if(qCyc.getDriver().isWantingToArriveOnCurrentLink()){
				qLinkImpl.letVehicleArrive(qCyc );
				numberOfCyclistsOnLink.decrementAndGet();
				continue;
			}

			//Auxiliary buffer created to fit the piece into MATSim.
			fffLink.addVehicleToLeavingVehicles(qCyc );
			fffLink.setLastTimeMoved(qCyc.getEarliestLinkExitTime());

			final QNodeI toNode = qLinkImpl.getToNode();
			if ( toNode instanceof AbstractQNode ) {
				((AbstractQNode) toNode).activateNode();
			} 
		}
		return true;
	}


	@Override public void addFromWait( final QVehicle veh ) {
		// ensuring that the first provisional earliest link exit cannot be before now.
		double now = context.getSimTimer().getTimeOfDay() ;
		QCycle qCyc = (QCycle) veh;
		
		
		if(qCyc.getDriver().isWantingToArriveOnCurrentLink()){
			qLinkImpl.letVehicleArrive(qCyc);
			return;
		}
		
		Sublink fffLink = fffLinkArray[0];
		numberOfCyclistsOnLink.incrementAndGet();
		fffLink.addVehicleToLeavingVehicles(veh);
		fffLink.setLastTimeMoved(now);
		qCyc.setEarliestLinkExitTime(now);

		final QNodeI toNode = qLinkImpl.getToNode();
		if ( toNode instanceof QNodeImpl ) {
			((QNodeImpl) toNode).activateNode();
		}
	}

	@Override
	public void placeVehicleAtFront(QVehicle veh){
		QCycle qCyc = (QCycle) veh; // Upcast
		numberOfCyclistsOnLink.incrementAndGet();
		this.fffLinkArray[this.lastIndex].addVehicleToFrontOfLeavingVehicles(qCyc);
	}
}
