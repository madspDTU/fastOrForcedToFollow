package org.matsim.run;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;

import fastOrForcedToFollow.configgroups.FFFConfigGroup;

public class NetworkCleaningUtils {


	public static int removeRedundantNodeUnidirectionalUnimodalOLD(Network network) {
		//Unimodal nodes...
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();

		//Roads that are digitalised per direction
		for(Node node : network.getNodes().values()){
			if(node.getInLinks().size() == 1 && node.getOutLinks().size() ==1){
				Link inLink = null;
				Link outLink = null;
				for(Link link : node.getInLinks().values()){
					inLink = link;
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}
				for(Link link : node.getOutLinks().values()){
					outLink = link;
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}

				//				int inBranches = inLink.getFromNode().getInLinks().size() +
				//						inLink.getFromNode().getOutLinks().size();
				//				int outBranches = outLink.getToNode().getInLinks().size() +
				//						outLink.getToNode().getOutLinks().size();

				if(inLink.getFromNode().getId() != outLink.getToNode().getId() && 
						inLink.getCapacity() == outLink.getCapacity() &&
						inLink.getFreespeed() == outLink.getFreespeed() &&
						inLink.getNumberOfLanes() == outLink.getNumberOfLanes() &&
						inLink.getAllowedModes() == outLink.getAllowedModes() &&
						inLink.getAttributes().getAttribute("type").equals(outLink.getAttributes().getAttribute("type"))){
					nodesToBeRemoved.addLast(node);		
				}
			}
		}
		int counter = 0;
		for(Node node : nodesToBeRemoved){

			Link inLink = null;
			Link outLink = null;
			for(Link link : node.getInLinks().values()){
				inLink = link;
			}
			for(Link link : node.getOutLinks().values()){
				outLink = link;
			}
			if(inLink.getFromNode().getId() == outLink.getToNode().getId()) {
				//Removing node would create a one-link loop
				continue;
			}


			Node toNode = outLink.getToNode();
			double length = inLink.getLength() + outLink.getLength();

			toNode.removeInLink(outLink.getId());
			toNode.addInLink(inLink);
			inLink.setToNode(toNode);
			inLink.setLength(length);

			node.removeInLink(inLink.getId());
			Gbl.assertIf(node.getInLinks().size() == 0);
			Gbl.assertIf(node.getOutLinks().size() == 1);
			network.removeNode(node.getId());

			counter++;
		}
		System.out.println(counter + " redundant unidirectional unimodal nodes removed from the network");
		return counter;
	}

