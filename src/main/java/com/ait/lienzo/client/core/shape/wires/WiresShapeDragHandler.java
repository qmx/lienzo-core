/*
   Copyright (c) 2014,2015,2016 Ahome' Innovation Technologies. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ait.lienzo.client.core.shape.wires;

import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragEndHandler;
import com.ait.lienzo.client.core.event.NodeDragMoveEvent;
import com.ait.lienzo.client.core.event.NodeDragMoveHandler;
import com.ait.lienzo.client.core.event.NodeDragStartEvent;
import com.ait.lienzo.client.core.event.NodeDragStartHandler;
import com.ait.lienzo.client.core.event.NodeMouseDownEvent;
import com.ait.lienzo.client.core.event.NodeMouseDownHandler;
import com.ait.lienzo.client.core.event.NodeMouseUpEvent;
import com.ait.lienzo.client.core.event.NodeMouseUpHandler;
import com.ait.lienzo.client.core.shape.MultiPath;
import com.ait.lienzo.client.core.shape.wires.picker.ColorMapBackedPicker;
import com.ait.lienzo.client.core.types.PathPartList;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.core.util.Geometry;
import com.ait.lienzo.client.widget.DragConstraintEnforcer;
import com.ait.lienzo.client.widget.DragContext;
import com.ait.tooling.nativetools.client.collection.NFastArrayList;
import com.ait.tooling.nativetools.client.util.Console;

import java.util.Set;

public class WiresShapeDragHandler implements NodeMouseDownHandler, NodeMouseUpHandler, NodeDragStartHandler, NodeDragMoveHandler, NodeDragEndHandler, DragConstraintEnforcer
{
    private WiresShape                 m_shape;

    private WiresContainer             m_parent;

    private WiresLayer                 m_layer;

    private WiresManager               m_wiresManager;

    private String                     m_priorFill;

    private double                     m_priorAlpha;

    private ColorMapBackedPicker picker;

    private DragContext dragContext;

    public WiresShapeDragHandler(WiresShape shape, WiresManager wiresManager)
    {
        m_shape = shape;
        m_wiresManager = wiresManager;
        m_layer = m_wiresManager.getLayer();
    }

    @Override
    public void onNodeDragStart(NodeDragStartEvent event)
    {
        picker = new ColorMapBackedPicker(m_layer.getLayer(), m_layer.getChildShapes(), m_layer.getLayer().getScratchPad(), m_shape, true);

        m_parent = m_shape.getParent();
        if (m_parent != null && m_parent instanceof WiresShape)
        {

            highlightBody((WiresShape) m_parent);
            m_layer.getLayer().batch();
        }
    }

    private PickerPart m_parentPart;
    private MultiPath m_path;

    @Override
    public void onNodeDragMove(NodeDragMoveEvent event)
    {
        WiresContainer parent = null;
        PickerPart parentPart = picker.findShapeAt(event.getX(), event.getY());

        if (parentPart != null)
        {
            parent = parentPart.getShape();
        }

        if (parent != m_parent || parentPart != m_parentPart)
        {
            boolean batch = false;

            if (m_parent != null && m_parent instanceof WiresShape )
            {
                if ( m_parentPart.getShapePart() == PickerPart.ShapePart.BODY ) {
                    restoreBody();
                } else {
                    m_path.removeFromParent();
                    m_path = null;

                }
                batch = true;
            }

            if (parent != null && parent instanceof WiresShape)
            {
                if ( parentPart.getShapePart() == PickerPart.ShapePart.BODY )
                {
                    if (  parent.getContainmentAcceptor().containmentAllowed(parent, m_shape)) {
                        highlightBody((WiresShape) parent);
                    }
                } else { // if ( parent.getDockingAcceptor().dockingAllowed(parent, m_shape) )
                    highlightBorder((WiresShape) parent);
                    m_shape.getGroup().setDragConstraints(this);
                }
                batch = true;
            }

            if (batch)
            {
                m_layer.getLayer().batch();
                m_layer.getLayer().getOverLayer().batch();
            }
        }
        m_parent = parent;
        m_parentPart = parentPart;
    }

    private void restoreBody() {
        ((WiresShape) m_parent).getPath().setFillColor(m_priorFill);
        ((WiresShape) m_parent).getPath().setFillAlpha(m_priorAlpha);
    }

    private void highlightBorder(WiresShape parent) {
        MultiPath path = parent.getPath();
        m_path = path.copy();
        m_path.setStrokeWidth(20);
        Point2D absLoc = path.getAbsoluteLocation();
        m_path.setX(absLoc.getX());
        m_path.setY(absLoc.getY());
        m_path.setStrokeColor("#CC1100");
        m_path.setStrokeAlpha(0.8);
        m_layer.getLayer().getOverLayer().add(m_path);
    }

    private void highlightBody(WiresShape parent)
    {
        m_priorFill = parent.getPath().getFillColor();
        m_priorAlpha = parent.getPath().getFillAlpha();
        parent.getPath().setFillColor("#CCCCCC");
        parent.getPath().setFillAlpha(0.8);
    }

    @Override
    public void onNodeDragEnd(NodeDragEndEvent event)
    {
        addShapeToParent();
    }

    @Override
    public void onNodeMouseDown(NodeMouseDownEvent event)
    {
        m_parent = m_shape.getParent();
    }

    @Override
    public void onNodeMouseUp(NodeMouseUpEvent event)
    {
        if (m_parent != m_shape.getParent())
        {
            addShapeToParent();
        }
    }

    private void addShapeToParent()
    {
        Point2D absLoc = m_shape.getGroup().getAbsoluteLocation();

        if (m_parent == null)
        {
            m_parent = m_layer;
        }

        if ( m_path != null ) {
            m_path.removeFromParent();
            m_layer.getLayer().getOverLayer().batch();
        }

        if ( m_parentPart == null || m_parentPart.getShapePart() == PickerPart.ShapePart.BODY ) {
            if (m_parent.getContainmentAcceptor().acceptContainment(m_parent, m_shape)) {
                if (m_parent instanceof WiresShape) {
                    restoreBody();
                }

                m_shape.removeFromParent();

                if (m_parent == m_layer) {
                    m_shape.getGroup().setLocation(absLoc);
                } else {
                    Point2D trgAbsOffset = m_parent.getContainer().getAbsoluteLocation();

                    m_shape.getGroup().setX(absLoc.getX() - trgAbsOffset.getX()).setY(absLoc.getY() - trgAbsOffset.getY());
                }
                m_parent.add(m_shape);

                m_layer.getLayer().batch();
            }
        }
        else // if ( parent.getDockingAcceptor().acceptDocking(parent, m_shape) )
        {

            // handle docking here
        }

        m_parent = null;
        m_priorFill = null;
        picker = null;
    }

    @Override public void startDrag(DragContext dragContext)
    {
        this.dragContext = dragContext;
    }

    @Override public boolean adjust(Point2D dxy)
    {
        if (dragContext != null && m_path != null)
        {
            int x = (int) (this.dragContext.getDragStartX() + dxy.getX());
            int y = (int) (this.dragContext.getDragStartY() + dxy.getY());
            Point2D pointerPosition = new Point2D(x, y);
            Point2D center = findCenter(m_path);
            NFastArrayList<PathPartList> pathPartListArray = m_path.getPathPartListArray();
            for (int i = 0; i < pathPartListArray.size(); i++)
            {
                Point2DArray listOfLines = new Point2DArray();
                listOfLines.push(center);
                listOfLines.push(getProjection(center, pointerPosition, m_path.getBoundingBox().getWidth()));
                Set<Point2D>[] intersections = new Set[1];
                Geometry.getCardinalIntersects(pathPartListArray.get(i), listOfLines, intersections);
                if (intersections.length == 2)
                {
                    Point2D intersection = intersections[1].iterator().next();
                    Console.get().info(intersection.toJSONString());
                    double dx = intersection.getX() - this.dragContext.getDragStartX();
                    dxy.setX(dx);
                    double dy = intersection.getY() - this.dragContext.getDragStartY();
                    dxy.setY(dy);
                }

            }
        }
        return false;
    }

    private Point2D findCenter(MultiPath rect)
    {
        Point2DArray cardinals = Geometry.getCardinals(rect.getBoundingPoints().getBoundingBox());
        return cardinals.get(0);
    }

    private Point2D getProjection(Point2D center, Point2D intersection, double length)
    {
        Point2D unit = intersection.sub(center).unit();
        return center.add(unit.mul(length));
    }
}
