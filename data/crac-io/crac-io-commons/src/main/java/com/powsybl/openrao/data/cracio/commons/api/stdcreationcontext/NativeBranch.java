/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NativeBranch {

    private String from;
    private String to;
    private String suffix;

    public NativeBranch(String from, String to, String suffix) {
        this.from = from;
        this.to = to;
        this.suffix = suffix;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getSuffix() {
        return suffix;
    }
}
