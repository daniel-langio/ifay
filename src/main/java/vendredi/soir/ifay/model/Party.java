package vendredi.soir.ifay.model;

/**
 * A sender or receiver, identified by phone number (E.164, e.g. {@code +261341234567}). Looked
 * up or created by phone number, so the same phone number reused across payments always resolves
 * to the same {@code Party} - this is what makes it an entity rather than a formatted string.
 */
public record Party(String id, String phoneNumber) {}
