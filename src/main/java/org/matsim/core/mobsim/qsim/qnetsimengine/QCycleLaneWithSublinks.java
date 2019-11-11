package org.matsim.core.mobsim.qsim.qnetsimengine;

import fastOrForcedToFollow.Cyclist;
import fastOrForcedToFollow.Sublink;
import fastOrForcedToFollow.configgroups.FFFConfigGroup;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;

class QCycleLaneWithSublinks extends QCycleLane{
	private static final Logger log = Logger.getLogger( QCycleLaneWithSublinks.class ) ;

	private QCycleLaneWithSublinks( Sublink[] fffLinkArray, AbstractQLink qLinkImpl, NetsimEngineContext context){
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
					(int) link.getNumberOfLanes(), link.getLength(), fffConfig.getLMax() );
			return new QCycleLaneWithSublinks(sublinkArray, qLinkImpl, context );
		}
	}


	@Override public boolean isAcceptingFromUpstream() { //Done!
		return !fffLinkArray[0].isLinkFull();
	}


	@Override public boolean doSimStep() {

		for(int i = this.lastIndex; i>=0; i--){
			Sublink currentFFFLink = fffLinkArray[i];
			QCycle qCyc;
			while((qCyc = currentFFFLink.getQ().peek()) != null){

				if( qCyc.getEarliestLinkExitTime() > context.getSimTimer().getTimeOfDay()){
					break;
				}

				Cyclist cyclist = qCyc.getCyclist();


				if(i < this.lastIndex){
					// internal fff logic:

					// Selecting the appropriate pseudoLane:
					Sublink receivingFFFLink = fffLinkArray[i+1];
					if(receivingFFFLink.isLinkFull()){
						break;
					}
					// qCyc can in fact leave current sublink
					currentFFFLink.getQ().remove();
					currentFFFLink.reduceOccupiedSpace(cyclist, cyclist.getSpeed() );			

					doFFFStuff(cyclist, receivingFFFLink);

					receivingFFFLink.addToQ(qCyc);

				} else { ///fffLink is last subLink

					currentFFFLink.getQ().remove();
					currentFFFLink.reduceOccupiedSpace(cyclist, cyclist.getSpeed() );

					if(qCyc.getDriver().isWantingToArriveOnCurrentLink()){
						qLinkImpl.letVehicleArrive(qCyc );
						numberOfCyclistsOnLink.decrementAndGet();
						continue;
					}


					//Auxiliary buffer created to fit the piece into MATSim.
					currentFFFLink.addVehicleToLeavingVehicles(qCyc );
					currentFFFLink.increaseOccupiedSpaceByBicycleLength(cyclist);
					currentFFFLink.setLastTimeMoved(qCyc.getEarliestLinkExitTime());


					final QNodeI toNode = qLinkImpl.getToNode();
					if ( toNode instanceof AbstractQNode ) {
						((AbstractQNode) toNode).activateNode();
					} 
				}
			}
		}
		return true;
	}

	@Override
	protected void doFFFStuff(Cyclist cyclist, Sublink fffLink) {
		cyclist.selectPseudoLaneAndUpdateSpeed( fffLink);

		//Only relevant when considering downscaled population
		//		double surplus = pseudoLane.getLength() / vTilde * (correctionFactor-1);
		//		pseudoLane.setTReady(tStart + tOneBicycleLength + surplus);
		//		pseudoLane.setTEnd(cyclist.getTEarliestExit() + tOneBicycleLength + surplus);
	}


	@Override public void addFromWait( final QVehicle veh ) {

		// ensuring that the first provisional earliest link exit cannot be before now.
		double now = context.getSimTimer().getTimeOfDay() ;
		QCycle qCyc = (QCycle) veh;

		if(qCyc.getDriver().isWantingToArriveOnCurrentLink()){
			qLinkImpl.letVehicleArrive(qCyc);
			return;
		}
		Sublink lastSubLink = fffLinkArray[this.lastIndex];

		// Essentially just skipping this first link (in order to be consistent with scorin mechanism)
		numberOfCyclistsOnLink.incrementAndGet();
		lastSubLink.addVehicleToLeavingVehicles(qCyc );
		lastSubLink.increaseOccupiedSpaceByBicycleLength(qCyc.getCyclist());
		lastSubLink.setLastTimeMoved(now);
		qCyc.setEarliestLinkExitTime(now);
		
		final QNodeI toNode = qLinkImpl.getToNode();
		if ( toNode instanceof AbstractQNode ) {
			((AbstractQNode) toNode).activateNode();
		} 
	}

	@Override
	public void placeVehicleAtFront(QVehicle veh){
		QCycle qCyc = (QCycle) veh; // Upcast
		numberOfCyclistsOnLink.incrementAndGet();
		this.fffLinkArray[this.lastIndex].addVehicleToFrontOfLeavingVehicles(qCyc);
		this.fffLinkArray[this.lastIndex].increaseOccupiedSpaceByBicycleLength(qCyc.getCyclist());
	}
	
}
