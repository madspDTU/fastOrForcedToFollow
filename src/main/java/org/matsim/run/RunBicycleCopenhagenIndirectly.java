package org.matsim.run;

import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.TransportMode;

public class RunBicycleCopenhagenIndirectly {

	public static void main(String[] args) throws IOException{
		
		
		

		// test: placeVehicleAtFront no longer activates the link. 
		
		// Directed of size 2 works!
		//Right-priority works!
		// Merging works!
		//Anti-priority works!
		if(args.length==0){
			args = new String[]{
					"fullRoWUneven", "5", "bike,car",  "true", "20"};
		}
		RunBicycleCopenhagen.main(args);




		//		 	ConstructSpeedFlowsFromCopenhagen.run(
		//	"/work1/s103232/ABMTRANS2019/withNodeModelling/smallRoWMixedQSimEndsAt100/", 
		//			"small", 0, Arrays.asList(),Arrays.asList(TransportMode.bike, TransportMode.car));
	}
}
