package com.abdulfatir.concanalyzer.util;

/**
 * Created by Abdul on 3/22/2016.
 */
public class LinearFunction {
    private double slope;
    private double intercept;

    public LinearFunction(double intercept, double slope) {
        this.intercept = intercept;
        this.slope = slope;
    }

    public LinearFunction(LinearFunction l) {
        this.slope = l.slope;
        this.intercept = l.intercept;
    }

    public LinearFunction swapAxes()
    {
        return new LinearFunction(1/this.slope,-this.intercept/this.slope);
    }

    public double getIntercept() {
        return intercept;
    }

    public void setIntercept(double intercept) {
        this.intercept = intercept;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }
}
