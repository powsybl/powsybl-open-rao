/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.data.crac.api.Crac;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class RaoInputWithNetworkPaths {

    public static final class RaoInputWithNetworkPathBuilder {
        private Crac crac;
        private String initalNetworkPath;
        private String postIcsImportNetworkPath;

        private RaoInputWithNetworkPathBuilder() {
        }

        public RaoInputWithNetworkPathBuilder withCrac(Crac crac) {
            this.crac = crac;
            return this;
        }

        public RaoInputWithNetworkPathBuilder withInitialNetworkPath(String initialNetworkPath) {
            this.initalNetworkPath = initialNetworkPath;
            return this;
        }

        public RaoInputWithNetworkPathBuilder withPostIcsImportNetworkPath(String postIcsImportNetworkPath) {
            this.postIcsImportNetworkPath = postIcsImportNetworkPath;
            return this;
        }

        public RaoInputWithNetworkPaths build() {
            RaoInputWithNetworkPaths raoInputWithNetworkPaths = new RaoInputWithNetworkPaths();
            raoInputWithNetworkPaths.crac = crac;
            raoInputWithNetworkPaths.initialNetworkPath = initalNetworkPath;
            raoInputWithNetworkPaths.postIcsImportNetworkPath = postIcsImportNetworkPath;
            return raoInputWithNetworkPaths;
        }
    }

    private Crac crac;
    private String initialNetworkPath;
    private String postIcsImportNetworkPath;

    private RaoInputWithNetworkPaths() {
    }

    public static RaoInputWithNetworkPathBuilder build(String initialNetworkPath, String postIcsImportNetworkPath, Crac crac) {
        return new RaoInputWithNetworkPathBuilder().withInitialNetworkPath(initialNetworkPath).withPostIcsImportNetworkPath(postIcsImportNetworkPath).withCrac(crac);
    }

    public static RaoInputWithNetworkPathBuilder build(String initialNetworkPath, Crac crac) {
        return new RaoInputWithNetworkPathBuilder().withInitialNetworkPath(initialNetworkPath).withCrac(crac);
    }

    public Crac getCrac() {
        return crac;
    }

    public String getInitialNetworkPath() {
        return initialNetworkPath;
    }

    public String getPostIcsImportNetworkPath() {
        return postIcsImportNetworkPath;
    }

}
