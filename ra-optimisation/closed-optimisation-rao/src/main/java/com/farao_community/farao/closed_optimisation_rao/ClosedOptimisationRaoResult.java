/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.powsybl.commons.extensions.AbstractExtension;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ClosedOptimisationRaoResult extends AbstractExtension<RaoComputationResult> {
    private SolverInfo solverInfo = new SolverInfo();
    private Map<String, VariableInfo> variableInfos = new HashMap<>();
    private Map<String, ConstraintInfo> constraintInfos = new HashMap<>();
    private ObjectiveInfo objectiveInfo = new ObjectiveInfo();

    @Override
    public String getName() {
        return "ClosedOptimisationRaoResult";
    }

    @Getter
    public class SolverInfo {
        private int numVariables;
        private int numConstraints;
        private long numIterations;
        private long wallTime;
        private String status;
    }

    @Getter
    public class VariableInfo {
        private String name;
        private double solutionValue;
        private double lb;
        private double ub;
    }

    @Getter
    public class ConstraintInfo {
        private String name;
        private double dualValue;
        private boolean isLazy;
        private double lb;
        private double ub;
        private String basisStatus;
    }

    @Getter
    public class ObjectiveInfo {
        private boolean maximization;
        private double value = Double.NaN;
    }

    public SolverInfo setSolverInfo(
            int numVariables,
            int numConstraints,
            long numIterations,
            long wallTime,
            String status) {
        solverInfo = new SolverInfo();
        solverInfo.numVariables = numVariables;
        solverInfo.numConstraints = numConstraints;
        solverInfo.numIterations = numIterations;
        solverInfo.wallTime = wallTime;
        solverInfo.status = status;
        return solverInfo;
    }

    public VariableInfo addVariableInfo(
            String name,
            double solutionValue,
            double lb,
            double ub) {
        VariableInfo variableInfo = new VariableInfo();
        variableInfo.name = name;
        variableInfo.solutionValue = solutionValue;
        variableInfo.lb = lb;
        variableInfo.ub = ub;
        variableInfos.put(name, variableInfo);
        return variableInfo;
    }

    public ConstraintInfo addConstraintInfo(
            String name,
            double dualValue,
            boolean isLazy,
            double lb,
            double ub,
            String basisStatus) {
        ConstraintInfo constraintInfo = new ConstraintInfo();
        constraintInfo.name = name;
        constraintInfo.dualValue = dualValue;
        constraintInfo.isLazy = isLazy;
        constraintInfo.lb = lb;
        constraintInfo.ub = ub;
        constraintInfo.basisStatus = basisStatus;
        constraintInfos.put(name, constraintInfo);
        return constraintInfo;
    }

    public ObjectiveInfo setObjectiveInfo(
            boolean maximization,
            double value) {
        objectiveInfo = new ObjectiveInfo();
        objectiveInfo.maximization = maximization;
        objectiveInfo.value = value;
        return objectiveInfo;
    }
}
