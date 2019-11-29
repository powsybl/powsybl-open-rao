/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initial PST treatment implementation that put them at neutral tap
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PstPreTreatmentNeutralTap implements PstPreTreatmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PstPreTreatmentNeutralTap.class);

    private void setNeutralTap(TwoWindingsTransformer twt) {
        // Find tap which angle is 0
        PhaseTapChanger phaseTapChanger = twt.getPhaseTapChanger();
        float minAngle = Float.POSITIVE_INFINITY;
        int neutralTap = phaseTapChanger.getLowTapPosition();
        for (int i = phaseTapChanger.getLowTapPosition(); i <= phaseTapChanger.getHighTapPosition(); i++) {
            if (Math.abs(phaseTapChanger.getStep(i).getAlpha()) < Math.abs(minAngle)) {
                minAngle = (float) phaseTapChanger.getStep(i).getAlpha();
                neutralTap = i;
            }
        }
        phaseTapChanger.setTapPosition(neutralTap);
        LOGGER.info("Putting PST {} in tap {}", twt.getId(), neutralTap);
    }

    @Override
    public void treatment(Network network, FullLineDecompositionParameters parameters) {
        LOGGER.info("Working on neutral tap");
        network.getTwoWindingsTransformerStream().filter(twt -> twt.getPhaseTapChanger() != null).forEach(this::setNeutralTap);
    }
}
