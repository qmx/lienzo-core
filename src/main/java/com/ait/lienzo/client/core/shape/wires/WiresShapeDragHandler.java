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
import com.ait.lienzo.client.core.shape.wires.picker.ColorMapBackedPicker;
import com.ait.lienzo.client.core.types.Point2D;

public class WiresShapeDragHandler implements NodeMouseDownHandler, NodeMouseUpHandler, NodeDragStartHandler, NodeDragMoveHandler, NodeDragEndHandler
{
    private WiresShape                 m_shape;

    private WiresContainer             m_parent;

    private WiresLayer                 m_layer;

    private WiresManager               m_wiresManager;

    private String                     m_priorFill;

    private double                     m_priorAlpha;

    private ColorMapBackedPicker picker;

    public WiresShapeDragHandler(WiresShape shape, WiresManager wiresManager)
    {
        m_shape = shape;
        m_wiresManager = wiresManager;
        m_layer = m_wiresManager.getLayer();
    }

    @Override
    public void onNodeDragStart(NodeDragStartEvent event)
    {
        picker = new ColorMapBackedPicker(m_layer.getChildShapes(), m_layer.getLayer().getScratchPad(), m_shape, true);

        m_parent = m_shape.getParent();
        if (m_parent != null && m_parent instanceof WiresShape)
        {
            highlightContainer((WiresShape) m_parent);
            m_layer.getLayer().batch();
        }
    }

    @Override
    public void onNodeDragMove(NodeDragMoveEvent event)
    {

        WiresContainer parent = null;
        PickerPart part = picker.findShapeAt(event.getX(), event.getY());
        if (part != null)
        {
            parent = part.getShape();
        }
        if (parent != m_parent)
        {
            boolean batch = false;
            if (m_parent != null && m_parent instanceof WiresShape
                    && m_parent.getContainmentAcceptor() != null)
            {

                ((WiresShape) m_parent).getPath().setFillColor(m_priorFill);
                ((WiresShape) m_parent).getPath().setFillAlpha(m_priorAlpha);
                batch = true;
            }
            if (parent != null && parent instanceof WiresShape
                    && parent.getContainmentAcceptor() != null
                    && parent.getContainmentAcceptor().containmentAllowed(parent, m_shape))
            {
                highlightContainer((WiresShape) parent);
                batch = true;
            }
            if (batch)
            {
                m_layer.getLayer().batch();
            }
        }
        m_parent = parent;
    }

    private void highlightContainer(WiresShape parent)
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

        if (m_parent.getContainmentAcceptor() !=null
                && m_parent.getContainmentAcceptor().acceptContainment(m_parent, m_shape))
        {
            if (m_parent instanceof WiresShape)
            {
                ((WiresShape) m_parent).getPath().setFillColor(m_priorFill);
                ((WiresShape) m_parent).getPath().setFillAlpha(m_priorAlpha);
            }

            m_shape.removeFromParent();

            if (m_parent == m_layer)
            {
                m_shape.getGroup().setLocation(absLoc);
            }
            else
            {
                Point2D trgAbsOffset = m_parent.getContainer().getAbsoluteLocation();

                m_shape.getGroup().setX(absLoc.getX() - trgAbsOffset.getX()).setY(absLoc.getY() - trgAbsOffset.getY());
            }
            m_parent.add(m_shape);

            m_layer.getLayer().batch();
        }

        m_parent = null;
        m_priorFill = null;
        picker = null;

    }
}
