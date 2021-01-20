/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.scripting

import com.farao_community.farao.commons.ZonalData
import com.farao_community.farao.commons.ZonalDataImpl
import com.farao_community.farao.data.crac_api.Crac
import com.farao_community.farao.data.flowbased_domain.json.JsonFlowbasedDomain
import com.farao_community.farao.flowbased_computation.FlowbasedComputationParameters
import com.farao_community.farao.flowbased_computation.FlowbasedComputationProvider
import com.farao_community.farao.flowbased_computation.FlowbasedComputationResult
import com.farao_community.farao.flowbased_computation.impl.FlowbasedComputationImpl;
import com.google.auto.service.AutoService
import com.powsybl.computation.ComputationManager
import com.powsybl.iidm.network.Network
import com.powsybl.scripting.groovy.GroovyScriptExtension
import com.powsybl.sensitivity.factors.variables.LinearGlsk

import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author Viktor Terrier <viktor.terrier at rte-france.com>
 */
@AutoService(GroovyScriptExtension.class)
class FlowbasedGroovyScriptExtension implements GroovyScriptExtension {

    private final FlowbasedComputationParameters parameters

    FlowbasedGroovyScriptExtension(FlowbasedComputationParameters parameters) {
        assert parameters
        this.parameters = parameters
    }

    FlowbasedGroovyScriptExtension() {
        this(FlowbasedComputationParameters.load())
    }

    static void run(Network network, Crac crac, Map<String, LinearGlsk> mapGlsks, String pathOutput, FlowbasedComputationParameters parameters) {
        ZonalData<LinearGlsk> linGlsk = new ZonalDataImpl<LinearGlsk>(mapGlsks);

        FlowbasedComputationProvider flowbasedComputationProvider = new FlowbasedComputationImpl();
        FlowbasedComputationResult fbRes = flowbasedComputationProvider.run(network, crac, linGlsk, parameters).join();
        JsonFlowbasedDomain.write(fbRes.getFlowBasedDomain(), Files.newOutputStream(Paths.get(pathOutput)));
    }

    @Override
    void load(Binding binding, ComputationManager computationManager) {
        binding.flowbased = { Network network, Crac crac, Map<String, LinearGlsk> mapGlsks, String pathOutput, FlowbasedComputationParameters parameters = this.parameters ->
            run(network, crac, mapGlsks, pathOutput, parameters)
        }
    }

    @Override
    void unload() {
    }
}
