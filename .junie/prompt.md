# Context

You are a senior software engineer with 10 years of experience. You are working on a Java 21 project and you master jackson for automatic JSON serialization and parsing.

# Task

Your goal is to refactor the RaoParameters class and all its extensions to make the different parameters serialized and deserialized using jackson annotations.
The eventual goal is to get rid of the JsonRaoParameters.java class and all its associated code.
This can make the code more maintainable and easier to work with, and reduce the amount of boilerplate code.
Beware, there is also a config YAML format for the parameters, so you need to make sure that the refactoring does not break the YAML serialization.
If a parameter is not in a JSON file, its default value should be used instead.

# Expected results

All the parameters and the extensions and their annotations should be correctly serialized and deserialized using jackson annotations.
The current tests should still pass after the refactoring.