package plugins.fab.azure.kinect;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

public class Channel3DStef
{
    ReentrantLock lock = new ReentrantLock();

    public short[] pointArray = null;
    public short[] colorArray = null;

    Point3f[] point3dArray = null;

    public Color color = Color.BLACK;
    public boolean enabled = false;

    Point3f translation = new Point3f(0, 0, 0);
    Matrix4d matrix = new Matrix4d();

    float xRot = 0;
    float yRot = 0;
    float zRot = 0;

    public int number;

    public Channel3DStef(int number)
    {
        this.number = number;
        this.computeMatrix();
    }

    public void setPoints(short[] newPointArray, short[] newColorArray)
    {
        

        lock.lock();
        try
        {
            pointArray = newPointArray;
            colorArray = newColorArray;

            // need to re-allocate point3dArray ?
            if ((point3dArray == null) || ((point3dArray.length * 3) != newPointArray.length))
            {
                final int len = newPointArray.length / 3;

                // re-allocate
                point3dArray = new Point3f[len];
                for (int i = 0; i < len; i++)
                    point3dArray[i] = new Point3f();
            }
        }
        finally
        {
            lock.unlock();
        }

        
    }

    public void computeMatrix()
    {
        final Matrix4d mul = new Matrix4d();

        matrix.setIdentity();

        mul.setIdentity();
        mul.setTranslation(new Vector3d(translation));
        matrix.mul(mul);

        mul.setIdentity();
        mul.rotY(yRot * (Math.PI / 180d));
        matrix.mul(mul);
        mul.setIdentity();
        mul.rotX(xRot * (Math.PI / 180d));
        matrix.mul(mul);
        mul.setIdentity();
        mul.rotZ(zRot * (Math.PI / 180d));
        matrix.mul(mul);
    }

    public void convertToPoint()
    {
        

        // convert to point3f
        lock.lock();
        try
        {
            if (this.pointArray == null)
            {
                System.out.println("No data on channel " + this.number + " (pointArray is null)");
                return;
            }

            int indDst = 0;
            for (int i = 0; i < this.pointArray.length; i += 3)
            {
                point3dArray[indDst].set(pointArray[i + 0], pointArray[i + 1], pointArray[i + 2]);
                indDst++;
            }
        }
        finally
        {
            this.lock.unlock();
        }

        
    }

    public void transformPoints()
    {
        

        for (Point3f p : point3dArray) // transform
        {
            matrix.transform(p);
        }

        
    }

    // public void translatePoints()
    // {
    // final ChronometerStef c = new ChronometerStef("translatePoints");
    //
    // for (Point3f p : point3dArray) // transform
    // {
    // p.x += this.translation.x;
    // p.y += this.translation.y;
    // p.z += this.translation.z;
    // }
    //
    // c.displayMs();
    // }

    public double getZstd()
    {
        

        double sum = 0.0;
        double standardDeviation = 0.0;
        int length = point3dArray.length;

        for (Point3f p : point3dArray)
        {
            sum += p.z;
        }

        double mean = sum / length;

        for (Point3f p : point3dArray)
        {
            standardDeviation += Math.pow(p.z - mean, 2);
        }

        

        return Math.sqrt(standardDeviation / length);
    }

    private List<Point3f> getHigherPoints(int nbPoints)
    {
        

        List<Point3f> resultList = new ArrayList<Point3f>();

        for (Point3f p : point3dArray)
        {
            if (resultList.size() < nbPoints)
            {
                resultList.add(p);
                continue;
            }
            Point3f pointToReplace = null;
            for (Point3f pInResult : resultList)
            {
                if (pInResult.z > p.z) // keep lower values
                {
                    pointToReplace = pInResult;
                }
            }
            if (pointToReplace != null)
            {
                resultList.remove(pointToReplace);
                resultList.add(p);
            }
        }

        

        return resultList;
    }

    public void performAutoTranslate(Channel3DStef channel3d)
    {

        // pick the lower (higher in scene) z pixels to register

        Channel3DStef refChannel = channel3d;
        refChannel.computeMatrix();
        refChannel.convertToPoint();
        refChannel.transformPoints();
        // refChannel.translatePoints();
        List<Point3f> refPointList = refChannel.getHigherPoints(100);

        Channel3DStef movingChannel = this;
        movingChannel.computeMatrix();
        movingChannel.convertToPoint();
        movingChannel.transformPoints();
        // movingChannel.translatePoints();
        List<Point3f> movingPointList = movingChannel.getHigherPoints(100);
    }
}
