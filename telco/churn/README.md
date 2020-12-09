# Hazelcast Platform Demo Applications - Telco - Churn Prediction

This example shows continuous updates of customer churn prections, in an environment with multiple
legacy integration points.

You will need a Jet Enterprise license to run it.

Register [here](https://hazelcast.com/download/) to request the evaluation license keys you
need, and put them in your `settings.xml` file as described in [Repository top-level README.md](../../README.md).
Be sure to mention this is for "Telco - Churn Predication" example so you get a license with the correct capabilities.

## `settings.xml`

You will need a Jet Enterprise license to run this example. If you wish to use alerting to Slack,
you will need a Slack access token, plus the channel name and id.

All need to go in your Maven `settings.xml` file, such as the below. You can define other properties.

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
        <my.jet.license.key>GOES HERE</my.jet.license.key>
	<my.slack.bot.user.oath.access.token>GOES HERE</my.slack.bot.user.oath.access.token>
	<my.slack.bot.channel.name>GOES HERE</my.slack.bot.channel.name>
	<my.slack.bot.channel.id>GOES HERE</my.slack.bot.channel.id>
      </properties>
    </profile>
  </profiles>
</settings>
```

The settings `my.slack.bot.user.oath.access.token`, `my.slack.bot.channel.name` and
`my.slack.bot.channel.id` are only needed if you use the [slack-integration](./slack-integration)
module.

# FIXME
## Mongo
Add Mongo Debezium One-Way CDC
Kafka Connect - "mongo" parameter to Dockerfile

# TODO
Make Cassandra automatically flush cdc to disk, use Cassandra 4.0 once Debezium supports
https://issues.apache.org/jira/browse/CASSANDRA-12148
