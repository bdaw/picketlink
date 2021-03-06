/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.picketlink.idm.model;

/**
 * A hierarchical abstraction representing a partitioned set or subset of services, for which
 * specialized Roles and Groups may be created.
 * 
 * @author Shane Bryzak
 */
public class Tier extends AbstractPartition {

    private static final long serialVersionUID = 7797059334915537276L;

    public static final String KEY_PREFIX = "TIER://";

    private String description;
    private Tier parent;

    public Tier(String name) {
        super(name);
    }
    
    public Tier(String name, String description, Tier parent) {
        this(name);
        this.description = description;
        this.parent = parent;
    }

    public Tier(String name, Tier applicationTier) {
        this(name, null, applicationTier);
    }

    public String getDescription() {
        return this.description;
    }

    public Tier getParent() {
        return this.parent;
    }
    
    @Override
    public String getKey() {
        return String.format("%s%s", KEY_PREFIX, getName());
    }

    public void setParent(Tier parent) {
        this.parent = parent;
    }
    
}
