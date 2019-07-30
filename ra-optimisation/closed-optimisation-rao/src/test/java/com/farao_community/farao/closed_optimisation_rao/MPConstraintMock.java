package com.farao_community.farao.closed_optimisation_rao;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.HashMap;

public class MPConstraintMock extends MPConstraint {

    private String name;
    private double lb;
    private double ub;
    private HashMap<String, Double> coefficients;

    protected MPConstraintMock(String pName, double pLb, double pUb) {
        super(0, false);
        this.name = pName;
        this.lb = pLb;
        this.ub = pUb;
        this.coefficients = new HashMap<>();
    }

    @Override
    public double lb() {
        return lb;
    }

    @Override
    public double ub() {
        return ub;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void setCoefficient(MPVariable var, double coeff) {
        coefficients.put(var.name(), coeff);
    }

    @Override
    public double getCoefficient(MPVariable var) {
        return coefficients.get(var.name());
    }
}
