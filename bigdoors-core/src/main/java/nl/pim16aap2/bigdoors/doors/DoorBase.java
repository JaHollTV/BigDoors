package nl.pim16aap2.bigdoors.doors;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an unspecialized door.
 *
 * @author Pim
 */
@EqualsAndHashCode(callSuper = false)
public final class DoorBase extends DatabaseManager.FriendDoorAccessor implements IDoor
{
    @Getter
    private final long doorUID;

    @Getter
    private final IPWorld world;

    @Getter
    private Vector3Di engine;

    @Getter
    private Vector3Di powerBlock;

    @Getter
    @Setter
    private String name;

    private Cuboid cuboid;

    @Getter
    @Setter
    private boolean isOpen;

    @Getter
    @Setter
    private RotateDirection openDir;

    /**
     * Represents the locked status of this door. True = locked, False = unlocked.
     */
    @Getter
    @Setter
    private volatile boolean isLocked;

    @EqualsAndHashCode.Exclude
    private final Map<UUID, DoorOwner> doorOwners;

    @Getter
    private final DoorOwner primeOwner;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final IPLogger logger;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final ILocalizer localizer;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DoorRegistry doorRegistry;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DoorOpeningHelper doorOpeningHelper;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final AutoCloseScheduler autoCloseScheduler;

    @EqualsAndHashCode.Exclude
    private final IBigDoorsPlatform bigDoorsPlatform;

    @EqualsAndHashCode.Exclude
    private final DatabaseManager databaseManager;

    @EqualsAndHashCode.Exclude
    private final DoorActivityManager doorActivityManager;

    @EqualsAndHashCode.Exclude
    private final LimitsManager limitsManager;

    @EqualsAndHashCode.Exclude
    private final DoorToggleRequestFactory doorToggleRequestFactory;

    @EqualsAndHashCode.Exclude
    private final IPPlayerFactory playerFactory;

    @AssistedInject //
    DoorBase(@Assisted long doorUID, @Assisted String name, @Assisted Cuboid cuboid,
             @Assisted("engine") Vector3Di engine, @Assisted("powerBlock") Vector3Di powerBlock,
             @Assisted IPWorld world, @Assisted("isOpen") boolean isOpen, @Assisted("isLocked") boolean isLocked,
             @Assisted RotateDirection openDir, @Assisted DoorOwner primeOwner,
             @Assisted @Nullable Map<UUID, DoorOwner> doorOwners, IPLogger logger, ILocalizer localizer,
             DatabaseManager databaseManager, DoorRegistry doorRegistry, DoorActivityManager doorActivityManager,
             LimitsManager limitsManager, AutoCloseScheduler autoCloseScheduler, DoorOpeningHelper doorOpeningHelper,
             DoorToggleRequestFactory doorToggleRequestFactory, IPPlayerFactory playerFactory,
             IBigDoorsPlatform bigDoorsPlatform)
    {
        this.doorUID = doorUID;
        this.name = name;
        this.cuboid = cuboid;
        this.engine = engine;
        this.powerBlock = powerBlock;
        this.world = world;
        this.isOpen = isOpen;
        this.isLocked = isLocked;
        this.openDir = openDir;
        this.primeOwner = primeOwner;

        final int initSize = doorOwners == null ? 1 : doorOwners.size();
        final Map<UUID, DoorOwner> doorOwnersTmp = new ConcurrentHashMap<>(initSize);
        if (doorOwners == null)
            doorOwnersTmp.put(primeOwner.pPlayerData().getUUID(), primeOwner);
        else
            doorOwnersTmp.putAll(doorOwners);
        this.doorOwners = doorOwnersTmp;

        this.logger = logger;
        this.localizer = localizer;
        this.databaseManager = databaseManager;
        this.doorRegistry = doorRegistry;
        this.doorActivityManager = doorActivityManager;
        this.limitsManager = limitsManager;
        this.autoCloseScheduler = autoCloseScheduler;
        this.doorOpeningHelper = doorOpeningHelper;
        this.doorToggleRequestFactory = doorToggleRequestFactory;
        this.playerFactory = playerFactory;
        this.bigDoorsPlatform = bigDoorsPlatform;
    }

