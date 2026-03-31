# Creating a MARIN release

1. Make sure the last CI build on `develop` was successful
2. Checkout branch `develop` locally, and create a new branch `feature/prepare-release-X.X.X` off of `develop`
3. Check [the dependabot logs](https://github.com/sse-labs/marin/security/dependabot) for vulnerabilities in the current build. If relevant vulnerabilities exist:
    * Update dependencies according to security advisories.
    * Run `mvn clean package` and make sure the dependency updates did not break compilation or tests
    * If the update breaks the MARIN build or tests, postpone release and implement dependency updates on `develop`
    * Add dependency updates as a commit to the release preparation branch
4. Verify that all code snippets in the README file actually work. To do so:
    * Run `mvn clean install -DskipTests` to install the current snapshot version locally
    * Create a demo client project that depends on the current snapshot version of MARIN
    * Copy-Paste all snippets from the MARIN README to the client project
    * Ensure the client projects builds successfully, and all executable code can in fact be executed.
    * Update README snippets if necessary, and add a commit to the release preparation branch
5. If the release preparation branch contains at least one new commit, push it to the remote repo and create a PR on GitHub to merge your changes **back into develop**
6. Update your local version of MARIN with the last changes: `git checkout develop` and `git pull`
7. Create a new branch `release/X.X.X` off of develop
8. Update the `<version>` tag within `pom.xml` to contain the actual version you want to release (i.e., remove `-SNAPSHOT`)
9. Perform a dry run for deployment:
    * Ensure `<skipPublishing>` is set to `true` in `pom.xml`
    * Ensure `settings.xml` with the publishing credentials is located in your current user's `.m2` directory
    * Ensure you have the GPG signing key installed and the passphrase is available
    * Run `mvn clean deploy -DskipTests -P release`, enter the GPG passphrase when prompted
    * If everything is configured correctly, Maven will report `BUILD SUCCESS` without actually publishing anything yet
10. Deploy the new version to Maven Central:
    * Set `<skipPublihsing>` to `false` in `pom.xml`
    * Run `mvn clean deploy -DskipTests -P release`, enter the GPG passphrase when prompted
    * Set `<skipPublihsing>` to back to `true` in `pom.xml`
11. Publish the deployment:
    * Go to the [Sonatype Website](https://central.sonatype.com/publishing/deployments) and log in to the publishing account
    * Select the recent deployment, assert that the status says `Validated`
    * Click on "Publish"
    * Publishing can take a few minutes - check back periodically until status says "Published"
12. Change current MARIN version in README.md to newly released version
13. Push your `release/X.X.X` branch and create a PR merging you changes **into the main branch**.
14. Merge your `main` branch back into `develop`, increase `<version>` attribute in `pom.xml` (either minor or patch version), and add a `-SNAPSHOT` suffix
15. Create a GitHub release on the `main` branch
16. Update your local repo with `git fetch` and delete the release branch