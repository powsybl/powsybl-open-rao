/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.SynchronizationException;
import com.farao_community.farao.linear_rao.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.powsybl.iidm.network.Network;

import static com.farao_community.farao.data.crac_api.Unit.MEGAWATT;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class PositiveMinMarginFiller extends AbstractProblemFiller {

    public PositiveMinMarginFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        super(linearRaoProblem, linearRaoData);
    }

    @Override
    public void fill() {
        Crac crac = linearRaoData.getCrac();
        Network network = linearRaoData.getNetwork();

        crac.synchronize(network);

        // Create the Minimum Positive Margin variable
        this.linearRaoProblem.addMinimumMarginVariable();

        // Add all the constraints (2 per CNEC)
        crac.getCnecs().forEach(this::fillConstraintsCnec);

        // Define the objective function
        fillObjective();

        linearRaoData.getCrac().desynchronize();
    }

    private void fillConstraintsCnec(Cnec cnec) {
        try {
            linearRaoProblem.addMinimumMarginConstraints(cnec.getId(), cnec.getThreshold().getMinThreshold(MEGAWATT).orElse(-LinearRaoProblem.infinity()), cnec.getThreshold().getMaxThreshold(MEGAWATT).orElse(LinearRaoProblem.infinity()));
        } catch (SynchronizationException e) {
            throw new FaraoException(e);
        }
    }

    private void fillObjective() {
        linearRaoProblem.addPosMinObjective();
    }
}

