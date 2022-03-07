package plugins.fab.azure.kinect;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.math.FPSMeter;
import icy.sequence.Sequence;
import icy.type.DataType;

public final class Rendering3DStef
{
    final Channel3DStef[] channelArray = new Channel3DStef[10];

    float scale = 0.6f;
    float xOffset = 500;
    float yOffset = 500;
    float xRot = 0;
    float yRot = 0;

    boolean performCalibration = false;
    boolean performAutoTranslate = false;

    int skipper = 1;

    int width = 512;
    int height = 424;
    IcyBufferedImage infraImage = new IcyBufferedImage(width, height, 1, DataType.USHORT); // short faster ?
    IcyBufferedImage depthImage = new IcyBufferedImage(width, height, 1, DataType.USHORT); // short faster ?

    short[] renderBuffer;
    short[] depthBuffer;
    float[] zBuffer = new float[width * height];

    Point3f mainTranslate = new Point3f(-404, -490, 0);
    Matrix4d matrix = new Matrix4d();

    Viewer viewer = null;
    Sequence infraSequence = new Sequence("3D infra");
    Sequence depthSequence = new Sequence("3D depth map");

    Sequence depthSequenceRecorded = new Sequence("3D depth map recorded");

    Overlay3DStef overlay3D = null;
    FPSMeter fpsMeter = new FPSMeter();

    double renderTimeNs = 0;
    double renderTimeMs = 0;
    public int zClipFar = 1000;
    public int zClipClose = 510;
    public int imageToRecord = 0;

    public Rendering3DStef()
    {
        infraImage.setData(0, 0, 0, 4000);// for lut
        infraImage.setData(0, 1, 0, 0); // for lut
        infraSequence.setImage(0, 0, infraImage);

        depthImage.setData(0, 0, 0, 300);// for lut
        depthImage.setData(0, 0, 0, 500); // for lut
        depthSequence.setImage(0, 0, depthImage);

        Icy.addSequence(infraSequence);
        Icy.addSequence(depthSequence);

        for (int i = 0; i < channelArray.length; i++)
        {
            channelArray[i] = new Channel3DStef(i);
        }
        channelArray[0].color = Color.BLACK;
        channelArray[1].color = Color.GREEN;
        channelArray[2].color = Color.BLUE;

        channelArray[0].translation = new Point3f(0, 257, 0);
        channelArray[1].translation = new Point3f(-16, -279, 23); // short distance config

        channelArray[0].zRot = 0;
        channelArray[0].yRot = 0;
        channelArray[0].xRot = 17;

        channelArray[1].zRot = 180;
        channelArray[1].yRot = -4;
        channelArray[1].xRot = -24;

        channelArray[1].computeMatrix();

        channelArray[0].enabled = true;
        channelArray[1].enabled = true;
        channelArray[2].enabled = false;

        overlay3D = new Overlay3DStef(this);

        infraSequence.addOverlay(overlay3D);
        depthSequence.addOverlay(overlay3D);

    }

    public void recordDepthMap()
    {
        depthSequenceRecorded.removeAllImages();
        Icy.addSequence(depthSequenceRecorded);
        imageToRecord = 100;
    }

