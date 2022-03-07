package plugins.fab.azure.kinect;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;

import com.sun.j3d.utils.universe.*;

import icy.gui.frame.IcyFrame;

import java.awt.image.BufferedImage;
import javax.media.j3d.*;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

public final class Rendering3D extends JPanel {


    int s = 0;
    BranchGroup scene = null;
    SimpleUniverse simpleU = null;
    
    public Rendering3D() {
    	        
        setLayout(new BorderLayout());
        GraphicsConfiguration gc=SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas3D = new Canvas3D(gc);
        add("Center", canvas3D);

        Point3f[] plaPts = new Point3f[4];
        
        int count = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j <2; j++) {
                plaPts[count] = new Point3f(i/10.0f,j/10.0f,0);
                count++;
            }
        }

        scene = createSceneGraph( plaPts );
        scene.compile();

        // SimpleUniverse is a Convenience Utility class
        simpleU = new SimpleUniverse(canvas3D);


        // This moves the ViewPlatform back a bit so the
        // objects in the scene can be viewed.
        simpleU.getViewingPlatform().setNominalViewingTransform();

        simpleU.addBranchGraph(scene);
        
        
    }
    
    public void setPoints( Point3f[] pointArray )
    {
    	//System.out.println("todo Refresh points 3D");
    	// refresh points
    	
    	//scene.removeAllChildren();
    	Point3f[] points = new Point3f[4];

    	int count=0;
    	for (int i = 0; i < 2; i++) {
            for (int j = 0; j <2; j++) {
                //System.out.println(count);
            	points[count] = new Point3f(i+(float)Math.random() /10.0f,j/10.0f,0);                
                count++;
            }
        }
        
    	// 
    	
        scene = createSceneGraph( points );
        scene.compile();        
        simpleU.addBranchGraph( scene );
    	
    }
    
    public BranchGroup createSceneGraph( Point3f[] plaPts ) {
        BranchGroup lineGroup = new BranchGroup();
        Appearance app = new Appearance();
        ColoringAttributes ca = new ColoringAttributes(new Color3f(204.0f, 204.0f, 204.0f), ColoringAttributes.SHADE_FLAT);
        app.setColoringAttributes(ca);

        PointArray pla = new PointArray(4, GeometryArray.COORDINATES);

        pla.setCoordinates(0, plaPts);

        PointAttributes a_point_just_bigger=new PointAttributes();
        a_point_just_bigger.setPointSize(10.0f);//10 pixel-wide point
        a_point_just_bigger.setPointAntialiasingEnable(true);//now points are sphere-like(not a cube)
        app.setPointAttributes(a_point_just_bigger);
        Shape3D plShape = new Shape3D(pla, app);
        TransformGroup objRotate = new TransformGroup();
        objRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objRotate.addChild(plShape);
        
        lineGroup.addChild(objRotate);
        return lineGroup;
    }

}