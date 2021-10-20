package plugins.fab.livemousetracker.transform;

import java.awt.geom.Point2D;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;

public class TriangleDrawerSlow {

	/** @deprecated */
	public static void drawTriangle(
			IcyBufferedImage targetImage, IcyBufferedImage sourceImage, Point2D p1, Point2D p2,
            Point2D p3, Point2D uv1, Point2D uv2, Point2D uv3)
    {
        final Point2D min, med, max;
        final Point2D minUV, medUV, maxUV;
        final Point2D endLeftUV, endRightUV;

        // find min, med & max y
        if (p1.getY() < p2.getY())
        {
            if (p1.getY() < p3.getY())
            {
                min = p1;
                minUV = uv1;

                if (p2.getY() < p3.getY())
                {
                    med = p2;
                    medUV = uv2;
                    max = p3;
                    maxUV = uv3;
                }
                else
                {
                    med = p3;
                    medUV = uv3;
                    max = p2;
                    maxUV = uv2;
                }
            }
            else
            {
                min = p3;
                minUV = uv3;
                med = p1;
                medUV = uv1;
                max = p2;
                maxUV = uv2;
            }
        }
        else
        {
            if (p2.getY() < p3.getY())
            {
                min = p2;
                minUV = uv2;

                if (p1.getY() < p3.getY())
                {
                    med = p1;
                    medUV = uv1;
                    max = p3;
                    maxUV = uv3;
                }
                else
                {
                    med = p3;
                    medUV = uv3;
                    max = p1;
                    maxUV = uv1;
                }
            }
            else
            {
                min = p3;
                minUV = uv3;
                med = p2;
                medUV = uv2;
                max = p1;
                maxUV = uv1;
            }
        }

        final double minX = min.getX();
        final double minY = min.getY();
        final double medDeltaX = med.getX() - minX;
        final double maxDeltaX = max.getX() - minX;
        final double medDeltaY = med.getY() - minY;
        final double maxDeltaY = max.getY() - minY;
        final double medStepX;
        final double maxStepX;

        if (medDeltaY != 0)
            medStepX = medDeltaX / medDeltaY;
        else
            medStepX = medDeltaX;
        if (maxDeltaY != 0)
            maxStepX = maxDeltaX / maxDeltaY;
        else
            maxStepX = maxDeltaX;

        // calculate coordinate step
        final double leftDeltaY;
        final double rightDeltaY;
        double leftStep;
        double rightStep;

        // find end left & end right
        if (medStepX < maxStepX)
        {
            endLeftUV = medUV;
            endRightUV = maxUV;

            leftDeltaY = medDeltaY;
            rightDeltaY = maxDeltaY;
            leftStep = medStepX;
            rightStep = maxStepX;
        }
        else
        {
            endLeftUV = maxUV;
            endRightUV = medUV;

            leftDeltaY = maxDeltaY;
            rightDeltaY = medDeltaY;
            leftStep = maxStepX;
            rightStep = medStepX;
        }

        double leftU = minUV.getX();
        double leftV = minUV.getY();
        double rightU = minUV.getX();
        double rightV = minUV.getY();
        double leftUStep;
        double leftVStep;
        double rightUStep;
        double rightVStep;

        // calculate UV step
        if (leftDeltaY != 0)
        {
            leftUStep = (endLeftUV.getX() - leftU) / leftDeltaY;
            leftVStep = (endLeftUV.getY() - leftV) / leftDeltaY;
        }
        else
        {
            leftUStep = 0;
            leftVStep = 0;
        }

        if (rightDeltaY != 0)
        {
            rightUStep = (endRightUV.getX() - rightU) / rightDeltaY;
            rightVStep = (endRightUV.getY() - rightV) / rightDeltaY;
        }
        else
        {
            rightUStep = 0;
            rightVStep = 0;
        }

        final Point2D leftUV = new Point2D.Double(leftU, leftV);
        final Point2D rightUV = new Point2D.Double(rightU, rightV);

        // first part
        int y;
        int bottom;
        double left = minX;
        double right = minX;

        y = (int) min.getY();
        bottom = (int) med.getY();

        while (y < bottom)
        {
            drawTexturedLine(y, left, right, leftUV, rightUV, sourceImage, targetImage);

            // next scanline
            left += leftStep;
            leftU += leftUStep;
            leftV += leftVStep;
            leftUV.setLocation(leftU, leftV);

            right += rightStep;
            rightU += rightUStep;
            rightV += rightVStep;
            rightUV.setLocation(rightU, rightV);

            y++;
        }

        final double endDeltaY = max.getY() - med.getY();

        if (medUV == endLeftUV)
        {
            // recalculate left step
            left = med.getX();
            leftU = medUV.getX();
            leftV = medUV.getY();
            leftUV.setLocation(leftU, leftV);

            if (endDeltaY != 0)
                leftStep = (max.getX() - left) / endDeltaY;
            else
                leftStep = 0;

            if (endDeltaY != 0)
            {
                leftUStep = (maxUV.getX() - leftU) / endDeltaY;
                leftVStep = (maxUV.getY() - leftV) / endDeltaY;
            }
            else
            {
                leftUStep = 0;
                leftVStep = 0;
            }
        }
        else
        {
            // recalculate right step
            right = med.getX();
            rightU = medUV.getX();
            rightV = medUV.getY();
            rightUV.setLocation(rightU, rightV);

            if (endDeltaY != 0)
                rightStep = (max.getX() - right) / endDeltaY;
            else
                rightStep = 0;

            if (endDeltaY != 0)
            {
                rightUStep = (maxUV.getX() - rightU) / endDeltaY;
                rightVStep = (maxUV.getY() - rightV) / endDeltaY;
            }
            else
            {
                rightUStep = 0;
                rightVStep = 0;
            }
        }

        // second part
        bottom = (int) max.getY();

        while (y < bottom)
        {
            drawTexturedLine(y, left, right, leftUV, rightUV, sourceImage, targetImage);

            // next scanline
            left += leftStep;
            leftU += leftUStep;
            leftV += leftVStep;
            leftUV.setLocation(leftU, leftV);

            right += rightStep;
            rightU += rightUStep;
            rightV += rightVStep;
            rightUV.setLocation(rightU, rightV);

            y++;
        }
    }

