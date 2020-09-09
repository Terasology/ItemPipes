// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.action;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.math.Side;
import org.terasology.engine.physics.StandardCollisionGroup;
import org.terasology.engine.physics.components.TriggerComponent;
import org.terasology.engine.physics.components.shapes.SphereShapeComponent;
import org.terasology.engine.physics.events.CollideEvent;
import org.terasology.engine.physics.events.ImpulseEvent;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.items.OnBlockItemPlaced;
import org.terasology.itempipes.components.SuctionCollisionManifold;
import org.terasology.itempipes.components.SuctionComponent;
import org.terasology.itempipes.controllers.PipeSystem;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
public class SuctionAction extends BaseComponentSystem {

    private static final Logger logger = LoggerFactory.getLogger(SuctionAction.class);
    @In
    EntityManager entityManager;
    @In
    PipeSystem teraPipeSystem;
    @In
    private Time time;

    @ReceiveEvent
    public void onSuctionPlaced(OnBlockItemPlaced event, EntityRef entityRef) {
        EntityRef blockEntity = event.getPlacedBlock();
        SuctionComponent suctionComponent = blockEntity.getComponent(SuctionComponent.class);
        if (suctionComponent == null || suctionComponent.collisionManifold != null)
            return;

        BlockComponent blockComponent = blockEntity.getComponent(BlockComponent.class);


        EntityRef ref = entityManager.create();
        ref.setOwner(blockEntity);

        SphereShapeComponent sphereShapeComponent = new SphereShapeComponent();
        sphereShapeComponent.radius = suctionComponent.range;

        TriggerComponent triggerComponent = new TriggerComponent();
        triggerComponent.detectGroups = Lists.newArrayList(StandardCollisionGroup.DEBRIS);

        LocationComponent locationComponent = new LocationComponent();
        locationComponent.setWorldPosition(blockComponent.getPosition().toVector3f());

        ref.addComponent(triggerComponent);
        ref.addComponent(sphereShapeComponent);
        ref.addComponent(locationComponent);

        ref.addComponent(new SuctionCollisionManifold());

        suctionComponent.collisionManifold = ref;

    }

    @ReceiveEvent(components = {SuctionCollisionManifold.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onBump(CollideEvent event, EntityRef entity) {

        EntityRef owner = entity.getOwner();
        if (!owner.exists()) {
            owner.destroy();
            return;
        }

        BlockComponent blockComponent = owner.getComponent(BlockComponent.class);
        SuctionComponent suctionComponent = owner.getComponent(SuctionComponent.class);

        LocationComponent locationComponent = event.getOtherEntity().getComponent(LocationComponent.class);

        if (blockComponent.getPosition().toVector3f().distance(locationComponent.getWorldPosition()) <= 1f) {
            if (suctionComponent.lastTime + suctionComponent.delay < time.getGameTimeInMs()) {
                suctionComponent.lastTime = time.getGameTimeInMs();
                Map<Side, EntityRef> pipes = teraPipeSystem.findPipes(blockComponent.getPosition());
                Optional<Side> side =
                        pipes.keySet().stream().skip((int) (pipes.keySet().size() * Math.random())).findFirst();
                if (side.isPresent()) {
                    EntityRef entityRef = pipes.get(side.get());
                    Set<Prefab> prefabs = teraPipeSystem.findingMatchingPathPrefab(entityRef, side.get().reverse());
                    Optional<Prefab> pick = prefabs.stream().skip((int) (prefabs.size() * Math.random())).findFirst();
                    teraPipeSystem.insertIntoPipe(event.getOtherEntity(), entityRef, side.get().reverse(), pick.get()
                            , 1f);
                }
            }
        }
        event.getOtherEntity().send(new ImpulseEvent(blockComponent.getPosition().toVector3f().sub(locationComponent.getWorldPosition()).normalize().mul(2)));
    }
}
