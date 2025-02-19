package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorBaseBuilder;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.tooluser.Procedure;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorBoolean;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorString;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorVoid;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.logging.Level;

/**
 * Represents a specialization of the {@link ToolUser} that is used for creating new {@link AbstractDoor}s.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@Flogger
public abstract class Creator extends ToolUser
{
    protected final LimitsManager limitsManager;

    protected final DoorBaseBuilder doorBaseBuilder;

    protected final DatabaseManager databaseManager;

    protected final IEconomyManager economyManager;

    /**
     * The name of the door that is to be created.
     */
    protected @Nullable String name;

    /**
     * The cuboid that defines the location and dimensions of the door.
     * <p>
     * This region is defined by {@link #firstPos} and the second position selected by the user.
     */
    protected @Nullable Cuboid cuboid;

    /**
     * The first point that was selected in the process.
     * <p>
     * Once a second point has been selected, these two are used to construct the {@link #cuboid}.
     */
    protected @Nullable Vector3Di firstPos;

    /**
     * The position of the rotation point selected by the user.
     */
    protected @Nullable Vector3Di rotationPoint;

    /**
     * The powerblock selected by the user.
     */
    protected @Nullable Vector3Di powerblock;

    /**
     * The opening direction selected by the user.
     */
    protected @Nullable RotateDirection openDir;

    /**
     * The {@link IPWorld} this door is created in.
     */
    protected @Nullable IPWorld world;

    /**
     * Whether the door is created in the open (true) or closed (false) position.
     */
    protected boolean isOpen = false;

    /**
     * Whether the door is created in the locked (true) or unlocked (false) state.
     */
    protected boolean isLocked = false;

    /**
     * IFactory for the {@link IStep} that sets the name.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factorySetName;

    /**
     * IFactory for the {@link IStep} that sets the first position of the area of the door.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factorySetFirstPos;

    /**
     * IFactory for the {@link IStep} that sets the second position of the area of the door, thus completing the
     * {@link Cuboid}.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factorySetSecondPos;

    /**
     * IFactory for the {@link IStep} that sets the position of the door's rotation point.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factorySetRotationPointPos;

    /**
     * IFactory for the {@link IStep} that sets the position of the door's power block.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factorySetPowerBlockPos;

    /**
     * IFactory for the {@link IStep} that sets the open direction of the door.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factorySetOpenDir;

    /**
     * IFactory for the {@link IStep} that allows the player to confirm or reject the price of the door.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factoryConfirmPrice;

    /**
     * IFactory for the {@link IStep} that completes this process.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factoryCompleteProcess;

    private static final MyDecimalFormat DECIMAL_FORMAT = new MyDecimalFormat();

    protected Creator(Context context, IPPlayer player, @Nullable String name)
    {
        super(context, player);
        limitsManager = context.getLimitsManager();
        doorBaseBuilder = context.getDoorBaseBuilder();
        databaseManager = context.getDatabaseManager();
        economyManager = context.getEconomyManager();

        player.sendMessage(localizer.getMessage("creator.base.init"));

        if (name != null)
            handleInput(name);
        prepareCurrentStep();
    }

    @Override
    protected void init()
    {
        factorySetName =
            new Step.Factory(localizer, "SET_NAME")
                .stepExecutor(new StepExecutorString(this::completeNamingStep))
                .messageKey("creator.base.give_name")
                .messageVariableRetriever(getDoorType()::getLocalizationKey);

        factorySetFirstPos =
            new Step.Factory(localizer, "SET_FIRST_POST")
                .stepExecutor(new StepExecutorPLocation(this::setFirstPos));

        factorySetSecondPos =
            new Step.Factory(localizer, "SET_SECOND_POS")
                .stepExecutor(new StepExecutorPLocation(this::setSecondPos));

        factorySetRotationPointPos =
            new Step.Factory(localizer, "SET_ROTATION_POINT")
                .stepExecutor(new StepExecutorPLocation(this::completeSetRotationPointStep));

        factorySetPowerBlockPos =
            new Step.Factory(localizer, "SET_POWER_BLOCK_POS")
                .messageKey("creator.base.set_power_block")
                .stepExecutor(new StepExecutorPLocation(this::completeSetPowerBlockStep));

        factorySetOpenDir =
            new Step.Factory(localizer, "SET_OPEN_DIRECTION")
                .stepExecutor(new StepExecutorString(this::completeSetOpenDirStep))
                .messageKey("creator.base.set_open_dir")
                .messageVariableRetrievers(Collections.singletonList(this::getOpenDirections));

        factoryConfirmPrice =
            new Step.Factory(localizer, "CONFIRM_DOOR_PRICE")
                .stepExecutor(new StepExecutorBoolean(this::confirmPrice))
                .skipCondition(this::skipConfirmPrice)
                .messageKey("creator.base.confirm_door_price")
                .messageVariableRetrievers(Collections.singletonList(() -> String.format("%.2f", getPrice().orElse(0))))
                .implicitNextStep(false);

        factoryCompleteProcess =
            new Step.Factory(localizer, "COMPLETE_CREATION_PROCESS")
                .stepExecutor(new StepExecutorVoid(this::completeCreationProcess))
                .waitForUserInput(false);
    }

    /**
     * Constructs the {@link DoorBase} for the current door. This is the same for all doors.
     *
     * @return The {@link DoorBase} for the current door.
     */
    protected final DoorBase constructDoorData()
    {
        final long doorUID = -1;
        final var owner = new DoorOwner(doorUID, 0, getPlayer().getPPlayerData());

        return doorBaseBuilder
            .builder()
            .uid(doorUID)
            .name(Util.requireNonNull(name, "Name"))
            .cuboid(Util.requireNonNull(cuboid, "cuboid"))
            .rotationPoint(Util.requireNonNull(rotationPoint, "rotationPoint"))
            .powerBlock(Util.requireNonNull(powerblock, "powerblock"))
            .world(Util.requireNonNull(world, "world"))
            .isOpen(isOpen)
            .isLocked(isLocked)
            .openDir(Util.requireNonNull(openDir, "openDir"))
            .primeOwner(owner)
            .build();
    }

    /**
     * Completes the creation process. It'll construct and insert the door and complete the {@link ToolUser} process.
     *
     * @return True, so that it fits the functional interface being used for the steps.
     * <p>
     * If the insertion fails for whatever reason, it'll just be ignored, because at that point, there's no sense in
     * continuing the creation process anyway.
     */
    protected boolean completeCreationProcess()
    {
        if (active)
            insertDoor(constructDoor());
        return true;
    }

    /**
     * Method used to give the BigDoors tool to the user.
     * <p>
     * Overriding methods may call {@link #giveTool(String, String, String)}.
     */
    protected abstract void giveTool();

    /**
     * Completes the naming step for this {@link Creator}. This means that it'll set the name, go to the next step, and
     * give the user the creator tool.
     * <p>
     * Note that there are some requirements that the name must meet. See {@link Util#isValidDoorName(String)}.
     *
     * @param str
     *     The desired name of the door.
     * @return True if the naming step was finished successfully.
     */
    protected boolean completeNamingStep(String str)
    {
        if (!Util.isValidDoorName(str))
        {
            log.at(Level.FINE).log("Invalid name '%s' for selected Creator: %s", str, this);
            getPlayer().sendMessage(localizer.getMessage("creator.base.error.invalid_name", str));
            return false;
        }

        name = str;
        giveTool();
        return true;
    }

    /**
     * Sets the first location of the selection and advances the procedure if successful.
     *
     * @param loc
     *     The first location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected boolean setFirstPos(IPLocation loc)
    {
        if (!playerHasAccessToLocation(loc))
            return false;

        world = loc.getWorld();
        firstPos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return true;
    }

    /**
     * Sets the second location of the selection and advances the procedure if successful.
     *
     * @param loc
     *     The second location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected boolean setSecondPos(IPLocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        final Cuboid newCuboid = new Cuboid(Util.requireNonNull(firstPos, "firstPos"),
                                            new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

        final OptionalInt sizeLimit = limitsManager.getLimit(getPlayer(), Limit.DOOR_SIZE);
        if (sizeLimit.isPresent() && newCuboid.getVolume() > sizeLimit.getAsInt())
        {
            getPlayer().sendMessage(localizer.getMessage("creator.base.error.area_too_big",
                                                         Integer.toString(newCuboid.getVolume()),
                                                         Integer.toString(sizeLimit.getAsInt())));
            return false;
        }

        cuboid = newCuboid;

        return playerHasAccessToCuboid(cuboid, Util.requireNonNull(world, "world"));
    }

    /**
     * Attempts to buy the door for the player and advances the procedure if successful.
     * <p>
     * Note that if the player does not end up buying the door, either because of insufficient funds or because they
     * rejected the offer, the current step is NOT advanced!
     *
     * @param confirm
     *     Whether the player confirmed they want to buy this door.
     * @return Always returns true, because either they can and do buy the door, or they cannot or refuse to buy the
     * door and the process is aborted.
     */
    // This method always returns the same value (S3516). However, in the case of this method, there is no reason to
    // return false as every input is valid and leads to a valid state.
    @SuppressWarnings("squid:S3516")
    protected boolean confirmPrice(boolean confirm)
    {
        if (!confirm)
        {
            getPlayer().sendMessage(localizer.getMessage("creator.base.error.creation_cancelled"));
            abort();
            return true;
        }
        if (!buyDoor())
        {

            getPlayer().sendMessage(localizer.getMessage("creator.base.error.insufficient_funds",
                                                         DECIMAL_FORMAT.format(getPrice().orElse(0))));
            abort();
            return true;
        }

        getProcedure().goToNextStep();
        return true;
    }

    /**
     * Parses the selected open direction from a String.
     * <p>
     * If the String is an integer value, it will try to get the {@link RotateDirection} at the corresponding index in
     * the list of valid open directions as obtained from {@link DoorType#getValidOpenDirections()}.
     * <p>
     * If the String is not an integer value, it will try to match it to the name of a {@link RotateDirection}. Note
     * that it has to be an exact match.
     *
     * @param str
     *     The name or index of the selected open direction.
     * @return The selected {@link RotateDirection}, if it exists.
     */
    // TODO: Do not match against the enum names of RotateDirection, but against localized RotateDirection names.
    protected Optional<RotateDirection> parseOpenDirection(String str)
    {
        final String openDirName = str.toUpperCase(Locale.ENGLISH);
        final OptionalInt idOpt = Util.parseInt(str);

        final List<RotateDirection> validOpenDirs = getValidOpenDirections();

        if (idOpt.isPresent())
        {
            final int id = idOpt.getAsInt();
            if (id < 0 || id >= validOpenDirs.size())
            {
                log.at(Level.FINE).log("Player %s selected ID: %d out of %d options.",
                                       getPlayer(), id, validOpenDirs.size());
                return Optional.empty();
            }

            return Optional.of(validOpenDirs.get(id));
        }

        return RotateDirection.getRotateDirection(openDirName).flatMap(
            foundOpenDir -> validOpenDirs.contains(foundOpenDir) ?
                            Optional.of(foundOpenDir) : Optional.empty());
    }

    /**
     * Attempts to complete the step that sets the {@link #openDir}. It uses the open direction as parsed from a String
     * using {@link #parseOpenDirection(String)} if possible.
     * <p>
     * If no valid open direction for this type can be found, nothing changes.
     *
     * @param str
     *     The name or index of the {@link RotateDirection} that was selected by the player.
     * @return True if the {@link #openDir} was set successfully.
     */
    protected boolean completeSetOpenDirStep(String str)
    {
        return parseOpenDirection(str).map(
            foundOpenDir ->
            {
                openDir = foundOpenDir;
                return true;
            }).orElseGet(
            () ->
            {
                getPlayer().sendMessage(localizer.getMessage("creator.base.error.invalid_option", str));
                return false;
            });
    }

    /**
     * Constructs the door at the end of the creation process.
     *
     * @return The newly-created door.
     */
    protected abstract AbstractDoor constructDoor();

    /**
     * Verifies that the world of the selected location matches the world that this door is being created in.
     *
     * @param targetWorld
     *     The world to check.
     * @return True if the world is the same world this door is being created in.
     */
    protected boolean verifyWorldMatch(IPWorld targetWorld)
    {
        if (Util.requireNonNull(world, "world").worldName().equals(targetWorld.worldName()))
            return true;
        log.at(Level.FINE).log("World mismatch in ToolUser for player: %s", getPlayer());
        return false;
    }

    /**
     * Takes care of inserting the door.
     *
     * @param door
     *     The door to send to the {@link DatabaseManager}.
     */
    @SneakyThrows
    protected void insertDoor(AbstractDoor door)
    {
        databaseManager.addDoor(door, getPlayer()).whenComplete(
            (result, throwable) ->
            {
                if (!result.cancelled())
                {
                    getPlayer().sendMessage(localizer.getMessage("creator.base.error.creation_cancelled"));
                    return;
                }

                if (result.door().isEmpty())
                {
                    getPlayer().sendMessage(localizer.getMessage("constants.error.generic"));
                    log.at(Level.SEVERE).log("Failed to insert door after creation!");
                }
            }).exceptionally(Util::exceptionally);
    }

    /**
     * Obtains the type of door this creator will create.
     *
     * @return The type of door that will be created.
     */
    protected abstract DoorType getDoorType();

    /**
     * Attempts to buy the door for the current player.
     *
     * @return True if the player has bought the door or if the economy is not enabled.
     */
    protected boolean buyDoor()
    {
        if (!economyManager.isEconomyEnabled())
            return true;

        return economyManager.buyDoor(getPlayer(), Util.requireNonNull(world, "world"), getDoorType(),
                                      Util.requireNonNull(cuboid, "cuboid").getVolume());
    }

    /**
     * Gets the price of the door based on its volume. If the door is free because the price is <= 0 or the
     * {@link nl.pim16aap2.bigdoors.api.IEconomyManager} is disabled, the price will be empty.
     *
     * @return The price of the door if a positive price could be found.
     */
    protected OptionalDouble getPrice()
    {
        if (!economyManager.isEconomyEnabled())
            return OptionalDouble.empty();
        return economyManager.getPrice(getDoorType(), Util.requireNonNull(cuboid, "cuboid").getVolume());
    }

    /**
     * Checks if the step that asks the user to confirm that they want to buy the door should be skipped.
     * <p>
     * It should be skipped if the door is free for whatever reason. See {@link #getPrice()}.
     *
     * @return True if the step that asks the user to confirm that they want to buy the door should be skipped.
     */
    protected boolean skipConfirmPrice()
    {

        return getPrice().isEmpty();
    }

    /**
     * Gets the list of available open directions for the {@link DoorType} that is being created in the following
     * format:
     * <p>
     * "idx: RotateDirection\n"
     *
     * @return The list of valid open directions for this type, each on their own line.
     */
    protected String getOpenDirections()
    {
        final var sb = new StringBuilder();
        int idx = 0;
        for (final RotateDirection rotateDirection : getValidOpenDirections())
            sb.append(idx++).append(": ")
              .append(localizer.getMessage(rotateDirection.getLocalizationKey())).append('\n');
        return sb.toString();
    }

    /**
     * Gets the list of valid open directions for this type. It returns a subset of
     * {@link DoorType#getValidOpenDirections()} based on the current physical aspects of the {@link AbstractDoor}.
     *
     * @return The list of valid open directions for this type given its current physical dimensions.
     */
    protected List<RotateDirection> getValidOpenDirections()
    {
        return getDoorType().getValidOpenDirections();
    }

    /**
     * Attempts to complete the step in the {@link Procedure} that sets the second position of the {@link AbstractDoor}
     * that is being created.
     *
     * @param loc
     *     The selected location of the rotation point.
     * @return True if the location of the area was set successfully.
     */
    protected boolean completeSetPowerBlockStep(IPLocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        final Vector3Di pos = loc.getPosition();
        if (Util.requireNonNull(cuboid, "cuboid").isPosInsideCuboid(pos))
        {
            getPlayer().sendMessage(localizer
                                        .getMessage("creator.base.error.powerblock_inside_door"));
            return false;
        }
        final OptionalInt distanceLimit = limitsManager.getLimit(getPlayer(), Limit.POWERBLOCK_DISTANCE);
        final double distance;
        if (distanceLimit.isPresent() &&
            (distance = cuboid.getCenter().getDistance(pos)) > distanceLimit.getAsInt())
        {
            getPlayer().sendMessage(localizer
                                        .getMessage("creator.base.error.powerblock_too_far",
                                                    DECIMAL_FORMAT.format(distance),
                                                    Integer.toString(distanceLimit.getAsInt())));
            return false;
        }

        powerblock = pos;

        removeTool();
        return true;
    }

    /**
     * Attempts to complete the step in the {@link Procedure} that sets the location of the rotation point for the
     * {@link AbstractDoor} that is being created.
     *
     * @param loc
     *     The selected location of the rotation point.
     * @return True if the location of the rotation point was set successfully.
     */
    protected boolean completeSetRotationPointStep(IPLocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        if (!Util.requireNonNull(cuboid, "cuboid").isInRange(loc, 1))
        {
            getPlayer().sendMessage(localizer
                                        .getMessage("creator.base.error.invalid_rotation_point"));
            return false;
        }

        rotationPoint = loc.getPosition();
        return true;
    }

    /**
     * Represents a synchronized wrapper for {@link DecimalFormat}.
     *
     * @author Pim
     */
    private static final class MyDecimalFormat
    {
        private final DecimalFormat decimalFormat;

        MyDecimalFormat()
        {
            decimalFormat = new DecimalFormat("0", DecimalFormatSymbols.getInstance());
            decimalFormat.setMaximumFractionDigits(2);
        }

        /**
         * Formats a double number as a String.
         *
         * @param number
         *     The double number to format
         * @return The String representation of the provided double value.
         *
         * @throws ArithmeticException
         *     If rounding is needed with rounding mode being set to RoundingMode.UNNECESSARY.
         */
        public synchronized String format(double number)
        {
            return decimalFormat.format(number);
        }
    }
}
