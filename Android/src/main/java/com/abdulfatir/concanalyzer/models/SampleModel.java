package com.abdulfatir.concanalyzer.models;

/**
 * Created by Abdul on 3/12/2016.
 */
public class SampleModel {
    public enum DataPointType
    {
        NONE,
        UNKNOWN,
        QUALITY_CONTROL,
        KNOWN
    }

    private double intensity;
    private double concentration;
    private DataPointType dpt;

    public SampleModel()
    {
        intensity = 0;
        concentration = 0;
        dpt = DataPointType.NONE;
    }

    public SampleModel(double intensity)
    {
        this.intensity = intensity;
        dpt = DataPointType.NONE;
    }

    public double getConcentration() {
        return concentration;
    }

    public void setConcentration(double concentration) {
        this.concentration = concentration;
    }

    public DataPointType getDpt() {
        return dpt;
    }

    public void setDpt(DataPointType dpt) {
        this.dpt = dpt;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }
}