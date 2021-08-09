package nl.pim16aap2.bigdoors.api.restartable;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a basic implementation of {@link IRestartableHolder}.
 *
 * @author Pim
 */
public class RestartableHolder implements IRestartableHolder
{
    protected final @NotNull Set<@NotNull IRestartable> restartables = new LinkedHashSet<>();

    @Override
    public void registerRestartable(final @NotNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(final @NotNull IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(final @NotNull IRestartable restartable)
    {
        restartables.remove(restartable);
    }
}
