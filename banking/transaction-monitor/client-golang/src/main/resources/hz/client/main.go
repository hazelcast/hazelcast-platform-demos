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

package main

import (
	"bufio"
	"context"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/hazelcast/hazelcast-go-client"
	"github.com/hazelcast/hazelcast-go-client/logger"
	"github.com/hazelcast/hazelcast-go-client/sql"
	"github.com/hazelcast/hazelcast-go-client/types"
)

const clusterName = "@my.cluster1.name@"
const instanceName = "@project.artifactId@"
const serviceDns = "@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local"

const viridianId = "@my.viridian.cluster1.id@"
const viridianDiscoveryToken = "@my.viridian.cluster1.discovery.token@"
const viridianKeyPassword = "@my.viridian.cluster1.key.password@"

const controlFile = "/tmp/control.file"
const cloudServerName = "hazelcast.cloud"
const genericRecordMapPrefix = "__map-store."
const genericRecordMap = "mysql_slf4j"
const loggingLevel = logger.InfoLevel
const useViridianKey = "use.viridian"
const viridianCaFile = "/tmp/ca.pem"
const viridianCertFile = "/tmp/cert.pem"
const viridianKeyFile = "/tmp/key.pem"

func getClient(ctx context.Context, kubernetes string, viridian bool) *hazelcast.Client {
	config := hazelcast.Config{}
	config.ClientName = instanceName
	home := os.Getenv("HOME")
	user := home[1:]
	launchTime := time.Now().Format(time.RFC3339)
	config.SetLabels(user, launchTime)
	config.Logger.Level = loggingLevel
	config.Stats.Enabled = true

	if viridian {
		config.Cluster.Name = viridianId
		config.Cluster.Cloud.Enabled = true
		config.Cluster.Cloud.Token = viridianDiscoveryToken
		config.Cluster.Network.SSL.Enabled = true
		//config.Cluster.Network.SSL.SetTLSConfig(&tls.Config{ServerName: cloudServerName})

		caFile, err := filepath.Abs(viridianCaFile)
		if err != nil {
			panic(err)
		}
		certFile, err := filepath.Abs(viridianCertFile)
		if err != nil {
			panic(err)
		}
		keyFile, err := filepath.Abs(viridianKeyFile)
		if err != nil {
			panic(err)
		}

		err = config.Cluster.Network.SSL.SetCAPath(caFile)
		if err != nil {
			panic(err)
		}
		err = config.Cluster.Network.SSL.AddClientCertAndEncryptedKeyPath(certFile, keyFile, viridianKeyPassword)
		if err != nil {
			panic(err)
		}
	} else {
		config.Cluster.Name = clusterName

		if strings.EqualFold(kubernetes, "true") {
			config.Cluster.Network.SetAddresses(serviceDns)
		} else {
			hostIp := os.Getenv("HOST_IP")
			config.Cluster.Network.SetAddresses(hostIp)
		}
	}

	client, err := hazelcast.StartNewClientWithConfig(ctx, config)
	if err != nil {
		log.Fatal(err)
	}
	return client
}

func runSqlQuery(ctx context.Context, hazelcastClient *hazelcast.Client, query string) {
	fmt.Printf("--------------------------------------\n")
	fmt.Printf("%s\n", query)
	result, err := hazelcastClient.SQL().Execute(ctx, query)
	if err != nil {
		log.Print(err)
		return
	}
	defer result.Close()
	iterator, _ := result.Iterator()
	metaData, _ := result.RowMetadata()
	cols := metaData.ColumnCount()
	var count = 0
	for iterator.HasNext() {
		row, _ := iterator.Next()
		str := ""
		for i := 0; i < cols; i++ {
			col, _ := row.Get(i)
			if metaData.Columns()[i].Type() == sql.ColumnTypeVarchar {
				if i == 0 {
					str = fmt.Sprintf("%v", col)
				} else {
					str = fmt.Sprintf("%s %v", str, col)
				}
			} else {
				if metaData.Columns()[i].Type() == sql.ColumnTypeTimestamp {
					dateTimeValue := col.(types.LocalDateTime)
					ts := time.Time(dateTimeValue)
					printDateTimeValue := ts.Format(time.RFC3339)
					if i == 0 {
						str = fmt.Sprintf("%v", printDateTimeValue)
					} else {
						str = fmt.Sprintf("%s %v", str, printDateTimeValue)
					}
				} else {
					if metaData.Columns()[i].Type() == sql.ColumnTypeBigInt {
						if i == 0 {
							str = fmt.Sprintf("%v", col)
						} else {
							str = fmt.Sprintf("%s %v", str, col)
						}
					} else {
						str = fmt.Sprintf("%s Unhandled Type for Column '%v'", str, metaData.Columns()[i].Name())
					}
				}
			}
		}
		fmt.Printf("%s\n", str)
		count = count + 1
	}
	fmt.Printf("[%d rows]\n", count)
}

