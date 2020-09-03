package nl.pim16aap2.bigdoors.managers;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class manages all {@link DoorType}s. Before a type can be used, it will have to be registered here.
 *
 * @author Pim
 */
public final class DoorTypeManager
{
    @NotNull
    private static final DoorTypeManager instance = new DoorTypeManager();
    @NotNull
    private final Map<DoorType, DoorTypeInfo> doorTypeToID = new ConcurrentHashMap<>();
    @NotNull
    private final Map<Long, DoorType> doorTypeFromID = new ConcurrentHashMap<>();
    @NotNull
    private final Map<String, DoorType> doorTypeFromName = new ConcurrentHashMap<>();

    /**
     * Gets all registered AND enabled {@link DoorType}s.
     */
    @Getter(onMethod = @__({@NotNull}))
    private final List<DoorType> sortedDoorTypes = new CopyOnWriteArrayList<DoorType>()
    {
        @Override
        public boolean add(DoorType doorType)
        {
            super.add(doorType);
            sortedDoorTypes.sort(Comparator.comparing(DoorType::getSimpleName));
            return true;
        }
    };

    private DoorTypeManager()
    {
    }

    /**
     * Gets all {@link DoorType}s that are currently registered.
     *
     * @return All {@link DoorType}s that are currently registered.
     */
    public @NotNull Set<DoorType> getRegisteredDoorTypes()
    {
        return Collections.unmodifiableSet(doorTypeToID.keySet());
    }

    /**
     * Gets all {@link DoorType}s that are currently enabled.
     *
     * @return All {@link DoorType}s that are currently enabled.
     */
    public @NotNull List<DoorType> getEnabledDoorTypes()
    {
        final List<DoorType> enabledDoorTypes = new ArrayList<>();
        for (final Map.Entry<DoorType, DoorTypeInfo> doorType : doorTypeToID.entrySet())
            if (doorType.getValue().status)
                enabledDoorTypes.add(doorType.getKey());
        return enabledDoorTypes;
    }

    /**
     * Obtain the instance of this class.
     *
     * @return The instance of this class.
     */
    public static @NotNull DoorTypeManager get()
    {
        return instance;
    }

    /**
     * Checks if an {@link DoorType} is enabled.
     *
     * @param doorType The {@link DoorType} to check.
     * @return True if the {@link DoorType} is enabled, otherwise false.
     */
    public boolean isRegistered(final @NotNull DoorType doorType)
    {
        return doorTypeToID.containsKey(doorType);
    }

    /**
     * Checks if an {@link DoorType} is enabled.
     *
     * @param doorTypeID The ID of the {@link DoorType} to check.
     * @return True if the {@link DoorType} is enabled, otherwise false.
     */
    public boolean isRegistered(final long doorTypeID)
    {
        return doorTypeFromID.containsKey(doorTypeID);
    }

    /**
     * Obtains the class of an {@link DoorType} as described by its ID.
     *
     * @param doorTypeID The ID of the {@link DoorType}.
     * @return An optional that contains the class of the {@link DoorType} if it is registered.
     */
    public @NotNull Optional<DoorType> getDoorType(final long doorTypeID)
    {
        return Optional.ofNullable(doorTypeFromID.get(doorTypeID));
    }

    /**
     * Tries to get a {@link DoorType} from it name as defined by {@link DoorType#getSimpleName()}. This method is
     * case-insensitive.
     *
     * @param typeName The name of the type.
     * @return The {@link DoorType} to retrieve, if possible.
     */
    public @NotNull Optional<DoorType> getDoorType(final @NotNull String typeName)
    {
        return Optional.ofNullable(doorTypeFromName.get(typeName.toLowerCase()));
    }

    /**
     * Obtains the ID of a {@link DoorType}.
     *
     * @param doorType The {@link Class} of the {@link DoorType}.
     * @return An optional that contains the ID of the {@link DoorType} if it is registered.
     */
    public @NotNull OptionalLong getDoorTypeID(final @NotNull DoorType doorType)
    {
        final @Nullable DoorTypeInfo info = doorTypeToID.get(doorType);
        return info == null ? OptionalLong.empty() : OptionalLong.of(info.id);
    }

    /**
     * Checks if a {@link DoorType} is enabled or not. Disabled {@link DoorType}s cannot be toggled or created.
     * <p>
     * {@link DoorType}s that are not registered, are disabled by definition.
     *
     * @param doorType The {@link DoorType} to check.
     * @return True if this {@link DoorType} is both registered and enabled.
     */
    public boolean isDoorTypeEnabled(final @NotNull DoorType doorType)
    {
        final @Nullable DoorTypeInfo info = doorTypeToID.get(doorType);
        return info != null && info.status;
    }

    private String a(final @NotNull DoorType doorType)
    {
        return "(" + doorType.getSimpleName() + ") ";
    }

