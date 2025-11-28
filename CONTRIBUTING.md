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

### General rules (branch/pr naming convention etc)

Some rules one must follow **as much as possible**:

1. Any feature shall be developed on its personal branch

2. Any branch shall handle a single feature/topic

3. Branches may be of type build|ci|doc|feat|fix|perf|refactor|test|poc|dependencies  -- Note that internships will typically fall into this last category.

4. A branch name shall start with its type, followed by a / and a short description; e.g. docs/writeContributingFile

5. All newly added code shall be thoroughly tested through unit tests and non regression tests 

A corollary of all this is that all branches should have both a short lifespan and length (in terms of lines of codes). These definitions, of course, are very subjective.

### Creating an issue

If you have a task which requires some heavier modifications of a repo (a brand new feature, a big fix etc), you must create an issue.

When creating an issue, you will:

- Add an explicit title and a complete description of the task (the task is well described if and only if anyone can tackle it without need of clarifications).
- Verify that the task is not too large (if it is expected for it to take much longer than a week worth of work, please split it into smaller sub-tasks)
- The milestone (version of your library)
- Add the labels 
- Set the type of the issue (can be either task, bug or feature).

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
- Final commit message should start with `xxxx(subject): text #yy` with `xxxx` equal to `feat`, `test`, `doc`, `fix` or `chore` and `yy`is the PR number.
- Before merging by "squash and merge", edit the commit message to only keep relevant PR info.

[^1]: See OpenRAO's list of labels for Issues & Pull Requests [here](https://github.com/powsybl/powsybl-open-rao/issues/labels)