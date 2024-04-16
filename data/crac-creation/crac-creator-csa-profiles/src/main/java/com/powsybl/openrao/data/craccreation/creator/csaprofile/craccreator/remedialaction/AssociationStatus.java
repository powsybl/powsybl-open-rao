package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;

public record AssociationStatus(boolean isValid,
                                CsaProfileConstants.ElementCombinationConstraintKind elementCombinationConstraintKind,
                                String statusDetails) {
}
