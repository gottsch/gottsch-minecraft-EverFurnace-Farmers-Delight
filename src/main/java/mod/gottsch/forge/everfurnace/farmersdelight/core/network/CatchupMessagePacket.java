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
package mod.gottsch.forge.everfurnace.farmersdelight.core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server → client when catch-up completes and at least one item was cooked on a Farmer's
 * Delight cooking pot or stove. The client handler shows an action-bar message — but only if the
 * player has enabled it in the client config.
 *
 * <p>The block position is carried for parity with core's particle packet and potential future
 * use (e.g. a positional message); the current handler does not require it.
 *
 * @author Mark Gottschling
 */
public class CatchupMessagePacket {

    private final BlockPos pos;

    public CatchupMessagePacket(BlockPos pos) {
        this.pos = pos;
    }

    // -------------------------------------------------------------------------
    // serialization
    // -------------------------------------------------------------------------

    public static void encode(CatchupMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
    }

    public static CatchupMessagePacket decode(FriendlyByteBuf buf) {
        return new CatchupMessagePacket(buf.readBlockPos());
    }

    // -------------------------------------------------------------------------
    // client handler
    // -------------------------------------------------------------------------

    public static void handle(CatchupMessagePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> CatchupMessageHandler.handle(packet.pos))
        );
        ctx.setPacketHandled(true);
    }
}
