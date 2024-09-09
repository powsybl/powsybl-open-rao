package com.powsybl.openrao.data.cracio.csaprofiles.nc;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record GridStateAlterationRemedialAction(String mrid, String name, String remedialActionSystemOperator, String kind, Boolean normalAvailable, String timeToImplement) implements RemedialAction {
}
