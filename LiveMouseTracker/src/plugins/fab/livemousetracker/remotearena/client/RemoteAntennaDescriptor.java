package plugins.fab.livemousetracker.remotearena.client;

import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * Used to send a message to server to init the remote antenna.
 *
 */
public class RemoteAntennaDescriptor implements Serializable {

	private static final long serialVersionUID = 6990476409044948311L;

	public Point2D location;
	public float ray;
	public String identifier;

	public RemoteAntennaDescriptor( Point2D location, float ray, String id ) {

		this.location = location;
		this.ray = ray;
		this.identifier = id;

	}

}
