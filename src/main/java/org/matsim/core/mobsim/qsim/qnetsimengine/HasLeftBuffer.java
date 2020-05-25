package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface HasLeftBuffer {
	
	public boolean isCarLeftTurn(Id<Link> fromLink, Id<Link> toLink);

}
