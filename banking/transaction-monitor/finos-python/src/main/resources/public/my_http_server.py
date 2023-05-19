#
# Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import datetime
import functools
import http.server
import logging
import os
import threading
import time

# From py-env paths
import cli_hz
import hz

DIR = '.'
HOST = ''
ONE_DAY = 24 * 60 * 60
PORT = 8080

logging.basicConfig(level=logging.INFO)

def run_my_http_server():
    directory = os.path.abspath(os.path.dirname(DIR))
    handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=directory)
    httpd = http.server.HTTPServer((HOST, PORT), handler)

    def my_thread(httpd):
        httpd.serve_forever()

    thread = threading.Thread(args=(httpd,), name = "HTTP-Thread", target=my_thread)
    thread.setDaemon(True)
    print("my_http_server.py : starting HTTP server, port " + str(PORT) + " thread: " + str(thread.name), flush=True)
    thread.start()

print("my_http_server.py : start", datetime.datetime.now().strftime('%Y-%m-%dT%H:%M:%S'), flush=True)
print("my_http_server.py : start1", "xxx", hz.xxx)
hz.xxx = "xxx)my_http"
print("my_http_server.py : start2", "xxx", hz.xxx)
cli_hz.connect_client()
run_my_http_server()
for thread in threading.enumerate(): 
    print("my_http_server.py", "thread.name", thread.name)
print("my_http_server.py : sleep " + str(ONE_DAY), flush=True)
time.sleep(ONE_DAY) 
print("my_http_server.py : awake", flush=True)