    // Copy constructor
    private DoorBase(DoorBase other, @Nullable Map<UUID, DoorOwner> doorOwners)
    {
        doorUID = other.doorUID;
        name = other.name;
        cuboid = other.cuboid;
        engine = other.engine;
        powerBlock = other.powerBlock;
        world = other.world;
        isOpen = other.isOpen;
        isLocked = other.isLocked;
        openDir = other.openDir;
        primeOwner = other.primeOwner;
        this.doorOwners = doorOwners == null ? new ConcurrentHashMap<>(0) : doorOwners;

        logger = other.logger;
        localizer = other.localizer;
        databaseManager = other.databaseManager;
        doorRegistry = other.doorRegistry;
        doorActivityManager = other.doorActivityManager;
        limitsManager = other.limitsManager;
        autoCloseScheduler = other.autoCloseScheduler;
        doorOpeningHelper = other.doorOpeningHelper;
        doorToggleRequestFactory = other.doorToggleRequestFactory;
        playerFactory = other.playerFactory;
        bigDoorsPlatform = other.bigDoorsPlatform;
    }

    /**
     * Gets a full copy of this {@link DoorBase}.
     * <p>
     * A full copy includes a full copy of {@link #doorOwners}. If this is not needed, consider using {@link
     * #getPartialSnapshot()} instead as it will be faster.
     *
     * @return A full copy of this {@link DoorBase}.
     */
    public synchronized DoorBase getFullSnapshot()
    {
        return new DoorBase(this, new HashMap<>(doorOwners));
    }

    /**
     * Gets a full copy of this {@link DoorBase}.
     * <p>
     * A partial copy does not include the {@link #doorOwners}. If these are needed, consider using {@link
     * #getFullSnapshot()} instead.
     *
     * @return A partial copy of this {@link DoorBase}.
     */
    public synchronized DoorBase getPartialSnapshot()
    {
        return new DoorBase(this, null);
    }

    /**
     * Synchronizes this {@link DoorBase} and the serialized type-specific data of an {@link AbstractDoor} with the
     * database.
     *
     * @param typeData
     *     The type-specific data of an {@link AbstractDoor}.
     * @return true if the synchronization was successful.
     */
    synchronized CompletableFuture<Boolean> syncData(byte[] typeData)
    {
        return databaseManager.syncDoorData(getPartialSnapshot(), typeData);
    }

    @Override
    protected void addOwner(UUID uuid, DoorOwner doorOwner)
    {
        if (doorOwner.permission() == 0)
        {
            logger.logThrowable(new IllegalArgumentException(
                "Failed to add owner: " + doorOwner.pPlayerData() + " as owner to door: " +
                    getDoorUID() +
                    " because a permission level of 0 is not allowed!"));
            return;
        }
        doorOwners.put(uuid, doorOwner);
    }

    /**
     * Checks if this door exceeds the size limit for the given player.
     * <p>
     * See {@link LimitsManager#exceedsLimit(IPPlayer, Limit, int)}.
     *
     * @param player
     *     The player whose limit to compare against this door's size.
     * @return True if {@link #getBlockCount()} exceeds the {@link Limit#DOOR_SIZE} for this door.
     */
    boolean exceedSizeLimit(IPPlayer player)
    {
        return limitsManager.exceedsLimit(player, Limit.DOOR_SIZE, getBlockCount());
    }

    void onRedstoneChange(AbstractDoor abstractDoor, int newCurrent)
    {
        final @Nullable DoorActionType doorActionType;
        if (newCurrent == 0 && isCloseable())
            doorActionType = DoorActionType.CLOSE;
        else if (newCurrent > 0 && isOpenable())
            doorActionType = DoorActionType.CLOSE;
        else
            doorActionType = null;

        if (doorActionType != null)
            doorToggleRequestFactory.builder()
                                    .door(abstractDoor)
                                    .doorActionCause(DoorActionCause.REDSTONE)
                                    .doorActionType(doorActionType)
                                    .messageReceiverServer()
                                    .responsible(playerFactory.create(getPrimeOwner().pPlayerData()))
                                    .build()
                                    .execute();
    }

    @Override
    protected boolean removeOwner(UUID uuid)
    {
        if (primeOwner.pPlayerData().getUUID().equals(uuid))
        {
            logger.logThrowable(new IllegalArgumentException(
                "Failed to remove owner: " + primeOwner.pPlayerData() + " as owner from door: " +
                    getDoorUID() + " because removing an owner with a permission level of 0 is not allowed!"));
            return false;
        }
        return doorOwners.remove(uuid) != null;
    }

    @Override
    public Cuboid getCuboid()
    {
        return cuboid;
    }

    @Override
    public boolean isOpenable()
    {
        return !isOpen;
    }

    @Override
    public boolean isCloseable()
    {
        return isOpen;
    }

