# Hazelcast Platform Demo Applications - Industry 4.0 - Part-wear Analysis

This is a [Hazelcast Cloud](https://cloud.hazelcast.com/) based demo.

## Note - package names

Hazelcast Cloud does not permit classes to be uploaded in the package `com.hazelcast` or it's sub-packages.

Packages are `hazelcast.platform.demos.industry.iiot` instead of `com.hazelcast.platform.demos.industry.iiot`.

## Note - `settings.xml`

Your `settings.xml` file needs two properties to connect to Hazelcast Cloud. `my.hz.cloud.cluster1.name` holds the name of the cluster. `my.hz.cloud.cluster1.discovery.token` is the connection token.

Both are provided when you set up the cluster. Refer to the top-level [README](../../README.md) for details.

## Testing

To facilitate testing and debugging, additional modules are provided. These are not part
of the main demo.

* [Cloud](./test/test-cloud) Online testing, runs against a Hazelcast Cloud instance
* [Local](./test/test-local) Offline testing, creates a local cluster and runs tests against it.

