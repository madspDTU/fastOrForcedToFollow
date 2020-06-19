package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.QFFFNode.MoveType;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifier;

public abstract class QFFFAbstractNode { //Interface or abstract

	static final boolean defaultStuckReturnValue = true;
	private static final boolean stuckChangesOrderAtRightPriority = true;
	
	//static final boolean allowFreeingGeneralVehiclesStuckBehindLeftBuffer = false;
	static final boolean allowStuckedVehiclesToMoveDespieConflictingMoves = false;
	private static final boolean timeoutChangesAtRightPriorityWhenStuckedVehiclesMoveDespiteConflictingMoves = false;
	static final boolean defaultTimeoutBehaviourWhenStuckedVehiclesMoveDespiteConflictingMoves = false;
	
//	static final boolean letVehiclesFreedFromBehindLeftVehiclesAlterTimeouts = false;
	public static final int smallRoadLeftBufferCapacity = 2;
	
	
	
	

	final Random random;
	final QFFFNode qNode;
	final QLinkI[] carInLinks;
	final HashMap<Id<Link>, Integer> carOutTransformations;
	final QLinkI[] bicycleInLinks;
	final HashMap<Id<Link>, Integer> bicycleOutTransformations;
	double[][] carTimeouts;
	double[][] bicycleTimeouts;

	protected QFFFAbstractNode(final QFFFNode qNode, final TreeMap<Double, LinkedList<Link>> bundleMap, final QNetwork qNetwork){		
		final int n = bundleMap.size();
		this.qNode = qNode;
		this.carInLinks = new QLinkI[n];
		this.carOutTransformations = new HashMap<Id<Link>, Integer>();
		this.bicycleInLinks = new QLinkI[n];
		this.bicycleOutTransformations = new HashMap<Id<Link>, Integer>();
		for(int i = 0; bundleMap.size()>0; i++){
			LinkedList<Link> list = bundleMap.pollFirstEntry().getValue();
			for(Link link : list){
				QLinkI qLink = qNetwork.getNetsimLink(link.getId());
				Id<Node> id = qNode.getNode().getId();
				if(link.getToNode().getId().equals(id)){
					if( link.getAllowedModes().contains(TransportMode.car)){
						this.carInLinks[i] = qLink;
					} else if (link.getAllowedModes().contains(TransportMode.bike)){
						this.bicycleInLinks[i] = qLink;
					}
				}
				if(link.getFromNode().getId().equals(id)){
					if( link.getAllowedModes().contains(TransportMode.car)){
						carOutTransformations.put(qLink.getLink().getId(), i);
					} else if (link.getAllowedModes().contains(TransportMode.bike)){
						bicycleOutTransformations.put(qLink.getLink().getId(), i);
					}
				}
			}
		}
		this.carTimeouts = new double[n][n];
		this.bicycleTimeouts = new double[n][n];
		this.random = MatsimRandom.getLocalInstance();
	}


	abstract boolean doSimStep(final double now);


	protected double getNowPlusDelayBicycle(final double now){
		return now + qNode.getFFFNodeConfig().getBicycleDelay();
	}

	protected double getNowPlusDelayCar(final double now){
		return now + qNode.getFFFNodeConfig().getCarDelay();
	}

	protected boolean bicycleMoveWithFullLeftTurns(final int inDirection, final double now, 
			final double nowish, TimeoutModifier timeoutModifier) {
		boolean returnValue = false;
		QLinkI inLink = bicycleInLinks[inDirection];
		if(inLink != null) {
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();	
					int outDirection = bicycleOutTransformations.get(nextLinkId);
					if(bicycleTimeouts[inDirection][outDirection] <= now){
						//Ignoring left turns when using right priority.
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now,  MoveType.GENERAL, defaultStuckReturnValue )) {
							break;
						}
						timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
								inDirection, outDirection, null, nowish);
						returnValue = true;
					} else {
						if(allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, MoveType.GENERAL)) {
							this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, MoveType.GENERAL);
							if(defaultTimeoutBehaviourWhenStuckedVehiclesMoveDespiteConflictingMoves) {
								timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
										inDirection, outDirection, null, nowish);
								continue;
							}
						}
						break;
					}
				}
			}
		}
		return returnValue;
	}

	protected boolean carMoveAllowingLeftTurns(final int inDirection, final double now, 
			final double nowish, TimeoutModifier timeoutModifier) {
		boolean returnValue = false;
		QLinkI inLink = carInLinks[inDirection];
		if(inLink != null){
			for(QLaneI lane : inLink.getOfferingQLanes()){
				while(! lane.isNotOfferingVehicle()){
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
					int outDirection = carOutTransformations.get(nextLinkId);
					if(carTimeouts[inDirection][outDirection] <= now){
						//Ignoring left turns when using right priority.
						if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now, MoveType.GENERAL, stuckChangesOrderAtRightPriority)) {
							break;
						}
						timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
								inDirection, outDirection, null, nowish);
						returnValue = true;
						continue;
					} else {
						if(allowStuckedVehiclesToMoveDespieConflictingMoves && this.qNode.vehicleIsStuck(lane, now, MoveType.GENERAL)) {
							this.qNode.moveVehicleFromInlinkToOutlink(veh, inLink, lane, now, MoveType.GENERAL);
							if(timeoutChangesAtRightPriorityWhenStuckedVehiclesMoveDespiteConflictingMoves) {
								timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
										inDirection, outDirection, null, nowish);
								continue;
							}
						}
						break;
					}
				}
			}
		}
		return returnValue;
	}
	
	
	
	
	protected int getBicycleOutDirection(Id<Link> nextLinkId, QVehicle veh) {
		if(bicycleOutTransformations.containsKey(nextLinkId)) {
			return bicycleOutTransformations.get(nextLinkId);
		} else {
			System.out.println(nextLinkId + " does not exist... Existing outlinks are: ");
			for(Id<Link> linkId : bicycleOutTransformations.keySet()) {
				System.out.println(linkId);
			}
			System.out.println(nextLinkId + " does not exist... Existing inlinks are: ");
			for( QLinkI link : bicycleInLinks) {
				System.out.println(link.getLink().getId());
			}
			System.out.println(veh.getDriver().getState());
			System.out.println(veh.getDriver().getMode());
			System.out.println(veh.getDriver().chooseNextLinkId());
			System.out.println(veh.getDriver().isWantingToArriveOnCurrentLink());
			System.out.println(veh.getDriver().getDestinationLinkId());
			System.out.println(veh.getDriver().getCurrentLinkId());
			System.out.println(veh.getDriver().getId());
			System.exit(-1);
			return -1;
		}
	}
	
	protected int decreaseInt(int i){
		return QFFFNodeUtils.decreaseInt(i, carInLinks.length);
	}
	protected int increaseInt(int i){
		return QFFFNodeUtils.increaseInt(i, carInLinks.length);
	}
	
	
	
}
