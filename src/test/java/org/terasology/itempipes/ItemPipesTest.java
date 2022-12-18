// Copyright 2022 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.itempipes;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.integrationenvironment.ModuleTestingHelper;
import org.terasology.engine.integrationenvironment.jupiter.IntegrationEnvironment;
import org.terasology.engine.logic.health.EngineDamageTypes;
import org.terasology.engine.logic.inventory.events.DropItemEvent;
import org.terasology.engine.math.Direction;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.BlockRegion;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.engine.world.block.family.BlockPlacementData;
import org.terasology.engine.world.block.items.BlockItemFactory;
import org.terasology.itempipes.components.PipeFollowingComponent;
import org.terasology.itempipes.controllers.PipeSystem;
import org.terasology.module.health.events.DoDamageEvent;
import org.terasology.module.inventory.components.InventoryComponent;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@IntegrationEnvironment(dependencies = {"ItemPipes", "CoreAdvancedAssets"}, worldGenerator = "unittest:empty")
public class ItemPipesTest {

    private BlockFamily itemPipesBlockFamily;
    private BlockFamily chestFamily;

    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private EntityManager entityManager;
    @In
    private PipeSystem pipeSystem;
    @In
    private Time time;
    @In
    private ModuleTestingHelper helper;

    @BeforeEach
    public void initialize() {
        Block airBlock = blockManager.getBlock("engine:air");
        itemPipesBlockFamily = blockManager.getBlockFamily("ItemPipes:basicPipe");
        chestFamily = blockManager.getBlockFamily("CoreAdvancedAssets:Chest.LEFT");

        BlockRegion region = new BlockRegion(0, 0, 0).expand(5, 5, 5);
        for (Vector3ic pos : region) {
            helper.forceAndWaitForGeneration(pos);
            worldProvider.setBlock(pos, airBlock);
        }
    }

    @Test
    public void connectionTest() {
        Vector3ic pipeLocation = new Vector3i(0, 0, 0);

        placePipe(pipeLocation);
        assertEquals(0, getConn(new Vector3i(pipeLocation)));

        //begin with 1 to omit the pipe without connections.
        for (byte connections = 1; connections < 64; connections++) {
            EnumSet<Side> sideSet = SideBitFlag.getSides(connections);

            for (Side side : sideSet) {
                placePipe(side.direction());
            }

            assertEquals(getConn(new Vector3i(pipeLocation)), connections);

            for (Side side : sideSet) {
                dealDamageOn(side.direction(), 10000);
            }
        }
    }

    @Test
    public void chestInputTest() {
        Vector3ic left = Direction.LEFT.asVector3i();
        Vector3ic center = new Vector3i();
        Vector3ic rChest = Direction.RIGHT.asVector3i();

        placePipe(left);
        placePipe(center);
        placeChest(rChest);

        EntityRef droppedItem = dropBlockItem(new Vector3f(Direction.LEFT.asVector3f()).add(Direction.UP.asVector3f()), "ItemPipes:suction");

        EntityRef startPipe = blockEntityRegistry.getBlockEntityAt(left);
        Prefab pathPrefab = pipeSystem.findingMatchingPathPrefab(startPipe, Side.RIGHT).iterator().next();
        pipeSystem.insertIntoPipe(droppedItem, startPipe, Side.RIGHT, pathPrefab, 1f);

        final long nextCheck = time.getGameTimeInMs() + 3000;
        helper.runWhile(() -> time.getGameTimeInMs() < nextCheck);

        EntityRef chestEntity = blockEntityRegistry.getBlockEntityAt(rChest);
        InventoryComponent inventory = chestEntity.getComponent(InventoryComponent.class);

        boolean foundDroppedItem = false;
        for (EntityRef slot : inventory.itemSlots) {
            if (slot == droppedItem) {
                foundDroppedItem = true;
                break;
            }
        }
        assertTrue(foundDroppedItem);
    }

    @Test
    public void minimumVelocityTest() {
        this.initialize();

        Vector3i newPipeLoc = new Vector3i();
        placePipe(new Vector3i(newPipeLoc));
        newPipeLoc.add(Direction.UP.asVector3i());
        placePipe(new Vector3i(newPipeLoc));
        newPipeLoc.add(Direction.RIGHT.asVector3i());
        placePipe(new Vector3i(newPipeLoc));
        newPipeLoc.add(Direction.DOWN.asVector3i());
        placePipe(new Vector3i(newPipeLoc));

        EntityRef droppedItem =
            dropBlockItem(new Vector3f(Direction.LEFT.asVector3f()).add(Direction.UP.asVector3f()), "ItemPipes" +
                ":suction");
        EntityRef startPipe = blockEntityRegistry.getBlockEntityAt(new Vector3i());
        Prefab pathPrefab = pipeSystem.findingMatchingPathPrefab(startPipe, Side.TOP).iterator().next();
        pipeSystem.insertIntoPipe(droppedItem, startPipe, Side.TOP, pathPrefab, 1f);

        for (int i = 0; i < 1000; i++) {
            final long nextCheck = time.getGameTimeInMs() + 100;
            helper.runWhile(() -> time.getGameTimeInMs() < nextCheck);
            PipeFollowingComponent pfComponent = droppedItem.getComponent(PipeFollowingComponent.class);
            assertTrue(pfComponent.velocity >= .5f || pfComponent.velocity <= -.5f);
        }

    }

    /**
     * Reads connection flags from an block at given location (used with ItemPipes)
     *
     * @param location location of the pipe we want to check.
     * @return byte connection flags.
     */
    private byte getConn(Vector3i location) {
        return Byte.valueOf(worldProvider.getBlock(location).getURI().getIdentifier().toString());
    }

    /**
     * Deals damage to block on given location (simulates the situation when player destroys a block)
     *
     * @param location location of the block to deal damage
     * @param damageAmount amount of the damage to be dealt.
     */
    private EntityRef dealDamageOn(Vector3ic location, int damageAmount) {
        EntityRef block = blockEntityRegistry.getBlockEntityAt(location);
        block.send(new DoDamageEvent(damageAmount, EngineDamageTypes.DIRECT.get()));

        return block;
    }

    private void placePipe(Vector3ic location) {
        helper.forceAndWaitForGeneration(location);

        worldProvider.setBlock(location,
            itemPipesBlockFamily.getBlockForPlacement(new BlockPlacementData(location, Side.FRONT,
                new Vector3f())));
    }

    private void placeChest(Vector3ic location) {
        helper.forceAndWaitForGeneration(location);

        worldProvider.setBlock(location,
            chestFamily.getBlockForPlacement(new BlockPlacementData(location, Side.FRONT,
                new Vector3f())));
    }


    /**
     * Spawns and drops an block item on desired location.
     *
     * @param location location of the item we want to drop.
     * @param id ID of blockItem's family.
     */
    private EntityRef dropBlockItem(Vector3f location, String id) {
        BlockFamily blockFamily = blockManager.getBlockFamily(id);
        BlockItemFactory blockItemFactory = new BlockItemFactory(entityManager);
        EntityRef newBlock = blockItemFactory.newInstance(blockFamily);

        newBlock.send(new DropItemEvent(location));

        return newBlock;
    }
}
