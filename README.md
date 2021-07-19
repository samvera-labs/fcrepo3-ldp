# fcrepo3-ldp

A JAX-RS resource to be bundled into a [fcrepo-3.8.1](https://github.com/fcrepo3/fcrepo/tree/v3.8.1) webapp exposing Fedora 3 objects behind a limited [LDP](https://www.w3.org/TR/ldp/) implementation.

## Installing

This project will produce a `fedora.war` file that is a drop-in replacement,
except that it includes the LDP implementation.

To wire it up, you will need to add the `ldp-jaxrs.xml` Spring Bean file to
your deployed config alongside the existing JAX-RS config files. This will
typically mean copying it to `${fedora.home}/server/config/spring/web/jaxrs`.

The requirements for building are the same as the rest of Fedora 3.8. Running
`mvn install` at the top level will build everything. Then
`fcrepo-webapp-fedora/target/fedora.war` can then be installed.

The LDP endpoint will be served at `/fedora/ldp`, alongside the other JAX-RS
endpoints, such as `/fedora/objects`.

## Structure

There are three Maven projects here:

 * fcrepo3-ldp - A simple aggregator project
 * fcrepo-jaxrs-ldp - LDP implementation over Fedora with JAX-RS/CXF (jar)
 * fcrepo-webapp-fedora - Project to overlay the LDP jar and produce `fedora.war`

## Cleanup to Do

 * Create a patch/transformation for web.xml, which is currently copied from the
   [fcrepo-webapp-fedora](https://github.com/fcrepo3/fcrepo/tree/v3.8.1/fcrepo-webapp/fcrepo-webapp-fedora)
   project and modified. The only changes are the addition of a CXFServlet and
   a `servlet-mapping`.
 * Look at switching from RDF4J to Sesame, which is already packed. There does
   not seem to be anything feature in RDF4J that necessitates shipping another
   version of the library.

## Contributing

If you're working on PR for this project, create a feature branch off of `main`.

This repository follows the [Samvera Community Code of Conduct](https://samvera.atlassian.net/wiki/spaces/samvera/pages/405212316/Code+of+Conduct) and [language recommendations](https://github.com/samvera/maintenance/blob/master/templates/CONTRIBUTING.md#language).  Please ***do not*** create a branch called `master` for this repository or as part of your pull request; the branch will either need to be removed or renamed before it can be considered for inclusion in the code base and history of this repository.
