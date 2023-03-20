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
        <my.license.key>GOES HERE</my.license.key>

        <my.slack.bot.user.oath.access.token>GOES HERE</my.slack.bot.user.oath.access.token>
        <my.slack.bot.channel.name>GOES HERE</my.slack.bot.channel.name>
        <my.slack.bot.channel.id>GOES HERE</my.slack.bot.channel.id>

        <my.viridian.cluster1.id>GOES HERE</my.viridian.cluster1.id>
        <my.viridian.cluster1.discovery.token>GOES HERE</my.viridian.cluster1.discovery.token>
        <my.viridian.cluster1.keys.location>GOES HERE</my.viridian.cluster1.keys.location>
        <my.viridian.cluster1.key.password>GOES HERE</my.viridian.cluster1.key.password>
        <my.viridian.api.key>GOES HERE</my.viridian.api.key>
        <my.viridian.api.secret>GOES HERE</my.viridian.api.secret>
      </properties>
    </profile>
  </profiles>
</settings>
```

*NOTE*: If you don't have a value for a setting, it is better to delete the line and allow the default value to be used.
If you leave it blank (e.g. `<my.something></my.something>`) then an empty string will be used instead of the default,
which might cause problems.

There are three groups of properties: licensing, Slack and Viridian. All are optional.

#### Licensing settings

Some of the demos use Hazelcast Enterprise, and some can optionally use Hazelcast Enterprise.

If you want to run these, contact Hazelcast [here](https://hazelcast.com/contact/) to obtain a trial license key.

Once provided, put the value in the `<my.license.key>` setting and build.

#### Slack settings

Some of the demos interact with the [Slack](https://slack.com/intl/en-gb/) team communication tool, for sending
and receiving messages.

If you have Slack and wish to use this feature, you need three settings.

`<my.slack.bot.user.oath.access.token>` is the OAuth access token that allows Hazelcast to connect to your Slack instance.
Refer to the Slack documentation for registering a bot and obtaining such a token.

`<my.slack.bot.channel.name>` and `my.slack.bot.channel.id` are the name and Id of the Slack channel that Hazelcast will
interact with.

#### Viridian settings

Some of the demos can run the Hazelcast server code on Viridian or on a Hazelcast instance that you host.

##### Activation

To build for Hazelcast Viridian, set the property `use.viridian` to `true` in the top-level _pom.xml_.

##### Properties

If you wish to use a managed Hazelcast instance, sign-up for [Hazelcast Viridian](https://viridian.hazelcast.com) and create a cluster.

This needs 6 properties.

For the first four, you can find them on the cluster list page [here](https://viridian.hazelcast.com/cluster/list), and then
select the cluster you want.

From there, the easiest way is to click on the "_Connect Client_" button, then the "_Advanced Set-up_" tab.

`<my.viridian.cluster1.id>` is the id of the cluster you create on Viridian. This is the name internally allocated, and will be
something like `pr-1234`.

`<my.viridian.cluster1.discovery.token>` is the counterpart to the ID, to enable the cluster to be found in the cloud.

On the "_Advanced Set-up_" tab, you should download the keystore files and put them somewhere suitable.

`<my.viridian.cluster1.keys.location>` specifies the location where you have placed the keystore files.
It might have a value such as `/home/myname/keys/hzcloud_1234_keys`. The build script will copy files from this directory
into the Docker images it builds.

`<my.viridian.cluster1.key.password>` is the password
shown on "_Advanced Set-up_" for the keystore and truststore. The same password is currently used for both.

Finally `<my.viridian.api.key>` and `<my.viridian.api.secret>` are used for automated upload of Maven artifacts
to Viridian. You can create API access [here](https://viridian.hazelcast.com/settings/developer).

You may have several Viridian clusters (`my.viridian.cluster1.id`, `my.viridian.cluster2.id`, etc) but the same API is used for all.

### `docker-maven-plugin`

If building Docker images (activated by `mvn install -Prelease`), not all properties are needed.

As per above, `my.viridian.cluster1.discovery.token` can be set, omitted but not null. (docker-maven-plugin)[https://dmp.fabric8.io/]
rejects empty string as a null value.

So do not have this in your `settings.xml`:

```
        <my.viridian.cluster1.discovery.token></my.viridian.cluster1.discovery.token>
```

Omit the line if you have no token.

## 3rd Party Software

These demos use the following 3rd party software. Please ensure their licensing models meet your needs.

1. Trade Monitor
* [Javalin](./banking/transaction-monitor/webapp) From https://javalin.io/
* [Kafdrop](./banking/transaction-monitor/kafdrop) From https://github.com/obsidiandynamics/kafdrop
* [Kafka](./banking/transaction-monitor/kafka-broker) From https://kafka.apache.org/
* [React](./banking/trade-monitor/webapp/src/main/app/package.json) See [package.json](./banking/trade-monitor/webapp/src/main/app/package.json)
* [Zookeeper](./banking/trade-monitor/zookeeper) From https://zookeeper.apache.org/
