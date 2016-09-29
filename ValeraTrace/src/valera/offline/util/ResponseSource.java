package valera.offline.util;


/**
 * Where the HTTP client should look for a response.
 *
 * @hide
 */
public enum ResponseSource {

    /**
     * Return the response from the cache immediately.
     */
    CACHE,

    /**
     * Make a conditional request to the host, returning the cache response if
     * the cache is valid and the network response otherwise.
     */
    CONDITIONAL_CACHE,

    /**
     * Return the response from the network.
     */
    NETWORK;

    public boolean requiresConnection() {
        return this == CONDITIONAL_CACHE || this == NETWORK;
    }
}
