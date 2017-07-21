# Sangria GraphQL Code Generator
[![Build Status]][Travis]
[![Latest version]][Bintray]

  [Build Status]: https://travis-ci.org/mediative/sangria-codegen.svg?branch=master
  [Travis]: https://travis-ci.org/mediative/sangria-codegen
  [Latest version]: https://api.bintray.com/packages/mediative/maven/sangria-codegen/images/download.svg
  [Bintray]: https://bintray.com/mediative/maven/sangria-codegen/_latestVersion

Generate API code based on a GraphQL schema and queries.

Inspired by [apollo-codegen](https://github.com/apollographql/apollo-codegen) and
trying to fix
[sangria-graphql/sangria#64](https://github.com/sangria-graphql/sangria/issues/64).

⚠️ *This project is still in early stages of development.* ⚠️

## Getting Started

Add the following to your `build.sbt`:

```sbt
resolvers += Resolver.bintrayRepo("mediative", "maven")
libraryDependencies += "com.mediative" %% "sangria-codegen" % "0.0.4"
```

## Documentation

 - [API](https://mediative.github.io/sangria-codegen/api/#com.mediative.sangria.codegen.package)
 - [sbt-plugin](https://mediative.github.io/sangria-codegen/sbt-plugin/#com.mediative.sangria.codegen.sbt.SangriaCodegenPlugin$)

## Building and Testing

This library is built with sbt, which needs to be installed. Run the following command from the project root, to build and run all test:

    $ sbt test

See [CONTRIBUTING.md](CONTRIBUTING.md) for how to contribute.

## Releasing

To release version `x.y.z` run:

    $ sbt -Dversion=x.y.z release

This will run the tests, create a tag and publishing JARs and API docs.

## License

Copyright 2017 Mediative

Licensed under the Apache License, Version 2.0. See LICENSE file for terms and
conditions for use, reproduction, and distribution.
