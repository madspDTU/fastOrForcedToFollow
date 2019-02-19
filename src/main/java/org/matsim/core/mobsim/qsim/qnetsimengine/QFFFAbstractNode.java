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
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import fastOrForcedToFollow.timeoutmodifiers.TimeoutModifierI;

public abstract class QFFFAbstractNode { //Interface or abstract

	
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
	 
	 protected void bicycleMoveWithFullLeftTurns(final int inDirection, final double now, 
				final double nowish, TimeoutModifierI timeoutModifier) {
			QLinkI inLink = bicycleInLinks[inDirection];
			if(inLink != null){
				for(QLaneI lane : inLink.getOfferingQLanes()){
					while(! lane.isNotOfferingVehicle()){
						QVehicle veh = lane.getFirstVehicle();
						Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();	
						int outDirection = bicycleOutTransformations.get(nextLinkId);
						if(bicycleTimeouts[inDirection][outDirection] <= now){
							//Ignoring left turns when using right priority.
							if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now )) {
								break;
							}
							timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
									inDirection, outDirection, null, nowish);
						} else {
							break;
						}
					}
				}
			}
		}

	 protected void carMoveAllowingLeftTurns(final int inDirection, final double now, 
				final double nowish, TimeoutModifierI timeoutModifier) {
			QLinkI inLink = carInLinks[inDirection];
			if(inLink != null){
				for(QLaneI lane : inLink.getOfferingQLanes()){
					while(! lane.isNotOfferingVehicle()){
						QVehicle veh = lane.getFirstVehicle();
						Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
						int outDirection = carOutTransformations.get(nextLinkId);
						if(carTimeouts[inDirection][outDirection] <= now){
							//Ignoring left turns when using right priority.
							if (! this.qNode.moveVehicleOverNode(veh, inLink, lane, now )) {
								break;
							}
							timeoutModifier.updateTimeouts(bicycleTimeouts, carTimeouts, 
									inDirection, outDirection, null, nowish);
						} else {
							break;
						}
					}
				}
			}
		}
}
