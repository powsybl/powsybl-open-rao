# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

Feature: US 12.15: export different reason per perimeter in SWE CNE

  @fast @rao @mock @contingency-scenarios
  ## US 12.15.1 : One contingency failing during 1st PRAO
  # Post-contingency sensi fails after 1P.
  # Contingency is skipped in curative, final objective must take into account sensi failure virtual cost.
  # RAO status is set to failure
  Scenario: US 12.15.1.1: One contingency failing during 1st PRAO, no 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_1.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    When I launch search_tree_rao
    Then the calculation partially fails
    And 1 remedial actions are used in preventive
    And the remedial action "PRA_OPEN_N1013_N1014" is used in preventive
    And 0 remedial actions are used after "CO_N4011_N4021" at "curative"
    And the worst margin is 6.2 A on cnec "N1011_N1013 - preventive"
    And the value of the objective function initially should be 10075.6
    And the value of the objective function after PRA should be 9993.8
    And the value of the objective function after CRA should be 9993.8

  @fast @rao @mock @ac @contingency-scenarios @second-preventive
  # 2P finds the same result as 1P
  Scenario: US 12.15.1.2: One contingency failing during 1st PRAO with 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_1.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjective.json"
    When I launch search_tree_rao
    Then the calculation partially fails
    And 1 remedial actions are used in preventive
    And the remedial action "PRA_OPEN_N1013_N1014" is used in preventive
    And 0 remedial actions are used after "CO_N4011_N4021" at "curative"
    And the worst margin is 6.2 A on cnec "N1011_N1013 - preventive"
    And the value of the objective function initially should be 10075.6
    And the value of the objective function after PRA should be 9993.8
    And the value of the objective function after CRA should be 9993.8

  @fast @rao @mock @ac @contingency-scenarios @second-preventive
  # Global 2P finds the same result as 1P
  Scenario: US 12.15.1.3: One contingency failing during 1st PRAO with global 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_1.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveWithGlobal2P.json"
    When I launch search_tree_rao
    Then the calculation partially fails
    And 1 remedial actions are used in preventive
    And the remedial action "PRA_OPEN_N1013_N1014" is used in preventive
    And 0 remedial actions are used after "CO_N4011_N4021" at "curative"
    And the worst margin is 6.2 A on cnec "N1011_N1013 - preventive"
    And the value of the objective function initially should be 10075.6
    And the value of the objective function after PRA should be 9993.8
    And the value of the objective function after CRA should be 9993.8

  @fast @cne-export @mock
  # CNE export
  Scenario: US 12.15.1.4: CNE export: one contingency failing during 1st PRAO
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_1.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    Given RaoResult file is "epic12/RaoResult_12_15_1.json"
    When I import data
    And I export SWE CNE
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_15_1.xml"

  @fast @rao @mock @ac @contingency-scenarios
  ## US 12.15.2 : One contingency failing during 1st ARAO
  # Failure happens when applying topo ARA.
  # Contingency is skipped in curative, final objective must take into account sensi failure virtual cost.
  # RAO status is set to failure
  Scenario: US 12.15.2.1: One contingency failing during 1st ARAO, no 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_2.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    When I launch search_tree_rao
    Then the execution details should be "First preventive fell back to initial situation"
    And 0 remedial actions are used in preventive
    And the worst margin is -1419.4 A

  @fast @rao @mock @ac @contingency-scenarios @second-preventive
  # sensi pre 2P partially fails
  Scenario: US 12.15.2.2: one contingency failing during 1st ARAO, with 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_2.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjective.json"
    When I launch search_tree_rao
    Then the execution details should be "Second preventive fell back to initial situation"
    And 0 remedial actions are used in preventive
    And the worst margin is -1419.4 A

  @fast @rao @mock @ac @contingency-scenarios @second-preventive
  # sensi pre 2P partially fails
  Scenario: US 12.15.2.3: one contingency failing during 1st ARAO, with global 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_2.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveWithGlobal2P.json"
    When I launch search_tree_rao
    Then the execution details should be "Second preventive fell back to initial situation"
    And 0 remedial actions are used in preventive
    And the worst margin is -1419.4 A

  @fast @cne-export @mock
  # CNE export
  Scenario: US 12.15.2.4: CNE export: one contingency failing during 1st ARAO
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_2.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    Given RaoResult file is "epic12/RaoResult_12_15_2.json"
    When I import data
    And I export SWE CNE
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_15_2.xml"

  @fast @rao @mock @ac @contingency-scenarios
  ## US 12.15.3 : One contingency failing during CRAO
  # Failure happens when applying topo ARA
  # Contingency is skipped in curative, final objective must take into account sensi failure virtual cost.
  # RAO status is set to failure
  Scenario: US 12.15.3.1: One contingency failing during CRAO, no 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_3.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    When I launch search_tree_rao
    Then the execution details should be "First preventive fell back to initial situation"
    And 0 remedial actions are used in preventive
    And the worst margin is -1419.4 A on cnec "N1013_N1014 - CO_N1012_N4012 - curative"

  @fast @rao @mock @ac @contingency-scenarios @second-preventive
  # 2P does not apply the same PRA as previously since it leads to a sensi divergence on a curative perimeter : no PRA is applied
  # RAO status is set to default
  # Keep in mind that since contingency CO_N1012_N4012 is no longer in failure, associated cnecs are now part of limiting elements and minimum margin computation
  Scenario: US 12.15.3.2: One contingency failing during CRAO, with 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_3.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjective.json"
    When I launch search_tree_rao
    Then the calculation succeeds
    And 0 remedial actions are used in preventive
    And 0 remedial actions are used after "CO_N1012_N4012" at "curative"
    And the worst margin is -1419.4 A on cnec "N1013_N1014 - CO_N1012_N4012 - curative"
    And the value of the objective function initially should be 1419.4
    And the value of the objective function after PRA should be 1419.4
    And the value of the objective function after CRA should be 1419.4

  @fast @rao @mock @ac @contingency-scenarios @second-preventive
  Scenario: US 12.15.3.3: One contingency failing during CRAO, with global 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_3.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveWithGlobal2P.json"
    When I launch search_tree_rao
    Then the calculation succeeds
    And 0 remedial actions are used in preventive
    And 0 remedial actions are used after "CO_N1012_N4012" at "curative"
    And the worst margin is -1419.4 A on cnec "N1013_N1014 - CO_N1012_N4012 - curative"
    And the value of the objective function initially should be 1419.4
    And the value of the objective function after PRA should be 1419.4
    And the value of the objective function after CRA should be 1419.4

  @fast @cne-export @mock
  # CNE export without 2P
  Scenario: US 12.15.3.4: CNE export: One contingency failing during CRAO, no 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_3.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    Given RaoResult file is "epic12/RaoResult_12_15_3.json"
    When I import data
    And I export SWE CNE
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_15_3.xml"

  @fast @cne-export @mock
  # CNE export with 2P : no divergence
  Scenario: US 12.15.3.5: CNE export: One contingency failing during CRAO, with 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_3.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjective.json"
    Given RaoResult file is "epic12/RaoResult_12_15_3_With2P.json"
    When I import data
    And I export SWE CNE
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_15_3_With2P.xml"

  @fast @rao @mock @ac @contingency-scenarios
  ## US 12.15.4 : A PRA can lead to contingency failure.
  # This PRA should not be selected due to sensitivity failure virtual cost, both for 1st PRAO and 2nd PRAO
  # RAO status is set to default
  Scenario: US 12.15.4.1: One contingency failing during 1st PRAO, no 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_4.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    When I launch search_tree_rao
    Then the calculation succeeds
    And 0 remedial actions are used in preventive
    And 0 remedial actions are used after "CO_N1012_N4012" at "curative"
    And the worst margin is -1419.4 A on cnec "N1013_N1014 - CO_N1012_N4012 - curative"
    And the value of the objective function initially should be 1419.4
    And the value of the objective function after PRA should be 1419.4
    And the value of the objective function after CRA should be 1419.4

  @fast @rao @mock @ac @contingency-scenarios @second-preventive
  # 2P finds the same result as 1P
  Scenario: US 12.15.4.2: One contingency failing during 1st and 2nd PRAO, with 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_4.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjective.json"
    When I launch search_tree_rao
    Then the calculation succeeds
    And 0 remedial actions are used in preventive
    And 0 remedial actions are used after "CO_N1012_N4012" at "curative"
    And the worst margin is -1419.4 A on cnec "N1013_N1014 - CO_N1012_N4012 - curative"
    And the value of the objective function initially should be 1419.4
    And the value of the objective function after PRA should be 1419.4
    And the value of the objective function after CRA should be 1419.4

  @fast @rao @mock @ac @contingency-scenarios @second-preventive
  # Global 2P finds the same result as 1P
  Scenario: US 12.15.4.3: One contingency failing during 1st and 2nd PRAO, with global 2P
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_4.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveWithGlobal2P.json"
    When I launch search_tree_rao
    Then the calculation succeeds
    And 0 remedial actions are used in preventive
    And 0 remedial actions are used after "CO_N1012_N4012" at "curative"
    And the worst margin is -1419.4 A on cnec "N1013_N1014 - CO_N1012_N4012 - curative"
    And the value of the objective function initially should be 1419.4
    And the value of the objective function after PRA should be 1419.4
    And the value of the objective function after CRA should be 1419.4

  @fast @cne-export @mock
  # CNE export
  Scenario: US 12.15.4.4: CNE export: one contingency failing during 2nd PRAO
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_4.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    Given RaoResult file is "epic12/RaoResult_12_15_4.json"
    When I import data
    And I export SWE CNE
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_15_4.xml"

  @fast @cne-export @mock
  Scenario: US 12.15.5: CNE export with angles
    # Copy of 12.15.4 with extra angle CNEC and extra angle values in RAO result
    # Expected CNE is the same as 12.5.4 with the extra angle value information
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_5.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    Given RaoResult file is "epic12/RaoResult_12_15_5.json"
    When I import data
    And I export SWE CNE
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_15_5.xml"

  @fast @cne-export @mock
  Scenario: US 12.15.6: CNE export with angles, no angle results
    # Copy of 12.15.4 with extra angle CNEC but no angle values in RAO result and a secure RAO
    # Should not fail. Should instead skip angle CNECs, thus expected CNE is the same as 12.5.4 (but secure)
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_5.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjectiveDisabled2P.json"
    Given RaoResult file is "epic12/RaoResult_12_15_6.json"
    When I import data at "2021-04-02 05:00"
    And I export SWE CNE
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_15_6.xml"

  @fast @cne-export @mock
  Scenario: US 12.15.7: CNE export: Check rounding on flows
    Given network file is "epic12/nordic32.xiidm"
    Given crac file is "epic12/CIM_12_15_7.xml"
    Given crac creation parameters file is "epic12/CimCracCreationParameters_MonitorLeftSide.json"
    Given configuration file is "epic12/raoParametersSweIDCC_minObjective.json"
    Given RaoResult file is "epic12/RaoResult_12_15_7.json"
    When I import data at "2021-04-02 05:00"
    And I export SWE CNE
    Then the exported CNE file is the same as "epic12/ExpectedCNE_12_15_7.xml"