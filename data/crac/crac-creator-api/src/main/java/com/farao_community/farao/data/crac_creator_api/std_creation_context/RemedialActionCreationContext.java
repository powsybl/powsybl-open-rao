/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api.std_creation_context;

public interface RemedialActionCreationContext {

    String getNativeRAId();

    boolean isImported();

    //idea: add here a method getStatus, which returns an enum with additional information on why the
    //native RA was not imported

    String getCreatedRAId();
}
