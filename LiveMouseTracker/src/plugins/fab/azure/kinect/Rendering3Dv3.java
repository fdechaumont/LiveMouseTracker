package plugins.fab.azure.kinect;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.system.profile.Chronometer;
import icy.type.DataType;




public final class Rendering3Dv3 {

	
	//ArrayList<Point3f> pointList = new ArrayList<Point3f>();
	Channel3D[] channelArray = new Channel3D[10];
	float currentRotation = 1;
    float scale = 0.56f;
	float xOffset=500;
	float yOffset=500;
	float xRot = 0;
	float yRot = 0;
	float xRotCam1 = 0;
	float zRotCam1 = 180;
	
	boolean performCalibration = false;
	boolean performAutoTranslate = false;
	
	int skipper = 1;
	private Point startDragPoint;
	
	int width = 512;
	int height = 424;
	IcyBufferedImage infraImage = new IcyBufferedImage( width, height, 1, DataType.USHORT ); // short faster ?
	IcyBufferedImage depthImage = new IcyBufferedImage( width, height, 1, DataType.USHORT ); // short faster ?
	float[] zBuffer= new float[width*height];
	
    Point3f mainTranslate = new Point3f(-414, -410, 0);

	Viewer viewer = null;
	Sequence infraSequence= new Sequence( "3D infra");
	Sequence depthSequence= new Sequence( "3D depth map");
	
	Sequence depthSequenceRecorded= new Sequence( "3D depth map recorded");
	
	Overlay3D overlay3D = null;
	
	long renderTimeMs = 0;
	public int zClipFar = 2000; // 1000
	public int zClipClose = 0;  // 510
	public int imageToRecord = 0;
	
    public Rendering3Dv3() {
    
    	
    	infraImage.setData( 0, 0, 0, 4000 );// for lut
    	infraImage.setData( 0, 1, 0, 0 ); // for lut
    	infraSequence.setImage( 0, 0, infraImage);
    	
    	depthImage.setData( 0, 0, 0, 300 );// for lut
    	depthImage.setData( 0, 0, 0, 500 ); // for lut
    	depthSequence.setImage( 0, 0, depthImage);
    	
    	
    	
    	Icy.addSequence(infraSequence);
    	Icy.addSequence(depthSequence);
    	
    	for ( int i=0; i< channelArray.length; i++ )
    	{
    		channelArray[i] = new Channel3D( i );    		
    	}
    	channelArray[0].color = Color.BLACK;
    	channelArray[1].color = Color.GREEN;
    	channelArray[2].color = Color.BLUE;
    	
    	//channelArray[0].translation = new Point3f( 0, 252 , 28 );
    	channelArray[0].translation = new Point3f( -11, 138 , -41);
    	//channelArray[1].translation = new Point3f( -16, -279 , 23 ); // short distance config
    	channelArray[1].translation = new Point3f( -32, -392 , -37 ); // short distance config
    	
    	channelArray[0].xRot= (float) 8.5; // 17
    	channelArray[0].yRot= 2;
    	channelArray[0].zRot= 0;
    	
    	channelArray[1].xRot= -23; // -24
    	channelArray[1].yRot= -3; // -2
    	channelArray[1].zRot= 180;

        channelArray[1].computeMatrix();

        channelArray[0].enabled = true;
        channelArray[1].enabled = true;
        channelArray[2].enabled = false;
    	
    	//channelArray[1].translation = new Point3f( -362, -8 , 0 ); // long distance config
    	
    	/*
        // cylinder
    	ArrayList<Point3f> pointList = new ArrayList<Point3f>();
    	ArrayList<Integer> colorList = new ArrayList();
        float ray = 10;
    	for ( float a = 0 ; a < 20 ; a ++ )
        {
        	for ( float y = 0 ; y < 20 ; y ++ )
        	{
        		pointList.add( new Point3f( (float)(Math.cos( a ) * ray) , y , (float)(Math.sin( a ) * ray) ) );
        		colorList.add( 0 );
        	}
        	
        }
    	this.setPoints( 0, pointList , colorList );
    	*/
        
    	/*
    	Timer timer = new Timer( 30 , new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				//currentRotation+=0.01;
				Rendering3Dv3.render();
			}
		});
    	timer.start();
    	*/
    
    	/*
    	addMouseWheelListener( this );
    	addMouseListener( this );
    	addMouseMotionListener( this );
    	addKeyListener( this );
    	this.setFocusable( true );
    	*/
    	
    	//Overlay3D overlay3D = new Overlay3D( this );
    	infraSequence.addOverlay( overlay3D );
    	depthSequence.addOverlay( overlay3D );
    	
    	
    }
    
    public float max( float val , float renderBuffer )
    {
    	if ( val > renderBuffer )
    		return val;
    	return renderBuffer;
    }
    
