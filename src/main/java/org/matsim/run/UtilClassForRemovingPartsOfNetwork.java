package org.matsim.run;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;


public class UtilClassForRemovingPartsOfNetwork {

	
	private static Coord[] getVertices() {
		LinkedList<Coord> coords = new LinkedList<Coord>();
		coords.addLast(new Coord(673977.7833, 6172099.281)); // 
		coords.addLast(new Coord(679281.4926, 6189542,191)); // 
		coords.addLast(new Coord(675045.9795, 6206992.6616)); // 
		coords.addLast(new Coord(703658.9441, 6228283.6447)); // 
		coords.addLast(new Coord(728969.9982, 6216640.5598)); // 
		coords.addLast(new Coord(738845.9346, 6137938.4159)); // 
		coords.addLast(new Coord(699130.2605, 6135696.2925)); // 
		coords.addLast(new Coord(686615.3337, 6142709.614)); // 

		Coord[] output = new Coord[coords.size()];
		for (int i = 0; i < output.length; i++) {
			output[i] = coords.pollFirst();
		}
		return output;
	}

	
	
	private static void removeSouthWesternPart(Network network) {
		// Based on
		// http://www.ytechie.com/2009/08/determine-if-a-point-is-contained-within-a-polygon/
	
		Coord[] vertices = getVertices();
	
		int linksBefore = network.getLinks().size();
		LinkedList<Node> nodesToBeRemoved = new LinkedList<Node>();
		for (Node node : network.getNodes().values()) {
			if (!isCoordInsidePolygon(node.getCoord(), vertices)) {
			}
		}
		for (Node node : nodesToBeRemoved) {
			network.removeNode(node.getId());
		}
		// System.out.println(isCoordInsidePolygon(new Coord(671092.33,
		// 6177550.04), vertices));
		System.out.println(nodesToBeRemoved.size() + " nodes and " + (linksBefore - network.getLinks().size())
				+ " links removed from South-Western part...");
	}

	
	private static boolean isCoordInsidePolygon(Coord c, Coord[] v) {
		int j = v.length - 1;
		boolean oddNodes = false;
		for (int i = 0; i < v.length; i++) {
			if ((v[i].getY() < c.getY() && v[j].getY() >= c.getY()) || v[j].getY() < c.getY()
					&& v[i].getY() >= c.getY()) {
				if (v[i].getX() + (c.getY() - v[i].getY()) / 
						(v[j].getY() - v[i].getY()) * (v[j].getX() - v[i].getX()) < c.getX()) {
					oddNodes = !oddNodes;
				}
			}
			j = i;
		}
		return oddNodes;
	}
	
}
