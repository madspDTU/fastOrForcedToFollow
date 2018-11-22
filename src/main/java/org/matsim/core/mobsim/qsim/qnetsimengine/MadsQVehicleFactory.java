package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.vehicles.Vehicle;

public final class MadsQVehicleFactory implements QVehicleFactory {
	
    @Override public QVehicle createQVehicle( Vehicle vehicle ){
    
    	QVehicle qvehicle ;
        if ( vehicle.getId().toString().contains( TransportMode.bike ) ) {
            qvehicle = new QCycle( vehicle) ;
        } else {
            qvehicle = new QVehicleImpl( vehicle ) ;
        }
        return qvehicle ;
    
    }

}
