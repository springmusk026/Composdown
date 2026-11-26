# Releasing

Releases are published from tags by the [release workflow](.github/workflows/release.yml). This
document is for maintainers.

## One-time setup

1. **Maven Central namespace.** Verify the `io.github.springmusk026` namespace in the
   [Central Portal](https://central.sonatype.com/) and generate a user token
   (**Account**, then **Generate User Token**).
2. **Signing key.** Create a GPG key for signing artifacts and publish its public key:

   ```bash
   gpg --full-generate-key
   ```

   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```

   Export the private key in ASCII armor for the `SIGNING_KEY` secret:

   ```bash
   gpg --export-secret-keys --armor <KEY_ID>
   ```

3. **Repository secrets** (**Settings**, **Secrets and variables**, **Actions**):

   | Secret | Value |
   |---|---|
   | `MAVEN_CENTRAL_USERNAME` | Central Portal token username |
   | `MAVEN_CENTRAL_PASSWORD` | Central Portal token password |
   | `SIGNING_KEY` | The exported private key |
   | `SIGNING_KEY_ID` | The last 8 characters of the key ID |
   | `SIGNING_KEY_PASSWORD` | The key's passphrase |

4. **GitHub Pages.** In **Settings**, **Pages**, set the source to **GitHub Actions**. The site (a
   landing page, the live web demo under `/demo/` and the API reference under `/api/`) is published
   there with each release, or on demand with the Site workflow.

## Releasing a version

1. Make sure `main` is green in CI, including the macOS job.
2. In [CHANGELOG.md](CHANGELOG.md), rename **Unreleased** to the version and date
   (`## [0.1.0] - 2026-12-01`), add an empty **Unreleased** section above it, and update the links at
   the bottom.
3. Set `VERSION_NAME` in [gradle.properties](gradle.properties) to the version.
4. Update the version in the README's installation instructions if it changed.
5. Commit (`chore: release 0.1.0`), then tag and push:

   ```bash
   git tag -a v0.1.0 -m "Composdown 0.1.0"
   ```

   ```bash
   git push origin main v0.1.0
   ```

6. The release workflow verifies the build, checks that the tag matches `VERSION_NAME`, uploads
   signed artifacts to Maven Central, creates the GitHub release with the changelog section as notes,
   and publishes the site.
7. Artifacts are uploaded as a deployment that is **not** released automatically. Review it in the
   Central Portal (**Deployments**) and select **Publish**. Artifacts appear on Maven Central within
   about 30 minutes.

## Checking a release locally

```bash
./gradlew publishToMavenLocal
```

publishes every module to `~/.m2/repository` without signing. Inspect the POMs and the artifacts, or
depend on them from a test project with `mavenLocal()`. Kotlin/Native artifacts for Apple targets are
only complete when built on macOS.

## Versioning

Composdown follows [Semantic Versioning](https://semver.org/). Before 1.0, a minor version may
contain breaking changes. Every breaking change is listed in the changelog, and changes to the public
API are visible in the `api/` dumps of each module.
