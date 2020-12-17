/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.telco.churn.domain;

import org.springframework.data.annotation.Id;

/**
 * <p>Java representation of a customer in Mongo.
 * As this is mapped to Json, fields don't need "{@code @Column}"
 * tags that Cassandra and MySql modules do.
 * </p>
 */
public class Customer {
    private static final int MAX_NOTES_LENGTH = 128;

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String accountType;
    private String createdBy;
    private Long createdDate;
    private String lastModifiedBy;
    private Long lastModifiedDate;
    private String notes;

    // Almost default, don't print all notes if lengthy
    @Override
    public String toString() {
        String notesPrint = (notes == null ? "'" : "'" + notes);
        if (notesPrint.length() > MAX_NOTES_LENGTH) {
            notesPrint = notesPrint.substring(0, MAX_NOTES_LENGTH) + "' [TRUNCATED]";
        }
        return "Customer [id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", accountType="
                + accountType + ", createdBy=" + createdBy + ", createdDate=" + createdDate + ", lastModifiedBy="
                + lastModifiedBy + ", lastModifiedDate=" + lastModifiedDate
                + ", notes=" + notesPrint + "]";
    }

    // Generated code below

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getAccountType() {
        return accountType;
    }
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    public String getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    public Long getCreatedDate() {
        return createdDate;
    }
    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
    public Long getLastModifiedDate() {
        return lastModifiedDate;
    }
    public void setLastModifiedDate(Long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

}