    @Override
    public List<DoorOwner> getDoorOwners()
    {
        final List<DoorOwner> ret = new ArrayList<>(doorOwners.size());
        ret.addAll(doorOwners.values());
        return ret;
    }

    @Override
    public Optional<DoorOwner> getDoorOwner(IPPlayer player)
    {
        return getDoorOwner(player.getUUID());
    }

    @Override
    public Optional<DoorOwner> getDoorOwner(UUID uuid)
    {
        return Optional.ofNullable(doorOwners.get(uuid));
    }

    @Override
    public void setCoordinates(Cuboid newCuboid)
    {
        cuboid = newCuboid;
    }

    @Override
    public void setCoordinates(Vector3Di posA, Vector3Di posB)
    {
        cuboid = new Cuboid(posA, posB);
    }

    @Override
    public Vector3Di getMinimum()
    {
        return cuboid.getMin();
    }

    @Override
    public Vector3Di getMaximum()
    {
        return cuboid.getMax();
    }

    @Override
    public Vector3Di getDimensions()
    {
        return cuboid.getDimensions();
    }

    @Override
    public synchronized void setEngine(Vector3Di pos)
    {
        engine = pos;
    }

    @Override
    public void setPowerBlockPosition(Vector3Di pos)
    {
        powerBlock = pos;
    }

    @Override
    public int getBlockCount()
    {
        return cuboid.getVolume();
    }

    @Override
    public synchronized long getSimplePowerBlockChunkHash()
    {
        return Util.simpleChunkHashFromLocation(powerBlock.x(), powerBlock.z());
    }

    /**
     * Registers a {@link BlockMover} with the {@link DoorActivityManager}.
     * <p>
     * doorBase method MUST BE CALLED FROM THE MAIN THREAD! (Because of MC, spawning entities needs to happen
     * synchronously)
     *
     * @param abstractDoor
     *     The {@link AbstractDoor} to use.
     * @param cause
     *     What caused doorBase action.
     * @param time
     *     The amount of time doorBase {@link DoorBase} will try to use to move. The maximum speed is limited, so at a
     *     certain point lower values will not increase door speed.
     * @param skipAnimation
     *     If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the door will take up after the toggle.
     * @param responsible
     *     The {@link IPPlayer} responsible for the door action.
     * @param actionType
     *     The type of action that will be performed by the BlockMover.
     * @return True when everything went all right, otherwise false.
     */
    synchronized boolean registerBlockMover(AbstractDoor abstractDoor, DoorActionCause cause, double time,
                                            boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
                                            DoorActionType actionType)
    {
        if (!bigDoorsPlatform.isMainThread(Thread.currentThread().getId()))
        {
            doorActivityManager.setDoorAvailable(getDoorUID());
            logger.logThrowable(
                new IllegalThreadStateException("BlockMovers must be instantiated on the main thread!"));
            return true;
        }

        try
        {
            final BlockMover.Context context = new BlockMover.Context(doorActivityManager, autoCloseScheduler, logger);
            doorOpeningHelper.registerBlockMover(abstractDoor.constructBlockMover(context, cause, time, skipAnimation,
                                                                                  newCuboid, responsible, actionType));
        }
        catch (Exception e)
        {
            logger.logThrowable(e);
            return false;
        }
        return true;
    }

    /**
     * @return String with (almost) all data of this door.
     */
    @Override
    public synchronized String toString()
    {
        return doorUID + ": " + name + "\n"
            + formatLine("Cuboid", getCuboid())
            + formatLine("Engine", getEngine())
            + formatLine("PowerBlock position: ", getPowerBlock())
            + formatLine("PowerBlock Hash: ", getSimplePowerBlockChunkHash())
            + formatLine("World", getWorld())
            + "This door is " + (isLocked ? "" : "NOT ") + "locked.\n"
            + "This door is " + (isOpen ? "open.\n" : "closed.\n")
            + formatLine("OpenDir", openDir.name());
    }

    private String formatLine(String name, @Nullable Object obj)
    {
        final String objString = obj == null ? "NULL" : obj.toString();
        return name + ": " + objString + "\n";
    }

    @AssistedFactory
    interface Factory
    {
        DoorBase create(long doorUID, String name, Cuboid cuboid, @Assisted("engine") Vector3Di engine,
                        @Assisted("powerBlock") Vector3Di powerBlock, @Assisted IPWorld world,
                        @Assisted("isOpen") boolean isOpen, @Assisted("isLocked") boolean isLocked,
                        RotateDirection openDir, DoorOwner primeOwner, @Nullable Map<UUID, DoorOwner> doorOwners);
    }
}
