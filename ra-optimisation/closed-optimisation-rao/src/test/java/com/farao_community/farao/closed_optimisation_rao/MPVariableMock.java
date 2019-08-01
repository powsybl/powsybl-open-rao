package com.farao_community.farao.closed_optimisation_rao;

import com.google.ortools.linearsolver.MPVariable;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MPVariableMock extends MPVariable {

    private String name;
    private double lb;
    private double ub;
    private boolean isBoolVariable;

    protected MPVariableMock() {
        super(0, false);
    }

    protected MPVariableMock(String pName, double pLb, double pUb, boolean pIsBoolVariable) {
        super(0, false);
        this.name = pName;
        this.lb = pLb;
        this.ub = pUb;
        this.isBoolVariable = pIsBoolVariable;
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
