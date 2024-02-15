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

package hazelcast.platform.demos.benchmark.nexmark.model;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;

/**
 * <p>People have "{@code name}" and a "{@code state}" location.
 * </p>
 * <p>Plus inherited fields "{@code id}" and "{@code timestamp}".
 * </p>
 */
public class Person extends Event {
    private final String name;
    private final String state;

    public Person(long id, long timestamp, String name, String state) {
        super(id, timestamp);
        this.name = name;
        this.state = state;
    }

    public String name() {
        return name;
    }

    public String state() {
        return state;
    }

    /**
     * <p>Custom serializer will be faster than Java default,
     * see <a href="https://hazelcast.com/blog/comparing-serialization-options/">here</a>.
     * </p>
     */
    public static class PersonSerializer implements StreamSerializer<Person> {

        @Override
        public int getTypeId() {
            return 3;
        }

        @Override
        public void write(ObjectDataOutput out, Person person) throws IOException {
            out.writeLong(person.id());
            out.writeLong(person.timestamp());
            //UPGRADE Jet 4.3 -> Platform 5.1 : was "out.writeUTF"
            out.writeString(person.name());
            //UPGRADE Jet 4.3 -> Platform 5.1 : was "out.writeUTF"
            out.writeString(person.state());
        }

        @Override
        public Person read(ObjectDataInput in) throws IOException {
            long id = in.readLong();
            long timestamp = in.readLong();
            //UPGRADE Jet 4.3 -> Platform 5.1 : was "in.readUTF"
            String name = in.readString();
            //UPGRADE Jet 4.3 -> Platform 5.1 : was "in.readUTF"
            String state = in.readString();
            return new Person(id, timestamp, name, state);
        }
    }
}
