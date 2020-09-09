// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.blocks;

import com.google.common.collect.Maps;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.math.Rotation;
import org.terasology.engine.math.Side;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.itempipes.event.PipeMappingEvent;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.segmentedpaths.SegmentMeta;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.segmentedpaths.components.BlockMappingComponent;
import org.terasology.segmentedpaths.components.PathDescriptorComponent;
import org.terasology.segmentedpaths.controllers.PathFollowerSystem;
import org.terasology.segmentedpaths.controllers.SegmentCacheSystem;
import org.terasology.segmentedpaths.controllers.SegmentMapping;
import org.terasology.segmentedpaths.controllers.SegmentSystem;
import org.terasology.segmentedpaths.segments.Segment;

import java.util.Map;

public class PipeBlockSegmentMapper implements SegmentMapping {
    private final PathFollowerSystem pathFollowerSystem;
    private final SegmentSystem segmentSystem;
    private final SegmentCacheSystem segmentCacheSystem;
    private final BlockEntityRegistry blockEntityRegistry;

    public PipeBlockSegmentMapper(BlockEntityRegistry blockEntityRegistry, PathFollowerSystem pathFollowerSystem,
                                  SegmentSystem segmentSystem, SegmentCacheSystem segmentCacheSystem) {
        this.blockEntityRegistry = blockEntityRegistry;
        this.pathFollowerSystem = pathFollowerSystem;
        this.segmentCacheSystem = segmentCacheSystem;
        this.segmentSystem = segmentSystem;
    }


    @Override
    public MappingResult nextSegment(SegmentMeta meta, SegmentEnd ends) {
        if (meta.association.hasComponent(BlockComponent.class)) {
            BlockComponent blockComponent = meta.association.getComponent(BlockComponent.class);

            BlockFamily blockFamily = blockComponent.getBlock().getBlockFamily();

            Vector3f v1 = segmentSystem.segmentPosition(meta.association);
            Quat4f q1 = segmentSystem.segmentRotation(meta.association);

            Segment currentSegment = segmentCacheSystem.getSegment(meta.prefab);

            BlockMappingComponent blockMappingComponent = meta.prefab.getComponent(BlockMappingComponent.class);
            if (blockFamily instanceof PathFamily) {

                Rotation rotation = ((PathFamily) blockFamily).getRotationFor(blockComponent.block.getURI());
                switch (ends) {
                    case START: {
                        Vector3i segment =
                                new Vector3i(blockComponent.position).add(rotation.rotate(blockMappingComponent.s1).getVector3i());
                        EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(segment);
                        PathDescriptorComponent pathDescriptor =
                                blockEntity.getComponent(PathDescriptorComponent.class);
                        if (pathDescriptor == null) {
                            return null;
                        }

                        Vector3f v2 = segmentSystem.segmentPosition(blockEntity);
                        Quat4f q2 = segmentSystem.segmentRotation(blockEntity);

                        Map<Side, Prefab> paths = Maps.newHashMap();
                        for (Prefab d : pathDescriptor.descriptors) {
                            Segment nextSegment = segmentCacheSystem.getSegment(d);
                            BlockMappingComponent nextBlockMapping = d.getComponent(BlockMappingComponent.class);
                            switch (segmentSystem.segmentMatch(currentSegment, v1, q1, nextSegment, v2, q2)) {
                                case Start_End:
                                    paths.put(Side.inDirection(q2.rotate(nextBlockMapping.s1.getVector3i().toVector3f())), d);
                                    break;
                                case Start_Start:
                                    paths.put(Side.inDirection(q2.rotate(nextBlockMapping.s2.getVector3i().toVector3f())), d);
                                    break;
                            }
                        }
                        PipeMappingEvent pipeMappingEvent = blockEntity.send(new PipeMappingEvent(paths.keySet()));
                        Prefab prefab = paths.get(pipeMappingEvent.getOutputSide());
                        if (prefab == null) {
                            return null;
                        }

                        return new MappingResult(prefab, blockEntity);

                    }
                    case END: {
                        Vector3i segment =
                                new Vector3i(blockComponent.position).add(rotation.rotate(blockMappingComponent.s2).getVector3i());
                        EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(segment);
                        PathDescriptorComponent pathDescriptor =
                                blockEntity.getComponent(PathDescriptorComponent.class);
                        if (pathDescriptor == null) {
                            return null;
                        }

                        Vector3f v2 = segmentSystem.segmentPosition(blockEntity);
                        Quat4f q2 = segmentSystem.segmentRotation(blockEntity);

                        Map<Side, Prefab> paths = Maps.newHashMap();
                        for (Prefab d : pathDescriptor.descriptors) {
                            Segment nextSegment = segmentCacheSystem.getSegment(d);
                            BlockMappingComponent nextBlockMapping = d.getComponent(BlockMappingComponent.class);
                            switch (segmentSystem.segmentMatch(currentSegment, v1, q1, nextSegment, v2, q2)) {
                                case End_End:
                                    paths.put(Side.inDirection(q2.rotate(nextBlockMapping.s1.getVector3i().toVector3f())), d);
                                    break;
                                case End_Start:
                                    paths.put(Side.inDirection(q2.rotate(nextBlockMapping.s2.getVector3i().toVector3f())), d);
                                    break;
                            }
                        }
                        PipeMappingEvent pipeMappingEvent = blockEntity.send(new PipeMappingEvent(paths.keySet()));
                        Prefab prefab = paths.get(pipeMappingEvent.getOutputSide());
                        if (prefab == null) {
                            return null;
                        }

                        return new MappingResult(prefab, blockEntity);
                    }
                }
            }
        }

        return null;
    }
}
