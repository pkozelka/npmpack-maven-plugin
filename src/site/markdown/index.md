## Overview

NPMPACK maven plugin simplifies integration NPM-based build of UI into Maven build.

It is designed to:

- reduce or prevent NPM to download bits from whole internet during regular project build
- cache npm dependencies by Maven's mature repository and caching system

## How it works

We work with the assumption that the npm dependencies (expressed in the `package.json` file) do not change very often.
Therefore we differentiate between two build types:

* **regular** build, which is the one that your CI system is configured to always perform, and developer uses most of the time. NPM is *not* allowed to access internet; everything must come through Maven repositories.
* **maintenance** build, during which developer prepares Maven artifacts wrapping the changed npm dependencies. NPM of course uses internet to gather the packages.

### Regular build
This is the everyday build, intended for use in Jenkins and most developer usecases.
To enforce it, add `-Dnpmpack.allowNpmInstall=false` to Maven commandline - but it is a default, unless your POMs define otherwise.

### Maintenance build
During this build, we allow NPM to install components, or different versions, from internet.
This gives the developer an opportunity to take care with any issues that NPM may have.
To use it, add `-Dnpmpack.allowNpmInstall` to your Maven Commandline.

In this case, the plugin does not fail when `package.json` is changed; instead, it invokes `npm install` and generates new package. The developer should upload this new package into a Nexus (or other Maven Repository Manager) so that CI and other developers can use it.

Note that the generated artifact comes together with generated pom file, to avoid any mistakes during upload.
