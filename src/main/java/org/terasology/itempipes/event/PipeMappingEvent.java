// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.event;

import org.terasology.entitySystem.event.AbstractConsumableEvent;
import org.terasology.math.Side;

import java.util.Set;

public class PipeMappingEvent extends AbstractConsumableEvent {
    private Set<Side> outputSides;
    private Side outputSide;

    public PipeMappingEvent(Set<Side> sides) {
        this.outputSides = sides;
        for (Side side : sides) {
            this.outputSide = side;
            break;
        }
    }

    public Set<Side> getOutputSides() {
        return outputSides;
    }

    public Side getOutputSide() {
        return outputSide;
    }

    public void setOutputSide(Side outputSide) {
        this.outputSide = outputSide;
    }
}
