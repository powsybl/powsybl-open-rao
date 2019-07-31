package com.farao_community.farao.closed_optimisation_rao;

import com.google.ortools.linearsolver.MPVariable;

public class MPVariableMock extends MPVariable {

    private String name;
    private double lb;
    private double ub;

    protected MPVariableMock() {
        super(0, false);
    }
    protected MPVariableMock (String pName, double pLb, double pUb)
    {
        super(0, false);
        this.name = pName;
        this.lb = pLb;
        this.ub = pUb;
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

}
