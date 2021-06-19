package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPWorld;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a factory for {@link IPWorld} objects.
 *
 * @author Pim
 */
public interface IPWorldFactory
{
    /**
     * Creates a new IPWorld.
     *
     * @param worldName The name of the world.
     * @return A new IPWorld object.
     */
    @NotNull IPWorld create(@NotNull String worldName);
}
