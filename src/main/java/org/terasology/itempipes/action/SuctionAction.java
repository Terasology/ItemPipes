// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.action;

import com.google.common.collect.Lists;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.itempipes.components.SuctionCollisionManifold;
import org.terasology.itempipes.components.SuctionComponent;
import org.terasology.itempipes.controllers.PipeSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Side;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.TriggerComponent;
import org.terasology.physics.components.shapes.SphereShapeComponent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.registry.In;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.items.OnBlockItemPlaced;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
public class SuctionAction  extends BaseComponentSystem {

    private static final Logger logger = LoggerFactory.getLogger(SuctionAction.class);

    @In
    private Time time;

    @In
    EntityManager entityManager;

    @In
    PipeSystem teraPipeSystem;

    @ReceiveEvent
    public void onSuctionPlaced(OnBlockItemPlaced event, EntityRef entityRef) {
        EntityRef blockEntity = event.getPlacedBlock();
        SuctionComponent suctionComponent = blockEntity.getComponent(SuctionComponent.class);
        if (suctionComponent == null || suctionComponent.collisionManifold != null) {
            return;
        }

        BlockComponent blockComponent = blockEntity.getComponent(BlockComponent.class);


        EntityRef ref = entityManager.create();
        ref.setOwner(blockEntity);

        SphereShapeComponent sphereShapeComponent = new SphereShapeComponent();
        sphereShapeComponent.radius = suctionComponent.range;

        TriggerComponent triggerComponent = new TriggerComponent();
        triggerComponent.detectGroups = Lists.<CollisionGroup>newArrayList(StandardCollisionGroup.DEBRIS);

        LocationComponent locationComponent = new LocationComponent();
        locationComponent.setWorldPosition(new Vector3f(blockComponent.getPosition(new Vector3i())));

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

        if (new Vector3f(blockComponent.getPosition(new Vector3i())).distanceSquared(locationComponent.getWorldPosition(new Vector3f())) <= 1f) {
            if (suctionComponent.lastTime + suctionComponent.delay < time.getGameTimeInMs()) {
                suctionComponent.lastTime = time.getGameTimeInMs();
                Map<Side, EntityRef> pipes = teraPipeSystem.findPipes(blockComponent.getPosition(new Vector3i()));
                Optional<Side> side =
                    pipes.keySet().stream().skip((int) (pipes.keySet().size() * Math.random())).findFirst();
                if (side.isPresent()) {
                    EntityRef entityRef = pipes.get(side.get());
                    Set<Prefab> prefabs = teraPipeSystem.findingMatchingPathPrefab(entityRef, side.get().reverse());
                    Optional<Prefab> pick = prefabs.stream().skip((int) (prefabs.size() * Math.random())).findFirst();
                    pick.ifPresent(prefab -> teraPipeSystem.insertIntoPipe(event.getOtherEntity(), entityRef,
                        side.get().reverse(), prefab, 1f));
                }
            }
        }
        event.getOtherEntity().send(new ImpulseEvent(new Vector3f(blockComponent.getPosition(new Vector3i())).sub(locationComponent.getWorldPosition(new Vector3f())).normalize().mul(2)));
    }
}
