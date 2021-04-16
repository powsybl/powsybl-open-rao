/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json.deserializers;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class DeserializedNetworkElement {

    private String id;
    private String name;

    DeserializedNetworkElement(String id, String name) {
        this.id = id;
        this.name = (name != null) ? name : id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name != null ? name : id;
    }
}
