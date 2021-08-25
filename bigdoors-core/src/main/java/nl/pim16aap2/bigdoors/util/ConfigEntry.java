package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.api.IConfigReader;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents an option in a config file.
 *
 * @author Pim
 */
public final class ConfigEntry<V>
{
    private final IPLogger logger;
    private final IConfigReader config;
    private final String optionName;
    private final V defaultValue;
    private final String @Nullable [] comment;
    private final @Nullable ConfigEntry.ITestValue<V> verifyValue;
    private V value;

    /**
     * ConfigEntry Constructor.
     *
     * @param logger
     *     The logger to use for exception reporting.
     * @param config
     *     The config file to read from.
     * @param optionName
     *     The name of this option as used in the config file.
     * @param defaultValue
     *     The default value of this option.
     * @param comment
     *     The comment that will precede this option.
     * @param verifyValue
     *     Function to use to verify the validity of a value and change it if necessary.
     */
    public ConfigEntry(IPLogger logger, IConfigReader config, String optionName, V defaultValue,
                       String @Nullable [] comment, @Nullable ConfigEntry.ITestValue<V> verifyValue)
    {
        this.logger = logger;
        this.config = config;
        this.optionName = optionName;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.verifyValue = verifyValue;
        setValue();
    }

    /**
     * ConfigEntry Constructor.
     *
     * @param logger
     *     The logger to use for exception reporting.
     * @param config
     *     The config file to read from.
     * @param optionName
     *     The name of this option as used in the config file.
     * @param defaultValue
     *     The default value of this option.
     * @param comment
     *     The comment that will precede this option.
     */
    public ConfigEntry(IPLogger logger, IConfigReader config, String optionName, V defaultValue,
                       String @Nullable [] comment)
    {
        this(logger, config, optionName, defaultValue, comment, null);
    }

    /**
     * Read the value of this config option from the config. If it fails, the  default value is used instead. If it is
     * available, the {@link #verifyValue} method is used to modify the value if it is invalid.
     */
    @SuppressWarnings("unchecked")
    private void setValue()
    {
        try
        {
            value = (V) config.get(optionName, defaultValue);
        }
        catch (Exception e)
        {
            logger.logThrowable(e,
                                "Failed to read config value of: \"" + optionName + "\"! Using default value instead!");
            value = defaultValue;
        }
        if (verifyValue != null)
            value = verifyValue.test(value);
    }

    /**
     * Get the value of this config option.
     *
     * @return The value of the config option.
     */
    public V getValue()
    {
        return value;
    }

    /**
     * Get the comment of this config option.
     *
     * @return The comment of the config option.
     */
    public String @Nullable [] getComment()
    {
        return comment;
    }

    /**
     * Convert the comment, name and value(s) of this config option into a string that can be used for writing the
     * config.
     *
     * @return The config option formatted for printing in the config file
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        // Print the comments, if there are any.
        if (comment != null)
            for (String comLine : comment)
                // Prefix every line by a comment-sign (#).
                sb.append("# ").append(comLine).append("\n");

        sb.append(optionName).append(": ");
        if (value.getClass().isAssignableFrom(String.class))
            sb.append("'").append(value).append("'");
        else if (value instanceof List<?>)
        {
            sb.append("\n");
            int listSize = ((List<?>) value).size();
            for (int index = 0; index < listSize; ++index)
                // Don't print newline at the end
                sb.append("  - ").append(((List<?>) value).get(index)).append(index == listSize - 1 ? "" : "\n");
        }
        else
            sb.append(value);
        return sb.toString();
    }

    /**
     * Interface that can be used to verify config values.
     *
     * @param <T>
     *     The type of the value.
     * @author Pim
     */
    public interface ITestValue<T>
    {
        /**
         * Checks if a given value is valid. If it is, it returns that value. If it isn't, it is changed so that it is
         * valid.
         * <p>
         * For example to check if a value doesn't exceed a certain threshold.
         *
         * @param value
         *     The value to check.
         * @return The value if it was valid, otherwise the value made valid.
         */
        T test(T value);
    }
}