	public static int removeRedundantNodeBidirectionalUnimodalOLD(Network network) {
		//Unimodal nodes...
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();

		//Roads that are digitalised as one feature despite having two directions.
		for(Node node : network.getNodes().values()){

			boolean giveUp = false;
			boolean atLeastOneLinkWhereCarsAreNotAllowed = false;
			boolean atLeastOneLinkWhereBicyclesAreNotAllowed = false;

			if(node.getInLinks().size() == 2 && node.getOutLinks().size() == 2){
				Link[] inLinks = new Link[2];
				Link[] outLinks = new Link[2];
				int i = 0;
				for(Link link : node.getInLinks().values()){
					Gbl.assertNotNull(link);
					inLinks[i] = link;
					if(!link.getAllowedModes().contains(TransportMode.bike)) {
						atLeastOneLinkWhereBicyclesAreNotAllowed = true;
					} else if (!link.getAllowedModes().contains(TransportMode.car)) {
						atLeastOneLinkWhereCarsAreNotAllowed = true;
					}
					i++;
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}
				for(Link link : node.getOutLinks().values()){
					if(link.getToNode().getId() == inLinks[0].getFromNode().getId()) {
						if(outLinks[0] != null) {
							giveUp = true;
							break;
						}
						outLinks[0] = link;
					} else if (link.getToNode().getId() == inLinks[1].getFromNode().getId()) {
						if(outLinks[1] != null) {
							giveUp = true;
							break;
						}
						outLinks[1] = link;
					} else {
						giveUp = true;
						break;
					}
					if(!link.getAllowedModes().contains(TransportMode.bike)) {
						atLeastOneLinkWhereBicyclesAreNotAllowed = true;
					} else if (!link.getAllowedModes().contains(TransportMode.car)) {
						atLeastOneLinkWhereCarsAreNotAllowed = true;
					}
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}

				if(giveUp || !(atLeastOneLinkWhereCarsAreNotAllowed ^ atLeastOneLinkWhereBicyclesAreNotAllowed)) {
					continue;
				}

				Gbl.assertNotNull(outLinks[0]);
				Gbl.assertNotNull(outLinks[1]);					

				// Otherwise, we have found pairs!

				i = 0;
				while(i < inLinks.length) {
					Gbl.assertNotNull(inLinks[i]);
					Gbl.assertNotNull(outLinks[i]);					
					if(     inLinks[i].getCapacity() != outLinks[i].getCapacity() ||
							inLinks[i].getFreespeed() != outLinks[i].getFreespeed() ||
							inLinks[i].getNumberOfLanes() != outLinks[i].getNumberOfLanes() ||
							inLinks[i].getAllowedModes() != outLinks[i].getAllowedModes() || 
							!((String) inLinks[i].getAttributes().getAttribute("type")).equals(
									(String) outLinks[i].getAttributes().getAttribute("type"))){
						giveUp = true;
						break;
					}
					i++;
				}
				if(!giveUp){
					nodesToBeRemoved.addLast(node);		
				}
			}
		}
		int counter = 0;
		for(Node node : nodesToBeRemoved){
			Link[] inLinks = new Link[2];
			Link[] outLinks = new Link[2];
			Gbl.assertIf(node.getInLinks().size() == 2);
			int i = 0;
			for(Link link : node.getInLinks().values()){
				Gbl.assertNotNull(link);
				inLinks[i] = link;
				i++;
			}
			Gbl.assertNotNull(inLinks[0]);
			Gbl.assertNotNull(inLinks[1]);

			Gbl.assertNotNull(node.getOutLinks().size() == 2);
			for(Link link : node.getOutLinks().values()){
				if(link.getToNode().getId() == inLinks[0].getFromNode().getId()) {
					outLinks[0] = link;
				}
				if (link.getToNode().getId() == inLinks[1].getFromNode().getId()) {
					outLinks[1] = link;
				} 
			}
			if(outLinks[0] == null || outLinks[1] == null) {
				continue;
			}



			//Parallel links..
			if(inLinks[0].getFromNode().getId() == inLinks[1].getFromNode().getId()) {
				int removeInt; 
				if(inLinks[0].getAllowedModes().contains(TransportMode.bike)) {
					if(inLinks[0].getNumberOfLanes() != inLinks[1].getNumberOfLanes()) {
						removeInt = inLinks[0].getNumberOfLanes() > inLinks[1].getNumberOfLanes() ? 1 : 0;  
					} else {
						removeInt = inLinks[0].getLength() < inLinks[1].getLength() ? 1 : 0;	
					}
				} else if(inLinks[0].getAllowedModes().contains(TransportMode.car) ) {
					if(inLinks[0].getCapacity() != inLinks[1].getCapacity()) {
						removeInt = inLinks[0].getCapacity() > inLinks[1].getCapacity() ? 1 : 0;
					} else if(inLinks[0].getFreespeed() != inLinks[1].getFreespeed()) {
						removeInt = inLinks[0].getFreespeed() > inLinks[1].getFreespeed() ? 1 : 0;
					} else {
						removeInt = inLinks[0].getLength() < inLinks[1].getLength() ? 1 : 0;
					}
				} else {
					System.err.println("Shouldn't happen... Link has neither bike nor car allowed");
					removeInt = 0;
					System.exit(-1);
				}
				for(Link link : Arrays.asList(inLinks[removeInt], outLinks[removeInt])) {
					link.getFromNode().removeOutLink(link.getId());
					link.getToNode().removeInLink(link.getId());
					network.removeLink(link.getId());
				}
				continue;
			}

			Gbl.assertIf(outLinks[0] != outLinks[1]);


			Node toNode = outLinks[1].getToNode();

			Gbl.assertIf(toNode != node);
			toNode.removeInLink(outLinks[1].getId());
			toNode.addInLink(inLinks[0]);
			inLinks[0].setToNode(toNode);
			inLinks[0].setLength(inLinks[0].getLength() + outLinks[1].getLength());
			node.removeInLink(inLinks[0].getId());

			toNode.removeOutLink(inLinks[1].getId());
			toNode.addOutLink(outLinks[0]);
			outLinks[0].setFromNode(toNode);
			outLinks[0].setLength(outLinks[0].getLength() + inLinks[1].getLength());
			node.removeOutLink(outLinks[0].getId());

			Gbl.assertIf(node.getInLinks().size() == 1);
			Gbl.assertIf(node.getOutLinks().size() == 1);
			network.removeNode(node.getId());

			counter++;
		}
		System.out.println(counter + " redundant bidirectional nodes removed from the network");
		return counter;
	}

	