    private @NotNull LoadResult dependenciesSatisfied(final @NotNull DoorType doorType,
                                                      final @NotNull HashMap<String, Pair<DoorType, DependencyCheckStatus>> registerQueue)
    {
        final @Nullable Pair<DoorType, DependencyCheckStatus> currentStatus =
            registerQueue.get(doorType.getSimpleName());
        if (currentStatus != null && currentStatus.second != DependencyCheckStatus.NOT_CHECKED)
        {
            return currentStatus.second == DependencyCheckStatus.OK ? new LoadResult(LoadResultType.SUCCESS, "") :
                   new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE, "");
        }

        for (final @NotNull Pair<String, Pair<Integer, Integer>> dependency : doorType.getDependencies())
        {
            // First check if it's already been registered or not.
            @NotNull Optional<DoorType> dependencyDoorType = getDoorType(dependency.first);


            // If it hasn't been registered yet, check if it exists in the registerQueue.
            // If the dependency will be installed in the future, then that's fine.
            // However, before we just use any old DoorType we find that will be registered in the future, we'll
            // have to make sure that that dependency's dependencies are also met.
            if (!dependencyDoorType.isPresent() && registerQueue.containsKey(dependency.first))
            {
                final @NotNull Pair<DoorType, DependencyCheckStatus> queuedDoorType = registerQueue
                    .get(dependency.first);

                @NotNull DependencyCheckStatus dependencyCheckStatus = queuedDoorType.second;
                final @NotNull DoorType queuedDoor = queuedDoorType.first;

                // If it hasn't been checked, recursively check if this dependency's dependencies are satisfied.
                if (dependencyCheckStatus == DependencyCheckStatus.NOT_CHECKED)
                {
                    final @NotNull LoadResult dependencyLoadResult = dependenciesSatisfied(queuedDoor, registerQueue);
                    if (dependencyLoadResult.getLoadResultType() == LoadResultType.SUCCESS)
                    {
                        registerQueue.replace(dependency.first, new Pair<>(queuedDoor, DependencyCheckStatus.OK));
                        dependencyCheckStatus = DependencyCheckStatus.OK;
                    }
                    else
                        registerQueue.replace(dependency.first, new Pair<>(queuedDoor, DependencyCheckStatus.NOT_OK));
                }

                if (dependencyCheckStatus == DependencyCheckStatus.OK)
                    dependencyDoorType = Optional.of(queuedDoor);
            }


            if (!dependencyDoorType.isPresent())
                return new LoadResult(LoadResultType.DEPENDENCY_UNAVAILABLE,
                                      "Type \"" + doorType.getSimpleName() + "\" depends on type: \"" +
                                          dependency.first + "\" which isn't available!");

            if (dependencyDoorType.get().getTypeVersion() < dependency.second.first ||
                dependencyDoorType.get().getTypeVersion() > dependency.second.second)
                return new LoadResult(LoadResultType.DEPENDENCY_UNSUPPORTED_VERSION,
                                      "Version " + doorType.getTypeVersion() + " of type: \"" +
                                          doorType.getSimpleName() +
                                          "\" requires " + dependency.second.first + ">= version <= " +
                                          dependency.second.second + " of type: \"" +
                                          dependencyDoorType.get().getSimpleName() + "\", but version " +
                                          dependencyDoorType.get().getTypeVersion() + " was found!");
        }

