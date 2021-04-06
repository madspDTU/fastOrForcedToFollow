package org.matsim.run;

import java.util.Arrays;
import java.util.LinkedList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;

public class NetworkCleaningUtils {


	private static int removeRedundantNodeUnidirectionalUnimodal(Network network) {
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

	private static int removeRedundantNodeBidirectionalUnimodal(Network network) {
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


	
}
