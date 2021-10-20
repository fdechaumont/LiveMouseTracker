package plugins.fab.livemousetracker.rfid;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public interface Antenna {

	public Point2D getLocation();

	public float getRay();

	public void setEnabled(boolean enabled);

	public double readFrequency();

	public void addRFIDAntennaListener( AntennaReadListener antennaReadListener );

	public void switchOff();

	public void shutdown();

	public boolean isFaulty();

	public void paint(Graphics2D g);

	public String getIdentifier();

}
