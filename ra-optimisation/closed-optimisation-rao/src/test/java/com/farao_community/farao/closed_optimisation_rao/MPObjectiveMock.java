package com.farao_community.farao.closed_optimisation_rao;

import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPVariable;

import java.util.HashMap;

public class MPObjectiveMock extends MPObjective {

    private HashMap<String, Double> coefficients;
    private boolean isMinimization;

    protected MPObjectiveMock() {
        super(0, true);
        coefficients = new HashMap<>();
        isMinimization = true;
    }

    @Override
    public void clear() {
        coefficients.clear();
    }

    @Override
    public void setCoefficient(MPVariable var, double coeff) {
        coefficients.put(var.name(), coeff);
    }

    @Override
    public double getCoefficient(MPVariable var) {
        return coefficients.get(var.name());
    }

    @Override
    public void setMinimization() {
        isMinimization = true;
    }
}
