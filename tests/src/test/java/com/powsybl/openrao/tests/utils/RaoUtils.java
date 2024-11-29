/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.utils;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThresholdAdder;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.openrao.tests.steps.CommonTestData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;

public final class RaoUtils {

    private RaoUtils() {
        // should not be instantiated
    }

    public static void buildLoopFlowExtensions(Crac crac, Network network, double loopflowAsPmaxPercentage) {
        if (loopflowAsPmaxPercentage > 0) {
            for (FlowCnec cnec : crac.getFlowCnecs(crac.getPreventiveState())) {
                Line cnecLine = network.getLine(cnec.getNetworkElement().getId());

                // check if the cnec is cross zonal, if yes, apply loopflowAsPmaxPercentage on it
                if (cnecLine != null &&
                    !getTerminalCountry(cnecLine.getTerminal1()).equals(getTerminalCountry(cnecLine.getTerminal2()))) {
                    cnec.newExtension(LoopFlowThresholdAdder.class).withUnit(Unit.PERCENT_IMAX).withValue(loopflowAsPmaxPercentage / 100.).add();
                }
            }
        }
    }

    private static Optional<Country> getTerminalCountry(Terminal terminal) {
        Optional<Substation> substation = terminal.getVoltageLevel().getSubstation();
        if (substation.isPresent()) {
            return substation.get().getCountry();
        } else {
            return Optional.empty();
        }
    }

    public static RaoResult runRao(String contingencyId, InstantKind instantKind, String raoType, Double loopflowAsPmaxPercentage,
                                   Integer timeLimitInSeconds) throws IOException {
        RaoParameters raoParameters = CommonTestData.getRaoParameters();
        ZonalData<SensitivityVariableSet> glsks = CommonTestData.getLoopflowGlsks();
        // Rao with loop-flows
        if (raoParameters.getLoopFlowParameters().isPresent() && glsks != null) {
            double effectiveLfPercentage = loopflowAsPmaxPercentage == null ? 0.0 : loopflowAsPmaxPercentage;
            buildLoopFlowExtensions(CommonTestData.getCrac(), CommonTestData.getNetwork(), effectiveLfPercentage);
        }

        return runRaoInMemory(Rao.find(raoType), CommonTestData.getNetwork(), CommonTestData.getCrac(), contingencyId, instantKind, glsks, CommonTestData.getReferenceProgram(), raoParameters, timeLimitInSeconds);
    }

    private static RaoResult runRaoInMemory(Rao.Runner raoRunner, Network network, Crac crac, String contingencyId, InstantKind instantKind,
                                            ZonalData<SensitivityVariableSet> glsks, ReferenceProgram referenceProgram, RaoParameters config,
                                            Integer timeLimitInSeconds) throws IOException {

        RaoInput.RaoInputBuilder raoInputBuilder;
        if (contingencyId == null) {
            if (instantKind == null) {
                // Will optimize all the perimeters
                raoInputBuilder = RaoInput.build(network, crac);
            } else if (crac.getInstant(instantKind).isPreventive()) {
                // Will optimize preventive state only
                raoInputBuilder = RaoInput.buildWithPreventiveState(network, crac);
            } else {
                throw new IllegalArgumentException(format("Contingency ID should not be null with instant being %s - only \"Preventive\" is accepted", instantKind));
            }
        } else {
            // Perform a curative optimization only on the specified state
            raoInputBuilder = RaoInput.buildWithState(network, crac, crac.getState(crac.getContingency(contingencyId), crac.getInstant(instantKind)));
        }

        raoInputBuilder.withGlskProvider(glsks);
        raoInputBuilder.withRefProg(referenceProgram);

        RaoResult raoResult;
        if (timeLimitInSeconds != null) {
            raoResult = raoRunner.run(raoInputBuilder.build(), config, java.time.Instant.now().plusSeconds(timeLimitInSeconds.longValue()));
        } else {
            raoResult = raoRunner.run(raoInputBuilder.build(), config);
        }

        /*
        roundTrip on RaoResult
        important to keep that here, as the conversion: SearchTreeRaoResult -> jsonFile -> RaoResultImpl cannot be
        properly test - on real data - in the unit tests
         */

        return roundTripOnRaoResult(raoResult, crac);
    }

    private static RaoResult roundTripOnRaoResult(RaoResult raoResult, Crac crac) throws IOException {

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        raoResult.write("JSON", crac, properties, outputStream);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return RaoResult.read(inputStream, crac);
    }
}
