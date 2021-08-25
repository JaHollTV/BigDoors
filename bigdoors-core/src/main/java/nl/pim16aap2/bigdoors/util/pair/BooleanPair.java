package nl.pim16aap2.bigdoors.util.pair;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a pair of booleans. This is a specialized version of {@link Pair} that avoids boxing.
 *
 * @author Pim
 */
@EqualsAndHashCode
@AllArgsConstructor
@ToString
// SonarLint doesn't like non-final public member variables (S1104).
// However, that's the point of this class and there's no reason to do it in another way.
@SuppressWarnings("squid:S1104")
// CHECKSTYLE:OFF
public class BooleanPair
{
    public boolean first;
    public boolean second;
}
