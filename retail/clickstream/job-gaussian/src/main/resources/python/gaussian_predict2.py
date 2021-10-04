#
# Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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
########################################################################
#
# Gaussian prediction mechanism.
#
# ----------------------------------------------------------------------
# Input:
#  CSV data with a control command.
#  If CSV begins "data," the next fields are the clickstream key, two timestamps, 
# then 23 true/false values (0==false, 1==true) for clickstream actions.
#  Eg. "data,neil,123,456,1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1"
#  If the CSV begins with anything else, it is ignored
# ----------------------------------------------------------------------
# Output:
#  CSV data with a control command
#  For input command "data," output is "data," + clickstream key and timestamps,
# the model version, and finally buy or not-buy prediction (0==not-buy, 1==buy). 
# So for above input the output "neil,123,456,somethig,1" for a buy prediction
# ------------s----------------------------------------------------------
########################################################################
import numpy as numpy
import pandas as pandas
from sklearn.model_selection  import train_test_split
import sklearn.metrics
from sklearn.naive_bayes import GaussianNB

# Replaced by Maven
version = "@maven.build.timestamp@"
col = ['basket_icon_click', 'basket_add_list',
         'basket_add_detail', 'sort_by', 'image_picker', 'account_page_click',
         'promo_banner_click', 'detail_wishlist_add', 'list_size_dropdown', 
         'closed_minibasket_click', 'checked_delivery_detail', 
         'checked_returns_detail', 'sign_in', 'saw_checkout', 
         'saw_sizecharts', 'saw_delivery', 'saw_account_upgrade',
         'saw_homepage', 'device_mobile', 'device_computer', 'device_tablet',
         'returning_user', 'loc_uk', 'ordered']

def predict(input_list):
    global version
    global col
    result = []

    print('pip freeze!')
    print('pip freeze!')
    print('pip freeze!')
    for entry in input_list:
        values = entry.replace(", ", ",").split(",")
        if values[0] == "data":
            # append values to features
            key = values[1]
            publish = values[2]
            ingest = values[3]
            diagnostic = ""
            values = [int(it) for it in values[4:]]

            # FIXME
            results = []

            print("len", len(col), len(values))
            numpy_array = numpy.array(results)
            df = pandas.DataFrame(numpy_array, columns=col)

            correlation = df.corr()['ordered'].tolist()
            to_drop = []
            for i in range(len(correlation)):
                if correlation[i] < 0:
                    to_drop.append(col[i])

            to_drop.append('ordered')
            predictors = df.drop(to_drop, axis=1)
            targets = df.ordered
            try:    
                print("A:")
                X_train, X_test, y_train, y_test  =  train_test_split(predictors, targets, test_size=.3)

                print("A1:")
                classifier=GaussianNB()
                print("A2:")
                classifier=classifier.fit(X_train,y_train)

                print("B:")
                predictions=classifier.predict(X_test)

                print("C:")
                predictors['propensity'] = classifier.predict_proba(predictors)
                accuracy = str(sklearn.metrics.accuracy_score(y_test, predictions))

                output = predictors.values.tolist()
                print("D:")
                for val in output:
                    val.insert(0, accuracy)

                print("E:")
                resultX = [str(val[0]) for val in output]

                print("F:", str(resultX))
                #return result
            except: 
                print("except:")
                #return[str(1.0) for _ in input_list]

            result.append(str(key) + "," + str(publish) + "," + str(ingest) + "," + version + "," + str(prediction[0]) + "," + diagnostic)
        else:
            raise NotImplementedError('Unexpected control', values[0])

    return result
