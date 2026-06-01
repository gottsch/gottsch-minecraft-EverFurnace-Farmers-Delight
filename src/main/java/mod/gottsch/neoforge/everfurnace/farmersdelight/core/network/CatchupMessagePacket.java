/*
 * This file is part of EverFurnace: Farmer's Delight.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * EverFurnace: Farmer's Delight is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EverFurnace: Farmer's Delight is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with EverFurnace: Farmer's Delight.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.neoforge.everfurnace.farmersdelight.core.network;

import io.netty.buffer.ByteBuf;
import mod.gottsch.neoforge.everfurnace.farmersdelight.EverFurnaceFD;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent server → client when catch-up completes and at least one item was cooked on a
 * Farmer's Delight cooking block. The client handler shows an action-bar message — but
 * only if the player has enabled it in their client config.
 *
 * <p>The block position is carried for potential future use (e.g. positional message);
 * the current handler does not require it.
 *
 * @author Mark Gottschling
 */
public record CatchupMessagePacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<CatchupMessagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EverFurnaceFD.MOD_ID, "catchup_message"));

    public static final StreamCodec<ByteBuf, CatchupMessagePacket> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(CatchupMessagePacket::new, CatchupMessagePacket::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles the packet on the client main thread (guaranteed by {@code playToClient}
     * registration). Performs a runtime dist check before delegating to the
     * {@code @OnlyIn(CLIENT)} handler.
     */
    public static void handle(CatchupMessagePacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CatchupMessageHandler.handle(packet.pos());
        }
    }
}
