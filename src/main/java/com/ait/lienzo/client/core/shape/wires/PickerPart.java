package com.ait.lienzo.client.core.shape.wires;


public class PickerPart
{
    private final WiresShape shape;

    private final ShapePart part;

    public WiresShape getShape()
    {
        return shape;
    }

    public ShapePart getShapePart()
    {
        return part;
    }

    public enum ShapePart {
        BORDER, BODY
    }

    public PickerPart(WiresShape shape, ShapePart part)
    {
        this.shape = shape;
        this.part = part;
    }
}
