package plugins.fab.aaa.voc.family;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import com.sun.prism.Image;

import icy.gui.component.ImageComponent;
import icy.image.IcyBufferedImage;
import icy.type.DataType;
import plugins.fab.aaa.voc.Voc;

public class VocPanel extends JPanel {

	//double[][][] vocMagnitude;
	IcyBufferedImage image;

	public VocPanel( double[][][] magnitude ) {

		//this.magnitude = vocMagnitude;
		this.setPreferredSize( new Dimension( magnitude[0].length+20, magnitude[0][0].length ) );

		int width = magnitude[0].length;
        int height = magnitude[0][0].length;

		image = new IcyBufferedImage( width, height, magnitude.length, DataType.FLOAT );
		image.beginUpdate();

		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
        for ( int channel = 0 ; channel < magnitude.length ; channel ++ )
        {
        	// draw FFT magnitude
        	for ( int x = 0 ; x < width ; x++ )
        	{
        		for ( int y = 0 ; y < height ; y++ )
        		{
        			double value = Math.log( magnitude[channel][x][image.getHeight()-y-1] );
        			if ( value < -4 ) value = -4;
        			if ( value > max ) max = value;
        			if ( value < min ) min = value;
        		}
        	}
        }

        for ( int channel = 0 ; channel < magnitude.length ; channel ++ )
        {
        	// draw FFT magnitude
        	for ( int x = 0 ; x < width ; x++ )
        	{
        		for ( int y = 0 ; y < height ; y++ )
        		{
        			double value = Math.log( magnitude[channel][x][image.getHeight()-y-1] );
        			if ( value < -4 ) value = -4;
        			//image.setData( (int)x , y, channel, 1- ( (value+min) / ( max -min ) ) ); // rescale between 0 and 1
        			image.setData( (int)x , y, channel, 1- ( (value-min) / ( max -min ) ) ); // rescale between 0 and 1
        		}
        	}
        }


        image.endUpdate();

        ImageComponent ic = new ImageComponent( image );
        this.add( ic );
        this.revalidate();

	}

	/*
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor( Color.black);
		g.drawLine( 0 , 0, 100, 100);
		g.setColor( Color.red);
		g.drawLine( 100 , 0, 0, 100);


		//new ImageComponent(ou Image .class draw dans un panel)

	}*/

}
