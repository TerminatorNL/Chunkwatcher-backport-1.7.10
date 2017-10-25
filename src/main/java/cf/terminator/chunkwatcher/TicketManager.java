package cf.terminator.chunkwatcher;

import com.google.gson.JsonElement;
import net.minecraftforge.common.ForgeChunkManager;

class TicketManager {

    static boolean checkTicket(ForgeChunkManager.Ticket ticket){
        String mod = ticket.getModId().toLowerCase();
        for(JsonElement e : Main.CONFIG.getAsJsonArray("Blacklist")){
            if(e.getAsString().equals(mod)){
                return false;
            }
        }
        return true;
    }
}