        if (registerQueue.containsKey(doorType.getSimpleName()))
            registerQueue.replace(doorType.getSimpleName(), new Pair<>(doorType, DependencyCheckStatus.OK));
        return new LoadResult(LoadResultType.SUCCESS, "");
    }

    private @NotNull LoadResult dependenciesSatisfied(final @NotNull DoorType doorType)
    {
        return dependenciesSatisfied(doorType, new HashMap<>(0));
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType The {@link DoorType} to register.
     * @return True if registration was successful.
     */
    public @NotNull CompletableFuture<OptionalLong> registerDoorType(final @NotNull DoorType doorType)
    {
        return registerDoorType(doorType, true);
    }

    /**
     * Unregisters a door-type. Note that it does <b>NOT</b> remove it or its doors from the database and that after
     * unregistering it, that won't be possible anymore either.
     * <p>
     * Once unregistered, this type will be completely disabled and doors of this type cannot be used for anything.
     *
     * @param doorType The type to unregister.
     */
    public void unregisterDoorType(final @NotNull DoorType doorType)
    {
        final @Nullable DoorTypeInfo doorTypeInfo = doorTypeToID.remove(doorType);
        if (doorTypeInfo == null)
        {
            PLogger.get().warn("Trying to unregister door of type: " + doorType.getSimpleName() + ", but it isn't " +
                                   "registered already!");
            return;
        }
        doorTypeFromID.remove(doorTypeInfo.id);
        doorTypeFromName.remove(doorTypeInfo.name);
        sortedDoorTypes.remove(doorType);
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType  The {@link DoorType} to register.
     * @param isEnabled Whether or not this {@link DoorType} should be enabled or not. Default = true.
     * @return True if registration was successful.
     */
    public @NotNull CompletableFuture<OptionalLong> registerDoorType(final @NotNull DoorType doorType,
                                                                     final boolean isEnabled)
    {
        final @NotNull LoadResult loadResult = dependenciesSatisfied(doorType);
        if (loadResult.getLoadResultType() != LoadResultType.SUCCESS)
        {
            PLogger.get().severe(loadResult.getMessage());
            return CompletableFuture.completedFuture(OptionalLong.empty());
        }

        return registerDoorTypeDirectly(doorType, isEnabled);
    }

    /**
     * Registers a {@link DoorType} without checking its dependencies
     *
     * @param doorType  The {@link DoorType} to register.
     * @param isEnabled Whether or not this {@link DoorType} should be enabled or not. Default = true.
     * @return True if registration was successful.
     */
    private @NotNull CompletableFuture<OptionalLong> registerDoorTypeDirectly(final @NotNull DoorType doorType,
                                                                              final boolean isEnabled)
    {
        PLogger.get().info("Registering door type: " + doorType.toString());
        CompletableFuture<Long> registrationResult = BigDoors.get().getDatabaseManager().registerDoorType(doorType);
        return registrationResult.handle(
            (doorTypeID, throwable) ->
            {
                if (doorTypeID < 1)
                    return OptionalLong.empty();
                doorTypeToID.put(doorType, new DoorTypeInfo(doorTypeID, isEnabled, doorType.getSimpleName()));
                doorTypeFromID.put(doorTypeID, doorType);
                doorTypeFromName.put(doorType.getSimpleName(), doorType);
                if (isEnabled)
                    sortedDoorTypes.add(doorType);
                return OptionalLong.of(doorTypeID);
            });
    }

    /**
     * Changes the status of a {@link DoorType}. If disabled, this type cannot be toggled or created.
     *
     * @param doorType  The {@link DoorType} to enabled or disable.
     * @param isEnabled True to enabled this {@link DoorType} (default), or false to disable it.
     */
    public void setDoorTypeEnabled(final @NotNull DoorType doorType, final boolean isEnabled)
    {
        final @Nullable DoorTypeInfo info = doorTypeToID.get(doorType);
        if (info != null)
        {
            info.status = isEnabled;
            if (!isEnabled)
                sortedDoorTypes.remove(doorType);
        }
    }

    @SneakyThrows
    public void registerDoorTypes(final @NotNull List<DoorType> doorTypes)
    {
//        PLogger.get().severe("Loading door types!");
        final @NotNull HashMap<String, Pair<DoorType, DependencyCheckStatus>> registrationQueue =
            new HashMap<>(doorTypes.size());

//        PLogger.get().severe("Adding door types to registration queue!");
        doorTypes.forEach(doorType -> registrationQueue.put(doorType.getSimpleName(),
                                                            new Pair<>(doorType, DependencyCheckStatus.NOT_CHECKED)));

//        Thread.sleep(50L);
//        {
//            StringBuilder sb = new StringBuilder();
//            registrationQueue.forEach((name, pair) -> sb.append(name).append(", "));
//            PLogger.get().severe("RegistrationQueue: " + sb.toString());
//        }
//        Thread.sleep(50L);

//        PLogger.get().severe("Processing registration queue now!");
        registrationQueue.forEach((name, pair) -> dependenciesSatisfied(pair.first, registrationQueue));
//        PLogger.get().severe("Processing registration queue FINISHED!");

//        Thread.sleep(50L);
//        {
//            StringBuilder sb = new StringBuilder();
//            sb.append("Results: \n");
//            registrationQueue
//                .forEach((name, pair) -> sb.append(name).append(": ").append(pair.second.name()).append("\n"));
//            PLogger.get().severe(sb.toString());
//        }
//        Thread.sleep(50L);

//        PLogger.get().severe("Registering registration queue!");
        registrationQueue.forEach(
            (name, pair) ->
            {
                if (pair.second == DependencyCheckStatus.OK)
                {
//                    PLogger.get().severe("Registering type: " + name);
                    registerDoorTypeDirectly(pair.first, true);
                }
                else if (pair.second == DependencyCheckStatus.NOT_OK)
                    PLogger.get().severe("FAILED TO LOAD DOORTYPE: " + name);
//                else
//                    PLogger.get().severe("WTF? Type: " + name);
            });
//        Thread.sleep(50L);
    }

    /**
     * Describes the ID value and the enabled status of a {@link DoorType}.
     *
     * @author Pim
     */
    @Value
    private static class DoorTypeInfo
    {
        long id;
        @NonFinal
        boolean status;
        String name;
    }

    @Value
    private static class LoadResult
    {
        LoadResultType loadResultType;
        String message;
    }

    private enum LoadResultType
    {
        DEPENDENCY_UNSUPPORTED_VERSION,
        DEPENDENCY_UNAVAILABLE,
        INVALID_DOOR_TYPE,
        SUCCESS
    }

    private enum DependencyCheckStatus
    {
        NOT_CHECKED,
        OK,
        NOT_OK
    }
}
