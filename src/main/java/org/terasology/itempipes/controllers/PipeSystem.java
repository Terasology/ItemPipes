// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.controllers;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.itempipes.blocks.PipeBlockFamily;
import org.terasology.itempipes.components.PipeFollowingComponent;
import org.terasology.logic.common.lifespan.LifespanComponent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.PickupComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.segmentedpaths.SegmentMeta;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.segmentedpaths.components.BlockMappingComponent;
import org.terasology.segmentedpaths.components.PathDescriptorComponent;
import org.terasology.segmentedpaths.components.PathFollowerComponent;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.segmentedpaths.segments.Segment;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.family.BlockFamily;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
@Share(value = PipeSystem.class)
public class PipeSystem extends BaseComponentSystem {
    @In
    private Time time;
    @In
    private WorldProvider worldProvider;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private SegmentSystem segmentSystem;
    @In
    private SegmentCacheSystem segmentCacheSystem;


    public boolean isConnected(Vector3ic location, Side side) {
        Vector3i toTest = new Vector3i(location).add(side.direction());
        if (worldProvider.isBlockRelevant(toTest)) {
            Block block = worldProvider.getBlock(toTest);
            final BlockFamily blockFamily = block.getBlockFamily();
            EntityRef pipeEntity = blockEntityRegistry.getBlockEntityAt(toTest);
            if (blockFamily instanceof PipeBlockFamily) {
                PathDescriptorComponent pathDescriptor = pipeEntity.getComponent(PathDescriptorComponent.class);
                EnumSet<Side> sides = ((PipeBlockFamily) blockFamily).getSides(block.getURI());
                for (Side s : sides) {
                    if (s.reverse().equals(side)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public Set<Prefab> findingMatchingPathPrefab(EntityRef pipe, Side side) {
        PathDescriptorComponent pathDescriptor = pipe.getComponent(PathDescriptorComponent.class);
        Quaternionf rotation = segmentSystem.segmentRotation(pipe);
        Set<Prefab> results = Sets.newHashSet();
        for (Prefab path : pathDescriptor.descriptors) {
            BlockMappingComponent blockMappingComponent = path.getComponent(BlockMappingComponent.class);
            Side s1 = Side.inDirection(rotation.transform(new Vector3f(blockMappingComponent.s1.direction())));
            Side s2 = Side.inDirection(rotation.transform(new Vector3f(blockMappingComponent.s2.direction())));
            if (s1.equals(side) || s2.equals(side)) {
                results.add(path);
            }
        }
        return results;
    }

    public Set<Prefab> filterPrefabBySide(Rotation rotation, Set<Prefab> prefabs, Side side) {
        Set<Prefab> result = Sets.newHashSet();
        for (Prefab prefab : prefabs) {
            BlockMappingComponent blockMappingComponent = prefab.getComponent(BlockMappingComponent.class);
            Side s1 = rotation.rotate(blockMappingComponent.s1);
            Side s2 = rotation.rotate(blockMappingComponent.s2);
            if (s1.equals(side) || s2.equals(side)) {
                result.add(prefab);
            }

        }
        return result;
    }


    public Segment getSegment(Prefab prefab) {
        return segmentCacheSystem.getSegment(prefab);
    }


    public Map<Side, EntityRef> findPipes(Vector3i location) {
        Map<Side, EntityRef> pipes = Maps.newHashMap();
        for (Side side : Side.values()) {
            Vector3i neighborLocation = new Vector3i(location);
            neighborLocation.add(side.direction());
            if (worldProvider.isBlockRelevant(neighborLocation)) {
                Block neighborBlock = worldProvider.getBlock(neighborLocation);
                final BlockFamily blockFamily = neighborBlock.getBlockFamily();
                if (blockFamily instanceof PipeBlockFamily) {
                    pipes.put(side, blockEntityRegistry.getBlockEntityAt(neighborLocation));
                }
            }
        }
        return pipes;
    }

    public void dropItem(EntityRef actor) {
        ItemComponent itemComponent = actor.getComponent(ItemComponent.class);

        Prefab prefab = itemComponent.pickupPrefab;
        PickupComponent pickupComponent = prefab.getComponent(PickupComponent.class);
        pickupComponent.timeDropped = time.getGameTimeInMs();
        RigidBodyComponent rigidBodyComponent = prefab.getComponent(RigidBodyComponent.class);
        LifespanComponent lifespanComponent = prefab.getComponent(LifespanComponent.class);

        actor.addComponent(rigidBodyComponent);
        actor.addComponent(lifespanComponent);
        actor.addComponent(pickupComponent);

        actor.removeComponent(PipeFollowingComponent.class);
        actor.removeComponent(PathFollowerComponent.class);
    }


    public boolean insertIntoPipe(EntityRef actor, EntityRef pipe, Side side, Prefab prefab, float velocity) {
        if (actor.hasComponent(PipeFollowingComponent.class)) {
            return false;
        }
        if (!actor.hasComponent(ItemComponent.class)) {
            return false;
        }

        BlockComponent blockComponent = pipe.getComponent(BlockComponent.class);
        if (blockComponent == null) {
            return false;
        }
        Block block = blockComponent.block;
        BlockFamily family = block.getBlockFamily();
        if (family instanceof PathFamily) {
            BlockMappingComponent blockMappingComponent = prefab.getComponent(BlockMappingComponent.class);
            if (blockMappingComponent == null) {
                return false;
            }
            Rotation rotation = ((PathFamily) family).getRotationFor(block.getURI());
            PathFollowerComponent pathFollowerComponent = new PathFollowerComponent();
            if (rotation.rotate(blockMappingComponent.s1).equals(side)) {
                pathFollowerComponent.segmentMeta = new SegmentMeta(0, pipe, prefab);
                pathFollowerComponent.segmentMeta.sign = 1;
            } else if (rotation.rotate(blockMappingComponent.s2).equals(side)) {
                Segment segment = segmentCacheSystem.getSegment(prefab);
                pathFollowerComponent.segmentMeta = new SegmentMeta(segment.maxDistance(), pipe, prefab);
                pathFollowerComponent.segmentMeta.sign = -1;
            } else {
                return false;
            }
            PipeFollowingComponent pipeFollowingComponent = new PipeFollowingComponent();
            pipeFollowingComponent.velocity = Math.abs(velocity);

            LocationComponent locationComponent = actor.getComponent(LocationComponent.class);
            locationComponent.setWorldRotation(new Quaternionf(new AxisAngle4f(0, 0, 1, 0)));

            actor.saveComponent(locationComponent);
            actor.addOrSaveComponent(pathFollowerComponent);
            actor.addComponent(pipeFollowingComponent);

            actor.removeComponent(PickupComponent.class);
            actor.removeComponent(RigidBodyComponent.class);
            actor.removeComponent(LifespanComponent.class);
            return true;
        }
        return false;
    }
}
