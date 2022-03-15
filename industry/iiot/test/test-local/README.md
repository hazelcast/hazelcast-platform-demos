# Hazelcast Platform Demo Applications - Industry 4.0 - Part-wear Analysis - Test - Local

Offline testing, modules to help validate the demo when no Hazelcast Cloud instance is available.

See also:
* [Cloud](../test-cloud) Online testing, runs against a Hazelcast Cloud instance

## Usage

Run

```
src/main/scripts/docker-test-local-hazelcast-0.sh
src/main/scripts/docker-test-local-hazelcast-1.sh
src/main/scripts/docker-test-local-hazelcast-2.sh
```
to create a 3-node cluster locally. Then

```
src/main/scripts/docker-test-local-client.sh
```
