/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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
	"context"
	"fmt"
	"log"
	"os"
	"strings"
	"time"

	"github.com/hazelcast/hazelcast-go-client"
	"github.com/hazelcast/hazelcast-go-client/logger"
)

const clusterName = "@my.cluster1.name@"
const instanceName = "@project.artifactId@"
const serviceDns = "@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local"

const loggingLevel = logger.InfoLevel

func getClient(ctx context.Context, kubernetes string) *hazelcast.Client {
	config := hazelcast.Config{}
	config.ClientName = instanceName
	config.Cluster.Name = clusterName
	home := os.Getenv("HOME")
	user := home[1:]
	launchTime := time.Now().Format(time.RFC3339)
	config.SetLabels(user, launchTime)
	config.Logger.Level = loggingLevel
	config.Stats.Enabled = true

	if strings.EqualFold(kubernetes, "true") {
		config.Cluster.Network.SetAddresses(serviceDns)
	} else {
		hostIp := os.Getenv("HOST_IP")
		config.Cluster.Network.SetAddresses(hostIp)
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
			if i == 0 {
				str = fmt.Sprintf("%s", col)
			} else {
				str = fmt.Sprintf("%s %v", str, col)
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
			fmt.Printf("%s\n", do_name)
			count++
		}
	}
	fmt.Printf("[%d rows]\n", count)
}

func getGenericRecord(ctx context.Context, hazelcastClient *hazelcast.Client) {
	fmt.Printf("--------------------------------------\n")
	fmt.Printf("GenericRecord\n")
	//FIXME Needs 1.4.0 hazelcast-go-client
}

func main() {
	ctx := context.TODO()

	fmt.Printf("--------------------------------------\n")
	kubernetes := os.Getenv("MY_KUBERNETES_ENABLED")
	fmt.Printf("MY_KUBERNETES_ENABLED '%s'\n", kubernetes)
	hazelcastClient := getClient(ctx, kubernetes)
	fmt.Printf("--------------------------------------\n")

	startTime := time.Now().Format(time.RFC3339)
	fmt.Printf("=================== %s ===================\n", startTime)

	fmt.Printf("Sleeping one minute, so cluster populated with data\n")
	time.Sleep(1 * time.Minute)

	runSqlQuery(ctx, hazelcastClient, "SHOW MAPPINGS")
	runSqlQuery(ctx, hazelcastClient, "SHOW VIEWS")

	listDistributedObjects(ctx, hazelcastClient)

	getGenericRecord(ctx, hazelcastClient)

	runSqlQuery(ctx, hazelcastClient, "SELECT * FROM \"__map-store.mysql_slf4j\"")

	endTime := time.Now().Format(time.RFC3339)
	fmt.Printf("=================== %s ===================\n", endTime)
	fmt.Printf("Sleeping for a day\n")
	time.Sleep(24 * 60 * time.Minute)
	fmt.Printf("Disconnecting\n")
	hazelcastClient.Shutdown(ctx)
}
