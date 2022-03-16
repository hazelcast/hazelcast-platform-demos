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
# Random Forest training mechanism.
#
# ----------------------------------------------------------------------
# Input:
#  Expects a batch of lines, each of the format
# "[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0]"
#  24 true (1) or false (0) values.
# ----------------------------------------------------------------------
# Output:
#  A timestamp and a trained model for this input
# ----------------------------------------------------------------------
# Note:
#  One model is produced. To give as many outputs as inputs, the
# rest are dummy strings.
########################################################################
import numpy as np
import pandas as pd
import codecs
from sklearn.model_selection  import train_test_split
from sklearn.ensemble import RandomForestClassifier

import pickle
from datetime import datetime

features_and_label: list = None

def train_model(input_list):
    global features_and_label
    if features_and_label is None:
        features_and_label = []

    for entry in input_list:
        values = entry.strip("[")
        values = values.strip("]")
        values = values.split(", ")

        # append values to features
        values = [int(it) for it in values]
        features_and_label.append(values)

    col = ['basket_icon_click', 'basket_add_list',
           'basket_add_detail', 'sort_by', 'image_picker', 'account_page_click',
           'promo_banner_click', 'detail_wishlist_add', 'list_size_dropdown',
           'closed_minibasket_click', 'checked_delivery_detail',
           'checked_returns_detail', 'sign_in', 'saw_checkout',
           'saw_sizecharts', 'saw_delivery', 'saw_account_upgrade',
           'saw_homepage', 'device_mobile', 'device_computer', 'device_tablet',
           'returning_user', 'loc_uk', 'ordered']

    numpy_array = np.array(features_and_label)
    df = pd.DataFrame(numpy_array,
                      columns=col)
    to_drop = ['ordered']
    predictors = df.drop(to_drop, axis=1)
    targets = df.ordered

    # Train model
    diagnostic = ""
    try:
        tree = RandomForestClassifier()
        tree.fit(predictors, targets)
    except Exception as e:
        diagnostic = "Exception: RandomForestClassifier() for " + str(input_list)
        raise RuntimeError(diagnostic) from None

    # Dump model to pass to prediction module
    result = []
    for i in range(len(input_list)):
        if i == 0:
            now = str(datetime.utcnow().timestamp())
            now = now.split('.')[0] + now.split('.')[1][:3]
            if len(diagnostic) > 0:
                result.append("failure," + now + ",diagnostic==" + diagnostic)
            else:
                model_dump = codecs.encode(pickle.dumps(tree), "base64").decode()
                output = now + "," + model_dump
                # 4MB, max https://github.com/hazelcast/hazelcast/issues/19503. Allow headroom from 4194304 for meta-data.
                if len(output) < 4160000:
                    result.append(output)
                else:
                    result.append("failure," + now + ",len==" + str(len(output)))
        else:
            result.append("")

    return result
