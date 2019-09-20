package nl.pim16aap2.bigdoors.listeners;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static nl.pim16aap2.bigdoors.util.Constants.DEVBUILD;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them any messages if needed.
 *
 * @author Pim
 */
public class LoginMessageListener implements Listener
{
    BigDoorsSpigot plugin;

    public LoginMessageListener(BigDoorsSpigot plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Listens to {@link Player}s logging in and sends them the login message.
     *
     * @param event The {@link PlayerJoinEvent}.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        try
        {
            Player player = event.getPlayer();
            // Normally, only send to those with permission, so they can disable it.
            // But when it's a devbuild, also send it to everyone who's OP, to make it
            // a bit harder to get around the message.
            if (player.hasPermission("bigdoors.admin") || player.isOp() && DEVBUILD)
                // Slight delay so the player actually receives the message;
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        String loginString = plugin.getLoginMessage();
                        if (loginString != null && !loginString.isEmpty())
                            player.sendMessage(ChatColor.AQUA + plugin.getLoginMessage());
                    }
                }.runTaskLater(plugin, 60);
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }
}
