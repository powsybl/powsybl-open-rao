/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.data.crac_file.CracFile;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public abstract class AbstractOptimisationProblemFiller {
    protected Network network;
    protected CracFile cracFile;
    protected Map<String, Object> data;

    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        this.network = network;
        this.cracFile = cracFile;
        this.data = data;
    }

    public List<String> variablesProvided() {
        return Collections.emptyList();
    }

    public List<String> constraintsProvided() {
        return Collections.emptyList();
    }

    public List<String> objectiveFunctionsProvided() {
        return Collections.emptyList();
    }

    public List<String> variablesExpected() {
        return Collections.emptyList();
    }

    public List<String> constraintsExpected() {
        return Collections.emptyList();
    }

    public List<String> objectiveFunctionsExpected() {
        return Collections.emptyList();
    }

    public Map<String, Class> dataExpected() {
        return Collections.emptyMap();
    }

    public abstract void fillProblem(MPSolver solver);
}
