# Hazelcast Platform Demo Applications

Demonstration applications for the [Hazelcast Platform](https://hazelcast.com/products/hazelcast-platform/), the combination
of streaming analytics and in-memory data.

## Demos

1. Banking
  * [Transaction Monitor](./banking/transaction-monitor) Monitoring and aggregation of stock market trading volumes.
    * [Watch The Video](https://hazelcast.com/resources/continuous-query-with-drill-down-demo/)
  * [Credit Value Adjustment](./banking/credit-value-adjustment) Risk exposure calculation for Interest Rate Swaps.
2. Benchmark
  * [NEXMark](./benchmark/nexmark) Processing billions of events per second
3. Industry 4.0
  * [IIOT](./industry/iiot) Predictive maintenance for plant machinery
4. Machine Learning
  * [RI](./ml/ml-ri) Reference Implementation for Machine Learning Inference, minimal dependencies.
5. Retail
  * [Clickstream](./retail/clickstream) E-Commerce analysis using ML prediction.
6. Telco
  * [Churn](./telco/churn) Uses ML to predict customer churn
7. Travel
  * [Booking](./travel/booking) Integrated travel booking for accommodation and transport

Also

8. Utils
  * [Utils](./utils) Utillity modules for the above projects to share.

## Build Instructions

### `settings.xml`

Projects that use commercial features require a license key to operate. 

Register [here](https://hazelcast.com/contact/) to request the license key you need.

Once you have the license key you need, create a `settings.xml` in your `.m2` folder. Copy
the example below, and replace the property value. Maven will read this file when you build
and apply the value in your build.

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers/>
  <profiles>
    <profile>
      <id>default</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <my.hz.cloud.cluster1.name>GOES HERE</my.hz.cloud.cluster1.name>
        <my.hz.cloud.cluster1.discovery.token>GOES HERE</my.hz.cloud.cluster1.discovery.token>
        <my.hz.cloud.cluster1.password>GOES HERE</my.hz.cloud.cluster1.password>
        <my.hz.cloud.cluster1.keys.location>GOES HERE</my.hz.cloud.cluster1.keys.location>
        <my.license.key>GOES HERE</my.license.key>
        <my.slack.bot.user.oath.access.token>GOES HERE</my.slack.bot.user.oath.access.token>
        <my.slack.bot.channel.name>GOES HERE</my.slack.bot.channel.name>
        <my.slack.bot.channel.id>GOES HERE</my.slack.bot.channel.id>
      </properties>
    </profile>
  </profiles>
</settings>
```

You will need `my.license.key` for use with Hazelcast Enterprise.

If using Hazelcast Cloud, you will need `my.hz.cloud.cluster1.name`, `my.hz.cloud.cluster1.password`, `my.hz.cloud.cluster1.keys.location` and `my.hz.cloud.cluster1.discovery.token` from your Cloud cluster's credentials. See [Hazelcast Cloud](#Hazelcast-Cloud) below.
Also set `use.hz-cloud` in the top level *pom.xml*.

If you plan to use the Slack integration in some modules, you will also need three Slack settings,
`my.slack.bot.user.oath.access.token`, `my.slack.bot.channel.name` and `my.slack.bot.channel.id`.

### `docker-maven-plugin`

If building Docker images (activated by `mvn install -Prelease`), not all properties are needed.

For example, `my.hz.cloud.cluster1.discovery.token` can be set, omitted but not null. (docker-maven-plugin)[https://dmp.fabric8.io/]
rejects empty string as a null value.

So do not have this in your `settings.xml`:

```
        <my.hz.cloud.cluster1.discovery.token></my.hz.cloud.cluster1.discovery.token>
```

Omit the line if you have no token.

## Hazelcast Cloud

If using Hazelcast Cloud, use the "*Connect Your Application*" page to find the following:

1. Cluster Name
  * This will be something like `pr-1234`.
  * This is the external name of the cluster, the value a client uses. It may not be the same as the internal name, used on the Hazelcast Cloud administration panels.
  * Use this for `my.hz.cloud.cluster1.name` in your `settings.xml` file.
2. Discovery Token
  * This is a text string like `a0b1c2d3e4f56g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z`.
  * It is used as part of connecting to the cluster.
  * Use this for `my.hz.cloud.cluster1.discovery.token` in your `settings.xml` file.
3. Keystore File
  * A client connecting from the outside world requires a *keystore* and *truststore* file that store its access credentials.
  * Use `my.hz.cloud.cluster1.keys.location` to hold the directory where these files are stored.
  * eg `<my.hz.cloud.cluster1.keys.location>${user.home}/Downloads/hzcloud_1234_keys</my.hz.cloud.cluster1.keys.location>`
4. Keystore/Truststore Password
  * This is the password to unlock the above *keystore* and *truststore*, the same value for both.
  * Use this for `my.hz.cloud.cluster1.password` in your `settings.xml` file.


## 3rd Party Software

These demos use the following 3rd party software. Please ensure their licensing models meet your needs.

1. Trade Monitor
* [Javalin](./banking/trade-monitor/webapp) From https://javalin.io/
* [Kafdrop](./banking/trade-monitor/kafdrop) From https://github.com/obsidiandynamics/kafdrop
* [Kafka](./banking/trade-monitor/kafka-broker) From https://kafka.apache.org/
* [React](./banking/trade-monitor/webapp/src/main/app/package.json) See [package.json](./banking/trade-monitor/webapp/src/main/app/package.json)
* [Zookeeper](./banking/trade-monitor/zookeeper) From https://zookeeper.apache.org/
