/*
 * File created ~ 24 - 4 - 2021 ~ Leaf
 */

package leaf.cosmere.network.packets;

import leaf.cosmere.cap.entity.SpiritwebCapability;
import leaf.cosmere.constants.Manifestations;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ChangeManifestationModeMessage
{
    Manifestations.ManifestationTypes powerType;
    int powerID;
    int dir;

    public ChangeManifestationModeMessage(Manifestations.ManifestationTypes powerType, int powerID, int dir)
    {
        this.powerType = powerType;
        this.powerID = powerID;
        this.dir = dir;
    }

    public static void handle(ChangeManifestationModeMessage message, Supplier<NetworkEvent.Context> ctx)
    {
        NetworkEvent.Context context = ctx.get();
        ServerPlayerEntity sender = context.getSender();
        MinecraftServer server = sender.getServer();
        server.submitAsync(() -> SpiritwebCapability.get(sender).ifPresent((cap) ->
        {
            TextComponent manifestationText;
            int newMode;

            if (message.dir == 1)
            {
                newMode = cap.nextMode(message.powerType, message.powerID);
            }
            else if (message.dir == -1)
            {
                newMode = cap.previousMode(message.powerType, message.powerID);
            }
            else if (message.dir != 0)
            {
                newMode = message.dir + cap.getMode(message.powerType, message.powerID);
                cap.setMode(message.powerType, message.powerID, newMode);
            }

            cap.manifestation(message.powerType, message.powerID).onModeChange(cap);

            //manifestationText = TextHelper.createTranslatedText(Constants.Strings.POWER_MODE_SET, TextHelper.createText(newMode));
            //sender.sendMessage(manifestationText, Util.DUMMY_UUID);

            cap.syncToClients(null);
        }));
        context.setPacketHandled(true);
    }

    public static void encode(ChangeManifestationModeMessage mes, PacketBuffer buf)
    {
        buf.writeInt(mes.powerType.getID());
        buf.writeInt(mes.powerID);
        buf.writeInt(mes.dir);
    }

    public static ChangeManifestationModeMessage decode(PacketBuffer buf)
    {
        final Manifestations.ManifestationTypes manifestationType = Manifestations.ManifestationTypes.valueOf(buf.readInt()).get();
        final int powerID = buf.readInt();
        final int dir = buf.readInt();
        return new ChangeManifestationModeMessage(manifestationType, powerID, dir);
    }

}
