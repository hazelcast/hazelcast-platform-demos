# Hazelcast Platform Demo Applications

## Demos

1. Machine Learning
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
