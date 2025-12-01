package plugins.fab.livemousetracker.rfid;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class RFIDPairing {
	
	
	String innerSerialNumber;
	Point2D location;
	String defaultComPort;
	float ray;
	boolean match;
	
	public RFIDPairing( Point2D location, float ray, String defaultComPort, String innerSerialNumber )
	{
		this.location = location;
		this.ray = ray;
		this.defaultComPort = defaultComPort;
		this.innerSerialNumber = innerSerialNumber;
		this.match = false;
		
	}

	public void submitCandidates(ArrayList<COMTester> comTestList) {

		for ( COMTester comTester : comTestList )
		{			
			// serial is "1234" by default on reader. If it is the case, nothing will append and the default com port will be used.
			if ( comTester.isRFIDReader() )
			{
				if ( comTester.innerSerialNumber.equals( this.innerSerialNumber ) )
				{
					System.out.println("RFIDPair match with comTester on port " + comTester.comPort );
					this.defaultComPort = comTester.comPort;
					match = true;
				}		
			}
		}
		
	}

	public RFIDAntenna createAntenna() {
		
		return new RFIDAntenna( this.location, this.ray, this.defaultComPort , this.innerSerialNumber );
		
	}
	
	
	
}