    public void render()
    {
        

        // update buffer
        renderBuffer = (short[]) infraImage.getDataXY(0);
        depthBuffer = (short[]) depthImage.getDataXY(0);

        // clear rendering and depth image
        Arrays.fill(renderBuffer, (short) 0);
        Arrays.fill(depthBuffer, (short) 0);

        int FUSION_MODE = 4;

        // init z buffer
        if (FUSION_MODE == 4)
        {
            Arrays.fill(zBuffer, 0f);
        }
        else
        {
            Arrays.fill(zBuffer, 100000f);
        }

        

        double meanZ = 0;

        if (performCalibration)
        {
            

            float bestAngle = 0;
            double minSTD = Double.MAX_VALUE;
            for (Channel3DStef channel : channelArray)
            {
                for (float angle = -45; angle < 45; angle += 1)
                {
                    channel.xRot = angle;
                    channel.computeMatrix();
                    channel.convertToPoint();
                    channel.transformPoints();
                    double std = channel.getZstd();
                    if (std < minSTD)
                    {
                        bestAngle = angle;
                        minSTD = std;
                    }
                }
                channel.xRot = bestAngle;
                System.out.println("Channel " + channel.number);
                System.out.println("Best angle : " + bestAngle);

            }

            
            performCalibration = false;
        }

        if (performAutoTranslate) // perform autoTranslation of the channel 1 ( 0 is not moving )
        {
            channelArray[1].performAutoTranslate(channelArray[0]);
        }

        meanZ = 561;
        // compute world matrix for once
        computeWorldMatrix(meanZ);

        for (Channel3DStef channel : channelArray)
        {
            if (!channel.enabled)
            {
                continue;
            }

            if (channel.pointArray == null)
            {
                continue; // stop rendering channel
            }

            
            boolean alternate = true;

            if (alternate)
            {
                computeWorldMatrix(meanZ);
                channel.convertToPoint();
                channel.computeMatrix();

                matrix.mul(channel.matrix);

                // then do world points transformation
                for (Point3f p : channel.point3dArray) // transform
                {
                    if (p.z < zClipClose || p.z > zClipFar) // z clip
                    {
                        p.z = 0; // set to out of view.
                        p.x = -100000;
                        p.y = 0;
                    }
                    else
                    {
                        // global transform
                        matrix.transform(p);
                    }
                }
            }
            else
            {
                // do channel local points transformation
                channel.convertToPoint();
                channel.computeMatrix();
                channel.transformPoints();

                // then do world points transformation
                for (Point3f p : channel.point3dArray) // transform
                {
                    if (p.z < zClipClose || p.z > zClipFar) // z clip
                    {
                        p.z = 0; // set to out of view.
                        p.x = -100000;
                        p.y = 0;
                    }
                    else
                    {
                        // global transform
                        matrix.transform(p);
                    }
                }
            }

            

            

            final int pointSize = 2;
            final Rectangle2D.Float bounds = new Rectangle2D.Float(0, 0, width - pointSize, height - pointSize);

            for (int i = 0; i < channel.point3dArray.length; i += skipper)
            {
                short val = channel.colorArray[i];

                if (val > 5000)
                {
                    // val = 1;
                    continue;
                }
                if (val < 0)
                {
                    val = 0;
                }

                final Point3f p = channel.point3dArray[i];

                // not in image bounds --> pass to next point
                if (!bounds.contains(p.x, p.y))
                    continue;

                drawPoint2x2(p, val, FUSION_MODE);
                // drawPoint(p, val, pointSize, FUSION_MODE);
            }

            
        }

        fpsMeter.update();

        // if (TestRendering3DStef.profile)
        // System.out.println("FPS: " + fpsMeter.getFPS());

        infraImage.dataChanged();
        depthImage.dataChanged();

        if (imageToRecord > 0)
        {
            depthSequenceRecorded.addImage(depthSequenceRecorded.getSizeT(), depthImage.getCopy());
            imageToRecord--;
            if (imageToRecord < 0)
            {
                imageToRecord = 0;
            }
        }
    }

    private void drawPoint(Point3f pt, short val, int pointSize, int fusion)
    {
        final float z = pt.z;

        int offset = (int) pt.x + ((int) pt.y * width);
        for (int yy = 0; yy < pointSize; yy++)
        {
            for (int xx = 0; xx < pointSize; xx++)
            {
                switch (fusion)
                {
                    default:
                    case 1: // z-buffer classique
                        if (z < zBuffer[offset]) // check z buffer
                        {
                            renderBuffer[offset] = val;
                            depthBuffer[offset] = (short) z;
                            zBuffer[offset] = z;
                        }
                        break;

                    case 2: // maxI or maxZ
                        if (z < zBuffer[offset] || val > renderBuffer[offset]) // check z buffer or higher intensity
                        {
                            renderBuffer[offset] = val;
                            depthBuffer[offset] = (short) z;
                            zBuffer[offset] = z;
                        }
                        break;

                    case 3: // maxI
                        if (val > renderBuffer[offset]) // check z buffer or higher intensity
                        {
                            renderBuffer[offset] = val;
                            depthBuffer[offset] = (short) z;
                        }
                        break;

                    case 4: // maxZ
                        /*
                         * if ( p.z > zBuffer[offset] ) // check z buffer or higher intensity
                         * {
                         * renderBuffer[offset] = (short)val;
                         * depthBuffer[offset] = (short)(p.z);
                         * zBuffer[offset] = p.z;
                         * }
                         */
                        if (z > zBuffer[offset]) // check z buffer or higher intensity
                        {
                            if (val > renderBuffer[offset]) // keep brightest
                            {
                                renderBuffer[offset] = val;
                            }

                            depthBuffer[offset] = (short) z;
                            zBuffer[offset] = z;
                        }
                        break;
                }

                offset++;
            }

            offset += width - pointSize;
        }
    }

    private void writePixel2x2(short[] buffer, short val, int offset, int width)
    {
        buffer[offset + 0] = val;
        buffer[offset + 1] = val;
        buffer[offset + width + 0] = val;
        buffer[offset + width + 1] = val;
    }

