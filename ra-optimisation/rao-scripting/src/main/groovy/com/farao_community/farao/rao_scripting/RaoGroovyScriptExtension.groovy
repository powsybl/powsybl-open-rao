/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_scripting

import com.farao_community.farao.data.crac_api.Crac
import com.farao_community.farao.data.crac_io_api.CracExporters
import com.farao_community.farao.rao_api.Rao
import com.farao_community.farao.rao_api.RaoInput
import com.farao_community.farao.rao_api.RaoParameters
import com.farao_community.farao.rao_api.RaoResult
import com.google.auto.service.AutoService
import com.powsybl.computation.ComputationManager
import com.powsybl.iidm.network.Network
import com.powsybl.scripting.groovy.GroovyScriptExtension

/**
 * @author Viktor Terrier <viktor.terrier at rte-france.com>
 */
@AutoService(GroovyScriptExtension.class)
class RaoGroovyScriptExtension implements GroovyScriptExtension {

    private final RaoParameters parameters

    RaoGroovyScriptExtension(RaoParameters parameters) {
        assert parameters
        this.parameters = parameters
    }

    RaoGroovyScriptExtension() {
        this(RaoParameters.load())
    }

/** Launch a rao computation and export results
 *
 * @param network: network
 * @param crac: cbcora containing contingencies, critical branches and remedial actions
 * @return pathOutput: location and name of output rao results
 */
    static void run(Network network, Crac crac, String pathOutput, RaoParameters raoParameters) {
        run(network, crac, raoParameters);

        OutputStream outputStream = new FileOutputStream(String.valueOf(pathOutput));
        CracExporters.exportCrac(crac, "Json", outputStream)
    }

/** Launch a rao computation
 *
 * @param network: network
 * @param crac: cbcora containing contingencies, critical branches and remedial actions
 */
    static void run(Network network, Crac crac, RaoParameters raoParameters) {

        RaoInput raoInput = RaoInput.build(network as Network, crac)
                .withNetworkVariantId("AfterVar")
                .build();
        RaoResult raoResult = Rao.run(raoInput, raoParameters);
    }

    @Override
    void load(Binding binding, ComputationManager computationManager) {
        binding.rao = { Network network, Crac crac, String pathOutput, RaoParameters parameters = this.parameters ->
            run(network, crac, pathOutput, parameters)
        }
    }

    @Override
    void unload() {
    }
}
