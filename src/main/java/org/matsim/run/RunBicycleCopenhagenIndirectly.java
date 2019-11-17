package org.matsim.run;

import java.io.IOException;

public class RunBicycleCopenhagenIndirectly {

	public static void main(String[] args) throws IOException{
			
		if(args.length==0){
			args = new String[]{
					"fullRoWBothDasAutoResume50", "50", "car",  "true", "24"}; 
		}
		RunBicycleCopenhagen.main(args);




		//		 	ConstructSpeedFlowsFromCopenhagen.run(
		//	"/work1/s103232/ABMTRANS2019/withNodeModelling/smallRoWMixedQSimEndsAt100/", 
		//			"small", 0, Arrays.asList(),Arrays.asList(TransportMode.bike, TransportMode.car));
	}
}
