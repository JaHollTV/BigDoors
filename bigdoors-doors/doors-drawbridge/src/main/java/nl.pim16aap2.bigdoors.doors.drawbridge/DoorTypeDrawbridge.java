package nl.pim16aap2.bigdoors.doors.drawbridge;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.Pair;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DoorTypeDrawbridge extends DoorType
{
    private static final int TYPE_VERSION = 1;
    @NotNull
    private static final List<Parameter> PARAMETERS;

    static
    {
        final @NotNull List<Parameter> parameterTMP = new ArrayList<>(3);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "modeUpDown"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    private static final @NotNull List<Pair<String, Pair<Integer, Integer>>> dependencies = Collections.emptyList();

    @NotNull
    private static final DoorTypeDrawbridge INSTANCE = new DoorTypeDrawbridge();

    private DoorTypeDrawbridge()
    {
        super(Constants.PLUGINNAME, "DrawBridge", TYPE_VERSION, PARAMETERS,
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
    public List<Pair<String, Pair<Integer, Integer>>> getDependencies()
    {
        return dependencies;
    }

    @Override
    protected @NotNull Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                              final @NotNull Object... typeData)
    {
        final @Nullable PBlockFace currentDirection = PBlockFace.valueOf((int) typeData[2]);
        if (currentDirection == null)
            return Optional.empty();

        final int autoCloseTimer = (int) typeData[0];
        final int autoOpenTimer = (int) typeData[1];

        final boolean modeUP = ((int) typeData[2]) == 1;
        return Optional.of(new Drawbridge(doorData,
                                          autoCloseTimer,
                                          autoOpenTimer,
                                          modeUP));
    }

    @Override
    protected @NotNull Object[] generateTypeData(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof Drawbridge))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Drawbridge from type: " + door.getDoorType().toString());

        final @NotNull Drawbridge drawbridge = (Drawbridge) door;
        return new Object[]{drawbridge.getAutoCloseTime(),
                            drawbridge.getAutoOpenTime(),
                            drawbridge.isModeUp() ? 1 : 0};
    }
}
