package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the setName command, which is used to provide a name for a {@link ToolUser}.
 *
 * @author Pim
 */
@ToString
public class SetName extends BaseCommand
{
    private final String name;
    private final ToolUserManager toolUserManager;

    @AssistedInject //
    SetName(@Assisted ICommandSender commandSender, ILocalizer localizer, @Assisted String name,
            ToolUserManager toolUserManager)
    {
        super(commandSender, localizer);
        this.name = name;
        this.toolUserManager = toolUserManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.SET_NAME;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(PermissionsStatus permissions)
    {
        final IPPlayer player = (IPPlayer) getCommandSender();
        final Optional<ToolUser> tu = toolUserManager.getToolUser(player.getUUID());
        if (tu.isPresent() && tu.get() instanceof Creator)
            return CompletableFuture.completedFuture(tu.get().handleInput(name));

        getCommandSender().sendMessage(localizer.getMessage("commands.base.error.no_pending_process"));
        return CompletableFuture.completedFuture(true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetName} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for providing the name.
         * @param name
         *     The new name specified by the command sender.
         * @return See {@link BaseCommand#run()}.
         */
        SetName newSetName(ICommandSender commandSender, String name);
    }
}
