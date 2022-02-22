/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.StandardCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;

import static com.farao_community.farao.data.core_cne_exporter.CneConstants.PATL_MEASUREMENT_TYPE;
import static com.farao_community.farao.data.core_cne_exporter.CneConstants.TATL_MEASUREMENT_TYPE;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CneHelper {

    private Crac crac;
    private Network network;
    private StandardCracCreationContext cracCreationContext;
    private boolean relativePositiveMargins;
    private boolean withLoopflows;
    private RaoResult raoResult;
    private StandardCneExporterParameters exporterParameters;
    private double mnecAcceptableMarginDiminution;

    public CneHelper(Crac crac, Network network, StandardCracCreationContext cracCreationContext, RaoResult raoResult, RaoParameters raoParameters, StandardCneExporterParameters exporterParameters) {
        this.crac = crac;
        this.network = network;
        this.cracCreationContext = cracCreationContext;
        this.raoResult = raoResult;
        this.exporterParameters = exporterParameters;

        relativePositiveMargins = raoParameters.getObjectiveFunction().relativePositiveMargins();
        withLoopflows = raoParameters.isRaoWithLoopFlowLimitation();
        mnecAcceptableMarginDiminution = raoParameters.getMnecAcceptableMarginDiminution();
    }

    public RaoResult getRaoResult() {
        return raoResult;
    }

    public boolean isWithLoopflows() {
        return withLoopflows;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public Network getNetwork() {
        return network;
    }

    public Crac getCrac() {
        return crac;
    }

    public StandardCracCreationContext getCracCreationContext() {
        return cracCreationContext;
    }

    public String instantToCodeConverter(Instant instant) {
        if (instant.equals(Instant.PREVENTIVE)) { // Before contingency
            return PATL_MEASUREMENT_TYPE;
        } else { // After contingency, before any post-contingency RA
            return TATL_MEASUREMENT_TYPE;
        }
    }

    public StandardCneExporterParameters getExporterParameters() {
        return exporterParameters;
    }

    public boolean isRelativePositiveMargins() {
        return relativePositiveMargins;
    }
}
