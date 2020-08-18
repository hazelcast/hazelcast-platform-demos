# Hazelcast Platform Demo Applications

Demonstration applications for the [Hazelcast Platform](https://hazelcast.org/platform/), the usage
of [Hazelcast Jet](https://hazelcast.org/jet/) and [Hazelcast IMDG](https://hazelcast.org/imdg/) together.

## Demos

1. Banking
  * [Trade Monitor](./banking/trade-monitor) Monitoring and aggregation of stock market trading volumes.
  * [Credit Value Adjustment](./banking/credit-value-adjustment) Risk exposure calculation for Interest Rate Swaps.
2. Machine Learning
  * [RI](./ml/ml-ri) Reference Implementation for Machine Learning Inference, minimal dependencies.

## Build Instructions

### `settings.xml`

Projects that use commercial features require license keys to operate. 

Register [here](https://hazelcast.com/download/) to request the license keys you need.

Once you have the license keys you need, create a `settings.xml` in your `.m2` folder. Copy
the example below, and replace the property values. Maven will read this file when you build
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
        <my.imdg.license.key>GOES HERE</my.imdg.license.key>
        <my.jet.license.key>GOES HERE</my.jet.license.key>
      </properties>
    </profile>
  </profiles>
</settings>
```

## 3rd Party Software

These demos use the following 3rd party software. Please ensure their licensing models meet your needs.

1. Trade Monitor
* [Javalin](./banking/trade-monitor/webapp) From https://javalin.io/
* [Kafdrop](./banking/trade-monitor/kafdrop) From https://github.com/obsidiandynamics/kafdrop
* [Kafka](./banking/trade-monitor/kafka-broker) From https://kafka.apache.org/
* [React](./banking/trade-monitor/webapp/src/main/app/package.json) See [package.json](./banking/trade-monitor/webapp/src/main/app/package.json)
* [Zookeeper](./banking/trade-monitor/zookeeper) From https://zookeeper.apache.org/
