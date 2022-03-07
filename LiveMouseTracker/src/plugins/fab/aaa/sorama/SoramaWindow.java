package plugins.fab.aaa.sorama;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class SoramaWindow {

	SoramaTest01 sorama = null;
	public TestPane pane = null;
	static public JFrame frame = null;
	
    public SoramaWindow( SoramaTest01 sorama ) {
    	this.sorama = sorama;
    	JFrame frame = new JFrame("Testing");
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	frame.setLayout(new BorderLayout());
    	this.pane = new TestPane();
    	
    	frame.add( pane );
    	frame.pack();
    	frame.setLocationRelativeTo(null);
    	frame.setVisible(true);
    	SoramaWindow.frame = frame;
    	/*
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                }

            }
        });
        */
    }

    public void refresh()
    {
    	 EventQueue.invokeLater(new Runnable() {

			@Override
             public void run() {
                 
                	 SoramaWindow.frame.revalidate();
                     
                 
             }
         });
    }
    public class TestPane extends JPanel {

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(48*20, 36*20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            float max = 0;
            for ( float v : SoramaWindow.this.sorama.soundSurfaceData )
            {
            	if ( v > max ) max = v;
            }
            
            int i = 0;
            for ( int y = 0 ; y < 36 ; y++ )
            {
            	for ( int x = 0 ; x < 48 ; x++ )
            	{
            		g.setColor(Color.BLACK);
            		//System.out.println( SoramaWindow.this.sorama.soundSurfaceData.length );
            		float value = SoramaWindow.this.sorama.soundSurfaceData[i];
            		
            		if (value == max)
            		{
            			g.setColor(Color.RED);
            		}
            		
            		g.fillOval(x*20, y*20, (int)(value ), (int)(value ));
            		i++;
            	}
            }
            g.setColor(Color.RED);
            g.setFont( new Font( "Arial" , Font.BOLD , 50 ));
            g.drawString("Max: " + max, 50 , 50 );


        }
    }
}
