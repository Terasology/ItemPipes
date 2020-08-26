// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.event;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.AbstractConsumableEvent;
import org.terasology.segmentedpaths.SegmentMeta;

public class PipeInsertEvent extends AbstractConsumableEvent {
    private SegmentMeta segmentMeta;
    private EntityRef actor;

    public PipeInsertEvent(EntityRef actor, SegmentMeta meta) {
        this.segmentMeta = meta;
        this.actor = actor;
    }

    public SegmentMeta getSegmentMeta() {
        return segmentMeta;
    }

    public EntityRef getActor() {
        return actor;
    }
}