    public void recordDepthMap()
    {
    	depthSequenceRecorded.removeAllImages();
    	Icy.addSequence( depthSequenceRecorded );    	
    	imageToRecord = 100;
    }
    
    public void render( ) {
    	
    	try
    	{
    		//depthSequence.getFirstViewer().getLut().getLutChannel(0).setMinMax( 340, 400 );    	
    	}catch( Exception e )
    	{
    		
    	}
    	
    	long nanoTime = System.nanoTime();
    	
    	// setup viewer
    	/*
    	if ( this.viewer != null )
    	{
    		try
    		{
    			this.viewer = renderingSequence.getFirstViewer();
	    		addMouseWheelListener( this );
	        	addMouseListener( this );
	        	addMouseMotionListener( this );
	        	addKeyListener( this );
    	}
    	*/
    	
    	
    	short[] renderBuffer = (short[]) infraImage.getDataXY(0);
    	short[] depthBuffer = (short[]) depthImage.getDataXY(0);

    	// clear rendering image
    	for ( int i = 0 ; i < renderBuffer.length ; i ++ )
    	{
    		renderBuffer[i] = 0;
    	}
    	// clear depth image
    	for ( int i = 0 ; i < renderBuffer.length ; i ++ )
    	{
    		depthBuffer[i] = 0;
    	}
    	
    	int FUSION_MODE = 4; //4
    	
    	
    	// init z buffer
    	if ( FUSION_MODE == 4 )
    	{
        	for ( int i = 0 ; i < zBuffer.length ; i ++ )
        	{
        		zBuffer[i] = 0;
        	}
    		
    	}else
    	{
        	for ( int i = 0 ; i < zBuffer.length ; i ++ )
        	{
        		zBuffer[i] = 100000;
        	}
    		
    	}
    	
    	
    	//Graphics2D g2 = ( Graphics2D ) renderingImage.getGraphics();
    	
    	
//    	float textSize = 10;
//    	float yText = textSize;
    	double meanZ = 0;
    	
    	
    	
    	/*
    	g2.setFont( new Font( "Arial" , Font.BOLD , 30 ) );
    	g2.drawString( "Skipper: "+ this.skipper, 10, yText );
		yText+=textSize;
    	g2.drawString( "xOffset: "+ this.xOffset, 10, yText );
		yText+=textSize;
    	g2.drawString( "yOffset: "+ this.yOffset, 10, yText );
		yText+=textSize;
		g2.drawString( "xRot: "+ this.xRot, 10, yText );
		yText+=textSize;
    	g2.drawString( "yRot: "+ this.yRot, 10, yText );
		yText+=textSize;
    	*/
		
    	if ( performCalibration )
    	{
    		Chronometer calibration = new Chronometer("Calibration.");
    		
    		float bestAngle = 0;
    		double minSTD = Double.MAX_VALUE;
    		for ( Channel3D channel : channelArray )
    		{
    			for ( float angle = -45 ; angle< 45 ; angle+=1 )
    			{
    				channel.xRot = angle;
    				channel.computeMatrix();
    				channel.convertToPoint();
    				channel.transformPoints();
    				double std = channel.getZstd();
    				if ( std < minSTD )
    				{
    					bestAngle = angle;
    					minSTD = std;
    				}
    			}
    			channel.xRot = bestAngle;
    			System.out.println("Channel " + channel.number );
    			System.out.println("Best angle : " + bestAngle );
    			
    		}
    		
    		
    		
    		calibration.displayMs();
    		performCalibration = false;
    	}
    	
    	if ( performAutoTranslate ) // perform autoTranslation of the channel 1 ( 0 is not moving )
    	{
    		channelArray[1].performAutoTranslate( channelArray[0] );
    	}
    	
		
    	for ( Channel3D channel : channelArray )
    	{
    		if ( !channel.enabled )
    		{
    			continue;
    		}
    		
    		//ArrayList<Point3f> pointList = channel.pointList;
    		//ArrayList<Integer> colorList = channel.colorList;
    		//short[] colorArray = channel.colorArray;
    		
    		// transform points
    		Matrix4d yRotationMatrix = new Matrix4d();
    		yRotationMatrix.rotY( this.yRot * ( Math.PI / 180. ) );

    		Matrix4d xRotationMatrix = new Matrix4d();
    		xRotationMatrix.rotX( this.xRot * ( Math.PI / 180. ) );

    		/*
    		Matrix4d zRotationMatrixCam1 = new Matrix4d(); // half turn in axis
    		zRotationMatrixCam1.rotZ( this.zRotCam1 * ( Math.PI / 180. ) );
    		
    		Matrix4d xRotationMatrixCam1 = new Matrix4d();
    		xRotationMatrixCam1.rotX( this.xRotCam1 * ( Math.PI / 180. ) );
    		*/

    		Matrix4d scaleMatrix = new Matrix4d();
    		scaleMatrix.setScale( scale ); // distance should be a Z 

    		//Matrix4d translationMatrix = new Matrix4d();
    		//translationMatrix.setTranslation( new Vector3d( 10 , 10 , 0 ) );



    		//ArrayList<Point3f> pointSource = new ArrayList(); // deep copy (not effective at all)
    		
    		if ( channel.pointArray == null )
    		{    			    			
    			continue; // stop rendering channel    			
    		}
    		
    		channel.convertToPoint();
    		channel.computeMatrix();
    		channel.transformPoints();
    		

    		meanZ = 561;
    		/*
    		// search for mean z. (only in channel 0)
    		if ( channel.number == 0 )
    		{
    			meanZ = 0;
    			for ( Point3f p : pointSource )
    			{
    				meanZ+=p.z;
    			}
    			meanZ/=pointSource.size();
    			g2.drawString( "MeanZ: "+ meanZ, 10,yText );
        		yText+=textSize;
    		}
    		*/

    		
    		
    		
    		
    		// Pour le Z buffer il faut projeter sur un plan virtuel et chercher le max Z sur ce plan
    		// et non pas sur le plan de rendu.
    		// C'est un peu comme le test du z clip, il faut le faire selon l'axe z de la camera, pas celui du rendu
    		// donc on aura 2 Z buffer à la fin
    		
    		
    		//if ( channel.number == 1 )
    		/*
    		{
    			for ( Point3f p : channel.pointSource ) // transform
    			{
    				channel.zRotationMatrix.transform( p );
    				channel.xRotationMatrix.transform( p );
    			}
    		}
    		*/
    		

    		for ( Point3f p : channel.pointSource ) // transform
    		{
    			// channel transform
    			p.x += channel.translation.x;
    			p.y += channel.translation.y;
    			p.z += channel.translation.z;
    			
    			/*
    			if ( p.z < zClipClose || p.z > zClipFar ) // z clip
    			{
    				p.z = 0; // set to out of view.
    				p.x = -100000;
    				p.y = 0; 
    			} 
    			*/   			
        		
    			// global transform
    			p.x += mainTranslate.x;
    			p.y += mainTranslate.y;
    			p.z += mainTranslate.z;
    			
    			// display transform
    			
    			p.z-=meanZ;
    			xRotationMatrix.transform( p );
    			yRotationMatrix.transform( p );
    			p.z+=meanZ;
    			
    			    			
    			scaleMatrix.transform( p );

    			p.x+=this.xOffset;
    			p.y+=this.yOffset;


    		}
    		
    		

    		//g2.setColor( channel.color );
    		/*
    		for ( Point3f p : pointSource ) // transform
    		{
    			g2.drawRect( (int)p.x, (int)p.y, 1, 1 );
    		}
    		*/
    		//g2.setColor( Color.black );
    		
    		// TODO: take all points and put them in a blitter with z-order
    		
    		for ( int i = 0 ; i < channel.pointSource.size() ; i+=skipper )
    		{
    			try
    			{
    				int color = channel.colorArray[i]; // .get( i ) ;

    				//float val = color/4000f;
    				float val = color;
    				if ( val < 0 )
    				{ 
    					val = 0; 
    				}
//    				if ( val > 1 )
//    				{
//					val = 1+ ( (val-1) / 5f ) ;
//				}
//    				if ( val > 0.8 )
//    				{
//    					//val = 1;
//    					continue;
//    				}
    				if ( val > 5000 )
    				{
    					//val = 1;
    					continue;
    				}
    				
        			
    				Point3f p = channel.pointSource.get(i);
    				int x = (int) p.x;
    				int y = (int) p.y;    				

    				
    				
    				for ( int xx = x ; xx < x+2 ; xx++ )
    				{
    					for ( int yy = y ; yy < y+2 ; yy++ )
    					{
    						if ( xx >= 0 && xx < width && yy >= 0 && yy < height ) // check clip
    	    				{
    	    					int offset = xx+yy*width;
    	    					
    	    					if ( FUSION_MODE == 1 ) // z-buffer classique.
    	    					{
    	    						if ( p.z < zBuffer[offset] ) // check z buffer
    	    						{
    	    							renderBuffer[offset] = (short)val;
    	    							depthBuffer[offset] = (short)(p.z);
    	    							zBuffer[offset] = p.z;
    	    						}
    	    					}
    	    					
    	    					if ( FUSION_MODE == 2 ) // maxI or maxZ
    	    					{
    	    						if ( p.z < zBuffer[offset] || val > renderBuffer[offset] ) // check z buffer or higher intensity
    	    						{
    	    							renderBuffer[offset] = (short)val;
    	    							depthBuffer[offset] = (short)(p.z);
    	    							zBuffer[offset] = p.z;
    	    						}
    	    					}

    	    					if ( FUSION_MODE == 3 ) // maxI
    	    					{
    	    						if ( val > renderBuffer[offset] ) // check z buffer or higher intensity
    	    						{
    	    							renderBuffer[offset] = (short)val;
    	    							depthBuffer[offset] = (short)(p.z);    	    							
    	    						}
    	    					}

    	    					if ( FUSION_MODE == 4 ) // maxZ
    	    					{
    	    						/*
    	    						if ( p.z > zBuffer[offset] ) // check z buffer or higher intensity
    	    						{
    	    							renderBuffer[offset] = (short)val;
    	    							depthBuffer[offset] = (short)(p.z);
    	    							zBuffer[offset] = p.z;
    	    						}*/
    	    						if ( p.z > zBuffer[offset] ) // check z buffer or higher intensity
    	    						{
    	    							if ( val > renderBuffer[offset] ) // keep brightest
    	    							{
    	    								renderBuffer[offset] = (short)val;
    	    							}
    	    						
    	    							depthBuffer[offset] = (short)(p.z);
    	    							zBuffer[offset] = p.z;
    	    						}
    	    					}
    	    					
    	    					
    	    					
    	    					/*
    	    					//if ( p.z < zBuffer[offset] ) // check z buffer
    	    					{
    	    						if ( val > renderBuffer[offset] )
    	    						{
    	    							renderBuffer[offset] = (short)val;
    	    							depthBuffer[offset] = (short)(p.z);
    	    						}
    	    						
    	    						//renderBuffer[offset] = max( val , renderBuffer[offset] ); // max
    	    						
    	    						//renderBuffer[offset] = val; // normal
    	    						zBuffer[offset] = p.z;
    	    						
    	    						//depthBuffer[offset] = max ( depthBuffer[offset] , p.z );
    	    					}
    	    					*/
    	    				}
    					}
    				}
    				
    				/*
    				if ( x >= 0 && x < width && y >= 0 && y < height ) // check clip
    				{
    					int offset = x+y*width;
    					if ( p.z < zBuffer[offset] ) // check z buffer
    					{
    						renderBuffer[offset] = val;
    						zBuffer[offset] = p.z;
    					}
    				}
    				*/
    				
    			}
    			catch( Exception e)
    			{
    				// System.out.println( "no color at i=" + i  );
    			}
    			
    		}
    		/*
    		g2.drawString( "Rotation: "+ this.currentRotation, 10,yText );
    		yText+=textSize;
    		g2.drawString( "Scale: "+ this.scale, 10,yText );
    		yText+=textSize;
    		g2.drawString( "Shift translation: "+ channel.translation, 10,yText );
    		yText+=textSize;
    		*/
    		
    		
    	}
    	
    	
    	/*
    	g2.setColor( Color.RED );
    	g2.drawLine( 0,0,100,100 );
    	*/
    	
    	//System.out.println("data changed");
    	this.renderTimeMs = ( System.nanoTime() - nanoTime ) / 1000000;
    	System.out.println( this.renderTimeMs );
    	
    	infraImage.dataChanged();
    	depthImage.dataChanged();
    	
    	if ( imageToRecord > 0 )
    	{
    		depthSequenceRecorded.addImage( depthSequenceRecorded.getSizeT(),  depthImage.getCopy() );
    		imageToRecord--;
    		if ( imageToRecord < 0 )
    		{
    			imageToRecord = 0;
    		}
    		
    		
    	}
    	
    }
    
    //public void setPoints( int channel , ArrayList<Point3f> newPointList , ArrayList<Integer> newColorList )
    public void setPoints( int channel , short[] newPointArray , short[] newColorArray )
    {
//    	System.out.println("receive points");
    	channelArray[channel].lock.lock();
		try
		{
    	channelArray[channel].pointArray = newPointArray;
		channelArray[channel].colorArray = newColorArray;
		}
		finally {
			channelArray[channel].lock.unlock();		
		}
//		System.out.println("receive points done");
		
    	/*
    	//System.out.println("receive points");
      	ArrayList<Point3f> pointList = channelArray[channel].pointList;
      	ArrayList<Integer> colorList = channelArray[channel].colorList;
    	
    	synchronized ( pointList ) {
    		pointList.clear();
    		pointList.addAll( newPointList );    		
    		colorList.clear();
    		colorList.addAll( newColorList );
		}
    	//System.out.println("receive points done");
    	*/
    }

    
    
    
    
    
    
    

}