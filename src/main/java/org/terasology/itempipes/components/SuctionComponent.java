// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.entitySystem.entity.EntityRef;

public class SuctionComponent implements Component {
    public  float range = 5f;
    public  EntityRef collisionManifold;

    public float delay = 1000f;
    public long lastTime;
}
