// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.action;

import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.registry.In;
import org.terasology.gestalt.entitysystem.component.Component;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.itempipes.event.PipeInsertEvent;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.systems.InventoryManager;

@RegisterSystem(RegisterMode.AUTHORITY)
public class InventoryInsertAction extends BaseComponentSystem {
    @In
    InventoryManager inventoryManager;

    @In
    EntityManager entityManager;

    @ReceiveEvent
    public void onInvetoryInsert(PipeInsertEvent event, EntityRef entityRef, InventoryComponent inventoryComponent) {
        if (inventoryManager.giveItem(entityRef, EntityRef.NULL, event.getActor())) {
            ItemComponent itemComponent = event.getActor().getComponent(ItemComponent.class);
            if (itemComponent != null) {
                for (Component component : itemComponent.pickupPrefab.iterateComponents()) {
                    event.getActor().removeComponent(component.getClass());
                }
            }
        }
    }
}
