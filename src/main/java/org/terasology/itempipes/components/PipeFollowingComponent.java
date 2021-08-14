// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.components;

import org.terasology.gestalt.entitysystem.component.Component;

public class PipeFollowingComponent implements Component<PipeFollowingComponent> {
    public float velocity = 0.0f;

    @Override
    public void copyFrom(PipeFollowingComponent other) {
        this.velocity = other.velocity;
    }
}
