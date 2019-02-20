package org.matsim.run;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.matsim.api.core.v01.TransportMode;

public class RunBicycleCopenhagenIndirectly {

	public static void main(String[] args) throws IOException{
		String[] arguments = new String[]{"smallRoWMixedQSimEndsAt100", "1"};
		RunBicycleCopenhagen.main(arguments);




		//			ConstructSpeedFlowsFromCopenhagen.run(
		//	"/work1/s103232/ABMTRANS2019/withNodeModelling/smallRoWMixedQSimEndsAt30/", 
		//			"small", 0, Arrays.asList(),Arrays.asList(TransportMode.bike, TransportMode.car));
	}
}
