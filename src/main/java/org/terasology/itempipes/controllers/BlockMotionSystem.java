// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.controllers;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.math.Rotation;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.itempipes.blocks.PipeBlockSegmentMapper;
import org.terasology.itempipes.components.PipeComponent;
import org.terasology.itempipes.components.PipeConnectionComponent;
import org.terasology.itempipes.components.PipeFollowingComponent;
import org.terasology.itempipes.event.PipeInsertEvent;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.segmentedpaths.components.BlockMappingComponent;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;

@RegisterSystem(RegisterMode.AUTHORITY)
public class BlockMotionSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    @In
    EntityManager entityManager;
    @In
    PathFollowerSystem pathFollowerSystem;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    SegmentSystem segmentSystem;
    @In
    SegmentCacheSystem segmentCacheSystem;
    @In
    PipeSystem pipeSystem;

    private PipeBlockSegmentMapper segmentMapping;

    @Override
    public void initialise() {
        segmentMapping = new PipeBlockSegmentMapper(blockEntityRegistry, pathFollowerSystem, segmentSystem, segmentCacheSystem);
    }

    @Override
    public void update(float delta) {
        for (EntityRef entityRef : entityManager.getEntitiesWith(PipeFollowingComponent.class)) {
            PathFollowerComponent pathFollowingComponent = entityRef.getComponent(PathFollowerComponent.class);
            EntityRef blockEntity = pathFollowingComponent.segmentMeta.association;
            if (!blockEntity.exists()) {
                pipeSystem.dropItem(entityRef);
                return;
            }
            PipeComponent pipeComponent = blockEntity.getComponent(PipeComponent.class);
            PipeFollowingComponent pipeFollowingComponent = entityRef.getComponent(PipeFollowingComponent.class);
            LocationComponent locationComponent = entityRef.getComponent(LocationComponent.class);

            pipeFollowingComponent.velocity -= pipeComponent.friction * delta;
            if (Math.abs(pipeFollowingComponent.velocity) < .5f) {
                pipeFollowingComponent.velocity = .5f * Math.signum(pipeFollowingComponent.velocity);
            }

            if (pathFollowerSystem.move(entityRef, delta * pipeFollowingComponent.velocity, segmentMapping)) {
                Vector3f position = pathFollowerSystem.vehiclePoint(entityRef);
                locationComponent.setWorldPosition(position);
            } else {
                BlockComponent blockComponent = blockEntity.getComponent(BlockComponent.class);
                BlockFamily blockFamily = blockComponent.getBlock().getBlockFamily();
                BlockMappingComponent blockMappingComponent = pathFollowingComponent.segmentMeta.prefab.getComponent(BlockMappingComponent.class);
                if (blockFamily instanceof PathFamily) {
                    Rotation rotation = ((PathFamily) blockFamily).getRotationFor(blockComponent.getBlock().getURI());
                    Vector3i position = blockComponent.getPosition(new Vector3i());
                    if (pathFollowingComponent.segmentMeta.sign == 1) {
                        position.add(rotation.rotate(blockMappingComponent.s2).direction());
                    } else {
                        position.add(rotation.rotate(blockMappingComponent.s1).direction());
                    }
                    EntityRef nextBlock = blockEntityRegistry.getBlockEntityAt(position);
                    pipeSystem.dropItem(entityRef);
                    if (nextBlock.hasComponent(PipeConnectionComponent.class)) {
                        nextBlock.send(new PipeInsertEvent(entityRef, pathFollowingComponent.segmentMeta));
                    }
                } else {
                    pipeSystem.dropItem(entityRef);

                }
                return;
            }
            entityRef.saveComponent(locationComponent);
            entityRef.saveComponent(pathFollowingComponent);
            entityRef.saveComponent(pipeFollowingComponent);
        }
    }
}
