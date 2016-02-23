package com.ait.lienzo.client.core.shape.wires;

import com.ait.lienzo.client.core.Context2D;
import com.ait.lienzo.client.core.shape.MultiPath;
import com.ait.lienzo.client.core.types.ImageData;
import com.ait.lienzo.client.core.types.PathPartEntryJSO;
import com.ait.lienzo.client.core.types.PathPartList;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.util.ScratchPad;
import com.ait.lienzo.shared.core.types.Color;
import com.ait.tooling.nativetools.client.collection.NFastArrayList;
import com.ait.tooling.nativetools.client.collection.NFastDoubleArrayJSO;
import com.ait.tooling.nativetools.client.collection.NFastStringMap;

public class BackingColorMapUtils
{
    public static ImageData drawShapesToBacking(NFastArrayList<WiresShape> prims, ScratchPad scratch, WiresContainer skip, NFastStringMap<WiresShape> shape_color_map)
    {
        scratch.clear();
        Context2D ctx = scratch.getContext();

        shape_color_map.clear();
        drawShapesToBacking(prims, ctx, skip, shape_color_map);

        return ctx.getImageData(0, 0, scratch.getWidth(), scratch.getHeight());
    }

    public static void drawShapesToBacking(NFastArrayList<WiresShape> prims, Context2D ctx, WiresContainer skip, NFastStringMap<WiresShape> shape_color_map)
    {
        for (int j = 0; j < prims.size(); j++)
        {
            WiresShape prim = prims.get(j);
            if (prim == skip)
            {
                continue;
            }
            drawShapeToBacking(ctx, prim, MagnetManager.m_c_rotor.next(), shape_color_map, false);
            drawShapeToBacking(ctx, prim, MagnetManager.m_c_rotor.next(), shape_color_map, true);

            if (prim.getChildShapes() != null)
            {
                drawShapesToBacking(prim.getChildShapes(), ctx, skip, shape_color_map);
            }
        }
    }

    public static void drawShapeToBacking(Context2D ctx, WiresShape shape, String color, NFastStringMap<WiresShape> m_shape_color_map, boolean shouldCloseOpenPaths)
    {
        m_shape_color_map.put(color, shape);
        MultiPath multiPath = shape.getPath();
        NFastArrayList<PathPartList> listOfPaths = multiPath.getPathPartListArray();

        for (int k = 0; k < listOfPaths.size(); k++)
        {
            PathPartList path = listOfPaths.get(k);

            if(shouldCloseOpenPaths)
            {
                ctx.setStrokeWidth(multiPath.getStrokeWidth());
            }
            else
            {
                ctx.setStrokeWidth(20);
            }
            ctx.setStrokeColor(color);
            if (!shouldCloseOpenPaths)
            {
                ctx.setFillColor(color);
            }
            ctx.beginPath();

            Point2D absLoc = multiPath.getAbsoluteLocation();
            double offsetX = absLoc.getX();
            double offsetY = absLoc.getY();

            ctx.moveTo(offsetX, offsetY);

            boolean closed = false;
            for (int i = 0; i < path.size(); i++)
            {
                PathPartEntryJSO entry = path.get(i);
                NFastDoubleArrayJSO points = entry.getPoints();

                switch (entry.getCommand())
                {
                    case PathPartEntryJSO.MOVETO_ABSOLUTE:
                    {
                        ctx.moveTo(points.get(0) + offsetX, points.get(1) + offsetY);
                        break;
                    }
                    case PathPartEntryJSO.LINETO_ABSOLUTE:
                    {
                        points = entry.getPoints();
                        double x0 = points.get(0) + offsetX;
                        double y0 = points.get(1) + offsetY;
                        ctx.lineTo(x0, y0);
                        break;
                    }
                    case PathPartEntryJSO.CLOSE_PATH_PART:
                    {
                        if (!shouldCloseOpenPaths)
                        {
                            ctx.closePath();
                            closed = true;
                        }
                        break;
                    }
                    case PathPartEntryJSO.CANVAS_ARCTO_ABSOLUTE:
                    {
                        points = entry.getPoints();

                        double x0 = points.get(0) + offsetX;
                        double y0 = points.get(1) + offsetY;

                        double x1 = points.get(2) + offsetX;
                        double y1 = points.get(3) + offsetY;
                        double r = points.get(4);
                        ctx.arcTo(x0, y0, x1, y1, r);

                    }
                        break;
                }
            }

            if (!closed && shouldCloseOpenPaths)
            {
                ctx.closePath();
            }
            ctx.fill();
            ctx.stroke();
        }
    }

    public static String findColorAtPoint(final ImageData imageData, final int x, final int y)
    {
        int red = imageData.getRedAt(x, y);
        int green = imageData.getGreenAt(x, y);
        int blue = imageData.getBlueAt(x, y);
        int alpha = imageData.getAlphaAt(x, y);

        if (alpha != 255)
        {
            return null;
        }
        String color = Color.rgbToBrowserHexColor(red, green, blue);

        return color;
    }
}
