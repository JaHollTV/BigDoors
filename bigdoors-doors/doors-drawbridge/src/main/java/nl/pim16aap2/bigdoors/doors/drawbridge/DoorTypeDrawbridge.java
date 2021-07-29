package nl.pim16aap2.bigdoors.doors.drawbridge;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class DoorTypeDrawbridge extends DoorType
{
    private static final int TYPE_VERSION = 1;

    private static final @NotNull DoorTypeDrawbridge INSTANCE = new DoorTypeDrawbridge();

    private DoorTypeDrawbridge()
    {
        super(Constants.PLUGIN_NAME, "DrawBridge", TYPE_VERSION,
              Arrays.asList(RotateDirection.NORTH, RotateDirection.EAST,
                            RotateDirection.SOUTH, RotateDirection.WEST));
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player)
    {
        return new CreatorDrawbridge(player);
    }

    @Override
    public @NotNull Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        return new CreatorDrawbridge(player, name);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static @NotNull DoorTypeDrawbridge get()
    {
        return INSTANCE;
    }

    @Override
    public @NotNull Class<? extends AbstractDoor> getDoorClass()
    {
        return Drawbridge.class;
    }
}
