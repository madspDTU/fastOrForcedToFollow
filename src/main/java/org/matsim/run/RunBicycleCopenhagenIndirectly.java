package org.matsim.run;

import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.TransportMode;

public class RunBicycleCopenhagenIndirectly {

	public static void main(String[] args) throws IOException{
		if(args.length==0){
			int iterations = 50;
			System.out.println("Using " + iterations + " cores");
			args = new String[]{"smallNoCongestion", "" + iterations, "bike",  "true"};
		}
		RunBicycleCopenhagen.main(args);




		//		 	ConstructSpeedFlowsFromCopenhagen.run(
		//	"/work1/s103232/ABMTRANS2019/withNodeModelling/smallRoWMixedQSimEndsAt100/", 
		//			"small", 0, Arrays.asList(),Arrays.asList(TransportMode.bike, TransportMode.car));
	}
}
