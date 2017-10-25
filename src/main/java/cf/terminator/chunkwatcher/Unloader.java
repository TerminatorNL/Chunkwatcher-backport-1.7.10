package cf.terminator.chunkwatcher;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeChunkManager;

public class Unloader {
    private final ChunkCoordIntPair pos;
    private final ForgeChunkManager.Ticket ticket;

    Unloader(ChunkCoordIntPair p, ForgeChunkManager.Ticket t){
        pos = p;
        ticket = t;
    }

    void unload(){
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void run(TickEvent.ServerTickEvent e){
        ForgeChunkManager.unforceChunk(ticket, pos);
        FMLCommonHandler.instance().bus().unregister(this);
        Main.LOGGER.info("Unloaded chunk: (X=" + pos.chunkXPos + ", Z=" + pos.chunkZPos + ") mod: " + ticket.getModId());
    }
}