	public static int removeRedundantNodeBidirectionalBimodal(Network network) {
		//Bimodal nodes...
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();

		//Roads that are digitalised as one feature despite having two directions.
		for(Node node : network.getNodes().values()){

			boolean giveUp = false;

			if(node.getInLinks().size() == 4 && node.getOutLinks().size() == 4){
				Link[] carInLinks = new Link[2];
				Link[] bicycleInLinks = new Link[2];
				Link[] carOutLinks = new Link[2];
				Link[] bicycleOutLinks = new Link[2];
				int i = 0;
				for(Link link : node.getInLinks().values()){				
					if(link.getAllowedModes().contains(TransportMode.car)) {
						if(i == 2) {
							i++;
							break;
						}
						carInLinks[i] = link;
						i++;
					}
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}
				if(i != 2) {
					continue;
				}
				for(Link link : node.getInLinks().values()){
					if(link.getAllowedModes().contains(TransportMode.bike)) {
						if(link.getFromNode().getId() == carInLinks[0].getFromNode().getId()) {
							bicycleInLinks[0] = link;
						} else if(link.getFromNode().getId() == carInLinks[1].getFromNode().getId()) {
							bicycleInLinks[1] = link;
						} else {
							giveUp = true;
							break;
						}
					}
				}
				if(giveUp || bicycleInLinks[0] == null || bicycleInLinks[1] == null ) {
					continue;
				}

				for(Link link : node.getOutLinks().values()){
					if(link.getAllowedModes().contains(TransportMode.car)) {
						if(link.getToNode().getId() == carInLinks[0].getFromNode().getId()) {
							carOutLinks[0] = link;
						} else if(link.getToNode().getId() == carInLinks[1].getFromNode().getId()) {
							carOutLinks[1] = link;
						} else {
							giveUp = true;
							break;
						}
					}
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}
				if(giveUp || carOutLinks[0] == null || carOutLinks[1] == null ) {
					continue;
				}

				for(Link link : node.getOutLinks().values()){
					if(link.getAllowedModes().contains(TransportMode.bike)) {
						if(link.getToNode().getId() == carInLinks[0].getFromNode().getId()) {
							bicycleOutLinks[0] = link;
						} else if(link.getToNode().getId() == carInLinks[1].getFromNode().getId()) {
							bicycleOutLinks[1] = link;
						} else {
							giveUp = true;
							break;
						}
					}
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}
				if(giveUp || bicycleOutLinks[0] == null || bicycleOutLinks[1] == null ) {
					continue;
				}

				// Otherwise, we have found pairs!

				i = 0;
				while(i < carInLinks.length) {
					if(     carInLinks[i].getCapacity() != carOutLinks[i].getCapacity() ||
							carInLinks[i].getFreespeed() != carOutLinks[i].getFreespeed() ||
							carInLinks[i].getNumberOfLanes() != carOutLinks[i].getNumberOfLanes() ||
							carInLinks[i].getAllowedModes() != carOutLinks[i].getAllowedModes() || 
							!((String) carInLinks[i].getAttributes().getAttribute("type")).equals(
									(String) carOutLinks[i].getAttributes().getAttribute("type"))   ||
							bicycleInLinks[i].getNumberOfLanes() != bicycleOutLinks[i].getNumberOfLanes()){
						giveUp = true;
						break;
					}
					i++;
				}
				if(!giveUp){
					nodesToBeRemoved.addLast(node);		
				}
			}
		}
		int counter = 0;
		for(Node node : nodesToBeRemoved){
			Link[] carInLinks = new Link[2];
			Link[] bicycleInLinks = new Link[2];
			Link[] carOutLinks = new Link[2];
			Link[] bicycleOutLinks = new Link[2];
			Gbl.assertIf(node.getInLinks().size() == 4 && node.getOutLinks().size() == 4);

			int i = 0;
			for(Link link : node.getInLinks().values()){				
				if(link.getAllowedModes().contains(TransportMode.car)) {
					carInLinks[i] = link;
					i++;
				}
				Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
			}
			Gbl.assertIf(i == 2);

			for(Link link : node.getInLinks().values()){
				if(link.getAllowedModes().contains(TransportMode.bike)) {
					if(link.getFromNode().getId() == carInLinks[0].getFromNode().getId()) {
						bicycleInLinks[0] = link;
					}
					if(link.getFromNode().getId() == carInLinks[1].getFromNode().getId()) {
						bicycleInLinks[1] = link;
					} 
				}
			}
			Gbl.assertNotNull(bicycleInLinks[0]);
			Gbl.assertNotNull(bicycleInLinks[1]);

			for(Link link : node.getOutLinks().values()){
				if(link.getAllowedModes().contains(TransportMode.car)) {
					if(link.getToNode().getId() == carInLinks[0].getFromNode().getId()) {
						carOutLinks[0] = link;
					}
					if(link.getToNode().getId() == carInLinks[1].getFromNode().getId()) {
						carOutLinks[1] = link;
					}
				}
				Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
			}
			Gbl.assertNotNull(carOutLinks[0]);
			Gbl.assertNotNull(carOutLinks[1]);

			for(Link link : node.getOutLinks().values()){
				if(link.getAllowedModes().contains(TransportMode.bike)) {
					if(link.getToNode().getId() == carInLinks[0].getFromNode().getId()) {
						bicycleOutLinks[0] = link;
					} 
					if(link.getToNode().getId() == carInLinks[1].getFromNode().getId()) {
						bicycleOutLinks[1] = link;
					} 
				}
				Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
			}
			Gbl.assertNotNull(bicycleOutLinks[0]);
			Gbl.assertNotNull(bicycleOutLinks[1]);


			//Parallel links..
			if(carInLinks[0].getFromNode().getId() == carInLinks[1].getFromNode().getId()) {
				int removeInt; 
				if(carInLinks[0].getCapacity() != carInLinks[1].getCapacity()) {
					removeInt = carInLinks[0].getCapacity() > carInLinks[1].getCapacity() ? 1 : 0;
				} else if(carInLinks[0].getFreespeed() != carInLinks[1].getFreespeed()) {
					removeInt = carInLinks[0].getFreespeed() > carInLinks[1].getFreespeed() ? 1 : 0;
				} else {
					removeInt = carInLinks[0].getLength() < carInLinks[1].getLength() ? 1 : 0;
				}
				for(Link link : Arrays.asList(carInLinks[removeInt], bicycleInLinks[removeInt], carOutLinks[removeInt], bicycleOutLinks[removeInt])) {
					link.getFromNode().removeOutLink(link.getId());
					link.getToNode().removeInLink(link.getId());
					network.removeLink(link.getId());
				}
				counter++;
				continue;
			}


			Node toNode = carOutLinks[1].getToNode();

			Gbl.assertIf(toNode != node);

			toNode.removeInLink(carOutLinks[1].getId());
			toNode.addInLink(carInLinks[0]);
			carInLinks[0].setToNode(toNode);
			carInLinks[0].setLength(carInLinks[0].getLength() + carOutLinks[1].getLength());
			node.removeInLink(carInLinks[0].getId());

			toNode.removeInLink(bicycleOutLinks[1].getId());
			toNode.addInLink(bicycleInLinks[0]);
			bicycleInLinks[0].setToNode(toNode);
			bicycleInLinks[0].setLength(bicycleInLinks[0].getLength() + bicycleOutLinks[1].getLength());
			node.removeInLink(bicycleInLinks[0].getId());


			toNode.removeOutLink(carInLinks[1].getId());
			toNode.addOutLink(carOutLinks[0]);
			carOutLinks[0].setFromNode(toNode);
			carOutLinks[0].setLength(carOutLinks[0].getLength() + carInLinks[1].getLength());
			node.removeOutLink(carOutLinks[0].getId());

			toNode.removeOutLink(bicycleInLinks[1].getId());
			toNode.addOutLink(bicycleOutLinks[0]);
			bicycleOutLinks[0].setFromNode(toNode);
			bicycleOutLinks[0].setLength(bicycleOutLinks[0].getLength() + bicycleInLinks[1].getLength());
			node.removeOutLink(bicycleOutLinks[0].getId());

			Gbl.assertIf(node.getInLinks().size() == 2);
			Gbl.assertIf(node.getOutLinks().size() == 2);
			network.removeNode(node.getId());

			counter++;
		}
		System.out.println(counter + " redundant bidirectional nodes removed from the network");
		return counter;
	}



