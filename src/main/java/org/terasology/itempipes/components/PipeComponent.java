// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.components;

import org.terasology.gestalt.entitysystem.component.Component;

public class PipeComponent implements Component<PipeComponent> {
    public float friction = .1f;

    @Override
    public void copyFrom(PipeComponent other) {
        this.friction = other.friction;
    }
}
