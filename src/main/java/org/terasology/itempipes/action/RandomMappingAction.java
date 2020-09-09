// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.action;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.math.Side;
import org.terasology.itempipes.components.PipeRandomMappingComponent;
import org.terasology.itempipes.event.PipeMappingEvent;

import java.util.Optional;

@RegisterSystem(RegisterMode.AUTHORITY)
public class RandomMappingAction extends BaseComponentSystem {
    @ReceiveEvent(components = {PipeRandomMappingComponent.class})
    public void onSuctionPlaced(PipeMappingEvent event, EntityRef entityRef) {
        Optional<Side> side =
                event.getOutputSides().stream().skip((int) (event.getOutputSides().size() * Math.random())).findFirst();
        if (side.isPresent()) {
            event.setOutputSide(side.get());
        }
    }
}