	public static int removeRedundantNodeUnidirectionalBimodal(Network network) {
		//Biimodal nodes...
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();

		//Roads that are digitalised per direction
		for(Node node : network.getNodes().values()){
			if(node.getInLinks().size() == 2 && node.getOutLinks().size() == 2){
				Link carInLink = null;
				Link bicycleInLink = null;
				Link carOutLink = null;
				Link bicycleOutLink = null;
				for(Link link : node.getInLinks().values()){
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
					if(link.getAllowedModes().contains(TransportMode.car)) {
						carInLink = link;
					} else if(link.getAllowedModes().contains(TransportMode.bike)) {
						bicycleInLink = link;
					}
				}
				if(carInLink == null || bicycleInLink == null) {
					continue;
				}
				if(carInLink.getFromNode().getId() != bicycleInLink.getFromNode().getId()) {
					continue;
				}

				for(Link link : node.getOutLinks().values()){
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
					if(link.getAllowedModes().contains(TransportMode.car)) {
						carOutLink = link;
					} else if(link.getAllowedModes().contains(TransportMode.bike)) {
						bicycleOutLink = link;
					}
				}

				if(carOutLink == null || bicycleOutLink == null) {
					continue;
				}  else if(carOutLink.getToNode().getId() != bicycleOutLink.getToNode().getId()) {
					continue;
				}

				if(carInLink.getFromNode().getId() != carOutLink.getToNode().getId() &&  //dead end
						carInLink.getCapacity() == carOutLink.getCapacity() &&
						carInLink.getFreespeed() == carOutLink.getFreespeed() &&
						carInLink.getNumberOfLanes() == carOutLink.getNumberOfLanes() &&
						carInLink.getAllowedModes() == carOutLink.getAllowedModes() &&
						carInLink.getAttributes().getAttribute("type").equals(carOutLink.getAttributes().getAttribute("type")) &&
						bicycleInLink.getNumberOfLanes() == bicycleOutLink.getNumberOfLanes()){
					nodesToBeRemoved.addLast(node);		
				}
			}
		}
		int counter = 0;
		for(Node node : nodesToBeRemoved){

			Gbl.assertIf(node.getInLinks().size() == 2 && node.getOutLinks().size() == 2);


			Link carInLink = null;
			Link bicycleInLink = null;
			Link carOutLink = null;
			Link bicycleOutLink = null;
			for(Link link : node.getInLinks().values()){
				Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				if(link.getAllowedModes().contains(TransportMode.car)) {
					carInLink = link;
				} else if(link.getAllowedModes().contains(TransportMode.bike)) {
					bicycleInLink = link;
				}
			}
			Gbl.assertNotNull(carInLink);
			Gbl.assertNotNull(bicycleInLink);
			Gbl.assertIf(bicycleInLink.getFromNode().getId() == carInLink.getFromNode().getId());

			for(Link link : node.getOutLinks().values()){
				Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				if(link.getAllowedModes().contains(TransportMode.car)) {
					carOutLink = link;
				} else if(link.getAllowedModes().contains(TransportMode.bike)) {
					bicycleOutLink = link;
				}
			}
			Gbl.assertNotNull(carOutLink);
			Gbl.assertNotNull(bicycleOutLink);
			Gbl.assertIf(bicycleOutLink.getToNode().getId() == carOutLink.getToNode().getId());
			if(carInLink.getFromNode().getId() == carOutLink.getToNode().getId()) {
				//Removing node would create a one-link loop
				continue;
			}


			Node toNode = carOutLink.getToNode();

			Gbl.assertIf(toNode != node);

			toNode.removeInLink(carOutLink.getId());
			toNode.addInLink(carInLink);
			carInLink.setToNode(toNode);
			carInLink.setLength(carInLink.getLength() + carOutLink.getLength());
			node.removeInLink(carInLink.getId());

			toNode.removeInLink(bicycleOutLink.getId());
			toNode.addInLink(bicycleInLink);
			bicycleInLink.setToNode(toNode);
			bicycleInLink.setLength(bicycleInLink.getLength() + bicycleOutLink.getLength());
			node.removeInLink(bicycleInLink.getId());

			Gbl.assertIf(node.getInLinks().size() == 0);
			Gbl.assertIf(node.getOutLinks().size() == 2);
			network.removeNode(node.getId());


			counter++;
		}
		System.out.println(counter + " redundant unidirectional bimodal nodes removed from the network");
		return counter;
	}




