package plugins.fab.aaa.voc;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import plugins.fab.livemousetracker.DrawUtil;

public class VocalizationOverlay extends Overlay {

    private double mouseX;
    private double mouseY;
    double N;
    double Fs;
    double magnitudeBinSize;
    /** frequency max per x */
    double[] frequencyMax;
    ArrayList<Voc> vocList;
    //ArrayList<Voc> vocPreFilteredList;
    double proba = -1;
    private FrequencyCancelerAndSTD frequencyCanceler;
    AudioFile2 audioFile;
    AudioFFTProcessing fftProcessing;
    double startSecondOffset;
    public float MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS = 40;

    public VocalizationOverlay(
            //double N, double Fs, double magnitudeBinSize, double[] frequencyMax,
                //				ArrayList<Integer> cancelFrequencyList,
                                ArrayList<Voc> vocList, FrequencyCancelerAndSTD frequencyCanceler, AudioFile2 audioFile,
                                AudioFFTProcessing fftProcessing, //, ArrayList<Voc> vocPreFilteredList,
                                //, double proba
                                DrawMode drawMode, double startSecondOffset, float MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS
                                ) {
        super( "Voc Overlay" , OverlayPriority.TOOLTIP_HIGH );

        this.MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS = MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS;
        if ( drawMode != null )
        {
        	currentDrawMode = drawMode;
        }
        this.N = N;
        this.Fs = Fs;
        this.magnitudeBinSize = magnitudeBinSize;
        this.frequencyMax = frequencyMax;
        this.startSecondOffset = startSecondOffset;

        this.vocList = vocList;
        this.proba = proba;
        this.frequencyCanceler = frequencyCanceler;
        this.audioFile = audioFile;
        this.fftProcessing = fftProcessing;
        // this.vocPreFilteredList = vocPreFilteredList;
    }

    public enum DrawMode { DEFAULT, EDIT, ALL, FREQUENCY, CANCEL_FREQUENCIES, WEB }

    DrawMode currentDrawMode = DrawMode.values()[0];

    icy.type.point.Point5D.Double imagePoint;

    @Override
    public void mouseMove(MouseEvent e, icy.type.point.Point5D.Double imagePoint, IcyCanvas canvas) {
        this.imagePoint = imagePoint;
        mouseX = imagePoint.getX();
        mouseY = imagePoint.getY();
        this.painterChanged();
    }

