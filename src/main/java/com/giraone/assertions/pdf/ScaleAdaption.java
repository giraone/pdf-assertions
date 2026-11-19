package com.giraone.assertions.pdf;

public class ScaleAdaption {

    final float xScale;
    final float yScale;

    public ScaleAdaption() {
        this.xScale = 1.0f;
        this.yScale = 1.0f;
    }

    public ScaleAdaption(float xScale, float yScale) {
        this.xScale = xScale;
        this.yScale = yScale;
    }

    public float getxScale() {
        return xScale;
    }

    public float getyScale() {
        return yScale;
    }

    @Override
    public String toString() {
        return "ScaleAdaption{" +
            "xScale=" + xScale +
            ", yScale=" + yScale +
            '}';
    }
}
