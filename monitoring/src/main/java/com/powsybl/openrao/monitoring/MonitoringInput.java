/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.Objects;

import static java.lang.String.format;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class MonitoringInput {

    public static final class MonitoringInputBuilder {
        private static final String REQUIRED_ARGUMENT_MESSAGE = "%s is mandatory when building Monitoring input.";
        private Crac crac;
        private Network network;
        private RaoResult raoResult;
        private PhysicalParameter physicalParameter;
        private ZonalData<Scalable> scalableZonalData;

        MonitoringInputBuilder() {
        }

        public MonitoringInputBuilder withCrac(Crac crac) {
            this.crac = crac;
            return this;
        }

        public MonitoringInputBuilder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public MonitoringInputBuilder withRaoResult(RaoResult raoResult) {
            this.raoResult = raoResult;
            return this;
        }

        public MonitoringInputBuilder withPhysicalParameter(PhysicalParameter physicalParameter) {
            this.physicalParameter = physicalParameter;
            return this;
        }

        public MonitoringInputBuilder withScalableZonalData(ZonalData<Scalable> scalableZonalData) {
            if (!physicalParameter.equals(PhysicalParameter.ANGLE)) {
                throw new OpenRaoException("Monitoring with scalable is possible only if physicalParameter is ANGLE");
            }
            this.scalableZonalData = scalableZonalData;
            return this;
        }

        public MonitoringInput build() {
            MonitoringInput monitoringInput = new MonitoringInput();
            monitoringInput.crac = Objects.requireNonNull(crac, format(REQUIRED_ARGUMENT_MESSAGE, "CRAC"));
            monitoringInput.network = Objects.requireNonNull(network, format(REQUIRED_ARGUMENT_MESSAGE, "Network"));
            monitoringInput.raoResult = Objects.requireNonNull(raoResult, format(REQUIRED_ARGUMENT_MESSAGE, "RaoResult"));
            monitoringInput.physicalParameter = physicalParameter;
            monitoringInput.scalableZonalData = scalableZonalData;
            return monitoringInput;
        }
    }

    private Crac crac;
    private Network network;
    private RaoResult raoResult;
    private PhysicalParameter physicalParameter;
    private ZonalData<Scalable> scalableZonalData;

    public static MonitoringInputBuilder buildWithVoltage(Network network, Crac crac, RaoResult raoResult) {
        return new MonitoringInputBuilder().withNetwork(network).withCrac(crac).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.VOLTAGE);
    }

    public static MonitoringInputBuilder buildWithAngle(Network network, Crac crac, RaoResult raoResult, ZonalData<Scalable> scalableZonalData) {
        return new MonitoringInputBuilder().withNetwork(network).withCrac(crac).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.ANGLE).withScalableZonalData(scalableZonalData);
    }

    public Crac getCrac() {
        return crac;
    }

    public Network getNetwork() {
        return network;
    }

    public RaoResult getRaoResult() {
        return raoResult;
    }

    public PhysicalParameter getPhysicalParameter() {
        return physicalParameter;
    }

    public ZonalData<Scalable> getScalableZonalData() {
        return scalableZonalData;
    }
}
