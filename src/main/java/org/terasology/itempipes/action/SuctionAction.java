// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.action;

import com.google.common.collect.Lists;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.Priority;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.math.Side;
import org.terasology.engine.physics.CollisionGroup;
import org.terasology.engine.physics.StandardCollisionGroup;
import org.terasology.engine.physics.components.TriggerComponent;
import org.terasology.engine.physics.components.shapes.SphereShapeComponent;
import org.terasology.engine.physics.events.CollideEvent;
import org.terasology.engine.physics.events.ImpulseEvent;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.items.OnBlockItemPlaced;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.itempipes.components.SuctionCollisionManifold;
import org.terasology.itempipes.components.SuctionComponent;
import org.terasology.itempipes.controllers.PipeSystem;

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

    @Priority(EventPriority.PRIORITY_HIGH)
    @ReceiveEvent(components = {SuctionCollisionManifold.class})
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
