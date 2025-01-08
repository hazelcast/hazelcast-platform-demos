/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
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

#include <algorithm>
#include <chrono>
#include <ctime>
#include <fstream>
#include <hazelcast/client/hazelcast_client.h>
#include <iostream>
#include <thread>

const char* clusterName = "@my.cluster1.name@";
const char* instanceName = "@project.artifactId@";
const char* serviceDns = "@my.docker.image.prefix@-@my.cluster1.name@-hazelcast.default.svc.cluster.local";

const char* hzCloudName = "@my.hz.cloud.cluster1.name@";
const char* hzCloudDiscoveryToken = "@my.hz.cloud.cluster1.discovery.token@";
const char* hzCloudKeyPassword = "@my.hz.cloud.cluster1.key.password@";

const char* controlFile = "/tmp/control.file";
std::string genericRecordMapPrefix = "__map-store.";
std::string genericRecordMap = "mysql_slf4j";
std::string useHzCloudKey = "use.hz.cloud";
const char* hzCloudCaFile = "/tmp/ca.pem";
const char* hzCloudCertFile = "/tmp/cert.pem";
const char* hzCloudKeyFile = "/tmp/key.pem";

char* get_time_iso8601() {
	time_t now;
    time(&now);
	char* result = (char*) malloc(20);
    strftime(result, 20, "%FT%T", gmtime(&now));
	return result;
}

bool is_hz_cloud() {
	std::string targetForTrue = useHzCloudKey.append("=true");
    std::ifstream file;
    file.open(controlFile);
    bool hzCloud = false;
    std::string line;
	while(getline(file, line)) {
        if (line.compare(targetForTrue)==0) {
    	    hzCloud = true;
        }
    }
    return hzCloud;
}

hazelcast::client::hazelcast_client get_client(const char* kubernetes, bool hzCloud) {
    hazelcast::client::client_config client_config;

    client_config.set_instance_name(instanceName);
	char* home = std::getenv("HOME");
	char* user = home + 1;
	char* launch_time = get_time_iso8601();
	std::unordered_set<std::string> myLabels;
	myLabels.insert(user);
	myLabels.insert(launch_time);
	client_config.set_labels(myLabels);
	//client_config.get_logger_config().level(hazelcast::logger::level::finest);
	client_config.set_property("hazelcast.client.statistics.enabled", "true");

	if (hzCloud) {
		client_config.set_cluster_name(hzCloudName);

		auto &cloud_configuration = client_config.get_network_config().get_cloud_config();
    	cloud_configuration.enabled = true;
    	cloud_configuration.discovery_token = hzCloudDiscoveryToken;
    	boost::asio::ssl::context ctx(boost::asio::ssl::context::tlsv12);
    	ctx.load_verify_file(hzCloudCaFile);
    	ctx.use_certificate_file(hzCloudCertFile, boost::asio::ssl::context::pem);
    	ctx.set_password_callback([&] (std::size_t max_length, boost::asio::ssl::context::password_purpose purpose) {
        	return hzCloudKeyPassword;
    	});
    	ctx.use_private_key_file(hzCloudKeyFile, boost::asio::ssl::context::pem);
    	client_config.get_network_config().get_ssl_config().set_context(std::move(ctx));
	} else {
		client_config.set_cluster_name(clusterName);

		std::string kubernetesStr(kubernetes);
		for (int i = 0; i < strlen(kubernetes); ++i) {
			char c = *(kubernetes + i);
			kubernetesStr[i] = tolower(c);
   		}

		if (kubernetesStr.compare("false")==0) {
   	 		client_config.get_network_config()
			.add_address(hazelcast::client::address(std::getenv("HOST_IP"), 5701));
		} else {
    		client_config.get_network_config()
			.add_address(hazelcast::client::address(serviceDns, 5701));
		}
	}

	auto hazelcast_client = hazelcast::new_client(std::move(client_config)).get();

	return hazelcast_client;
}

/*TODO Remove once https://github.com/hazelcast/hazelcast-cpp-client/issues/1125 
 * and https://github.com/hazelcast/hazelcast-cpp-client/pull/1127 fixed							
 */
inline std::ostream& operator<<(std::ostream& os, const hazelcast::client::local_date_time& dt)
{
    return os << int(dt.date.year)
              << "-"
              << int(dt.date.month)
              << "-"
              << int(dt.date.day_of_month)
              << "T"
              << int(dt.time.hours)
              << ":"
              << int(dt.time.minutes)
              << ":"
              << int(dt.time.seconds)
			  ;
}

