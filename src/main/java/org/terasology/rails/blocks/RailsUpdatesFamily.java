/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.rails.blocks;

import gnu.trove.map.TByteObjectMap;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.Vector3i;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.AbstractBlockFamily;
import org.terasology.world.block.family.ConnectionCondition;

import java.util.EnumSet;
import java.util.List;

public class RailsUpdatesFamily extends AbstractBlockFamily {
    private ConnectionCondition connectionCondition;
    private Block archetypeBlock;
    private TByteObjectMap<Block> blocks;
    private byte connectionSides;

    public RailsUpdatesFamily(ConnectionCondition connectionCondition, BlockUri blockUri,
                              List<String> categories, Block archetypeBlock, TByteObjectMap<Block> blocks, byte connectionSides) {
        super(blockUri, categories);

        this.connectionCondition = connectionCondition;
        this.archetypeBlock = archetypeBlock;
        this.blocks = blocks;
        this.connectionSides = connectionSides;

        for (Block block : blocks.valueCollection()) {
            block.setBlockFamily(this);
        }
    }

    @Override
    public Block getArchetypeBlock() {
        return archetypeBlock;
    }

    @Override
    public Block getBlockForPlacement(WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry, Vector3i location, Side attachmentSide, Side direction) {
        return blocks.get(getByteConnections(worldProvider, blockEntityRegistry, location));
    }

    public Block getBlockForNeighborRailUpdate(WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry, Vector3i location, Block oldBlock) {
        return blocks.get(getByteConnections(worldProvider, blockEntityRegistry, location));
    }

    @Override
    public Block getBlockFor(BlockUri blockUri) {
        if (getURI().equals(blockUri.getFamilyUri())) {
            try {
                byte connections = Byte.parseByte(blockUri.getIdentifier());
                return blocks.get(connections);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Iterable<Block> getBlocks() {
        return blocks.valueCollection();
    }

    private byte getByteConnections(WorldProvider worldProvider, BlockEntityRegistry blockEntityRegistry, Vector3i location) {
        byte connections = 0;
        int countConnetions = 0;
        for (Side connectSide : SideBitFlag.getSides(connectionSides)) {
            if (connectionCondition.isConnectingTo(location, connectSide, worldProvider, blockEntityRegistry)) {
                connections += SideBitFlag.getSide(connectSide);
            }
        }
        countConnetions = SideBitFlag.getSides(connections).size();
        Vector3i upLocation = new Vector3i(location);
        upLocation.y -= 1;
        for (Side connectSide : SideBitFlag.getSides(connectionSides)) {
            if (connectionCondition.isConnectingTo(upLocation, connectSide, worldProvider, blockEntityRegistry)) {
                connections += SideBitFlag.getSide(connectSide);
            }
        }
        upLocation.y += 2;
        switch(countConnetions) {
            case 0:
                for (Side connectSide : SideBitFlag.getSides(connectionSides)) {
                    if (connectionCondition.isConnectingTo(upLocation, connectSide, worldProvider, blockEntityRegistry)) {
                        connections += SideBitFlag.getSide(connectSide);
                        connections += SideBitFlag.getSide(Side.TOP);
                        break;
                    }
                }
                break;
            case 1:
                EnumSet<Side> sides = SideBitFlag.getSides(connections);
                Side connectSide = (Side)sides.toArray()[0];
                connectSide = connectSide.reverse();
                if (connectionCondition.isConnectingTo(upLocation, connectSide, worldProvider, blockEntityRegistry)) {
                    connections += SideBitFlag.getSide(connectSide);
                    connections += SideBitFlag.getSide(Side.TOP);
                    break;
                }
                break;
        }
        return connections;
    }
}
