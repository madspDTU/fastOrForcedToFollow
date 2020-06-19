/* *********************************************************************** *
 * project: org.matsim.*
 * RunNetworkSimplifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.algorithms.intersectionSimplifier.MyIntersectionSimplifier;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

/**
 * Example to illustrate how the density-based algorithm is used to simplify a
 * network's intersections.
 * 
 * @author jwjoubert
 */
public class RunIntersectionSimplifierCopenhagen {
	final private static Logger LOG = Logger.getLogger(RunIntersectionSimplifierCopenhagen.class);

	final private static String dir = "/zhome/81/e/64390/MATSim/DTA2020/input/";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] args2 = new String[]{dir + "MATSimCopenhagenNetwork_WithBicycleInfrastructure_NOTSIMPLIFIED.xml.gz",
				dir + "MATSimCopenhagenNetwork_WithBicycleInfrastructure_SIMPLIFIED_", "20"};
		for(int i = 5; i <= 25; i += 5) {
			args2[2] = String.valueOf(i);
			run(args2);
		}
	}

	public static void run(String[] args) {
		String input = args[0];
		String output = args[1];
		int radius = Integer.parseInt(args[2]);
		String fullOutput = output + radius + ".xml.gz";

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(input);

	//	LOG.info("\n\n\n\nNetwork statistics very beginning...");	
	//	MyIntersectionSimplifier.reportNetworkStatistics(network);

		
		for(Node node : network.getNodes().values()) {
			boolean highwayPresent = false;
			for(Link link : node.getInLinks().values()) {
				if(link.getAttributes().getAttribute("type").equals("motorway")) {
					highwayPresent = true;
				}
			}
			for(Link link : node.getInLinks().values()) {
				if(link.getAttributes().getAttribute("type").equals("motorway") ) {
					highwayPresent = true;
				}
			}
			if(node.getCoord().getX() <  745000 &&  highwayPresent && (node.getInLinks().size() > 2 || node.getOutLinks().size() > 2 || 
					node.getInLinks().size()  + node.getOutLinks().size()  == 4) ) {
				System.out.println(node.getInLinks().size() + "->" + node.getOutLinks().size() );
				System.out.println("   " + node.getCoord().getX() + "," + node.getCoord().getY());
			}
		}
		System.exit(-1);
		
		
		

		RunMatsim.cleanBicycleNetwork(network, ConfigUtils.createConfig());
	//	LOG.info("\n\n\n\nNetwork statistics after links cleaning...");	
	//  MyIntersectionSimplifier.reportNetworkStatistics(network);


		MyIntersectionSimplifier ns = new MyIntersectionSimplifier(radius, 2, true);
		Network newNetwork = ns.simplify(network);

	//	LOG.info("\n\n\nNetwork calc topo type...");
		NetworkCalcTopoType nct = new NetworkCalcTopoType();
		nct.run(newNetwork);

		//	LOG.info("\n\n\nSimplifying the network...");
		//	new NetworkSimplifier().run(newNetwork);

	//	LOG.info("\n\n\n\nCleaning the network...");
	//	new NetworkCleaner().run(newNetwork);

	//	LOG.info("\n\n\n\nNetwork statistics after...");	
	//	MyIntersectionSimplifier.reportNetworkStatistics(newNetwork);
		new NetworkWriter(newNetwork).write(fullOutput);
	}

}
