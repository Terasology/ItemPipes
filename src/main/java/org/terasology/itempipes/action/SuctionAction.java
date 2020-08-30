/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.itempipes.action;

import com.google.common.collect.Lists;
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
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.JomlUtil;
import org.terasology.math.Side;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.TriggerComponent;
import org.terasology.physics.components.shapes.SphereShapeComponent;
import org.terasology.physics.events.CollideEvent;
import org.terasology.physics.events.ImpulseEvent;
import org.terasology.registry.In;
import org.terasology.itempipes.components.SuctionCollisionManifold;
import org.terasology.itempipes.components.SuctionComponent;
import org.terasology.itempipes.controllers.PipeSystem;
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
                Map<Side, EntityRef> pipes = teraPipeSystem.findPipes(JomlUtil.from(blockComponent.position));
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
        event.getOtherEntity().send(new ImpulseEvent(blockComponent.getPosition().toVector3f().sub(locationComponent.getWorldPosition()).normalize().mul(2)));
    }
}