    private void drawPoint2x2(Point3f pt, short val, int fusion)
    {
        final int w = width;
        final float z = pt.z;
        final int offset = (int) pt.x + ((int) pt.y * w);

        switch (fusion)
        {
            default:
            case 1: // z-buffer classique
                if (z < zBuffer[offset]) // check z buffer
                {
                    writePixel2x2(renderBuffer, val, offset, w);
                    writePixel2x2(depthBuffer, (short) z, offset, w);
                    zBuffer[offset] = z;
                }
                break;

            case 2: // maxI or maxZ
                if (z < zBuffer[offset] || val > renderBuffer[offset]) // check z buffer or higher intensity
                {
                    writePixel2x2(renderBuffer, val, offset, w);
                    writePixel2x2(depthBuffer, (short) z, offset, w);
                    zBuffer[offset] = z;
                }
                break;

            case 3: // maxI
                if (val > renderBuffer[offset]) // check z buffer or higher intensity
                {
                    writePixel2x2(renderBuffer, val, offset, w);
                    writePixel2x2(depthBuffer, (short) z, offset, w);
                }
                break;

            case 4: // maxZ
                /*
                 * if ( p.z > zBuffer[offset] ) // check z buffer or higher intensity
                 * {
                 * writePixel2x2(renderBuffer, val, offset, w);
                 * writePixel2x2(depthBuffer, (short) z, offset, w);
                 * zBuffer[offset] = p.z;
                 * }
                 */
                if (z > zBuffer[offset]) // check z buffer or higher intensity
                {
                    if (val > renderBuffer[offset]) // keep brightest
                    {
                        writePixel2x2(renderBuffer, val, offset, w);
                    }

                    writePixel2x2(depthBuffer, (short) z, offset, w);
                    zBuffer[offset] = z;
                }
                break;
        }
    }

    public void computeWorldMatrix(double meanZ)
    {
        final Matrix4d mul = new Matrix4d();

        matrix.setIdentity();

        // offset translation
        mul.setIdentity();
        mul.setTranslation(new Vector3d(xOffset, yOffset, 0d));
        matrix.mul(mul);

        // scale
        mul.setIdentity();
        mul.setScale(scale);
        matrix.mul(mul);

        // Z mean origin translation
        mul.setIdentity();
        mul.setTranslation(new Vector3d(0d, 0d, meanZ));
        matrix.mul(mul);

        // then do Y and X rotation
        mul.setIdentity();
        mul.rotY(yRot * (Math.PI / 180d));
        matrix.mul(mul);
        mul.setIdentity();
        mul.rotX(xRot * (Math.PI / 180d));
        matrix.mul(mul);

        // put it back to its Z position
        mul.setIdentity();
        mul.setTranslation(new Vector3d(0d, 0d, -meanZ));
        matrix.mul(mul);

        // and finally apply world translation
        mul.setIdentity();
        mul.setTranslation(new Vector3d(mainTranslate));
        matrix.mul(mul);
    }

    public void testMatrix()
    {
        final double meanZ = 561d;

        yRot = 15;
        xRot = 45;

        // transform points
        Matrix4d yRotationMatrix = new Matrix4d();
        yRotationMatrix.rotY(this.yRot * (Math.PI / 180.));

        Matrix4d xRotationMatrix = new Matrix4d();
        xRotationMatrix.rotX(this.xRot * (Math.PI / 180.));

        Matrix4d scaleMatrix = new Matrix4d();
        scaleMatrix.setScale(scale); // distance should be a Z

        Point3f p = new Point3f(50f, -75f, 125f);

        // global transform
        p.x += mainTranslate.x;
        p.y += mainTranslate.y;
        p.z += mainTranslate.z;

        // display transform

        p.z -= meanZ;
        xRotationMatrix.transform(p);
        yRotationMatrix.transform(p);
        p.z += meanZ;

        scaleMatrix.transform(p);

        p.x += this.xOffset;
        p.y += this.yOffset;

        System.out.println(
                "Original: point before trans = 50, -75, 125 - after trans = " + p.x + ", " + p.y + ", " + p.z);

        computeWorldMatrix(meanZ);

        p = new Point3f(50f, -75f, 125f);
        matrix.transform(p);

        System.out.println("New: point before trans = 50, -75, 125 - after trans = " + p.x + ", " + p.y + ", " + p.z);
    }

    public void setPoints(int channel, short[] newPointArray, short[] newColorArray)
    {
        if (channel < channelArray.length)
        {
            channelArray[channel].setPoints(newPointArray, newColorArray);
        }
    }
}