// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.action;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.itempipes.event.PipeInsertEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.registry.In;

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
