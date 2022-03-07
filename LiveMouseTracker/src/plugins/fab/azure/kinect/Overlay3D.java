package plugins.fab.azure.kinect;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;

import javax.vecmath.Point3f;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.point.Point5D.Double;
import plugins.fab.livemousetracker.LiveMouseTracker;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class Overlay3D extends Overlay {

	Rendering3Dv3 renderer = null;
	int channelControl = 0;

	public Overlay3D(Rendering3Dv3 renderer ) {
		super("Overlay3D");
		this.renderer = renderer;
		
		
	}
		
	
	
	@Override
	public void keyPressed(KeyEvent e, Double imagePoint, IcyCanvas canvas) {

		int keyCode = e.getKeyCode();
		
		if ( keyCode == KeyEvent.VK_RIGHT )
		{			
			renderer.yRot+=10;	 
		}
		if ( keyCode == KeyEvent.VK_LEFT )
		{
			renderer.yRot-=10;	 
		}
		if ( keyCode == KeyEvent.VK_UP )
		{
			renderer.xRot+=10;	 
		}
		if ( keyCode == KeyEvent.VK_DOWN )
		{
			renderer.xRot-=10;	 
		}
		if ( e.getKeyChar()==':' )
		{
			renderer.channelArray[channelControl].yRot-=1;
			renderer.channelArray[channelControl].computeMatrix();
			System.out.println("yRot : " + renderer.channelArray[channelControl].yRot );
		}
		
		if ( e.getKeyChar()=='!' )
		{
			renderer.channelArray[channelControl].yRot+=1;
			renderer.channelArray[channelControl].computeMatrix();
			System.out.println("yRot : " + renderer.channelArray[channelControl].yRot );
		}
		
		if ( e.getKeyChar()==',' )
		{
			renderer.channelArray[channelControl].xRot-=1;
			renderer.channelArray[channelControl].computeMatrix();
			System.out.println("xRot : " + renderer.channelArray[channelControl].xRot );
		}
		
		if ( e.getKeyChar()==';' )
		{
			renderer.channelArray[channelControl].xRot+=1;
			renderer.channelArray[channelControl].computeMatrix();
			System.out.println("xRot : " + renderer.channelArray[channelControl].xRot );
		}

		if ( e.getKeyChar()=='c' )
		{
			renderer.performCalibration = true;
		}

		
		
		if ( e.getKeyChar()=='0' )
		{
			renderer.channelArray[0].enabled = !renderer.channelArray[0].enabled;
			channelControl = 0;
			e.consume();
		}
		
		if ( e.getKeyChar()=='1' )
		{
			renderer.channelArray[1].enabled = !renderer.channelArray[1].enabled;
			channelControl = 1;
			e.consume();
		}
		if ( e.getKeyChar()=='2' )
		{
			renderer.channelArray[2].enabled = !renderer.channelArray[1].enabled;
			channelControl = 2;
			e.consume();
		}
		
		if ( e.getKeyChar()=='t' )
		{
			System.out.println("Translate x+");
			renderer.channelArray[channelControl].translation.x+=1;	 
		}
		if ( e.getKeyChar()=='T' )
		{
			System.out.println("Translate x-");
			renderer.channelArray[channelControl].translation.x-=1;	 
		}

		if ( e.getKeyChar()=='y' )
		{
			System.out.println("Translate y+");
			renderer.channelArray[channelControl].translation.y+=1;	 
		}
		if ( e.getKeyChar()=='Y' )
		{
			System.out.println("Translate y-");
			renderer.channelArray[channelControl].translation.y-=1;	 
		}
		if ( e.getKeyChar()=='u' )
		{
			System.out.println("Translate z+");
			renderer.channelArray[channelControl].translation.z+=1;	 
		}
		if ( e.getKeyChar()=='U' )
		{
			System.out.println("Translate z-");
			renderer.channelArray[channelControl].translation.z-=1;	 
		}
		
		if ( e.getKeyChar()=='z' )
		{			
			renderer.mainTranslate.y-=10;	 
		}
		if ( e.getKeyChar()=='s' )
		{			
			renderer.mainTranslate.y+=10;	 
		}
		if ( e.getKeyChar()=='d' )
		{			
			renderer.mainTranslate.x+=10;	 
		}
		if ( e.getKeyChar()=='q' )
		{			
			renderer.mainTranslate.x-=10;	 
		}
		if ( e.getKeyChar()=='a' )
		{			
			renderer.mainTranslate.z+=10;	 
		}
		if ( e.getKeyChar()=='e' )
		{			
			renderer.mainTranslate.z-=10;	 
		}
		

		if ( e.getKeyChar()=='+' )
		{			
			renderer.scale+=0.01;	 
		}
		if ( e.getKeyChar()=='-' )
		{
			renderer.scale-=0.01;
		}

		
		if ( e.getKeyChar()=='f' )
		{			
			renderer.zClipFar+=10;	 
		}
		if ( e.getKeyChar()=='F' )
		{
			renderer.zClipFar-=10;
		}
		if ( e.getKeyChar()=='g' )
		{			
			renderer.zClipClose+=10;
			e.consume();
		}
		if ( e.getKeyChar()=='G' )
		{
			renderer.zClipClose-=10;
			e.consume();
		}
		
		
		if ( e.getKeyChar()=='v' )
		{			
			renderer.xRot=-90;
			renderer.mainTranslate.x=-474;
			renderer.mainTranslate.y=-730;
			renderer.mainTranslate.z=-600;
			/*
			renderer.mainTranslate.x = 0;
			renderer.mainTranslate.y = 0;
			renderer.mainTranslate.z = 0;
			*/
		}
		
		if ( e.getKeyChar()=='$' )
		{
			// start recording
			renderer.recordDepthMap();
		}


		
		if ( e.getKeyChar()=='/' )
		{
			renderer.skipper--;
			if ( renderer.skipper < 1 )
			{
				renderer.skipper = 1;
			}
		}
		if ( e.getKeyChar()=='*' )
		{
			renderer.skipper++;	 
		}

		
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
		int fontSize = 5;
		int yText = fontSize;
		
		g.setColor( Color.ORANGE );
		g.setFont( new Font( "Arial" , Font.BOLD , fontSize ) );
    	g.drawString( "Render time: "+ renderer.renderTimeMs + " ms", 10, yText );
    	yText+=fontSize;
    	g.drawString( "Main translation: "+ renderer.mainTranslate, 10, yText );
    	yText+=fontSize;
    	g.drawString( "Main scale: "+ renderer.scale, 10, yText );
    	yText+=fontSize;
    	g.drawString( "Close z-clip: "+ renderer.zClipClose, 10, yText );
    	yText+=fontSize;
    	g.drawString( "Far z-clip: "+ renderer.zClipFar, 10, yText );
    	yText+=fontSize;
    	g.drawString( "rotX/Y: "+ renderer.xRot + "/" + renderer.yRot, 10, yText );
    	yText+=fontSize;
    	g.setColor( Color.green );
    	for ( Channel3D channel : renderer.channelArray )
    	{
    		if ( channel.enabled )
    		{
    			g.drawString( "Channel "+ channel.number + ": translate: "+ channel.translation, 10, yText );
    			yText+=fontSize;
    		}
    	}    	
    	    	
    	g.setColor( Color.ORANGE.darker() );
    	g.drawRect( 84, 33 , 428-84, 383-33 );
    	g.setColor( Color.ORANGE );
    	g.drawRect( 114, 63 , 398-114 , 353-63 );
    	
    	/*
		roiCage50x50 = new ROI2DPolygon( new Point2D.Double( 86-5+3, 55-5-17 ) );
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 55-5 -17), false);
		roiCage50x50.addNewPoint( new Point2D.Double( 420+5+3, 395+5 -17), false);
		roiCage50x50.addNewPoint( new Point2D.Double(  86-5+3, 395+5 -17), false);
		roiCage50x50.setCreating( false );

		roiCage50x50Floor = new ROI2DPolygon( new Point2D.Double( 86-5+3 +30, 55-5-17 +30 ) );
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 55-5 -17 +30 ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double( 420+5+3 -30 , 395+5 -17 -30 ), false);
		roiCage50x50Floor.addNewPoint( new Point2D.Double(  86-5+3 +30 , 395+5 -17 -30 ), false);
		roiCage50x50Floor.setCreating( false );
    	 */
    	
    	
	}
	
}
