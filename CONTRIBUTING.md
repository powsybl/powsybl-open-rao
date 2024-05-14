# How to contribute to PowSyBl OpenRAO

Welcome to the OpenRAO developers and users community!  
Here is all the information you need to know before making your first contribution.

## Submitting your ideas
If you are an OpenRAO user, and you found a bug, or you would like a new feature, or 
if you are an active OpenRAO developer, and have ideas to improve the code, please let us know! 
We'll be more than happy to fix all bugs and discuss all improvement ideas.
- First, make sure that the bug/feature request/improvement idea has not been already reported under [Issues](https://github.com/powsybl/powsybl-open-rao/issues).
- If not, [open a new one](https://github.com/powsybl/powsybl-open-rao/issues/new). Provide us with as many details as possible, 
  so we can understand the issue and try to address it ASAP. 
- Label[^1] your new issue as much as possible, this makes addressing it much more efficient.

Your ideas may be discussed by the OpenRAO developers and the [PowSyBl Technical Steering Committee](https://www.powsybl.org/pages/overview/governance#technical-steering-committee) 
in order to reach a consensus.

## Development

### Getting started
The PowSyBl OpenRAO team is always pleased to welcome developers into the project.  
The best way to get a quick start is to clone the project's main branch and try running the [functional tests](tests/README.md) 
on your PC. If everything works, then you're good to make your first contribution to the code!

### Code formatting
OpenRAO uses a custom profile for JAVA code formatting.  
You can download these rules for your IDE from this directory: [developer-resources](docs/_static/developer-resources).  

### Pull Requests
The `main` branch is protected by a set of rules, and Pull Requests from your branch are mandatory.  

Here is a set of rules your pull request must follow:
- your commits must be **signed-off**
- the description of the PR should be well detailed: it must be self-explanatory and detailed enough for developers and 
  users to understand **what** has changed, **why**, and **how**
- if the PR addresses an open issue, the **issue should be linked** in the description
- you can open a PR even if it's not ready to merge, but in that case you must set it as a **draft**
- the PR must be **reviewed** by at least one other developer before it can be merged
- you must add unit **tests** and/or [functional tests](tests/README.md) to your new features
- the PR must pass a few **automatic checks** before it can be merged: build & unit tests, functional tests, sonar quality, sign-off check, doc build check
- if the PR changes a part of the code that is [documented](https://powsybl.readthedocs.io/projects/openrao/en/latest), 
  then [**documentation**](docs) must be updated before it can be merged
- **label**[^1] the PR as much as possible, this makes addressing it much more efficient


[^1]: See OpenRAO's list of labels for Issues & Pull Requests [here](https://github.com/powsybl/powsybl-open-rao/issues/labels)