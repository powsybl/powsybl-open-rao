package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class FbConstraintImporterTest {

    private FbConstraintCracCreationParameters fbConstraintCracCreationParameters;
    private CracCreationParameters cracCreationParameters;
    private Network network;

    @BeforeEach
    void setUp() {
        final VirtualHubsConfiguration virtualHubsConfiguration = XmlVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/hvdc/virtualhubsconfiguration.xml"));
        fbConstraintCracCreationParameters = new FbConstraintCracCreationParameters();
        fbConstraintCracCreationParameters.setTimestamp(OffsetDateTime.parse("2026-01-27T17:00Z"));
        fbConstraintCracCreationParameters.setInternalHvdcs(virtualHubsConfiguration.getInternalHvdcs());
        cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(FbConstraintCracCreationParameters.class, fbConstraintCracCreationParameters);
        network = Network.read("network_mini2.uct", getClass().getResourceAsStream("/hvdc/network.uct"));
    }

    @Test
    void testExistsFileIsNotXml() {
        final FbConstraintImporter importer = new FbConstraintImporter();
        Assertions.assertThat(importer.exists("complex_variants.txt", getClass().getResourceAsStream("/merged_cb/complex_variants.xml"))).isFalse();
    }

    @Test
    void testExistsFileHasInvalidFormat() {
        final FbConstraintImporter importer = new FbConstraintImporter();
        Assertions.assertThat(importer.exists("with_invalid_format.xml", getClass().getResourceAsStream("/merged_cb/with_invalid_format.xml"))).isFalse();
    }

    @Test
    void testExistsFileHasUnsupportedFormat() {
        final FbConstraintImporter importer = new FbConstraintImporter();
        Assertions.assertThat(importer.exists("with_xsd_v11.xml", getClass().getResourceAsStream("/merged_cb/with_xsd_v11.xml"))).isFalse(); // v11
    }

    @Test
    void testExistsOK() {
        final FbConstraintImporter importer = new FbConstraintImporter();
        Assertions.assertThat(importer.exists("complex_variants.xml", getClass().getResourceAsStream("/merged_cb/complex_variants.xml"))).isTrue(); // v17
        Assertions.assertThat(importer.exists("thresholds_test.xml", getClass().getResourceAsStream("/merged_cb/thresholds_test.xml"))).isTrue(); // v18
        Assertions.assertThat(importer.exists("MNEC_test.xml", getClass().getResourceAsStream("/merged_cb/MNEC_test.xml"))).isTrue(); // v23
    }

    @Test
    void testImportHvdcNoComplexVariant() {
        // When
        final CracCreationContext context = new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac_without_complex_variant.xml"),
            cracCreationParameters,
            network);

        // Then
        final Set<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions();
        Assertions.assertThat(injectionRangeActions).isEmpty();
        Assertions.assertThat(context.getCreationReport().getReport())
            .contains("[WARN] the flow-based constraint document does not contain any complex variant");
    }

    @Test
    void testImportHvdcHvdcNoComplexVariantForTimestamp() {
        // Given
        fbConstraintCracCreationParameters.setTimestamp(OffsetDateTime.parse("2026-01-27T00:00Z"));

        // When
        final CracCreationContext context = new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac.xml"),
            cracCreationParameters,
            network);

        // Then
        final Set<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions();
        Assertions.assertThat(injectionRangeActions).isEmpty();
        Assertions.assertThat(context.getCreationReport().getReport())
            .contains("[WARN] the flow-based constraint document does not contain any complex variant for the requested timestamp");
    }

    @Test
    void testImportHvdcInvalidActionsSet() {
        // When
        final FbConstraintCreationContext context = (FbConstraintCreationContext) new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac_with_invalid_actionsset.xml"),
            cracCreationParameters,
            network);

        // Then
        Assertions.assertThat(context.getCrac().getInjectionRangeActions()).isEmpty();
        Assertions.assertThat(context.getRemedialActionCreationContexts()).hasSize(4);
        final List<String> notImportedStatusDetails = context.getRemedialActionCreationContexts().stream()
            .filter(e -> !e.isImported())
            .map(ElementaryCreationContext::getImportStatusDetail)
            .toList();
        Assertions.assertThat(notImportedStatusDetails)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                "complex variant D4_RA_99991 was removed as it should contain one and only one actionSet",
                "complex variant D4_RA_99992 was removed as it should contain one and only one actionSet"
            );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "crac_with_invalid_action_data.xml, action's elementCategory not recognized",
        "crac_with_no_action_in_actionsset.xml, it must contain at least one action",
        "crac_with_multiple_action_in_actionsset.xml, it contains several actions",
        "crac_with_action_preventive_and_curative.xml, it cannot be preventive and curative"
    })
    void testImportHvdcInvalidActionData(final String inputFile, final String importStatusDetails) {
        // When
        final FbConstraintCreationContext context = (FbConstraintCreationContext) new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/" + inputFile),
            cracCreationParameters,
            network);

        // Then
        Assertions.assertThat(context.getCrac().getInjectionRangeActions()).isEmpty();
        Assertions.assertThat(context.getRemedialActionCreationContexts()).hasSize(2);
        final List<String> notImportedStatusDetails = context.getRemedialActionCreationContexts().stream()
            .filter(e -> !e.isImported())
            .map(ElementaryCreationContext::getImportStatusDetail)
            .toList();
        Assertions.assertThat(notImportedStatusDetails)
            .hasSize(1)
            .containsExactly("complex variant D4_RA_99991 was removed as " + importStatusDetails);
    }

    @Test
    void testImportHvdcNoAfterCOForCurative() {
        // When
        final FbConstraintCreationContext context = (FbConstraintCreationContext) new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac_with_curative_action_but_no_after_co.xml"),
            cracCreationParameters,
            network);

        // Then
        Assertions.assertThat(context.getCrac().getInjectionRangeActions()).isEmpty();
        Assertions.assertThat(context.getRemedialActionCreationContexts()).hasSize(2);
        final List<String> notImportedStatusDetails = context.getRemedialActionCreationContexts().stream()
            .filter(e -> !e.isImported())
            .map(ElementaryCreationContext::getImportStatusDetail)
            .toList();
        Assertions.assertThat(notImportedStatusDetails)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                "complex variant D4_RA_99991 was removed as its 'afterCOList' is empty",
                "complex variant D7_RA_99991 was removed as its 'afterCOList' is empty"
            );
    }

    @Test
    void testImportHvdcInvalidAfterCOIdForCurative() {
        // When
        final FbConstraintCreationContext context = (FbConstraintCreationContext) new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac_with_curative_action_but_invalid_after_co_ids.xml"),
            cracCreationParameters,
            network);

        // Then
        Assertions.assertThat(context.getCrac().getInjectionRangeActions()).isEmpty();
        Assertions.assertThat(context.getRemedialActionCreationContexts()).hasSize(2);
        final List<String> notImportedStatusDetails = context.getRemedialActionCreationContexts().stream()
            .filter(e -> !e.isImported())
            .map(ElementaryCreationContext::getImportStatusDetail)
            .toList();
        Assertions.assertThat(notImportedStatusDetails)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                "complex variant D4_RA_99991 was removed as all its 'afterCO' are invalid",
                "complex variant D7_RA_99991 was removed as all its 'afterCO' are invalid"
            );
    }

    @Test
    void testImportHvdcWithSingleComplexVariant() {
        // When
        final FbConstraintCreationContext context = (FbConstraintCreationContext) new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac_with_single_complex_variant.xml"),
            cracCreationParameters,
            network);

        // Then
        Assertions.assertThat(context.getCrac().getInjectionRangeActions()).isEmpty();
        Assertions.assertThat(context.getRemedialActionCreationContexts()).hasSize(1);
    }

    @Test
    void testImportHvdcDisconnectedFromMainComponent() {
        // Given
        final Network network = Network.read("network_mini2_hvdc_disconnected_from_main_connected_component.uct", getClass().getResourceAsStream("/hvdc/network_with_hvdc_disconnected_from_main_connected_component.uct"));

        // When
        final FbConstraintCreationContext context = (FbConstraintCreationContext) new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac.xml"),
            cracCreationParameters,
            network);

        // Then
        final Set<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions();
        Assertions.assertThat(injectionRangeActions).isEmpty();
        Assertions.assertThat(context.getRemedialActionCreationContexts())
            .hasSize(4);
        final SoftAssertions softly = new SoftAssertions();
        for (int i = 0; i < 4; i++) {
            final ElementaryCreationContext element = context.getRemedialActionCreationContexts().get(i);
            softly.assertThat(element.getImportStatus()).isEqualTo(ImportStatus.INCONSISTENCY_IN_DATA);
            softly.assertThat(element.getImportStatusDetail())
                .startsWith("Buses matching ")
                .endsWith(" in the network do not hold generators connected to the main grid");
        }
        softly.assertAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"preventive", "curative"})
    void testImportHvdcInvalidUsageRules(String state) {
        // When
        final FbConstraintCreationContext context = (FbConstraintCreationContext) new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac_with_invalid_" + state + ".xml"),
            cracCreationParameters,
            network);

        // Then
        final Set<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions();
        Assertions.assertThat(injectionRangeActions).isEmpty();
        Assertions.assertThat(context.getRemedialActionCreationContexts())
            .hasSize(2);
        final SoftAssertions softly = new SoftAssertions();
        for (int i = 0; i < 2; i++) {
            final ElementaryCreationContext element = context.getRemedialActionCreationContexts().get(i);
            softly.assertThat(element.getImportStatus()).isEqualTo(ImportStatus.INCONSISTENCY_IN_DATA);
            softly.assertThat(element.getImportStatusDetail())
                .isEqualTo("Invalid because other ComplexVariant has opposite activation rule on " + state + " state");
        }
        softly.assertAll();
    }

    @Test
    void testImportHvdcPreventiveOnly() {
        // When
        final CracCreationContext context = new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac_with_preventive_complex_variants_only.xml"),
            cracCreationParameters,
            network);

        // Then
        final List<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions().stream()
            .sorted(Comparator.comparing(InjectionRangeAction::getId))
            .toList();
        Assertions.assertThat(injectionRangeActions).hasSize(2);

        final InjectionRangeAction firstRA = injectionRangeActions.get(0);
        final InjectionRangeAction secondRA = injectionRangeActions.get(1);

        SoftAssertions softAssertions = new SoftAssertions();
        assertRangeActionContent(softAssertions, firstRA, "D7_RA_99991_D4_RA_99991", "PRA_TEST_1A_PRA_TEST_1", "D7_D4", Optional.of("Esgaroth_Numenor"), 1000.0);
        softAssertions.assertThat(firstRA.getUsageRules()).hasSize(1);
        final Instant firstRAInstant = firstRA.getUsageRules().stream().findFirst().get().getInstant();
        softAssertions.assertThat(firstRAInstant.isPreventive()).isTrue();
        softAssertions.assertThat(firstRAInstant.isCurative()).isFalse();
        softAssertions.assertThat(firstRA.getRanges()).hasSize(1);
        softAssertions.assertThat(firstRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("min", -800.0);
        softAssertions.assertThat(firstRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("max", 1000.0);
        softAssertions.assertThat(firstRA.getInjectionDistributionKeys()).hasSize(2);
        softAssertions.assertThat(
                firstRA.getInjectionDistributionKeys().entrySet().stream()
                    .collect(
                        Collectors.toMap(entry -> entry.getKey().getId(),
                            Map.Entry::getValue))
            )
            .containsExactlyInAnyOrderEntriesOf(Map.of("D4AAA11B_generator", 1.0, "D7AAA11A_generator", -1.0));
        softAssertions.assertAll();

        softAssertions = new SoftAssertions();
        assertRangeActionContent(softAssertions, secondRA, "D7_RA_99992_D4_RA_99992", "PRA_TEST_2A_PRA_TEST_2", "D7_D4", Optional.of("Esgaroth_Numenor"), 1000.0);
        softAssertions.assertThat(secondRA.getUsageRules()).hasSize(1);
        final Instant secondRAInstant = secondRA.getUsageRules().stream().findFirst().get().getInstant();
        softAssertions.assertThat(secondRAInstant.isPreventive()).isTrue();
        softAssertions.assertThat(secondRAInstant.isCurative()).isFalse();
        softAssertions.assertThat(secondRA.getRanges()).hasSize(1);
        softAssertions.assertThat(secondRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("min", -700.0);
        softAssertions.assertThat(secondRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("max", -100.0);
        softAssertions.assertThat(secondRA.getInjectionDistributionKeys()).hasSize(2);
        softAssertions.assertThat(
                secondRA.getInjectionDistributionKeys().entrySet().stream()
                    .collect(
                        Collectors.toMap(entry -> entry.getKey().getId(),
                            Map.Entry::getValue))
            )
            .containsExactlyInAnyOrderEntriesOf(Map.of("D4AAA21B_generator", 1.0, "D7AAA21A_generator", -1.0));
        softAssertions.assertAll();
    }

    @Test
    void testImportHvdcCurativeOnly() {
        // When
        final CracCreationContext context = new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac_with_curative_complex_variants_only.xml"),
            cracCreationParameters,
            network);

        // Then
        final List<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions().stream()
            .sorted(Comparator.comparing(InjectionRangeAction::getId))
            .toList();
        Assertions.assertThat(injectionRangeActions).hasSize(2);

        final InjectionRangeAction firstRA = injectionRangeActions.get(0);
        final InjectionRangeAction secondRA = injectionRangeActions.get(1);

        SoftAssertions softAssertions = new SoftAssertions();
        assertRangeActionContent(softAssertions, firstRA, "D7_RA_99991_D4_RA_99991", "PRA_TEST_1A_PRA_TEST_1", "D7_D4", Optional.of("Esgaroth_Numenor"), 1000.0);
        softAssertions.assertThat(firstRA.getUsageRules()).hasSize(2);
        final List<OnContingencyState> firstRAUsageRules = firstRA.getUsageRules().stream().map(ur -> (OnContingencyState) ur).toList();
        softAssertions.assertThat(firstRAUsageRules.stream().map(OnContingencyState::getInstant).map(Instant::isPreventive))
            .containsExactly(false, false);
        softAssertions.assertThat(firstRAUsageRules.stream().map(OnContingencyState::getInstant).map(Instant::isCurative))
            .containsExactly(true, true);
        softAssertions.assertThat(firstRAUsageRules.stream().map(OnContingencyState::getContingency).map(Contingency::getId))
            .containsExactlyInAnyOrder("OUTAGE_1", "OUTAGE_2");
        softAssertions.assertThat(firstRA.getRanges()).hasSize(1);
        softAssertions.assertThat(firstRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("min", -800.0);
        softAssertions.assertThat(firstRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("max", 1000.0);
        softAssertions.assertThat(firstRA.getInjectionDistributionKeys()).hasSize(2);
        softAssertions.assertThat(
                firstRA.getInjectionDistributionKeys().entrySet().stream()
                    .collect(
                        Collectors.toMap(entry -> entry.getKey().getId(),
                            Map.Entry::getValue))
            )
            .containsExactlyInAnyOrderEntriesOf(Map.of("D4AAA11B_generator", 1.0, "D7AAA11A_generator", -1.0));
        softAssertions.assertAll();

        softAssertions = new SoftAssertions();
        assertRangeActionContent(softAssertions, secondRA, "D7_RA_99992_D4_RA_99992", "PRA_TEST_2A_PRA_TEST_2", "D7_D4", Optional.of("Esgaroth_Numenor"), 1000.0);
        softAssertions.assertThat(secondRA.getUsageRules()).hasSize(1);
        final OnContingencyState secondRAUsageRule = (OnContingencyState) secondRA.getUsageRules().stream().findFirst().get();
        softAssertions.assertThat(secondRAUsageRule.getInstant().isPreventive()).isFalse();
        softAssertions.assertThat(secondRAUsageRule.getInstant().isCurative()).isTrue();
        softAssertions.assertThat(secondRAUsageRule.getContingency().getId()).isEqualTo("OUTAGE_1");
        softAssertions.assertThat(secondRA.getRanges()).hasSize(1);
        softAssertions.assertThat(secondRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("min", -700.0);
        softAssertions.assertThat(secondRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("max", -100.0);
        softAssertions.assertThat(secondRA.getInjectionDistributionKeys()).hasSize(2);
        softAssertions.assertThat(
                secondRA.getInjectionDistributionKeys().entrySet().stream()
                    .collect(
                        Collectors.toMap(entry -> entry.getKey().getId(),
                            Map.Entry::getValue))
            )
            .containsExactlyInAnyOrderEntriesOf(Map.of("D4AAA21B_generator", 1.0, "D7AAA21A_generator", -1.0));
        softAssertions.assertAll();
    }

    @Test
    void testImportHvdcMixedPreventiveCurative() {
        // When
        final CracCreationContext context = new FbConstraintImporter().importData(
            getClass().getResourceAsStream("/hvdc/crac_with_mixed_preventive_curative_complex_variants.xml"),
            cracCreationParameters,
            network);

        // Then
        final List<InjectionRangeAction> injectionRangeActions = context.getCrac().getInjectionRangeActions().stream()
            .sorted(Comparator.comparing(InjectionRangeAction::getId))
            .toList();
        Assertions.assertThat(injectionRangeActions).hasSize(2); // TODO If we implement removal of inconsistent InjectionRangeActions, the size will be 0

        final InjectionRangeAction firstRA = injectionRangeActions.get(0);
        final InjectionRangeAction secondRA = injectionRangeActions.get(1);

        SoftAssertions softAssertions = new SoftAssertions();
        assertRangeActionContent(softAssertions, firstRA, "D7_RA_99991_D4_RA_99991", "PRA_TEST_1A_PRA_TEST_1", "D7_D4", Optional.of("Esgaroth_Numenor"), 1000.0);
        softAssertions.assertThat(firstRA.getUsageRules()).hasSize(1);
        final Instant firstRAInstant = firstRA.getUsageRules().stream().findFirst().get().getInstant();
        softAssertions.assertThat(firstRAInstant.isPreventive()).isTrue();
        softAssertions.assertThat(firstRAInstant.isCurative()).isFalse();
        softAssertions.assertThat(firstRA.getRanges()).hasSize(1);
        softAssertions.assertThat(firstRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("min", -800.0);
        softAssertions.assertThat(firstRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("max", 1000.0);
        softAssertions.assertThat(firstRA.getInjectionDistributionKeys()).hasSize(2);
        softAssertions.assertThat(
                firstRA.getInjectionDistributionKeys().entrySet().stream()
                    .collect(
                        Collectors.toMap(entry -> entry.getKey().getId(),
                            Map.Entry::getValue))
            )
            .containsExactlyInAnyOrderEntriesOf(Map.of("D4AAA11B_generator", 1.0, "D7AAA11A_generator", -1.0));
        softAssertions.assertAll();

        softAssertions = new SoftAssertions();
        assertRangeActionContent(softAssertions, secondRA, "D7_RA_99992_D4_RA_99992", "PRA_TEST_2A_PRA_TEST_2", "D7_D4", Optional.of("Esgaroth_Numenor"), 1000.0);
        softAssertions.assertThat(secondRA.getUsageRules()).hasSize(1);
        final OnContingencyState secondRAUsageRule = (OnContingencyState) secondRA.getUsageRules().stream().findFirst().get();
        softAssertions.assertThat(secondRAUsageRule.getInstant().isPreventive()).isFalse();
        softAssertions.assertThat(secondRAUsageRule.getInstant().isCurative()).isTrue();
        softAssertions.assertThat(secondRAUsageRule.getContingency().getId()).isEqualTo("OUTAGE_1");
        softAssertions.assertThat(secondRA.getRanges()).hasSize(1);
        softAssertions.assertThat(secondRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("min", -700.0);
        softAssertions.assertThat(secondRA.getRanges().getFirst()).hasFieldOrPropertyWithValue("max", -100.0);
        softAssertions.assertThat(secondRA.getInjectionDistributionKeys()).hasSize(2);
        softAssertions.assertThat(
                secondRA.getInjectionDistributionKeys().entrySet().stream()
                    .collect(
                        Collectors.toMap(entry -> entry.getKey().getId(),
                            Map.Entry::getValue))
            )
            .containsExactlyInAnyOrderEntriesOf(Map.of("D4AAA21B_generator", 1.0, "D7AAA21A_generator", -1.0));
        softAssertions.assertAll();
    }

    private static void assertRangeActionContent(final SoftAssertions softAssertions,
                                                 final InjectionRangeAction firstRA,
                                                 final String id,
                                                 final String name,
                                                 final String operator,
                                                 final Optional<String> groupId,
                                                 final double initialSetpoint) {
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("id", id);
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("name", name);
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("operator", operator);
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("groupId", groupId);
        softAssertions.assertThat(firstRA).hasFieldOrPropertyWithValue("initialSetpoint", initialSetpoint);
    }
}
