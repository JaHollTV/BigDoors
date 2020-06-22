package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

/**
 * Represents all {@link EDoorType}s that are aligned on the North/South or East/West axis. For example: {@link
 * SlidingDoor}.
 * <p>
 * Only doors with a depth of 1 block can be extended.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public interface IHorizontalAxisAlignedDoorArchetype extends IDoorBase
{
    /**
     * Checks if the {@link AbstractDoorBase} is aligned with the z-axis (North/South).
     */
    boolean isNorthSouthAligned();

    /** {@inheritDoc} */
    @NotNull
    @Override
    default RotateDirection cycleOpenDirection()
    {
        if (isNorthSouthAligned())
            return getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.WEST : RotateDirection.EAST;
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.SOUTH : RotateDirection.NORTH;
    }
}
