## Features

### Avoid uncontrolled dependency on internet

With `npm install` usued in the naive way, the stability of your build is affected by the availability of servers which provide npm packages.
And this is often very annoying.

### Use Maven Artifact Manager to share your artifacts

Putting artifacts into Maven repository allows your team to minimize the need for maintenance builds. The first developer uploads binary and others do not have to care.
Which is very logical; because the first developer is typically the one who changes the dependencies.

### Reproducibility of build

is the natural result of involving npmpack. You can switch back to any revision and be sure that you get the same NPM dependencies as you had by that time.
This is, surprisingly, not usual in the NPM world.

### Anonymize - share binary for several Maven modules

It often happens that you use the same set of dependencies among multiple Maven modules or even projects.
The version computed by npmpack is based on computing MD5 hash of the `package.json` file.
To allow reuse between different (and differently named, versioned) modules, we anonymize the `package.json` before computing the hash, by these means

* replacing its `name` and `version` properties with constant string
* sorting its content alphabetically
* serializing it in *prettyfied* form.

All this makes the chance for reuse similar modules quite high.
