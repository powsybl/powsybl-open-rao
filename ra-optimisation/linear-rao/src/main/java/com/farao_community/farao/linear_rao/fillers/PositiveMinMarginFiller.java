/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.SynchronizationException;
import com.farao_community.farao.linear_rao.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.powsybl.iidm.network.Network;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class PositiveMinMarginFiller extends AbstractProblemFiller {
    @Override
    public void fill(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        this.linearRaoData = linearRaoData;
        this.linearRaoProblem = linearRaoProblem;
        Crac crac = linearRaoData.getCrac();
        Network network = linearRaoData.getNetwork();

        crac.synchronize(network);

        // Create the Minimum Positive Margin variable
        this.linearRaoProblem.getMinPosMargin();

        // Add all the constraints (2 per CNEC)
        crac.getCnecs().forEach(this::fillConstraintsCnec);

        // Define the objective function
        //TODO

        linearRaoData.getCrac().desynchronize();
    }

    private void fillConstraints() {

    }

    private void fillConstraintsCnec(Cnec cnec) {
        try {
            this.linearRaoProblem.addMinPosMargin(cnec.getId(), cnec.getThreshold().getMinThreshold().orElse(-Double.MAX_VALUE), cnec.getThreshold().getMaxThreshold().orElse(Double.MAX_VALUE));
        } catch (SynchronizationException e) {
            e.printStackTrace();
        }
    }
}

