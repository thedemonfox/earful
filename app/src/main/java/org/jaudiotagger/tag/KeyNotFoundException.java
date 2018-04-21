package org.jaudiotagger.tag;

/**
 * Thrown if the key cannot be found
 * <p/>
 * <p>Shoudl not happen with well written code, hence RuntimeException.
 */
public class KeyNotFoundException extends RuntimeException
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new KeyNotFoundException datatype.
     */
    public KeyNotFoundException()
    {
    }

    /**
     * Creates a new KeyNotFoundException datatype.
     *
     * @param ex the cause.
     */
    public KeyNotFoundException(Throwable ex)
    {
        super(ex);
    }

    /**
     * Creates a new KeyNotFoundException datatype.
     *
     * @param msg the detail message.
     */
    public KeyNotFoundException(String msg)
    {
        super(msg);
    }

    /**
     * Creates a new KeyNotFoundException datatype.
     *
     * @param msg the detail message.
     * @param ex  the cause.
     */
    public KeyNotFoundException(String msg, Throwable ex)
    {
        super(msg, ex);
    }
}
