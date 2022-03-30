# Hazelcast Platform Demo Applications

Demonstration applications for the [Hazelcast Platform](https://hazelcast.com/products/hazelcast-platform/), the combination
of streaming analytics and in-memory data.

## Demos

1. Banking
  * [Trade Monitor](./banking/trade-monitor) Monitoring and aggregation of stock market trading volumes.
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

7. Utils
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

If using Hazelcast Cloud, you will need `my.hz.cloud.cluster1.name` and `my.hz.cloud.cluster1.discovery.token` from your Cloud cluster's credentials.

If you plan to use the Slack integration in some modules, you will also need three Slack settings,
`my.slack.bot.user.oath.access.token`, `my.slack.bot.channel.name` and `my.slack.bot.channel.id`.


## 3rd Party Software

These demos use the following 3rd party software. Please ensure their licensing models meet your needs.

1. Trade Monitor
* [Javalin](./banking/trade-monitor/webapp) From https://javalin.io/
* [Kafdrop](./banking/trade-monitor/kafdrop) From https://github.com/obsidiandynamics/kafdrop
* [Kafka](./banking/trade-monitor/kafka-broker) From https://kafka.apache.org/
* [React](./banking/trade-monitor/webapp/src/main/app/package.json) See [package.json](./banking/trade-monitor/webapp/src/main/app/package.json)
* [Zookeeper](./banking/trade-monitor/zookeeper) From https://zookeeper.apache.org/
