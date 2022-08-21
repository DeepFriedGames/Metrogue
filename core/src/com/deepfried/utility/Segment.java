package com.deepfried.utility;

public class Segment {
    public final float minX, minY, maxX, maxY;
    public final float xa, ya, xb, yb;
    public final float b;
    public float m = Float.MAX_VALUE; //undefined by default

    public Segment(float x1, float y1, float x2, float y2) {
        this.xa = x1;
        this.ya = y1;
        this.xb = x2;
        this.yb = y2;
        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.minY = Math.min(y1, y2);
        this.maxY = Math.max(y1, y2);
        if (x2 != x1)
            this.m = (y2 - y1) / (x2 - x1);
        this.b = y1 - m * x1;
    }

    public boolean intersects(Segment other) {
        float minXI = Math.max(other.minX, this.minX);
        float maxXI = Math.min(other.maxX, this.maxX);
        float minYI = Math.max(other.minY, this.minY);
        float maxYI = Math.min(other.maxY, this.maxY);
        if (maxXI < minXI) return false; //domains have no intersection
        if (other.m == this.m) return other.b == this.b; //edges are parallel
        float xi = (other.b - this.b) / (this.m - other.m);
        float yi = this.m * xi + this.b;
        return minXI <= xi && xi <= maxXI && minYI <= yi && yi <= maxYI;

    }

    public float getY(float x) {
        return m * x + b;
    }
}
