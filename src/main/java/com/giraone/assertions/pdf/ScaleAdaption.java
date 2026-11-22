package com.giraone.assertions.pdf;

public record ScaleAdaption(float xScale, float yScale) {

    public ScaleAdaption() {
        this(1.0f, 1.0f);
    }
}
