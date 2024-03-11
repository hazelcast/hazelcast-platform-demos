/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using System;
using System.Security.Authentication;
using System.Text.Json;
using System.Threading.Tasks;
using Hazelcast;
using Hazelcast.Core;
using Hazelcast.Networking;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace Client
{
    internal static class MyConstants {
        public const string clusterName = "@my.cluster1.name@";
        public const string instanceName = "@project.artifactId@";
        public const string serviceDns = "@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local";

        public const string viridianId = "@my.viridian.cluster1.id@";
        public const string viridianDiscoveryToken = "@my.viridian.cluster1.discovery.token@";
        public const string viridianKeyPassword = "@my.viridian.cluster1.key.password@";

        public const string controlFile = "/tmp/control.file";
        public const string genericRecordMapPrefix = "__map-store.";
        public const string genericRecordMap = "mysql_slf4j";
        public const string useViridianKey = "use.viridian";
        public const string viridianPfxFile = "/tmp/client.pfx";

        public const int ONE_MINUTE = 60 * 1000;
        public const int ONE_DAY = 24 * 60 * 60 * 1000;
    }

    internal static class Program
    {
        public static HazelcastOptionsBuilder WithConsoleLogger(this HazelcastOptionsBuilder builder)
        {
            return builder
                .With("Logging:LogLevel:Default", "None")
                .With("Logging:LogLevel:System", "None")
                .With("Logging:LogLevel:Microsoft", "None")
                .With((configuration, options) =>
                {
                    // configure logging factory and add the console provider
                    options.LoggerFactory.Creator = () => LoggerFactory.Create(loggingBuilder =>
                        loggingBuilder
                            .AddConfiguration(configuration.GetSection("logging"))
                            .AddConsole());
                });
        }
        public static bool IsViridian() {
            string[] lines = File.ReadAllLines(MyConstants.controlFile);
            bool viridian = false;
            foreach (string line in lines) {
                if (line.Equals(MyConstants.useViridianKey + "=true", StringComparison.OrdinalIgnoreCase)) {
                    viridian = true;
                }
            }
            return viridian;
        }
        public static HazelcastOptions GetClientConfig(string kubernetes, bool viridian)
        {
            var options = new HazelcastOptionsBuilder()
                .WithConsoleLogger()
                .With("Logging:LogLevel:Hazelcast", "Information")
                //.With("Logging:LogLevel:Hazelcast", "Debug")
                .Build();

            options.ClientName = MyConstants.instanceName;
            string home = Environment.GetEnvironmentVariable("HOME") ?? "/?";
            string user = "?";
            if (home.Length > 1) {
                user = home.Substring(1);
            }
            options.Labels.Add(user);
            DateTime launchTime = DateTime.Now;
            string launchTimeStr = launchTime.ToUniversalTime().ToString("u").Replace(" ", "T");
            options.Labels.Add(launchTimeStr);

            options.Metrics.Enabled = true;

            if (viridian) {
                options.ClusterName = MyConstants.viridianId;

                options.Networking.Cloud.DiscoveryToken = MyConstants.viridianDiscoveryToken;
                //FIXME IS THIS NEEDED
                Console.WriteLine("TODO IS THIS NEEDED");
                options.Networking.Cloud.Url = new Uri("https://api.viridian.hazelcast.com");

                options.Networking.Ssl.Enabled = true;
                options.Networking.Ssl.ValidateCertificateChain = false;
                options.Networking.Ssl.Protocol = SslProtocols.Tls12;
                options.Networking.Ssl.CertificatePath = MyConstants.viridianPfxFile;
                options.Networking.Ssl.CertificatePassword = MyConstants.viridianKeyPassword;

            } else {
                options.ClusterName = MyConstants.clusterName;

                if (kubernetes.Equals("false", StringComparison.OrdinalIgnoreCase)) {
                    string hostIp = Environment.GetEnvironmentVariable("HOST_IP") ?? "";
                    if (String.IsNullOrEmpty(hostIp)) {
                        hostIp = "127.0.0.1";
                    }
                    options.Networking.Addresses.Add(hostIp);
                } else {
                    options.Networking.Addresses.Add(MyConstants.serviceDns);
                }
            }

            return options;
        }

        private static async Task RunSqlQuery(IHazelcastClient client, string query) {
	        Console.WriteLine("--------------------------------------");
            Console.WriteLine(query);
            try {
                await using var result =
                    await client.Sql.ExecuteQueryAsync(query);
                var count = 0;
                await foreach (var row in result)
                {
                    IEnumerable<Hazelcast.Sql.SqlColumnMetadata> columns = row.Metadata.Columns;
                    for(int i = 0; i < columns.Count(); i++) 
                    {
                        if (i > 0) 
                        {
                            Console.Write(" ");
                        }
                        Hazelcast.Sql.SqlColumnType type = columns.ElementAt(i).Type;
                        if (type == Hazelcast.Sql.SqlColumnType.Varchar) {
                            Console.Write(row.GetColumn<string>(i));
                        } else {
                            if (type == Hazelcast.Sql.SqlColumnType.Timestamp) {
                                Hazelcast.Models.HLocalDateTime timestamp =
                                    row.GetColumn<Hazelcast.Models.HLocalDateTime>(i);
                                Console.Write(timestamp);
                           } else {
                                if (type == Hazelcast.Sql.SqlColumnType.BigInt) {
                                    Console.Write(row.GetColumn<long>(i));
                                } else {
                                    Console.Write("Unhandled Type " + type + " for Column '" + columns.ElementAt(i).Name + "'");
                                }
                            }
                        }
                    }
                    Console.WriteLine("");
                    count++;
                }
                Console.WriteLine("[" + count + " rows]");
            } catch (Exception e) {
                Console.WriteLine(e.Message);
            }
        }

        private static async Task ListDistributedObjects(IHazelcastClient client) {
	        Console.WriteLine("--------------------------------------");
	        Console.WriteLine("Distributed Objects (excluding system objects)");
            var distributedObjects = await client.GetDistributedObjectsAsync();
            var count = 0;
            foreach (var distributedObject in distributedObjects) {
                var name = distributedObject.Name;
                if (! name.StartsWith("__")) {
                    var serviceName = distributedObject.ServiceName;
        	        Console.WriteLine(serviceName + " => '" + name + "'");
                    count = count + 1;
                }
            }
            Console.WriteLine("[" + count + " rows]");
        }

        private static async Task GetGenericRecord(IHazelcastClient client) {
	        Console.WriteLine("--------------------------------------");
	        Console.WriteLine("GenericRecord, map '" + MyConstants.genericRecordMap + "'");
            await using var map = await client.GetMapAsync<object, object>(MyConstants.genericRecordMap);
            var count = 0;
            var keySet = await map.GetKeysAsync();
            var enumerator = keySet.GetEnumerator();
            while(enumerator.MoveNext())
            {
                var key = enumerator.Current;
                var value = await map.GetAsync(key);
                //FIXME Need ToString() to work better than it does in 5.3.0
                Console.WriteLine(key + "," + value.ToString());
                count++;
        	}
            Console.WriteLine("[" + count + " rows]");
        }

        public static async Task Main(string[] args)
        {
            Console.WriteLine("--------------------------------------");
            string kubernetes = Environment.GetEnvironmentVariable("MY_KUBERNETES_ENABLED") ?? "";
            Console.WriteLine("MY_KUBERNETES_ENABLED '" + kubernetes + "'");
            bool viridian = IsViridian();
            Console.WriteLine("VIRIDIAN '" + viridian + "'");
            var options = GetClientConfig(kubernetes, viridian);
            await using var hazelcast_client = await HazelcastClientFactory.StartNewClientAsync(options);
            Console.WriteLine("--------------------------------------");

            DateTime startTime = DateTime.Now;
            string startTimeStr = startTime.ToUniversalTime().ToString("u").Replace(" ", "T");
            Console.WriteLine("=================== " + startTimeStr + " ===================");

            Console.WriteLine("Sleeping one minute, so cluster populated with data");
            Thread.Sleep(MyConstants.ONE_MINUTE);

            await RunSqlQuery(hazelcast_client, "SHOW MAPPINGS");
	        await RunSqlQuery(hazelcast_client, "SHOW VIEWS");
	
	        await ListDistributedObjects(hazelcast_client);

	        await GetGenericRecord(hazelcast_client);

            await RunSqlQuery(hazelcast_client, "SELECT * FROM \"" + MyConstants.genericRecordMapPrefix + MyConstants.genericRecordMap + "\"");

            DateTime endTime = DateTime.Now;
            string endTimeStr = endTime.ToUniversalTime().ToString("u").Replace(" ", "T");
            Console.WriteLine("=================== " + endTimeStr + " ===================");
            Console.WriteLine("Sleeping for a day");
            Thread.Sleep(MyConstants.ONE_DAY);
            Console.WriteLine("Disconnecting");
            await hazelcast_client.DisposeAsync();   
        }
    }
}
