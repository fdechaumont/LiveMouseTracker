package plugins.fab.azure.kinect;

import java.awt.Color;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3f;


public class Channel3D {

	ReentrantLock lock = new ReentrantLock();
	//public ArrayList<Point3f> pointList = new ArrayList();
	public short[] pointArray = null;
	ArrayList<Point3f> pointSource = new ArrayList<>();
	
	public short[] colorArray = null;
	
	public Color color= Color.BLACK;
	public Point3f translation= new Point3f( 0 , 0 , 0 );
	public boolean enabled=true;
	
	Matrix4d xRotationMatrix = new Matrix4d();
	Matrix4d yRotationMatrix = new Matrix4d();
	Matrix4d zRotationMatrix = new Matrix4d();	

	float xRot = 0;
	float yRot = 0;
	float zRot = 0;
	
	public int number;
	
	public Channel3D( int number ) {
		this.number = number;
		this.computeMatrix();
	}

	public void computeMatrix() { // recalculate the matrix
		xRotationMatrix.rotX( this.xRot * ( Math.PI / 180. ) );
		yRotationMatrix.rotY( this.yRot * ( Math.PI / 180. ) );
		zRotationMatrix.rotZ( this.zRot * ( Math.PI / 180. ) );		
	}

	public void convertToPoint() { // convert to point3f

		this.pointSource = new ArrayList<>();
		this.lock.lock();
		try {
			if ( this.pointArray == null )
			{
				System.out.println("No data on channel " + this.number + " (pointArray is null)");
				return;
			}			
			for ( int i = 0 ; i < this.pointArray.length; i +=3 )									
			{					
				pointSource.add( new Point3f( this.pointArray[i], this.pointArray[i+1], this.pointArray[i+2] ) );
			}    		
		}
		finally {			
			this.lock.unlock();
		}

		
	}

	public void transformPoints() {
		
		for ( Point3f p : this.pointSource ) // transform
		{
			this.zRotationMatrix.transform( p );
			this.xRotationMatrix.transform( p );
			this.yRotationMatrix.transform( p );
		}
	}
	
	public void translatePoints() {
		
		for ( Point3f p : this.pointSource ) // transform
		{
			p.x+= this.translation.x;
			p.y+= this.translation.y;
			p.z+= this.translation.z;
		}
	}

	public double getZstd() {

		double sum = 0.0;
		double standardDeviation = 0.0;
        int length = this.pointSource.size();

        for ( Point3f p : this.pointSource )
        {
            sum += p.z;
        }

        double mean = sum/length;

        for ( Point3f p : this.pointSource )
        {
            standardDeviation += Math.pow(p.z - mean, 2);
        }

        return Math.sqrt(standardDeviation/length);
		
		
	}

	private ArrayList<Point3f> getHigherPoints( int nbPoints )
	{
		ArrayList<Point3f> resultList = new ArrayList<Point3f>();
		
		for ( Point3f p : this.pointSource )
		{
			if ( resultList.size() < nbPoints )
			{
				resultList.add( p );
				continue;
			}
			Point3f pointToReplace = null;
			for ( Point3f pInResult : resultList )
			{
				if ( pInResult.z > p.z ) // keep lower values
				{
					pointToReplace = pInResult;
				}
			}
			if ( pointToReplace != null )
			{
				resultList.remove( pointToReplace );
				resultList.add( p );
			}
			
		}
		
		
		return pointSource;		
	}
	
	public void performAutoTranslate(Channel3D channel3d) {

		
		// pick the lower (higher in scene) z pixels to register
		
		Channel3D refChannel = channel3d;
		refChannel.computeMatrix();
		refChannel.convertToPoint();
		refChannel.transformPoints();
		refChannel.translatePoints();
		ArrayList<Point3f> refPointList = refChannel.getHigherPoints( 100 );
		
		Channel3D movingChannel = this;
		movingChannel.computeMatrix();
		movingChannel.convertToPoint();
		movingChannel.transformPoints();
		movingChannel.translatePoints();
		ArrayList<Point3f> movingPointList = movingChannel.getHigherPoints( 100 );
		
		
		

		
	}
	
}
