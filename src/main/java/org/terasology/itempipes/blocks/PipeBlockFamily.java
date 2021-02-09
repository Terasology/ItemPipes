// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes.blocks;

import com.google.common.collect.Sets;
import gnu.trove.map.TByteObjectMap;
import gnu.trove.map.hash.TByteObjectHashMap;
import org.joml.Vector3ic;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.itempipes.components.PipeComponent;
import org.terasology.itempipes.components.PipeConnectionComponent;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.naming.Name;
import org.terasology.segmentedpaths.blocks.PathFamily;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.BlockSections;
import org.terasology.world.block.family.MultiConnectFamily;
import org.terasology.world.block.family.RegisterBlockFamily;
import org.terasology.world.block.loader.BlockFamilyDefinition;
import org.terasology.world.block.shapes.BlockShape;

import java.util.EnumSet;
import java.util.Set;

@RegisterBlockFamily("pipe")
@BlockSections({"no_connections", "one_connection", "line_connection", "2d_corner", "3d_corner", "2d_t", "cross", "3d_side", "five_connections", "all"})
public class PipeBlockFamily extends MultiConnectFamily implements PathFamily {
    public static final String NO_CONNECTIONS = "no_connections";
    public static final String ONE_CONNECTION = "one_connection";
    public static final String TWO_CONNECTIONS_LINE = "line_connection";
    public static final String TWO_CONNECTIONS_CORNER = "2d_corner";
    public static final String THREE_CONNECTIONS_CORNER = "3d_corner";
    public static final String THREE_CONNECTIONS_T = "2d_t";
    public static final String FOUR_CONNECTIONS_CROSS = "cross";
    public static final String FOUR_CONNECTIONS_SIDE = "3d_side";
    public static final String FIVE_CONNECTIONS = "five_connections";
    public static final String SIX_CONNECTIONS = "all";

    private TByteObjectMap<Rotation> rotationMap = new TByteObjectHashMap<>();


    public PipeBlockFamily(BlockFamilyDefinition definition, BlockShape shape, BlockBuilderHelper blockBuilder) {
        super(definition, shape, blockBuilder);
    }

    public PipeBlockFamily(BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder) {
        super(definition, blockBuilder);

        BlockUri blockUri = new BlockUri(definition.getUrn());

        this.registerBlock(blockUri, definition, blockBuilder, NO_CONNECTIONS, (byte) 0, Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, ONE_CONNECTION, SideBitFlag.getSides(Side.BACK),
            Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, TWO_CONNECTIONS_LINE, SideBitFlag.getSides(Side.BACK,
            Side.FRONT), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, TWO_CONNECTIONS_CORNER, SideBitFlag.getSides(Side.LEFT
            , Side.BACK), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, THREE_CONNECTIONS_CORNER,
            SideBitFlag.getSides(Side.LEFT, Side.BACK, Side.TOP), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, THREE_CONNECTIONS_T, SideBitFlag.getSides(Side.LEFT,
            Side.BACK, Side.FRONT), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, FOUR_CONNECTIONS_CROSS,
            SideBitFlag.getSides(Side.RIGHT, Side.LEFT, Side.BACK, Side.FRONT), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, FOUR_CONNECTIONS_SIDE, SideBitFlag.getSides(Side.LEFT,
            Side.BACK, Side.FRONT, Side.TOP), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, FIVE_CONNECTIONS, SideBitFlag.getSides(Side.LEFT,
            Side.BACK, Side.FRONT, Side.TOP, Side.BOTTOM), Rotation.allValues());
        this.registerBlock(blockUri, definition, blockBuilder, SIX_CONNECTIONS, SideBitFlag.getSides(Side.LEFT,
            Side.BACK, Side.FRONT, Side.TOP, Side.BOTTOM, Side.RIGHT), Rotation.allValues());
    }

    public EnumSet<Side> getSides(BlockUri blockUri) {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                byte connections = Byte.parseByte(blockUri.getIdentifier().toString());
                return SideBitFlag.getSides(connections);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }


    @Override
    public byte getConnectionSides() {
        return SideBitFlag.getSides(Side.LEFT, Side.BACK, Side.FRONT, Side.TOP, Side.BOTTOM, Side.RIGHT);
    }

    @Override
    public Set<Block> registerBlock(BlockUri root, BlockFamilyDefinition definition, BlockBuilderHelper blockBuilder,
                                    String name, byte sides, Iterable<Rotation> rotations) {
        Set<Block> result = Sets.newLinkedHashSet();
        for (Rotation rotation : rotations) {
            byte sideBits = 0;
            for (Side side : SideBitFlag.getSides(sides)) {
                sideBits += SideBitFlag.getSide(rotation.rotate(side));
            }
            Block block = blockBuilder.constructTransformedBlock(definition, name, rotation, new BlockUri(root,
                new Name(String.valueOf(sideBits))), this);
            rotationMap.put(sideBits, rotation);
            blocks.put(sideBits, block);
            result.add(block);
        }
        return result;
    }

    @Override
    protected boolean connectionCondition(Vector3ic blockLocation, Side connectSide) {
        org.joml.Vector3i neighborLocation = new org.joml.Vector3i(blockLocation);
        neighborLocation.add(connectSide.direction());

        EntityRef neighborEntity = blockEntityRegistry.getBlockEntityAt(neighborLocation);
        return neighborEntity != null && (neighborEntity.hasComponent(PipeComponent.class) || neighborEntity.hasComponent(PipeConnectionComponent.class));
    }


    @Override
    public Block getArchetypeBlock() {
        return blocks.get((byte) 0);
    }

    @Override
    public Rotation getRotationFor(BlockUri blockUri) {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                byte connections = Byte.parseByte(blockUri.getIdentifier().toString());
                return rotationMap.get(connections);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