	public static int removeRedundantNodeUnidirectionalUnimodal(Network network) {
		//Unimodal nodes...
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();

		//Roads that are digitalised per direction
		for(Node node : network.getNodes().values()){
			if(node.getInLinks().size() == 1 && node.getOutLinks().size() ==1){
				Link inLink = null;
				Link outLink = null;
				for(Link link : node.getInLinks().values()){
					inLink = link;
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}
				for(Link link : node.getOutLinks().values()){
					outLink = link;
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}

				//				int inBranches = inLink.getFromNode().getInLinks().size() +
				//						inLink.getFromNode().getOutLinks().size();
				//				int outBranches = outLink.getToNode().getInLinks().size() +
				//						outLink.getToNode().getOutLinks().size();

				if(inLink.getFromNode().getId() != outLink.getToNode().getId() && 
						inLink.getCapacity() == outLink.getCapacity() &&
						inLink.getFreespeed() == outLink.getFreespeed() &&
						inLink.getNumberOfLanes() == outLink.getNumberOfLanes() &&
						inLink.getAllowedModes() == outLink.getAllowedModes() &&
						inLink.getAttributes().getAttribute("type").equals(outLink.getAttributes().getAttribute("type"))){
					nodesToBeRemoved.addLast(node);		
				}
			}
		}
		int counter = 0;
		for(Node node : nodesToBeRemoved){

			Link inLink = null;
			Link outLink = null;
			for(Link link : node.getInLinks().values()){
				inLink = link;
			}
			for(Link link : node.getOutLinks().values()){
				outLink = link;
			}
			if(inLink.getFromNode().getId() == outLink.getToNode().getId()) {
				//Removing node would create a one-link loop
				continue;
			}


			Node toNode = outLink.getToNode();
			double length = inLink.getLength() + outLink.getLength();

			toNode.removeInLink(outLink.getId());
			toNode.addInLink(inLink);
			inLink.setToNode(toNode);
			inLink.setLength(length);

			node.removeInLink(inLink.getId());
			Gbl.assertIf(node.getInLinks().size() == 0);
			Gbl.assertIf(node.getOutLinks().size() == 1);
			network.removeNode(node.getId());

			counter++;
		}
		System.out.println(counter + " redundant unidirectional unimodal nodes removed from the network");
		return counter;
	}

