/**
 	@author Fabrice de Chaumont
 	copyright Fabrice de Chaumont @ Institut Pasteur

 	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package plugins.fab.livemousetracker.device.control;

import java.util.Arrays;

import SimpleDynamixel.Servo;

public class ServoUtil {

	public enum SERVO_TYPE
	{
		PLASTIC, METAL
	}

	public static Servo initServo( ServoUtil.SERVO_TYPE servoType , String port )
	{
		System.out.println( "Servo Init on port " + port );
		Servo servo = new Servo();
		int speed = 0;

		System.out.println( "Servo init: Servo Type: " + servoType );

		switch( servoType )
		{
		case PLASTIC:
			speed = 1000000;
			break;
		case METAL:
			speed = 57600;
			break;
		}

		System.out.println( "Servo init: Servo Communication speed: " + speed );

		servo.init( port, speed );

		int maxId = 5;
		System.out.println( "Servo init: Pinging from id 0 to " + maxId + "...");
		int [] servoList = servo.pingRange( 0 , maxId );
		System.out.println( "Servo init: Ping done.");
		System.out.println( "Servo init: Present Servo id(s): " +  Arrays.toString( servoList ) );
		return servo;
	}
}