    @Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas ) {

        synchronized( vocList )
        {
//		if ( sequence.getChannelName(0) !="Spectrum" )
//		{
//			// init all lookup table and channels
//			sequence.setChannelName(0, "Spectrum");
//			sequence.getFirstViewer().getLut().getLutChannel(0).setColorMap( LinearColorMap.gray_inv_ , false );
//			//sequence.setColormap( 0 , LinearColorMap.gray_inv_ );
//			sequence.getFirstViewer().getLut().getLutChannel( 0 ).setMinMax( -4 , 1.7 );
//		}

        Canvas2D ca = (Canvas2D) canvas;
        Graphics2D gAbsolute = (Graphics2D)g.create();
        gAbsolute.setFont( new Font("Arial", Font.BOLD , 20 ) );
        gAbsolute.transform( ca.getInverseTransform() );
        if ( currentDrawMode != DrawMode.WEB )
        {
        	DrawUtil.drawHint(gAbsolute, currentDrawMode.name(), 0, canvas.getHeight()-50, Color.red , Color.white);
        	DrawUtil.drawCenteredHint(gAbsolute, "nbVoc: "+vocList.size(), canvas.getWidth()/2, canvas.getHeight()-50, Color.black , Color.white);
        }

        // DRAW TIME SCALE
        {
            sequence.getWidth();
            float nbPixFor1Second = 1000f / fftProcessing.xTimeInMs;
            // 0.1 s step.
            float cursor = 0;
            int value = (int)startSecondOffset;
            g.setColor( Color.black );
            while ( cursor < sequence.getWidth() )
            {

                g.drawLine( (int)cursor, 0, (int)cursor, 10 );
                g.drawLine( (int)cursor, sequence.getHeight() - 10, (int)cursor, sequence.getHeight() );
                if ( value/10f == Math.ceil(value/10f) )
                {
                    g.drawLine( (int)cursor, 0, (int)cursor, 60 );
                    g.drawLine( (int)cursor, sequence.getHeight() - 60, (int)cursor, sequence.getHeight() );
                }
                DrawUtil.drawCenteredHint(g, ""+value/10f, (int)cursor, sequence.getHeight() - 40, Color.black, Color.white );
                cursor+= nbPixFor1Second  / 10f;
                value += 1;
            }

        }

        if ( this.proba >= 0 )
        {
            String text ="";
            Color textColor = Color.green;
            if ( proba >= 0.5 )
            {
                text = "VOC";
            }else
            {
                text = "NOISE";
                textColor = Color.black;
            }
            text += " " + ( Math.round( proba * 100 ) / 100f );
            DrawUtil.drawCenteredHint(gAbsolute, text, canvas.getWidth()/2, 0, textColor , Color.white);
        }

/*
         canvas.setBackground( Color.DARK_GRAY );
        Line2D line = new Line2D.Double( 0, mouseY, sequence.getWidth(), mouseY );
        g.setColor( Color.white );
        g.draw( line );
//		double N = bufferSize;
//		double Fs = 300000;
        //double frequency = ( ( sequence.getHeight()-mouseY) * magnitudeBinSize ) * Fs / N; // to reverse y-orientation
        double frequency = ( mouseY * magnitudeBinSize ) * Fs / N; // to reverse y-orientation
        Font smallFont = new Font( "Arial", Font.BOLD, 5 );
        g.setFont( smallFont );

        String text = "Freq: " + (int)frequency + " Hz";
        g.setColor( Color.yellow );
        g.drawString( text, (float)mouseX-40 , (float) mouseY-5 );
*/

        if( currentDrawMode == DrawMode.WEB )
        {
            drawVocs( g, sequence, canvas );
        }


        if( currentDrawMode == DrawMode.DEFAULT )
        {
            drawVocs( g, sequence, canvas );
        }


        if( currentDrawMode == DrawMode.EDIT )
        {
            drawEditor( g , sequence, canvas , MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS );
            drawCancelFrequencies( g, sequence, canvas );
//			drawVoc( g, sequence, canvas );
//			drawCancelFrequencies( g, sequence, canvas );
//			draw80percent( g, sequence, canvas );

            /*
            gAbsolute.setFont( new Font("Arial", Font.BOLD , 10 ) );
            DrawUtil.drawCenteredHint(gAbsolute,
                    "(Del/Suppr) Del Voc - (s) Short - (f/right) Flat\n(up) Upward - (down) Downward - (m) Modulated - (c) Complex\n(u) Unstructured - (h) Harmonics - (o) others - (enter) export and next voc"
            , canvas.getWidth()/2, canvas.getHeight()-130, Color.black , Color.white);
*/
        }

        if( currentDrawMode == DrawMode.ALL )
        {
            drawVocs( g, sequence, canvas );
            drawCancelFrequencies( g, sequence, canvas );
            draw80percent( g, sequence, canvas );
        }

        if( currentDrawMode == DrawMode.CANCEL_FREQUENCIES )
        {
            drawCancelFrequencies( g, sequence, canvas );
        }

//		if( currentDrawMode == DrawMode.PRE_FILTERING )
//		{
//			drawPreFilteringVocs( g, sequence, canvas );
//			draw80percent( g, sequence, canvas );
//		}

        }
    }

    Voc currentSelectedVoc = null;

    private void drawEditor( Graphics2D g, Sequence sequence, IcyCanvas canvas, float MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS )
    {

//		System.out.println("draw editor");
        //System.out.println("mouseX " + mouseX );

        try
        {
            int width = (int) ( MAX_GAP_DURATION_BETWEEN_SEQUENCE_TO_FUSE_VOC_IN_MS / vocList.get( 0 ).xLengthInMs);
            g.setColor( Color.pink );
            g.fillRect( 0 , 0, width, 10 );
            g.fillRect( (int)mouseX-width/2 , (int)mouseY, width, 10 );
        }catch( IndexOutOfBoundsException e )
        {
            // no voc found
        }

        // get Closest voc
        double distance = Double.MAX_VALUE;
        Voc bestVoc = null;
        for ( Voc voc : vocList )
        {
            double d = Math.abs( voc.getCenterX() - mouseX );
            if ( d < distance )
            {
                bestVoc = voc;
                distance = d;
            }
        }

        Voc voc = bestVoc;
        currentSelectedVoc = voc;
        if ( bestVoc == null )
        {
            return;
        }

        // draw all other voc in dark

        for ( Voc vocToShow : vocList )
        {
            if ( voc == vocToShow ) continue;
            drawVoc( g, sequence, vocToShow , Color.gray );
        }

        Color color = Color.orange;

        // draw voc

        g.setColor( Color.white );
        g.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.5f ) );
        g.fillRect( voc.getStartX() - 50 , 0 , voc.getEndX() - voc.getStartX() + 100, sequence.getHeight() );
        g.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 1f ) );

        // draw line fit

        {
            g.setColor( Color.black );
            Line2D line = voc.getFitLine2D( sequence.getHeight() );
            g.setStroke( new BasicStroke( 4 ));
            g.draw( line );

            g.setColor( Color.white );
            g.setStroke( new BasicStroke( 1 ));
            g.draw( line );
        }

        // draw jumps

        if ( voc.jumpList.size() > 0 )
        {
            Stroke dashedBig = new BasicStroke( 3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
            Stroke dashedSmall = new BasicStroke( 1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
            float meanY = sequence.getHeight() - voc.getMeanY();
            for ( Integer x :voc.jumpList )
            {
                g.setColor( Color.black );
                Line2D line = new Line2D.Double( x, meanY-40, x, meanY+40 );
                g.setStroke( dashedBig );
                g.draw( line );

                g.setColor( color );
                g.setStroke( dashedSmall );
                g.draw( line );
            }
        }

        // draw modulated changes

        g.setStroke( new BasicStroke( 1f ) );
        g.setColor( color );
        if ( voc.modulationList.size() > 0 )
        {
            float meanY = sequence.getHeight() - voc.getMeanY();
            for ( Integer x :voc.modulationList )
            {
                Line2D line = new Line2D.Double( x, meanY-100, x, meanY+100 );
                g.draw( line );
            }
        }

        // draw signal

        {
            drawVoc( sequence, g, voc );
//			ArrayList<Polygon> polyList = voc.getPolygon( sequence.getHeight() );
//			for ( Polygon poly : polyList )
//			{
//				drawPoly( g , poly , color );
//			}
        }

        // draw harmonics
        {
            drawHarmonics(sequence, g, voc);
//			Polygon poly = voc.getPolygonHarmonics( sequence.getHeight() );
//			drawPoly( g , poly , color.darker() );
        }

        g.setFont( classificationFont );
        int locY = 150;
        DrawUtil.drawCenteredHint(g, voc.getDurationInMs() + " ms"+
                "\nDynamic:" + (int)voc.getFrequencyDynamicInHz() + " Hz"
                + "\nLinearity: "+ String.format("%.3g%n", voc.linearityIndex )
                + "Modulation index: "+ voc.nbModulation
                + "\nnb pt Harmonics: " + voc.pointListHarmonics.size()
                + "\n power: " + voc.meanPower
                + "\n freq TV: " + voc.getFrequencyTVInHz()

                + "\n\n#"+vocList.indexOf( voc), voc.getCenterX(), locY, color, Color.black );

        {
            int y = 350;
            for ( String string : voc.getClassificationDescription() )
            {
                DrawUtil.drawCenteredHint(g, string, voc.getCenterX(), y, color, Color.black );
                y+=20;
            }
        }

        // draw frequency canceller profile
        /*
        {
            float displayMagnification = 50;
            int x = -100;
            {
                Polygon profile = new Polygon();
                ArrayList<Double> valueLineList = new ArrayList<Double>();

                profile.addPoint( 0, 512 );

                for ( int yy = Constant.MIN_Y_IN_SPECTRUM ; yy < Constant.MAX_Y_IN_SPECTRUM ; yy++ )
                {
                    double value = frequencyCanceler.valuesDif[ yy-Constant.MIN_Y_IN_SPECTRUM ];
                    profile.addPoint( (int)(x + value*displayMagnification) , 512-yy );
                    valueLineList.add( value );
                }
                profile.addPoint( 0, 0 );

                double[] valueLineArray = new double[valueLineList.size()];
                for ( int i = 0 ; i < valueLineList.size(); i++ )
                {
                    valueLineArray[i] = valueLineList.get( i );
                }

                double stddev = MathUtil.stddev( valueLineArray );
                double mean = MathUtil.mean( valueLineArray );
                g.setColor( Color.black );
                g.draw( profile );

                g.drawLine( (int)(x), 0, (int)(x), 512 );

                if ( stddev > Constant.MIN_STD_FOR_VERTICAL_DETECTION )
                {
                    g.setColor( Color.green );
                }else
                {
                    g.setColor( Color.red );
                }
                int stdDisplayX =(int)(x+mean+stddev*3d*displayMagnification);
                g.drawLine( stdDisplayX, 0, stdDisplayX , 512 );
            }
        }*/

        /*
        if ( imagePoint != null ) // draw profile
        {
            float displayMagnification = 50;
            int x = (int) imagePoint.getX();
            int y = (int) ( sequence.getHeight()-imagePoint.getY() );
            if ( x > 0 && x < sequence.getWidth() && y > 0 && y < sequence.getHeight() )
            {
                Polygon profile = new Polygon();
                ArrayList<Double> valueLineList = new ArrayList<Double>();

                profile.addPoint( 0, 512 );
                //for ( int yy = (int)( sequence.getHeight()*0.3 ) ; yy < (int)(sequence.getHeight()*0.8) ; yy++ )
                for ( int yy = Constant.MIN_Y_IN_SPECTRUM ; yy < Constant.MAX_Y_IN_SPECTRUM ; yy++ )
                {
                    double value = fftProcessing.getMagnitudeDenoised( 0 )[x][yy];
                    profile.addPoint( (int)(x + value*displayMagnification) , 512-yy );
                    valueLineList.add( value );
                }
                profile.addPoint( 0, 0 );

                double[] valueLineArray = new double[valueLineList.size()];
                for ( int i = 0 ; i < valueLineList.size(); i++ )
                {
                    valueLineArray[i] = valueLineList.get( i );
                }

                double stddev = MathUtil.stddev( valueLineArray );
                double mean = MathUtil.mean( valueLineArray );
                g.setColor( Color.black );
                g.draw( profile );

                g.drawLine( (int)(x+mean*displayMagnification), 0, (int)(x+mean*displayMagnification), 512 );

                if ( stddev > Constant.MIN_STD_FOR_VERTICAL_DETECTION )
                {
                    g.setColor( Color.green );
                }else
                {
                    g.setColor( Color.red );
                }
                int stdDisplayX =(int)(x+mean+stddev*Constant.STD_MULTIPLICATOR_FOR_DETECTION*displayMagnification);
                g.drawLine( stdDisplayX, 0, stdDisplayX , 512 );

            }
        }

        if ( imagePoint != null )
        {
            int x = (int) imagePoint.getX();
            int y = (int) ( sequence.getHeight()-imagePoint.getY() );

            if ( x > 0 && x < sequence.getWidth() && y > 0 && y < sequence.getHeight() )
            {
                // get line info (noise frequency canceller)
                ArrayList<Double> valueLineList = new ArrayList<Double>();
//				System.out.println("x:" + x );
                for ( int xx = -146 ; xx < 146 ; xx++ )
                {
                    if ( x+xx < 0 ) continue;
                    if ( x+xx >= sequence.getWidth() ) continue;
                    valueLineList.add( fftProcessing.getMagnitudeDenoised( 0 )[x+xx][y] );
                }

                double[] valueLineArray = new double[valueLineList.size()];
                for ( int i = 0 ; i < valueLineList.size(); i++ )
                {
                    valueLineArray[i] = valueLineList.get( i );
                }

                double stddev = MathUtil.stddev( fftProcessing.getMagnitudeDenoised( 0 )[x] );
                double mean = MathUtil.mean( fftProcessing.getMagnitudeDenoised( 0 )[x] );
                double sum = MathUtil.sum( fftProcessing.getMagnitudeDenoised( 0 )[x] );

                double stddevLine = MathUtil.stddev( valueLineArray );
                double meanLine = MathUtil.mean( valueLineArray );
                double sumLine = MathUtil.sum( valueLineArray );

                String string ="v-stdev (denoised):"+stddev
                        +"\nv-mean (denoised):"+mean
                        +"\nv-sum (denoised):"+sum
                        +"\nline-std (denoised):"+stddevLine
                        +"\nline-mean (denoised):"+meanLine
                        +"\nline-sum (denoised):"+sumLine;

                DrawUtil.drawCenteredHint(g, string, x, sequence.getHeight()-50, Color.pink, Color.black );
                y+=20;
            }
        }
        */


    }

    private void drawVoc( Graphics2D g, Sequence sequence, Voc voc, Color color) {

        // draw line fit

        {
            drawVoc( sequence, g, voc);
//			ArrayList<Polygon> polyList = voc.getPolygon( sequence.getHeight() );
//			for ( Polygon poly : polyList )
//			{
//				drawPoly( g , poly , color );
//			}
        }

        // draw harmonics
        {
            drawHarmonics( sequence, g, voc);
//			ArrayList<Polygon> polyList = voc.getPolygonHarmonics( sequence.getHeight() );
//			for ( Polygon poly : polyList )
//			{
//				drawPoly( g , poly , color.darker() );
//			}
        }


    }

    private void draw80percent(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
        Stroke dashed = new BasicStroke(
                3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{100}, 0);
        g.setStroke( dashed );
        g.setColor( Color.black );
        int h = (int) (sequence.getHeight() * 0.8);
        g.drawLine( 0, h, sequence.getWidth(), h );
        h = (int) (sequence.getHeight() * 0.2);
        g.drawLine( 0, h, sequence.getWidth(), h );

    }

    private void drawCancelFrequencies(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

        for ( FrequencyCancel frequencyCancel : this.frequencyCanceler.getCancelList() )
        {
            int cf = sequence.getHeight() -1 - frequencyCancel.y;
            g.setStroke( new BasicStroke( 3 ) );
            g.setColor( Color.white );
            //g.drawLine( frequencyCancel.startX, cf, frequencyCancel.endX, cf );
            g.drawLine( frequencyCancel.startX, cf, 100, cf );
            g.setStroke( new BasicStroke( 2 ) );
            g.setColor( Color.black );
            //g.drawLine( frequencyCancel.startX, cf, frequencyCancel.endX, cf );
            g.drawLine( frequencyCancel.startX, cf, 100, cf );

//			String mean = String.format("%.2g%n", frequencyCancel.mean );
//			String stdev = String.format("%.2g%n", frequencyCancel.stdev );
//			g.drawString("m:"+ mean+ " / dv:" + stdev, frequencyCancel.startX, cf );
        }

    }

    /*
    private void drawPreFilteringVocs( Graphics2D g, Sequence sequence, IcyCanvas canvas )
    {
        for ( Voc voc : vocPreFilteredList )
        {
            {
                drawVoc( sequence, g, voc );
            }

            // draw harmonics
            {
                drawHarmonics( sequence, g, voc );
            }

        }


    }
    */

    Font classificationFont = new Font("Arial", Font.BOLD, 24 );
    private void drawVocs( Graphics2D g, Sequence sequence, IcyCanvas canvas )
    {

        synchronized ( vocList ) {

            for ( Voc voc : vocList )
            {

                // draw line fit
            	if ( currentDrawMode != DrawMode.WEB )
                {
                    g.setColor( Color.black );
                    Line2D line = voc.getFitLine2D( sequence.getHeight() );
                    g.setStroke( new BasicStroke( 4 ));
                    g.draw( line );

                    g.setColor( Color.white );
                    g.setStroke( new BasicStroke( 1 ));
                    g.draw( line );
                }

                // draw jumps

                if ( voc.jumpList.size() > 0 )
                {
                    Stroke dashedBig = new BasicStroke( 3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
                    Stroke dashedSmall = new BasicStroke( 1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
                    float meanY = sequence.getHeight() - voc.getMeanY();
                    for ( Integer x :voc.jumpList )
                    {
                        g.setColor( Color.black );
                        Line2D line = new Line2D.Double( x, meanY-40, x, meanY+40 );
                        g.setStroke( dashedBig );
                        g.draw( line );

                        g.setColor( voc.color );
                        g.setStroke( dashedSmall );
                        g.draw( line );
                    }
                }

                // draw modulated changes

                g.setStroke( new BasicStroke( 1f ) );
                g.setColor( voc.color );
                if ( voc.modulationList.size() > 0 )
                {
                    float meanY = sequence.getHeight() - voc.getMeanY();
                    for ( Integer x :voc.modulationList )
                    {
                        Line2D line = new Line2D.Double( x, meanY-100, x, meanY+100 );
                        g.draw( line );
                    }
                }

                // draw signal

                {
                    drawVoc( sequence, g, voc );
                }

                // draw harmonics
                {
                    drawHarmonics( sequence, g, voc );
                }

                int y = 0;
                g.setFont( classificationFont );
                int locY = 400 ; //+ ( vocList.indexOf( voc ) * 75 ) % 100;
                DrawUtil.drawCenteredHint(g, //voc.getDurationInMs() + " ms"
//                        "\nDynamic:" + (int)voc.getFrequencyDynamicInHz() + " Hz"
//                        + "\nLinearity: "+ String.format("%.3g%n", voc.linearityIndex )
//                        "Modulation index: "+ voc.nbModulation
//                        + "\nnb pt Harmonics: " + voc.pointListHarmonics.size()
//                        + "\nnb power: " + voc.meanPower

                        "# "+vocList.indexOf( voc), voc.getCenterX(), locY, voc.color, Color.black );

                for ( String string : voc.getClassificationDescription() )
                {
                    DrawUtil.drawCenteredHint(g, string, voc.getCenterX(), y, voc.color, Color.black );
                    y+=40;
                }

            }
        }

    }

    private void drawVoc( Sequence sequence, Graphics2D g, Voc voc) {

        ArrayList<Polygon> polyList = voc.getPolygon( sequence.getHeight() );
        for ( Polygon poly : polyList )
        {
            drawPoly( g , poly , voc.color );
        }

        Rectangle2D rectangle = voc.getBoundingRectangle( sequence.getHeight() , true , 5 );

        if ( voc.getMeanFrequencyInHz() < 50000 )
        {
        	g.setColor( Color.black );
        	g.drawString("mFreq<50K", (float)rectangle.getMinX(), (float)rectangle.getMinY() );
        }

        g.setColor( voc.color );
        if ( voc.isInBadRepeat )
        {
        	g.drawLine( (int)rectangle.getMinX(), (int)rectangle.getMinY(), (int)rectangle.getMaxX(), (int)rectangle.getMaxY() );
        	g.drawLine( (int)rectangle.getMaxX(), (int)rectangle.getMinY(), (int)rectangle.getMinX(), (int)rectangle.getMaxY() );
        }
        g.draw( rectangle );

    }

    private void drawHarmonics( Sequence sequence, Graphics2D g, Voc voc) {
        ArrayList<Polygon> polyList = voc.getPolygonHarmonics( sequence.getHeight() );
        for ( Polygon poly : polyList )
        {
            drawPoly( g , poly , voc.color.darker() );
        }
    }

    private void drawPoly(Graphics2D g, Polygon poly , Color color ) {

        if ( poly.npoints < 2 )
        {
            return;
        }
        g.setStroke( new BasicStroke( 3 ) );
        g.setColor( Color.black );
        g.drawPolyline( poly.xpoints, poly.ypoints, poly.npoints );
//		g.drawPolygon( poly );
        g.setStroke( new BasicStroke( 2 ) );
        g.setColor( color );
        g.drawPolyline( poly.xpoints, poly.ypoints, poly.npoints );
//		g.drawPolygon( poly );

    }

    @Override
    public void keyPressed(KeyEvent e, icy.type.point.Point5D.Double imagePoint, IcyCanvas canvas) {

        synchronized ( vocList ) {

        if ( e.getKeyChar() == 'd' )
        {
            System.out.println("d typed");
            int index = ( (currentDrawMode.ordinal()+1) % ( DrawMode.values().length ) );
            currentDrawMode = DrawMode.values()[ index ];
            painterChanged();
            e.consume();
        }

        if( currentDrawMode == DrawMode.EDIT )
        {
            if ( e.getKeyCode()==KeyEvent.VK_DELETE || e.getKeyCode()==KeyEvent.VK_BACK_SPACE )
            {
                synchronized ( vocList ) {
                    vocList.remove( currentSelectedVoc );
                }
            }

            if ( e.getKeyChar() == 's')
            {
                boolean result = currentSelectedVoc.switchClassificationDescription( "Short" );
                if ( result )
                {
                    currentSelectedVoc.removeClassificationDescription("Flat");
                    currentSelectedVoc.removeClassificationDescription("Upward");
                    currentSelectedVoc.removeClassificationDescription("Downward");
                    currentSelectedVoc.removeClassificationDescription("Complex");
                    currentSelectedVoc.removeClassificationDescription("Unstructured");
                    currentSelectedVoc.removeClassificationDescription("Harmonics");
                }
            }

            if ( e.getKeyChar() == 'f' || e.getKeyCode() == KeyEvent.VK_RIGHT )
            {
                boolean result = currentSelectedVoc.switchClassificationDescription( "Flat" );
                if ( result )
                {
                    currentSelectedVoc.removeClassificationDescription("Upward");
                    currentSelectedVoc.removeClassificationDescription("Downward");
                    currentSelectedVoc.removeClassificationDescription("Short");
                }
            }

            if ( e.getKeyCode() == KeyEvent.VK_UP )
            {
                boolean result = currentSelectedVoc.switchClassificationDescription( "Upward" );
                if ( result )
                {
                    currentSelectedVoc.removeClassificationDescription("Flat");
                    currentSelectedVoc.removeClassificationDescription("Downward");
                    currentSelectedVoc.removeClassificationDescription("Short");
                }
            }

            if ( e.getKeyCode() == KeyEvent.VK_DOWN )
            {
                boolean result = currentSelectedVoc.switchClassificationDescription( "Downward" );
                if ( result )
                {
                    currentSelectedVoc.removeClassificationDescription("Flat");
                    currentSelectedVoc.removeClassificationDescription("Upward");
                    currentSelectedVoc.removeClassificationDescription("Short");
                }
            }

            if ( e.getKeyChar() == 'm' )
            {
                currentSelectedVoc.switchClassificationDescription( "Modulated" );
            }

            if ( e.getKeyChar() == 'c' )
            {
                currentSelectedVoc.switchClassificationDescription( "Complex" );
            }

            if ( e.getKeyChar() == 'u' )
            {
                currentSelectedVoc.switchClassificationDescription( "Unstructured" );
            }

            if ( e.getKeyChar() == '+' )
            {
                // add jump
            }

            if ( e.getKeyChar() == '-' )
            {
                // remove jump
            }

            if ( e.getKeyChar() == 'h' )
            {
                currentSelectedVoc.switchClassificationDescription( "Harmonics" );
            }

            if ( e.getKeyChar() == 'o' )
            {
                currentSelectedVoc.switchClassificationDescription( "Others" );
            }

            /*
            if ( e.getKeyCode() == KeyEvent.VK_ENTER )
            {

                // Export and proceed to next event.
                VocalisationLabelExporter.export( "vocLog.txt" , audioFile , vocList );
                canvas.getSequence().close();
            }
            */



            painterChanged();
        }
        }

    }
/*
    @Override
    public void mouseMove(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

//		System.out.println( imagePoint );
        mouseX = imagePoint.getX();
        mouseY = imagePoint.getY();

//		canvas.getSequence().painterChanged( this );
//		this.painterChanged();

        painterChanged();



    }
    */


}