void run_sql_query(hazelcast::client::hazelcast_client hazelcast_client, std::string query) {
	using namespace hazelcast::client::sql;
	std::cout << "--------------------------------------" << std::endl;
	std::cout << query << std::endl;
	try {
		auto result = hazelcast_client.get_sql().execute(query).get();
		int count = 0;
    	for (auto iter = result->iterator(); iter.has_next();) {
        	auto page = iter.next().get();
			for (auto const& row : page->rows()) {
				auto row_metadata = row.row_metadata();
				for (int i = 0; i < row_metadata.column_count(); i++) {
					if (i > 0) {
						std::cout << ", ";
					}
					auto sql_column_type = row_metadata.column(i).type;
					if (sql_column_type == hazelcast::client::sql::sql_column_type::varchar) {
						std::cout << row.get_object<std::string>(i);
					} else {
						if (sql_column_type == hazelcast::client::sql::sql_column_type::timestamp) {
							hazelcast::client::local_date_time ts =
								row.get_object<hazelcast::client::local_date_time>(i).get();
							std::cout << ts;
						} else {
							if (sql_column_type == hazelcast::client::sql::sql_column_type::bigint) {
								std::cout << row.get_object<int64_t>(i).get();
							} else {
								std::cout << "Unhandled Type for Column '" << row_metadata.column(i).name << "'";
							}
						}
					}
				}
				std::cout << std::endl;
				count++;
        	}
    	}		
		std::cout << "[" << count << " rows]" << std::endl;
	} catch (const hazelcast::client::sql::hazelcast_sql_exception& e) {
		std::cout << e.get_message() << std::endl;
	}
}

void list_distributed_objects(hazelcast::client::hazelcast_client hazelcast_client) {
	std::cout << "--------------------------------------" << std::endl;
	std::cout << "Distributed Objects (excluding system objects)" << std::endl;
	//TODO Needs API extension
	std::cout << "See https://github.com/hazelcast/hazelcast-cpp-client/issues/1120" << std::endl;
}

void get_generic_record(hazelcast::client::hazelcast_client hazelcast_client) {
	using namespace hazelcast::client::serialization::generic_record;
	std::cout << "--------------------------------------" << std::endl;
	std::cout << "GenericRecord, map '" << genericRecordMap << "'" << std::endl;
	auto map = hazelcast_client.get_map(genericRecordMap).get();
	int count = 0;

	for (auto& key : map->key_set<hazelcast::client::typed_data>().get()) {
		boost::optional<hazelcast::client::typed_data> value =
            map->get<hazelcast::client::typed_data, hazelcast::client::typed_data>(key).get();
		hazelcast::client::serialization::pimpl::object_type keyType = key.get_type();
		hazelcast::client::serialization::pimpl::object_type valueType = value->get_type();
		if (keyType.type_id == hazelcast::client::serialization::pimpl::serialization_constants::CONSTANT_TYPE_LONG) {
			if (valueType.type_id == hazelcast::client::serialization::pimpl::serialization_constants::CONSTANT_TYPE_COMPACT) {
				auto keyRecord = key.get<int64_t>().value();
				auto valueRecord = value->get<generic_record>().value();
    			std::cout << keyRecord << " :: " << valueRecord << std::endl;
			} else {
				std::cout << "Row " << count << ": Unknown value type: " << valueType << std::endl;
			}
		} else {
			std::cout << "Row " << count << ": Unknown key type: " << keyType << std::endl;
		}
        count++;
	}
	std::cout << "[" << count << " rows]" << std::endl;

}

int main (int argc, char *argv[]) {
	std::cout << "--------------------------------------" << std::endl;
	const char* kubernetes = std::getenv("MY_KUBERNETES_ENABLED");
	bool hzCloud = is_hz_cloud();
	std::cout << "MY_KUBERNETES_ENABLED '" << kubernetes << "'" << std::endl;
	std::cout << "HZ_CLOUD '" << hzCloud << "'" << std::endl;
    auto hazelcast_client = get_client(kubernetes, hzCloud);
	std::cout << "--------------------------------------" << std::endl;

	const char* start_time = get_time_iso8601();
	std::cout << "=================== " << start_time << " ===================" << std::endl;

	std::cout << "Sleeping one minute, so cluster populated with data" << std::endl;
	std::this_thread::sleep_for(std::chrono::minutes(1));
	std::cout << "AWAKE" << std::endl;

	run_sql_query(hazelcast_client, "SHOW MAPPINGS");
	run_sql_query(hazelcast_client, "SHOW VIEWS");
	
	list_distributed_objects(hazelcast_client);

	get_generic_record(hazelcast_client);

	run_sql_query(hazelcast_client, "SELECT * FROM \"" + genericRecordMapPrefix + genericRecordMap + "\"");

	const char* end_time = get_time_iso8601();
	std::cout << "=================== " << end_time << " ===================" << std::endl;
	std::cout << "Sleeping for a day" << std::endl;
	std::this_thread::sleep_for(std::chrono::minutes(24 * 60));
	std::cout << "Disconnecting" << std::endl;
	hazelcast_client.shutdown().get();

    return 0;
}
