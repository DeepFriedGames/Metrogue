package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Shape2D;
import com.badlogic.gdx.math.Vector2;

public class ShapeComponent implements Component {
    private final Shape2D shape;

    public ShapeComponent(Shape2D shape) {
        this.shape = shape;
    }

    public Shape2D getShape() {
        return shape;
    }

    public Rectangle getRectangle() {
        if(shape.getClass() == Rectangle.class)
            return (Rectangle) shape;

        return null;
    }

    public Polyline getPolyline() {
        if(shape.getClass() == Polyline.class)
            return (Polyline) shape;

        return null;
    }
}
