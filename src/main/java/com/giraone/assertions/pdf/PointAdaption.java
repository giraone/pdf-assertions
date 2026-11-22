package com.giraone.assertions.pdf;

public record PointAdaption(float x, float y) {

    public PointAdaption() {
        this(0.0f, 0.0f);
    }
}
