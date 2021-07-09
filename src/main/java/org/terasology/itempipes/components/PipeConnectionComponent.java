// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.components;

import com.google.common.collect.Lists;
import org.terasology.engine.math.Side;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.List;

public class PipeConnectionComponent implements Component<PipeConnectionComponent> {
    List<Side> sides;

    @Override
    public void copy(PipeConnectionComponent other) {
        this.sides = Lists.newArrayList(other.sides);
    }
}
