
/* *********************************************************************** *
 * project: org.matsim.*
 * SingleModeNetworksCache.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.router;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.core.router.FFFLinkToLinkRouting.FFFTravelTimesInvertedNetworkProxy;

public class SingleModeInvertedTravelTimesCache {

	private Map<String, FFFTravelTimesInvertedNetworkProxy> singleModeInvertedTravelTimesCache = new ConcurrentHashMap<>();

	public Map<String, FFFTravelTimesInvertedNetworkProxy> getSingleModeInvertedTravelTimesCache() {
		return singleModeInvertedTravelTimesCache;
	}
}