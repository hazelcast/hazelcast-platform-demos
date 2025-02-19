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

package hazelcast.platform.demos.benchmark.nexmark.model;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

/**
 * <p>Actions have "{@code sellerId}", a "{@code category}" and a
 * "{@code expires}", they come to an end at some point.
 * </p>
 * <p>Plus inherited fields "{@code id}" and "{@code timestamp}".
 * </p>
 */
public class Auction extends Event {
    private final long sellerId;
    private final int category;
    private final long expires;

    public Auction(long id, long timestamp, long sellerId, int category, long expires) {
        super(id, timestamp);
        this.sellerId = sellerId;
        this.category = category;
        this.expires = expires;
    }

    public long sellerId() {
        return sellerId;
    }

    public int category() {
        return category;
    }

    public long expires() {
        return expires;
    }

    /**
     * <p>Custom serializer will be faster than Java default,
     * see <a href="https://hazelcast.com/blog/comparing-serialization-options/">here</a>.
     * </p>
     */
    public static class AuctionSerializer implements StreamSerializer<Auction> {

        @Override
        public int getTypeId() {
            return 1;
        }

        @Override
        public void write(ObjectDataOutput out, Auction auction) throws IOException {
            out.writeLong(auction.id());
            out.writeLong(auction.timestamp());
            out.writeLong(auction.sellerId());
            out.writeInt(auction.category());
            out.writeLong(auction.expires());
        }

        @Override
        public Auction read(ObjectDataInput in) throws IOException {
            long id = in.readLong();
            long timestamp = in.readLong();
            long sellerId = in.readLong();
            int category = in.readInt();
            long expires = in.readLong();

            return new Auction(id, timestamp, sellerId, category, expires);
        }
    }
}
