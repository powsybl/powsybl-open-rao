# OpenRAO functional tests

## Introduction
This package contains 470+ automated functional tests for OpenRAO, for behavior-driven development. 
They are automatically run on each Pull Request using the [dedicated workflow](../.github/workflows/build_and_test.yml) 
and are a required check for merging PRs.  
The package uses the [Cucumber framework](https://cucumber.io/), 
and the tests are divided into "scenarios" and "features", according to the Cucumber terminology:
- a scenario is one functional test that covers one specific tested behavior of OpenRAO
- a feature is a collection of related scenarios

## Source code content
The source code of this package is divided in four main parts:
1. The [RunCucumberTest](src/test/java/com/powsybl/openrao/tests/RunCucumberTest.java) main class is a test class that 
   runs the Cucumber tests through a JUnit plugin
2. The [features](src/test/resources/com/powsybl/openrao/tests/features) directory contains the "feature" files, that contain the actual description 
   of the features, written in [Gerkin](https://cucumber.io/docs/gherkin/) syntax (see [Features](#features)).
3. The [files](src/test/resources/files) directory contains the file resources for the tests (CRACs, Networks, etc) 
   separated according to their business type.
4. The [java source](src/test/java/com/powsybl/openrao/tests) directory contains code that ["glues"](#glue)
   the [Gerkin features](#features) to the actual OpenRAO code.

### Features
The [features](src/test/resources/features) directory contains the "feature" files, that contain the actual description
of the features, written in [Gerkin](https://cucumber.io/docs/gherkin/) syntax.

#### Feature description
- A "feature" file actually contains multiple scenarios (tests), grouped into one major "feature"
- A feature has a title and a number (OpenRAO was historically developed following the Agile methodology, so epic numbers were used)
- Every scenario has a number and a brief title (user story numbers were used, but we continue to use numbers in order to easily identify tests)
- Every scenario can have a set of tags (`@...`) that allows advanced filtering of scenarios

#### General scenario structure
A scenario is a collection of "steps", which is an instruction written in (almost) natural language, starting with `Given`,
`When`, or `Then`.  
A scenario is generally divided into 3 parts (each part can contain multiple steps):
1. Test setup: this consists in selecting the input files for the feature of OpenRAO that is being tested (using `Given` steps). 
   For example, the RAO feature needs at least a CRAC, a Network, and a RaoParameters object.
2. Feature execution: this consists in calling the tested OpenRAO feature (using `When` steps). Currently, the following 
   OpenRAO features are tested in this package: CRAC import & creation, search-tree RAO, angle & voltage monitoring, 
   CORE & SWE CNE export, flow-based computations.
3. Output control: this consists in checking that the executed feature outputs the expected values (using `Then` steps). 
   For example, after the RAO, we can check to most limiting element's margin, or the margin of a specific CNEC after RAO, etc. 

### Glue
The "glue" term is what Cucumber describes as the actual code that translates the Gerkin into code that can call the 
tested application. For instance, here is a glue that describes how the CORE CNE export step should call OpenRAO:
```java
@When("I export CORE CNE at {string}")
public void iExportCoreCne(String timestamp) throws IOException {
    cneVersion = CneHelper.CneVersion.CORE;
    CommonTestData.loadData(timestamp);
    exportedCne = CneHelper.exportCoreCne(CommonTestData.getCrac(), CommonTestData.getCracCreationContext(), CommonTestData.getNetwork(), CommonTestData.getRaoResult(), CommonTestData.getRaoParameters());
}
```
The `@When(...)` lines glues this `iExportCoreCne` method to the Gerkin phrase `I export CORE CNE at {string}`, 
where `{string}` is a Gerkin input argument (that represents the timestamp).  
As you can see, the body of the `iExportCoreCne` method simply calls the CORE CNE API of OpenRAO.  

OpenRAO's glue code is located in the [test](src/test/java/com/powsybl/openrao/tests) directory.

## Contributing
If your development changes a behavior, you may need to update the tests.  
- If you add a new behavior to OpenRAO, try to add new "scenarios" that test this behavior, in different contexts
- If you changed or improved some behavior and broke a test, you would have to update it. Whenever possible, instead of only 
  changing a given test, try to duplicate it in order to test the old behavior (if it is still possible, using a 
  specific configuration), and the new one, in two different scenarios.

> Note: Please read the other contribution guidelines [here](../CONTRIBUTING.md).

## Running the tests

### Using maven
These tests are part of the projects build cycle, so they are automatically run using (at the root of the project):
```bash
mvn install
```
or 
```bash
mvn test
```

If you only want to run tests in this package, use:
```bash
mvn test -pl tests
```

### Using your IDE
If your IDE allows it:
- You can directly run the [RunCucumberTest](src/test/java/com/powsybl/openrao/tests/RunCucumberTest.java) class, 
  which executes all the Cucumber tests through JUnit. Optionally, you can select specific feature files to run by 
  changing its input arguments.
- You can run specific scenarios or features using IDE-specific Cucumber plugins
