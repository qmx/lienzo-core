package com.ait.lienzo.client.core.shape.wires;

import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragEndHandler;
import com.ait.lienzo.client.core.shape.MultiPath;
import com.ait.lienzo.client.core.types.ImageData;
import com.ait.lienzo.client.core.types.PathPartList;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.core.util.Geometry;
import com.ait.lienzo.client.widget.DragConstraintEnforcer;
import com.ait.lienzo.client.widget.DragContext;
import com.ait.tooling.nativetools.client.collection.NFastArrayList;
import com.ait.tooling.nativetools.client.collection.NFastStringMap;

import java.util.Set;

public class WiresShapeDockingHandler implements DragConstraintEnforcer, NodeDragEndHandler
{
    private final WiresShape shape;

    private NFastStringMap<WiresShape> colorMap;

    private ImageData m_shapesBacking;

    private DragContext dragContext;

    public WiresShapeDockingHandler(WiresShape shape)
    {
        this.shape = shape;
        this.shape.getPath().setDragConstraints(this);
    }

    @Override public void startDrag(DragContext dragContext)
    {
        this.dragContext = dragContext;
        this.colorMap = new NFastStringMap<>();
        this.m_shapesBacking = BackingColorMapUtils.drawShapesToBacking(this.shape.getWiresLayer().getChildShapes(), shape.getWiresLayer().getLayer().getScratchPad(), shape, this.colorMap);
    }

    @Override public boolean adjust(Point2D dxy)
    {
        if (this.dragContext != null && this.m_shapesBacking != null)
        {
            int x = (int) (this.dragContext.getDragStartX() + dxy.getX());
            int y = (int) (this.dragContext.getDragStartY() + dxy.getY());
            String color = BackingColorMapUtils.findColorAtPoint(m_shapesBacking, x, y);
            WiresShape parent = null;
            if (color != null)
            {
                parent = this.colorMap.get(color);
                if (parent != null)
                {
                    Point2D pointerPosition = new Point2D(x, y);
                    Point2D center = findCenter(parent.getPath());
                    NFastArrayList<PathPartList> pathPartListArray = parent.getPath().getPathPartListArray();
                    for (int i = 0; i < pathPartListArray.size(); i++)
                    {
                        Point2DArray listOfLines = new Point2DArray();
                        listOfLines.push(center);
                        listOfLines.push(getProjection(center, pointerPosition, parent.getPath().getBoundingBox().getWidth()));
                        Set<Point2D>[] intersections = new Set[1];
                        Geometry.getCardinalIntersects(pathPartListArray.get(i), listOfLines, intersections);
                        if (intersections.length == 2)
                        {
                            Point2D intersection = intersections[1].iterator().next();
                            double dx = intersection.getX() - this.dragContext.getDragStartX();
                            dxy.setX(dx);
                            double dy = intersection.getY() - this.dragContext.getDragStartY();
                            dxy.setY(dy);
                        }

                    }
                }
            }
        }
        return false;
    }

    public Point2D findCenter(MultiPath rect)
    {
        Point2DArray cardinals = Geometry.getCardinals(rect.getBoundingPoints().getBoundingBox());
        return cardinals.get(0);
    }

    public Point2D getProjection(Point2D center, Point2D intersection, double length)
    {
        Point2D unit = intersection.sub(center).unit();
        return center.add(unit.mul(length));
    }

    @Override public void onNodeDragEnd(NodeDragEndEvent event)
    {
        this.colorMap.clear();
        this.m_shapesBacking = null;
    }
}
