/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.crac_scripting

import com.farao_community.farao.data.crac_api.Crac
import com.farao_community.farao.data.crac_io_api.CracImporters
import com.farao_community.farao.data.crac_util.CracCleaner
import com.google.auto.service.AutoService
import com.powsybl.computation.ComputationManager
import com.powsybl.iidm.network.Network
import com.powsybl.scripting.groovy.GroovyScriptExtension

import java.nio.file.Paths

/**
 * @author Viktor Terrier <viktor.terrier at rte-france.com>
 */
@AutoService(GroovyScriptExtension.class)
class CracGroovyScriptExtension implements GroovyScriptExtension {

    RaoGroovyScriptExtension() {
    }

    /** Import Crac
     *
     * @param cracPath: path of the crac file
     */
    static Crac run(String cracPath) {
        return CracImporters.importCrac(Paths.get(cracPath));
    }

    /** Import Crac, clean it and synchronize it with a network
     *
     * @param cracPath: path of the crac file
     * @param network: network
     */
    static Crac run(String cracPath, Network network) {
        Crac crac = CracImporters.importCrac(Paths.get(cracPath));
        CracCleaner cleaner = new CracCleaner();
        cleaner.cleanCrac(crac, network);
        crac.synchronize(network);
        return crac;
    }

    @Override
    void load(Binding binding, ComputationManager computationManager) {
        binding.loadCrac = { String pathOutput ->
            run(pathOutput)
        }
        binding.loadCrac = { String pathOutput, Network network ->
            run(pathOutput, network)
        }
    }

    @Override
    void unload() {
    }
}
