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
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.algorithms.intersectionSimplifier.IntersectionSimplifier;
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

	final private static String dir = "/zhome/81/e/64390/MATSim/ABMTRANS2019/input/";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] args2 = new String[]{dir + "MATSimCopenhagenNetwork_WithBicycleInfrastructure_NOTSIMPLIFIED.xml.gz",
				dir + "MATSimCopenhagenNetwork_WithBicycleInfrastructure.xml.gz"};
		run(args2);
	}
	
	public static void run(String[] args) {
		String input = args[0];
		String output = args[1];
		
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(input);
		
		IntersectionSimplifier ns = new IntersectionSimplifier(10., 2);
		Network newNetwork = ns.simplify(network);
		NetworkCalcTopoType nct = new NetworkCalcTopoType();
		nct.run(newNetwork);
		
		LOG.info("Simplifying the network...");
		new NetworkSimplifier().run(newNetwork);
		LOG.info("Cleaning the network...");
		new NetworkCleaner().run(newNetwork);
		
		IntersectionSimplifier.reportNetworkStatistics(newNetwork);
		new NetworkWriter(newNetwork).write(output);
	}

}
