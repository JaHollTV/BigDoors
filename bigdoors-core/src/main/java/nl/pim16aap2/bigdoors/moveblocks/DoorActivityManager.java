package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.util.Constants;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Keeps track of which doors are currently active.
 *
 * @author Pim
 */
@Singleton
public final class DoorActivityManager extends Restartable
{
    private final Map<Long, Optional<BlockMover>> busyDoors = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@link DoorActivityManager}.
     *
     * @param holder
     *     The {@link IRestartableHolder} that manages this object.
     */
    public DoorActivityManager(IRestartableHolder holder)
    {
        super(holder);
    }

    /**
     * Checks if a {@link DoorBase} is 'busy', i.e. currently being animated.
     *
     * @param doorUID
     *     The UID of the {@link DoorBase}.
     * @return True if the {@link DoorBase} is busy.
     */
    @SuppressWarnings("unused")
    public boolean isDoorBusy(long doorUID)
    {
        return busyDoors.containsKey(doorUID);
    }

    /**
     * Attempts to register a door (as described by its UID) as busy. If the door was not previously registered as busy,
     * it will be registered now and this method will return <code>true</code>. If it was already registered as busy, it
     * will not touch it and return <code>false</code>.
     *
     * @param doorUID
     *     The UID of the door to register.
     * @return True if the door was not registered before (but is now), otherwise false.
     */
    // The busyDoors map stores the values as optional and here we just want to know if a value exists for the key.
    // If it doesn't, the value will be null, but both IntelliJ and SonarLint will complain about comparing an
    // optional to null. Because that is _exactly_ what we want to do here, we ignore the warnings.
    @SuppressWarnings({"OptionalAssignedToNull", "squid:S2789"})
    public boolean attemptRegisterAsBusy(long doorUID)
    {
        return busyDoors.putIfAbsent(doorUID, Optional.empty()) == null;
    }

    /**
     * Register a door as available.
     *
     * @param doorUID
     *     The UID of the door.
     */
    public void setDoorAvailable(long doorUID)
    {
        busyDoors.remove(doorUID);
    }

    /**
     * Processed a finished {@link BlockMover}.
     * <p>
     * The {@link DoorBase} that was being used by the {@link BlockMover} will be registered as inactive and any
     * scheduling that is required will be performed.
     *
     * @param blockMover
     *     The {@link BlockMover} to postprocess.
     * @param allowReschedule
     *     Whether to allow rescheduling (e.g. autoClose).
     */
    void processFinishedBlockMover(BlockMover blockMover, boolean allowReschedule)
    {
        final int delay = Math.max(Constants.MINIMUM_DOOR_DELAY,
                                   BigDoors.get().getPlatform().getConfigLoader().coolDown() * 20);

        BigDoors.get().getPlatform().getPExecutor()
                .runSyncLater(() -> handleFinishedBlockMover(blockMover, allowReschedule), delay);
    }

    private void handleFinishedBlockMover(BlockMover blockMover, boolean allowReschedule)
    {
        setDoorAvailable(blockMover.getDoor().getDoorUID());

        if (!allowReschedule)
            return;

        BigDoors.get().getPlatform().callDoorEvent(
            BigDoors.get().getPlatform().getBigDoorsEventFactory()
                    .createToggleEndEvent(blockMover.getDoor(), blockMover.getCause(), blockMover.getActionType(),
                                          blockMover.getPlayer(), blockMover.getTime(),
                                          blockMover.isSkipAnimation()));

        if (blockMover.getDoor() instanceof ITimerToggleable)
            BigDoors.get().getAutoCloseScheduler()
                    .scheduleAutoClose(blockMover.getPlayer(),
                                       (AbstractDoor & ITimerToggleable) blockMover.getDoor(),
                                       blockMover.getTime(), blockMover.isSkipAnimation());
    }

    /**
     * Stores a {@link BlockMover} in the appropriate slot in {@link #busyDoors}
     *
     * @param mover
     *     The {@link BlockMover}.
     */
    public void addBlockMover(BlockMover mover)
    {
        busyDoors.replace(mover.getDoorUID(), Optional.of(mover));
    }

    /**
     * Gets all the currently active {@link BlockMover}s.
     *
     * @return All the currently active {@link BlockMover}s.
     */
    @SuppressWarnings("unused")
    public Stream<BlockMover> getBlockMovers()
    {
        return busyDoors.values().stream().filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Gets the {@link BlockMover} of a busy {@link DoorBase}, if it has been registered.
     *
     * @param doorUID
     *     The UID of the {@link DoorBase}.
     * @return The {@link BlockMover} of a busy {@link DoorBase}.
     */
    public Optional<BlockMover> getBlockMover(long doorUID)
    {
        return busyDoors.containsKey(doorUID) ? busyDoors.get(doorUID) : Optional.empty();
    }

    /**
     * Clears all busy doors.
     */
    private void emptyBusyDoors()
    {
        busyDoors.clear();
    }

    /**
     * Stops all doors that are currently active.
     */
    public void stopDoors()
    {
        busyDoors.forEach((key, value) -> value.ifPresent(BlockMover::abort));
        emptyBusyDoors();
    }

    @Override
    public void restart()
    {
        busyDoors.forEach((key, value) -> value.ifPresent(BlockMover::restart));
        busyDoors.clear();
    }

    @Override
    public void shutdown()
    {
        busyDoors.forEach((key, value) -> value.ifPresent(BlockMover::shutdown));
        busyDoors.clear();
    }
}