	public static int removeRedundantNodeBidirectionalUnimodal(Network network) {
		//Unimodal nodes...
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();

		//Roads that are digitalised as one feature despite having two directions.
		for(Node node : network.getNodes().values()){

			boolean giveUp = false;
			boolean atLeastOneLinkWhereCarsAreNotAllowed = false;
			boolean atLeastOneLinkWhereBicyclesAreNotAllowed = false;

			if(node.getInLinks().size() == 2 && node.getOutLinks().size() == 2){
				Link[] inLinks = new Link[2];
				Link[] outLinks = new Link[2];
				int i = 0;
				for(Link link : node.getInLinks().values()){
					Gbl.assertNotNull(link);
					inLinks[i] = link;
					if(!link.getAllowedModes().contains(TransportMode.bike)) {
						atLeastOneLinkWhereBicyclesAreNotAllowed = true;
					} else if (!link.getAllowedModes().contains(TransportMode.car)) {
						atLeastOneLinkWhereCarsAreNotAllowed = true;
					}
					i++;
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}
				for(Link link : node.getOutLinks().values()){
					if(link.getToNode().getId() == inLinks[0].getFromNode().getId()) {
						if(outLinks[0] != null) {
							giveUp = true;
							break;
						}
						outLinks[0] = link;
					} else if (link.getToNode().getId() == inLinks[1].getFromNode().getId()) {
						if(outLinks[1] != null) {
							giveUp = true;
							break;
						}
						outLinks[1] = link;
					} else {
						giveUp = true;
						break;
					}
					if(!link.getAllowedModes().contains(TransportMode.bike)) {
						atLeastOneLinkWhereBicyclesAreNotAllowed = true;
					} else if (!link.getAllowedModes().contains(TransportMode.car)) {
						atLeastOneLinkWhereCarsAreNotAllowed = true;
					}
					Gbl.assertIf(link.getFromNode().getId() != link.getToNode().getId());
				}

				if(giveUp || !(atLeastOneLinkWhereCarsAreNotAllowed ^ atLeastOneLinkWhereBicyclesAreNotAllowed)) {
					continue;
				}

				Gbl.assertNotNull(outLinks[0]);
				Gbl.assertNotNull(outLinks[1]);					

				// Otherwise, we have found pairs!

				i = 0;
				while(i < inLinks.length) {
					Gbl.assertNotNull(inLinks[i]);
					Gbl.assertNotNull(outLinks[i]);					
					if(     inLinks[i].getCapacity() != outLinks[i].getCapacity() ||
							inLinks[i].getFreespeed() != outLinks[i].getFreespeed() ||
							inLinks[i].getNumberOfLanes() != outLinks[i].getNumberOfLanes() ||
							inLinks[i].getAllowedModes() != outLinks[i].getAllowedModes() || 
							!((String) inLinks[i].getAttributes().getAttribute("type")).equals(
									(String) outLinks[i].getAttributes().getAttribute("type"))){
						giveUp = true;
						break;
					}
					i++;
				}
				if(!giveUp){
					nodesToBeRemoved.addLast(node);		
				}
			}
		}
		int counter = 0;
		for(Node node : nodesToBeRemoved){
			Link[] inLinks = new Link[2];
			Link[] outLinks = new Link[2];
			Gbl.assertIf(node.getInLinks().size() == 2);
			int i = 0;
			for(Link link : node.getInLinks().values()){
				Gbl.assertNotNull(link);
				inLinks[i] = link;
				i++;
			}
			Gbl.assertNotNull(inLinks[0]);
			Gbl.assertNotNull(inLinks[1]);

			Gbl.assertNotNull(node.getOutLinks().size() == 2);
			for(Link link : node.getOutLinks().values()){
				if(link.getToNode().getId() == inLinks[0].getFromNode().getId()) {
					outLinks[0] = link;
				}
				if (link.getToNode().getId() == inLinks[1].getFromNode().getId()) {
					outLinks[1] = link;
				} 
			}
			if(outLinks[0] == null || outLinks[1] == null) {
				continue;
			}



			//Parallel links..
			if(inLinks[0].getFromNode().getId() == inLinks[1].getFromNode().getId()) {
				int removeInt; 
				if(inLinks[0].getAllowedModes().contains(TransportMode.bike)) {
					if(inLinks[0].getNumberOfLanes() != inLinks[1].getNumberOfLanes()) {
						removeInt = inLinks[0].getNumberOfLanes() > inLinks[1].getNumberOfLanes() ? 1 : 0;  
					} else {
						removeInt = inLinks[0].getLength() < inLinks[1].getLength() ? 1 : 0;	
					}
				} else if(inLinks[0].getAllowedModes().contains(TransportMode.car) ) {
					if(inLinks[0].getCapacity() != inLinks[1].getCapacity()) {
						removeInt = inLinks[0].getCapacity() > inLinks[1].getCapacity() ? 1 : 0;
					} else if(inLinks[0].getFreespeed() != inLinks[1].getFreespeed()) {
						removeInt = inLinks[0].getFreespeed() > inLinks[1].getFreespeed() ? 1 : 0;
					} else {
						removeInt = inLinks[0].getLength() < inLinks[1].getLength() ? 1 : 0;
					}
				} else {
					System.err.println("Shouldn't happen... Link has neither bike nor car allowed");
					removeInt = 0;
					System.exit(-1);
				}
				for(Link link : Arrays.asList(inLinks[removeInt], outLinks[removeInt])) {
					link.getFromNode().removeOutLink(link.getId());
					link.getToNode().removeInLink(link.getId());
					network.removeLink(link.getId());
				}
				continue;
			}

			Gbl.assertIf(outLinks[0] != outLinks[1]);


			Node toNode = outLinks[1].getToNode();

			Gbl.assertIf(toNode != node);
			toNode.removeInLink(outLinks[1].getId());
			toNode.addInLink(inLinks[0]);
			inLinks[0].setToNode(toNode);
			inLinks[0].setLength(inLinks[0].getLength() + outLinks[1].getLength());
			node.removeInLink(inLinks[0].getId());

			toNode.removeOutLink(inLinks[1].getId());
			toNode.addOutLink(outLinks[0]);
			outLinks[0].setFromNode(toNode);
			outLinks[0].setLength(outLinks[0].getLength() + inLinks[1].getLength());
			node.removeOutLink(outLinks[0].getId());

			Gbl.assertIf(node.getInLinks().size() == 1);
			Gbl.assertIf(node.getOutLinks().size() == 1);
			network.removeNode(node.getId());

			counter++;
		}
		System.out.println(counter + " redundant bidirectional nodes removed from the network");
		return counter;
	}



	
	public static void cleanBicycleNetwork(Network network, Config config){
		System.out.println("Stage -1: #Links: " + network.getLinks().size() + "\t#Nodes: " + network.getNodes().size());
		removeOneLinkLoops(network);
		System.out.println("Stage 0: #Links: " + network.getLinks().size() + "\t#Nodes: " + network.getNodes().size());
		removeRedundantNodes(network); 
		System.out.println("Stage 1: #Links: " + network.getLinks().size() + "\t#Nodes: " + network.getNodes().size());
		removeDuplicateLinks(network);
		System.out.println("Stage 2: #Links: " + network.getLinks().size() + "\t#Nodes: " + network.getNodes().size());
		setFreespeed(network, config);
	}