func listDistributedObjects(ctx context.Context, hazelcastClient *hazelcast.Client) {
	fmt.Printf("--------------------------------------\n")
	fmt.Printf("Distributed Objects (excluding system objects)\n")
	distributedObjectInfo, _ := hazelcastClient.GetDistributedObjectsInfo(ctx)
	count := 0
	for _, distributedObject := range distributedObjectInfo {
		do_name := distributedObject.Name
		if strings.Compare("__", do_name[0:2]) != 0 {
			do_service_name := distributedObject.ServiceName
			fmt.Printf("%s => '%s'\n", do_service_name, do_name)
			count++
		}
	}
	fmt.Printf("[%d rows]\n", count)
}

func getGenericRecord(ctx context.Context, hazelcastClient *hazelcast.Client) {
	fmt.Printf("--------------------------------------\n")
	fmt.Printf("GenericRecord, map '%s'\n", genericRecordMap)
	//TODO Allow to fail
	fmt.Printf("TODO: Not yet available\n")
	fmt.Printf("TODO: Not yet available\n")
	fmt.Printf("TODO: Not yet available\n")
	count := 0
	m, err := hazelcastClient.GetMap(ctx, genericRecordMap)
	if err != nil {
		log.Print(err)
	} else {
		entries, err := m.GetEntrySet(ctx)
		if err != nil {
			log.Print(err)
		} else {
			for _, entry := range entries {
				fmt.Printf("%v,%v\n", entry.Key, entry.Value)
				count++
			}
			fmt.Printf("[%d rows]\n", count)
		}
	}
}

func main() {
	ctx := context.TODO()

	fmt.Printf("--------------------------------------\n")
	kubernetes := os.Getenv("MY_KUBERNETES_ENABLED")
	fmt.Printf("MY_KUBERNETES_ENABLED '%s'\n", kubernetes)
	viridian := false
	f, err := os.Open(controlFile)
	if err != nil {
		log.Print(err)
	} else {
		scanner := bufio.NewScanner(f)
		for scanner.Scan() {
			line := scanner.Text()
			if strings.HasPrefix(line, useViridianKey) {
				viridian = strings.EqualFold(line, useViridianKey+"=true")
			}
		}
	}
	defer f.Close()
	fmt.Printf("VIRIDIAN '%t'\n", viridian)
	hazelcastClient := getClient(ctx, kubernetes, viridian)
	fmt.Printf("--------------------------------------\n")

	startTime := time.Now().Format(time.RFC3339)
	fmt.Printf("=================== %s ===================\n", startTime)

	fmt.Printf("Sleeping one minute, so cluster populated with data\n")
	time.Sleep(1 * time.Minute)

	runSqlQuery(ctx, hazelcastClient, "SHOW MAPPINGS")
	runSqlQuery(ctx, hazelcastClient, "SHOW VIEWS")

	listDistributedObjects(ctx, hazelcastClient)

	getGenericRecord(ctx, hazelcastClient)

	runSqlQuery(ctx, hazelcastClient, "SELECT * FROM \""+genericRecordMapPrefix+genericRecordMap+"\"")

	endTime := time.Now().Format(time.RFC3339)
	fmt.Printf("=================== %s ===================\n", endTime)
	fmt.Printf("Sleeping for a day\n")
	time.Sleep(24 * 60 * time.Minute)
	fmt.Printf("Disconnecting\n")
	hazelcastClient.Shutdown(ctx)
}
