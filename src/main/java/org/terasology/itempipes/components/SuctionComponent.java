// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.components;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.component.Component;

public class SuctionComponent implements Component<SuctionComponent> {
    public  float range = 5f;
    public  EntityRef collisionManifold;

    public float delay = 1000f;
    public long lastTime;

    @Override
    public void copy(SuctionComponent other) {
        this.range = other.range;
        this.collisionManifold = other.collisionManifold;
        this.delay = other.delay;
        this.lastTime = other.lastTime;
    }
}
