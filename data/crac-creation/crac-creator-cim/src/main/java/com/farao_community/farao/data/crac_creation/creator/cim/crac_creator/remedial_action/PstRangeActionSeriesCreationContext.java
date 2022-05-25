package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;

import java.util.Set;

public class PstRangeActionSeriesCreationContext extends RemedialActionSeriesCreationContext {
    private final String networkElementNativeMrid;
    private final String networkElementNativeName;

    private PstRangeActionSeriesCreationContext(String nativeId, Set<String> createdIds, String networkElementNativeMrid, String networkElementNativeName,
                                                ImportStatus importStatus, boolean isAltered, boolean isInverted, String importStatusDetail) {
        super(nativeId, createdIds, importStatus, isAltered, isInverted, importStatusDetail);
        this.networkElementNativeMrid = networkElementNativeMrid;
        this.networkElementNativeName = networkElementNativeName;
    }

    static PstRangeActionSeriesCreationContext notImported(String nativeId, ImportStatus importStatus, String importStatusDetail) {
        return new PstRangeActionSeriesCreationContext(nativeId, Set.of(nativeId), null, null, importStatus, false, false, importStatusDetail);
    }

    static PstRangeActionSeriesCreationContext imported(String nativeId, String networkElementNativeMrid, String networkElementNativeName, boolean isAltered, String importStatusDetail) {
        return new PstRangeActionSeriesCreationContext(nativeId, Set.of(nativeId), networkElementNativeMrid, networkElementNativeName, ImportStatus.IMPORTED, isAltered, false, importStatusDetail);
    }

    public String getNetworkElementNativeMrid() {
        return networkElementNativeMrid;
    }

    public String getNetworkElementNativeName() {
        return networkElementNativeName;
    }
}