    public static void drawTexturedLine(int y,
    		double left, double right, Point2D leftUV, Point2D rightUV,
            IcyBufferedImage sourceImage, IcyBufferedImage finalImage)
    {
        final double delta = right - left;
        double u = leftUV.getX();
        double v = leftUV.getY();
        final double uStep;
        final double vStep;

        final int wSrc = sourceImage.getWidth();
        final int hSrc = sourceImage.getHeight();
        final int wDst = finalImage.getWidth();
        final int hDst = finalImage.getHeight();

        if ((left >= wDst) || (right < 0) || (y >= hDst) || (y < 0))
            return;

        // calculate UV step
        if (delta != 0)
        {
            uStep = (rightUV.getX() - u) / delta;
            vStep = (rightUV.getY() - v) / delta;
        }
        else
        {
            uStep = 0;
            vStep = 0;
        }

        final int x;
        final int r;

        // do clipping
        if (left < 0)
        {
            u += uStep * -left;
            v += vStep * -left;
            x = 0;
        }
        else
            x = (int) left;

        if (right >= wDst)
            r = wDst - 1;
        else
            r = (int) right;

//        final byte[] src = sourceImage.getDataXYAsByte(0);
//        final byte[] dst = finalImage.getDataXYAsByte(0);


        final int src_pitch = sourceImage.getSizeX();
        int offset = x + (y * finalImage.getWidth());
        final int limit = offset + (r - x);

        while (offset < limit)
        {
        	final int u_i = (int) u;
        	final int v_i = (int) v;

        	if ((u_i >= 0) && (v_i >= 0) && (u_i < wSrc) && (v_i < hSrc))
        	{
        		for ( int c = 0 ; c < sourceImage.getSizeC() ; c++ )
                {
        			Object srcArray = sourceImage.getDataXY( c );
        			Object dstArray = finalImage.getDataXY( c );

        			Array1DUtil.setValue(dstArray, offset,
        					Array1DUtil.getValue(srcArray, (v_i * src_pitch) + u_i, sourceImage.isSignedDataType() ));

        			//dst[offset] = src[(v_i * src_pitch) + u_i];

        			// alphaDst[offset] = (byte) 255;
                }

        		// set alpha to 255
//    			Object dstArray = finalImage.getDataXY( finalImage.getSizeC() -1 );
//    			Array1DUtil.setValue(dstArray, offset, 255 );

        	}
//        	else
//        	{
//        		// put 0 value to all channels (so is transparent )
//        		// for ( int c = 0 ; c < sourceImage.getSizeC() ; c++ )
//        		for ( int c = 0 ; c < finalImage.getSizeC() ; c++ )
//                {
//        			System.out.println(c);
//        			Object dstArray = finalImage.getDataXY( c );
//        			Array1DUtil.setValue(dstArray, offset, 0 );
//                }
//
//        	}

        	u += uStep;
        	v += vStep;
        	offset++;
        }

    }

}
