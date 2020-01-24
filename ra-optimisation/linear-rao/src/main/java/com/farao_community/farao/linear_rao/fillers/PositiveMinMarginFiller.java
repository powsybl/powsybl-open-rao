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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class PositiveMinMarginFiller extends AbstractProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(PositiveMinMarginFiller.class);

    @Override
    public void fill(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        this.linearRaoData = linearRaoData;
        this.linearRaoProblem = linearRaoProblem;
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
            linearRaoProblem.addMinimumMarginConstraints(cnec.getId(), cnec.getThreshold().getMinThreshold().orElse(-Double.MAX_VALUE), cnec.getThreshold().getMaxThreshold().orElse(Double.MAX_VALUE));
        } catch (SynchronizationException e) {
            throw new FaraoException(e);
        }
    }

    private void fillObjective() {
        linearRaoProblem.getPosMinObjective();
    }
}