	private static void removeOneLinkLoops(Network network) {
		LinkedList<Link> loopLinks = new LinkedList<Link>();
		for(Link link : network.getLinks().values()) {
			if(link.getFromNode().getId() == link.getToNode().getId()) {
				loopLinks.add(link);
			}
		}
		for(Link link : loopLinks) {
			link.getFromNode().removeOutLink(link.getId());
			link.getToNode().removeInLink(link.getId());
			network.removeLink(link.getId());
		}
	}




	private static void setFreespeed(Network network, Config config) {
		FFFConfigGroup fffConfigGroup = ConfigUtils.addOrGetModule(config, FFFConfigGroup.class);
		System.out.print("Setting free speed of bicycle links to " + 
				fffConfigGroup.getMaximumAllowedDesiredSpeed() + "... ");
		for(Link link : network.getLinks().values()){
			if(link.getAllowedModes().contains(TransportMode.bike)){
				Gbl.assertIf(link.getAllowedModes().size() == 1); // Otherwise this is wrong
				link.setFreespeed(fffConfigGroup.getMaximumAllowedDesiredSpeed());
			}
		}	
		System.out.println(" Done!");
	}




	private static void removeRedundantNodes(Network network){
		System.out.println("Starting to remove redundant nodes...");

		int removedStage1 = NetworkCleaningUtils.removeRedundantNodeUnidirectionalUnimodal(network);
		int removedStage2 = NetworkCleaningUtils.removeRedundantNodeBidirectionalUnimodal(network);
		int removedStage3 = NetworkCleaningUtils.removeRedundantNodeUnidirectionalBimodal(network);
		int removedStage4 = NetworkCleaningUtils.removeRedundantNodeBidirectionalBimodal(network);

	}

	


	private static void removeDuplicateLinks(Network network){

		for(String mode : Arrays.asList(TransportMode.car, TransportMode.bike)){
			System.out.print("Starting to remove duplicate links network...");
			LinkedList<Link> linksToBeRemoved = new LinkedList<Link>();
			for(Node node : network.getNodes().values()){
				HashMap<Node, Link> outNodes = new HashMap<Node, Link>();
				for(Link link : node.getOutLinks().values()){
					if(link.getAllowedModes().contains(mode)){
						if(!outNodes.containsKey(link.getToNode())){
							outNodes.put(link.getToNode(),link);
						} else {
							if(link.getNumberOfLanes() > outNodes.get(link.getToNode()).getNumberOfLanes()){
								linksToBeRemoved.add(outNodes.get(link.getToNode()));
								outNodes.put(link.getToNode(), link);
							} else {
								linksToBeRemoved.add(link);
							}
						}
					}
				}
			}
			int counter = 0;
			for(Link link : linksToBeRemoved){
				network.removeLink(link.getId());
				counter++;
			}
			System.out.println(counter + " duplicate " + mode + " links removed from the network");
		}
	}

	
	
	

}
