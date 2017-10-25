package cf.terminator.chunkwatcher;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeChunkManager;


public class ChunkLoadListener {
    @SubscribeEvent
    public void onLoad(ForgeChunkManager.ForceChunkEvent event){

        if(TicketManager.checkTicket(event.ticket) == false){
            for(ChunkCoordIntPair chunkPos : event.ticket.getChunkList()) {
                new Unloader(chunkPos, event.ticket).unload();
            }
        }
    }
}

