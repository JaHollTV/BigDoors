package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Represents a Clock doorType.
 *
 * @author pim
 * @see HorizontalAxisAlignedBase
 */
public class Clock extends HorizontalAxisAlignedBase
{
    Clock(final PLogger pLogger, final long doorUID, DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    Clock(final PLogger pLogger, final long doorUID)
    {
        this(pLogger, doorUID, DoorType.CLOCK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D[] calculateChunkRange()
    {
        Chunk minChunk = min.getChunk();
        Chunk maxChunk = max.getChunk();

        return new Vector2D[]{new Vector2D(minChunk.getX(), minChunk.getZ()),
                              new Vector2D(maxChunk.getX(), maxChunk.getZ())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        switch (openDir)
        {
            case NORTH:
                return PBlockFace.NORTH;
            case EAST:
                return PBlockFace.EAST;
            case SOUTH:
                return PBlockFace.SOUTH;
            case WEST:
                return PBlockFace.WEST;
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            openDir = RotateDirection.NORTH;
        else
            openDir = RotateDirection.EAST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNewLocations(PBlockFace openDirection, RotateDirection rotateDirection, @NotNull Location newMin,
                                @NotNull Location newMax, int blocksMoved, @Nullable Mutable<PBlockFace> newEngineSide)
    {
        newMin.setX(min.getBlockX());
        newMin.setY(min.getBlockY());
        newMin.setZ(min.getBlockZ());

        newMax.setX(max.getBlockX());
        newMax.setY(max.getBlockY());
        newMax.setZ(max.getBlockZ());
    }
}
